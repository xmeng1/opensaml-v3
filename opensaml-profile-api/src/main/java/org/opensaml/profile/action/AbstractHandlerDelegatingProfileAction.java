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

package org.opensaml.profile.action;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.messaging.handler.MessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.ParentProfileRequestContextLookup;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Base class for a profile action which just delegates to an instance of {@link MessageHandler}.
 * 
 * @param <DelegateType> type of MessageHandler to which to delegate.
 */
public abstract class AbstractHandlerDelegatingProfileAction<DelegateType extends MessageHandler> 
        extends AbstractConditionalProfileAction {
    
    /** Lookup function for parent ProfileRequestContext. */
    private static final ParentProfileRequestContextLookup PRC_LOOKUP = new ParentProfileRequestContextLookup();
    
    /** The message handler delegate. */
    @Nonnull private DelegateType delegate;
    
    /** Lookup function for the message context on which to operate. */
    @Nonnull private ContextDataLookupFunction<ProfileRequestContext, MessageContext> messageContextLookup;
    
    /**
     * Constructor.
     *
     * @param delegateClass the delegate class. Must have a no-argument constructor. For those that do not,
     *          instead pass in a pre-constructed instance via 
     *          {@link AbstractHandlerDelegatingProfileAction(MessageHandler, ContextDataLookupFunction)}.
     * @param lookup the lookup function for the message context on which to operate, 
     *          typically for either the inbound or outbound context
     */
    public AbstractHandlerDelegatingProfileAction(@Nonnull final Class<DelegateType> delegateClass, 
            @Nonnull final ContextDataLookupFunction<ProfileRequestContext, MessageContext> lookup) {
        Constraint.isNotNull(delegateClass, "Delegate class may not be null");
        try {
            delegate = delegateClass.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        
        messageContextLookup = Constraint.isNotNull(lookup, "MessageContext lookup function may not be null");
    }
    
    /**
     * Constructor.
     *
     * @param delegateInstance the delegate instance
     * @param lookup the lookup function for the message context on which to operate, 
     *          typically for either the inbound or outbound context
     */
    public AbstractHandlerDelegatingProfileAction(@Nonnull final DelegateType delegateInstance, 
            @Nonnull final ContextDataLookupFunction<ProfileRequestContext, MessageContext> lookup) {
        delegate = Constraint.isNotNull(delegateInstance, "Delegate instance may not be null");
        messageContextLookup = Constraint.isNotNull(lookup, "MessageContext lookup function may not be null");
    }
    
    /** {@inheritDoc} */
    protected void doDestroy() {
        super.doDestroy();
        if (delegate != null && delegate instanceof DestructableComponent) {
            ((DestructableComponent) delegate).destroy();
        }
    }
    
    /**
     * Get the delegate instance.
     * 
     * @return the delegate instance
     */
    @Nonnull protected DelegateType getDelegate() {
        return delegate;
    }
    
    /**
     * Adapt a {@link ProfileRequestContext} predicate into a {@link MessageContext} predicate via composing
     * with a lookup function.
     * 
     * @param predicate the profile request context predicate
     * @return the message context predicate
     */
    @Nullable protected Predicate<MessageContext> adapt(@Nullable final Predicate<ProfileRequestContext> predicate) {
        if (predicate == null) {
            return null;
        } else {
            return Predicates.compose(predicate, PRC_LOOKUP);
        }
    }

    /**
     * Adapt a {@link ProfileRequestContext} function to a {@link MessageContext} function via composing
     * with a lookup function.
     * 
     * @param function the profile request context function
     * @return the message context function
     * 
     * @param <T> the output type of the functions
     */
    @Nullable protected <T> Function<MessageContext, T> adapt(
            @Nullable final Function<ProfileRequestContext, T> function) {
        if (function == null) {
            return null;
        } else {
            return Functions.compose(function, PRC_LOOKUP);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        final MessageContext messageContext = messageContextLookup.apply(profileRequestContext);
        if (messageContext == null) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return;
        }
        
        try {
            delegate.invoke(messageContext);
            ActionSupport.buildProceedEvent(profileRequestContext);
        } catch (final MessageHandlerException e) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.MESSAGE_PROC_ERROR);
        }
    }

}
