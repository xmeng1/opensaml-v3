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

package org.opensaml.saml.metadata.resolver.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.joda.time.DateTime;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.RefreshableMetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * A {@link MetadataResolver} implementation that answers requests by composing the answers of child
 * {@link MetadataResolver}s.
 */
public class CompositeMetadataResolver extends AbstractIdentifiedInitializableComponent implements MetadataResolver,
        RefreshableMetadataResolver {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CompositeMetadataResolver.class);

    /** Resolvers composed by this resolver. */
    @Nonnull @NonnullElements private List<MetadataResolver> resolvers;

    /** Constructor. */
    public CompositeMetadataResolver() {
        resolvers = Collections.emptyList();
    }

    /**
     * Gets an immutable the list of currently registered resolvers.
     * 
     * @return list of currently registered resolvers
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public List<MetadataResolver> getResolvers() {
        return ImmutableList.copyOf(resolvers);
    }

    /**
     * Sets the current set of metadata resolvers.
     * 
     * @param newResolvers the metadata resolvers to use
     * 
     * @throws ResolverException thrown if there is a problem adding the metadata provider
     */
    public void setResolvers(@Nonnull @NonnullElements final List<MetadataResolver> newResolvers) 
            throws ResolverException {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        if (newResolvers == null || newResolvers.isEmpty()) {
            resolvers = Collections.emptyList();
            return;
        }

        resolvers = new ArrayList<>(Collections2.filter(newResolvers, Predicates.notNull()));
    }

    /** {@inheritDoc} */
    @Override public boolean isRequireValidMetadata() {
        log.warn("Attempt to access unsupported requireValidMetadata property on ChainingMetadataResolver");
        return false;
    }

    /** {@inheritDoc} */
    @Override public void setRequireValidMetadata(final boolean requireValidMetadata) {
        throw new UnsupportedOperationException("Setting require valid metadata is not supported on chaining resolver");
    }

    /** {@inheritDoc} */
    @Override @Nullable public MetadataFilter getMetadataFilter() {
        log.warn("Attempt to access unsupported MetadataFilter property on ChainingMetadataResolver");
        return null;
    }

    /** {@inheritDoc} */
    @Override public void setMetadataFilter(@Nullable final MetadataFilter newFilter) {
        throw new UnsupportedOperationException("Metadata filters are not supported on ChainingMetadataProviders");
    }

    /** {@inheritDoc} */
    @Override public Iterable<EntityDescriptor> resolve(@Nullable final CriteriaSet criteria) throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        return new CompositeMetadataResolverIterable(resolvers, criteria);
    }

    /** {@inheritDoc} */
    @Override public EntityDescriptor resolveSingle(@Nullable final CriteriaSet criteria) throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        EntityDescriptor metadata = null;
        for (final MetadataResolver resolver : resolvers) {
            metadata = resolver.resolveSingle(criteria);
            if (metadata != null) {
                return metadata;
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (resolvers == null) {
            log.warn("CompositeMetadataResolver was not configured with any member MetadataResolvers");
            resolvers = Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    @Override protected void doDestroy() {
        super.doDestroy();

        resolvers = Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override public void refresh() throws ResolverException {
        for (final MetadataResolver resolver : resolvers) {
            if (resolver instanceof RefreshableMetadataResolver) {
                ((RefreshableMetadataResolver) resolver).refresh();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public DateTime getLastUpdate() {
        DateTime ret = null;
        for (final MetadataResolver resolver : resolvers) {
            if (resolver instanceof RefreshableMetadataResolver) {
                final DateTime lastUpdate = ((RefreshableMetadataResolver) resolver).getLastUpdate();
                if (ret == null || ret.isBefore(lastUpdate)) {
                    ret = lastUpdate;
                }
            }
        }
        
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public DateTime getLastRefresh() {
        DateTime ret = null;
        for (final MetadataResolver resolver : resolvers) {
            if (resolver instanceof RefreshableMetadataResolver) {
                final DateTime lastRefresh = ((RefreshableMetadataResolver) resolver).getLastRefresh();
                if (ret == null || ret.isBefore(lastRefresh)) {
                    ret = lastRefresh;
                }
            }
        }
        
        return ret;
    }

    /**
     * {@link Iterable} implementation that provides an {@link Iterator} that lazily iterates over each composed
     * resolver.
     */
    private static class CompositeMetadataResolverIterable implements Iterable<EntityDescriptor> {

        /** Class logger. */
        private final Logger log = LoggerFactory.getLogger(CompositeMetadataResolverIterable.class);

        /** Resolvers over which to iterate. */
        private final List<MetadataResolver> resolvers;

        /** Criteria being search for. */
        private final CriteriaSet criteria;

        /**
         * Constructor.
         * 
         * @param composedResolvers resolvers from which results will be pulled
         * @param metadataCritiera criteria for the resolver query
         */
        public CompositeMetadataResolverIterable(final List<MetadataResolver> composedResolvers,
                final CriteriaSet metadataCritiera) {
            resolvers =
                    ImmutableList.<MetadataResolver> builder()
                            .addAll(Iterables.filter(composedResolvers, Predicates.notNull())).build();

            criteria = metadataCritiera;
        }

        /** {@inheritDoc} */
        @Override public Iterator<EntityDescriptor> iterator() {
            return new CompositeMetadataResolverIterator();
        }

        /** {@link Iterator} implementation that lazily iterates over each composed resolver. */
        private class CompositeMetadataResolverIterator implements Iterator<EntityDescriptor> {

            /** Iterator over the composed resolvers. */
            private Iterator<MetadataResolver> resolverIterator;

            /** Current resolver from which we are getting results. */
            private MetadataResolver currentResolver;

            /** Iterator over the results of the current resolver. */
            private Iterator<EntityDescriptor> currentResolverMetadataIterator;

            /** Constructor. */
            public CompositeMetadataResolverIterator() {
                resolverIterator = resolvers.iterator();
            }

            /** {@inheritDoc} */
            @Override public boolean hasNext() {
                if (!currentResolverMetadataIterator.hasNext()) {
                    proceedToNextResolverIterator();
                }

                return currentResolverMetadataIterator.hasNext();
            }

            /** {@inheritDoc} */
            @Override public EntityDescriptor next() {
                if (!currentResolverMetadataIterator.hasNext()) {
                    proceedToNextResolverIterator();
                }

                return currentResolverMetadataIterator.next();
            }

            /** {@inheritDoc} */
            @Override public void remove() {
                throw new UnsupportedOperationException();
            }

            /**
             * Proceed to the next composed resolvers that has a response to the resolution query.
             */
            private void proceedToNextResolverIterator() {
                try {
                    while (resolverIterator.hasNext()) {
                        currentResolver = resolverIterator.next();
                        currentResolverMetadataIterator = currentResolver.resolve(criteria).iterator();
                        if (currentResolverMetadataIterator.hasNext()) {
                            return;
                        }
                    }
                } catch (final ResolverException e) {
                    log.debug("Error encountered attempting to fetch results from resolver", e);
                }
            }
        }
    }
}