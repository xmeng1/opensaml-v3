/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.messaging.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.ClassIndexedSet;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.MessageRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of a component which represents the context used to store state 
 * used for purposes related to messaging.
 * 
 * <p>
 * Specific implementations of contexts would normally add additional properties to the
 * context to represent the state that is to be stored by that particular context implementation.
 * </p>
 * 
 * <p>
 * A context may also function as a container of subcontexts.
 * Access to subcontexts is class-based.  The parent context may hold only
 * one instance of a given class at a given time.  This class-based indexing approach
 * is used to enforce type-safety over the subcontext instances returned from the parent context,
 * and avoids the need for casting.
 * </p>
 * 
 * <p>
 * When a subcontext is requested and it does not exist in the parent context, it may optionally be
 * auto-created.  In order to be auto-created in this manner, the subcontext type
 * <strong>MUST</strong> have a no-arg constructor. If the requested subcontext does not conform 
 * to this convention, auto-creation will fail.
 * </p>
 */
public abstract class BaseContext implements Iterable<BaseContext> {

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseContext.class);
    
    /** The owning parent context. */
    @Nullable private BaseContext parent;

    /** The subcontexts being managed. */
    @Nonnull @NonnullElements private ClassIndexedSet<BaseContext> subcontexts;
    
    /** Flag indicating whether subcontexts should, by default, be created if they do not exist. */
    private boolean autoCreateSubcontexts;
    
    /** Constructor. Generates a random context id. */
    public BaseContext() {
        subcontexts = new ClassIndexedSet<>();
        
        setAutoCreateSubcontexts(false);
    }
    
    /**
     * Get the parent context, if there is one.
     * 
     * @return the parent context or null 
     */
    @Nullable public BaseContext getParent() {
        return parent;
    }
    
    /**
     * Set the context parent. 
     * 
     * @param newParent the new context parent
     */
    protected void setParent(@Nullable final BaseContext newParent) {
        parent = newParent;
    }
    
    /**
     * Get a subcontext of the current context.
     * 
     * @param <T> the type of subcontext being operated on
     * @param clazz the class type to obtain
     * @return the held instance of the class, or null
     */
    @Nullable public <T extends BaseContext> T getSubcontext(@Nonnull final Class<T> clazz) {
        return getSubcontext(clazz, isAutoCreateSubcontexts());
    }
    
    /**
     * Get a subcontext of the current context.
     * 
     * @param <T> the type of subcontext being operated on
     * @param clazz the class type to obtain
     * @param autocreate flag indicating whether the subcontext instance should be auto-created
     * @return the held instance of the class, or null
     */ 
    @Nullable public <T extends BaseContext> T getSubcontext(@Nonnull final Class<T> clazz, final boolean autocreate) {
        Constraint.isNotNull(clazz, "Class type cannot be null");
        
        log.trace("Request for subcontext of type: {}", clazz.getName());
        T subcontext = subcontexts.get(clazz);
        if (subcontext != null) {
            log.trace("Subcontext found of type: {}", clazz.getName());
            return subcontext;
        }
        
        if (autocreate) {
            log.trace("Subcontext not found of type, autocreating: {}", clazz.getName());
            subcontext = createSubcontext(clazz);
            addSubcontext(subcontext);
            return subcontext;
        }
        
        log.trace("Subcontext not found of type: {}", clazz.getName());
        return null;
    }

    /**
     * Get a subcontext of the current context.
     * 
     * @param className the name of the class type to obtain
     * @return the held instance of the class, or null
     * @throws ClassNotFoundException 
     */ 
    @Nullable public BaseContext getSubcontext(@Nonnull @NotEmpty final String className)
            throws ClassNotFoundException {
        return getSubcontext(className, isAutoCreateSubcontexts());
    }
    
    /**
     * Get a subcontext of the current context.
     * 
     * @param className the name of the class type to obtain
     * @param autocreate flag indicating whether the subcontext instance should be auto-created
     * @return the held instance of the class, or null
     * @throws ClassNotFoundException 
     */ 
    @Nullable public BaseContext getSubcontext(@Nonnull @NotEmpty final String className, final boolean autocreate)
            throws ClassNotFoundException {
        return getSubcontext(Class.forName(className).asSubclass(BaseContext.class), autocreate);
    }
    
    /**
     * Add a subcontext to the current context.
     * 
     * @param subContext the subcontext to add
     * 
     * @return the context added
     */
    @Nonnull public BaseContext addSubcontext(@Nonnull final BaseContext subContext) {
        return addSubcontext(subContext, false);
    }
    
    /**
     * Add a subcontext to the current context.
     * 
     * @param subcontext the subcontext to add
     * @param replace flag indicating whether to replace the existing instance of the subcontext if present
     * 
     * @return the context added
     */
    @Nonnull public BaseContext addSubcontext(@Nonnull final BaseContext subcontext, final boolean replace) {
        Constraint.isNotNull(subcontext, "Subcontext cannot be null");
        
        final BaseContext existing = subcontexts.get(subcontext.getClass());
        if (existing == subcontext) {
            log.trace("Subcontext to add is already a child of the current context, skipping");
            return subcontext;
        }
        
        // Note: This will throw if replace == false and existing != null.
        // In that case, no link management happens, which is what we want, to leave things in a consistent state.
        log.trace("Attempting to store a subcontext with type '{}' with replace option '{}'", 
                new Object[]{subcontext.getClass().getName(), new Boolean(replace).toString()});
        subcontexts.add(subcontext, replace);
        
        // Manage parent/child links
        
        // If subcontext was formerly a child of another parent, remove that link
        final BaseContext oldParent = subcontext.getParent();
        if (oldParent != null && oldParent != this) {
            log.trace("New subcontext with type '{}' is currently a subcontext of "
                    + "parent with type '{}', removing it",
                    new Object[]{subcontext.getClass().getName(), oldParent.getClass().getName(),});
            subcontext.getParent().removeSubcontext(subcontext);
        }
        
        // Set parent pointer of new subcontext to this instance
        log.trace("New subcontext with type '{}' set to have parent with type '{}'",
                new Object[]{subcontext.getClass().getName(), getClass().getName(),});
        subcontext.setParent(this);
        
        // If we're replacing an existing subcontext (if class was a duplicate, will only get here if replace == true),
        // then clear out its parent pointer.
        if (existing != null) {
            log.trace("Old subcontext with type '{}' will have parent cleared", existing.getClass().getName());
            existing.setParent(null);
        }
        
        return subcontext;
    }
    
    /**
     * Remove a subcontext from the current context.
     * 
     * @param subcontext the subcontext to remove
     */
    public void removeSubcontext(@Nonnull final BaseContext subcontext) {
        Constraint.isNotNull(subcontext, "Subcontext cannot be null");
        
        log.trace("Removing subcontext with type '{}' from parent with type '{}'",
                new Object[]{subcontext.getClass().getName(), getClass().getName()});
        subcontext.setParent(null);
        subcontexts.remove(subcontext);
    }
    
    /**
     * Remove the subcontext from the current context which corresponds to the supplied class.
     * 
     * @param <T> the type of subcontext being operated on
     * @param clazz the subcontext class to remove
     */
    public <T extends BaseContext>void removeSubcontext(@Nonnull final Class<T> clazz) {
        final BaseContext subcontext = getSubcontext(clazz, false);
        if (subcontext != null) {
            removeSubcontext(subcontext);
        }
    }
    
    /**
     * Return whether the current context currently contains an instance of
     * the specified subcontext class.
     * 
     * @param <T> the type of subcontext being operated on
     * @param clazz the class to check
     * @return true if the current context contains an instance of the class, false otherwise
     */
    public <T extends BaseContext> boolean containsSubcontext(@Nonnull final Class<T> clazz) {
        Constraint.isNotNull(clazz, "Class type cannot be null");
        
        return subcontexts.contains(clazz);
    }
    
    /**
     * Clear the subcontexts of the current context.
     */
    public void clearSubcontexts() {
        log.trace("Clearing all subcontexts from context with type '{}'", this.getClass().getName());
        for (BaseContext subcontext : subcontexts) {
            subcontext.setParent(null);
        }
        subcontexts.clear();
    }
    
    /**
     * Get whether the context auto-creates subcontexts by default.
     * 
     * @return true if the context auto-creates subcontexts, false otherwise
     */
    public boolean isAutoCreateSubcontexts() {
        return autoCreateSubcontexts;
    }
    
    /**
     * Set whether the context auto-creates subcontexts by default.
     * 
     * @param autoCreate whether the context should auto-create subcontexts
     */
    public void setAutoCreateSubcontexts(final boolean autoCreate) {
        autoCreateSubcontexts = autoCreate;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull public Iterator<BaseContext> iterator() {
        return new ContextSetNoRemoveIteratorDecorator(subcontexts.iterator());
    }
    
    /**
     * Create an instance of the specified subcontext class.
     * 
     * @param <T> the type of subcontext
     * @param clazz the class of the subcontext instance to create
     * @return the new subcontext instance
     */
    @Nonnull protected <T extends BaseContext> T createSubcontext(@Nonnull final Class<T> clazz) {
        Constructor<T> constructor;
        try {
            constructor = clazz.getConstructor();
            return constructor.newInstance();
        } catch (final SecurityException e) {
            log.error("Security error on creating subcontext", e);
            throw new MessageRuntimeException("Error creating subcontext", e);
        } catch (final NoSuchMethodException e) {
            log.error("No such method error on creating subcontext", e);
            throw new MessageRuntimeException("Error creating subcontext", e);
        } catch (final IllegalArgumentException e) {
            log.error("Illegal argument error on creating subcontext", e);
            throw new MessageRuntimeException("Error creating subcontext", e);
        } catch (final InstantiationException e) {
            log.error("Instantiation error on creating subcontext", e);
            throw new MessageRuntimeException("Error creating subcontext", e);
        } catch (final IllegalAccessException e) {
            log.error("Illegal access error on creating subcontext", e);
            throw new MessageRuntimeException("Error creating subcontext", e);
        } catch (final InvocationTargetException e) {
            log.error("Invocation target error on creating subcontext", e);
            throw new MessageRuntimeException("Error creating subcontext", e);
        }
    }
    
    /**
     * Iterator decorator which disallows the remove() operation on the iterator.
     */
    protected class ContextSetNoRemoveIteratorDecorator implements Iterator<BaseContext> {
        
        /** The decorated iterator. */
        private Iterator<BaseContext> wrappedIterator;
        
        /**
         * Constructor.
         *
         * @param iterator the iterator instance to decorator
         */
        protected ContextSetNoRemoveIteratorDecorator(Iterator<BaseContext> iterator) {
            wrappedIterator = iterator;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return wrappedIterator.hasNext();
        }

        /** {@inheritDoc} */
        @Override
        public BaseContext next() {
            return wrappedIterator.next();
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Removal of subcontexts via the iterator is unsupported");
        }
        
    }
    
}
