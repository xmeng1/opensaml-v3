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
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.criterion.SatisfyAnyCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.metadata.criteria.role.EvaluableRoleDescriptorCriterion;
import org.opensaml.saml.metadata.criteria.role.impl.RoleDescriptorCriterionPredicateRegistry;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.RoleDescriptorResolver;
import org.opensaml.saml.saml2.common.IsTimeboundSAMLObjectValidPredicate;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.CriterionPredicateRegistry;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.resolver.ResolverSupport;

/**
 * Implementation of {@link RoleDescriptorResolver} which wraps an instance of {@link MetadataResolver} to
 * support basic EntityDescriptor resolution, and then performs further role-related filtering over the
 * returned EntityDescriptor.
 * 
 * <p>
 * This implementation passes the input {@link CriteriaSet} through to the wrapped metadata resolver as-is.
 * </p>
 * 
 * <p>
 * This implementation also supports applying arbitrary predicates to the returned role descriptors, either passed
 * directly as instances of {@link EvaluableRoleDescriptorCriterion} in the criteria, or resolved dynamically
 * from other criteria via an instance of {@link CriterionPredicateRegistry}.
 * </p>
 */
public class PredicateRoleDescriptorResolver extends AbstractIdentifiedInitializableComponent 
        implements RoleDescriptorResolver {
    
    /** Predicate for evaluating whether a TimeboundSAMLObject is valid. */
    private static final Predicate<XMLObject> IS_VALID_PREDICATE = new IsTimeboundSAMLObjectValidPredicate();
    
    /** Logger. */
    private Logger log = LoggerFactory.getLogger(PredicateRoleDescriptorResolver.class);
    
    /** Whether metadata is required to be valid. */
    private boolean requireValidMetadata;
    
    /** Resolver of EntityDescriptors. */
    private MetadataResolver entityDescriptorResolver;
    
    /** Flag which determines whether predicates used in filtering are connected by 
     * a logical 'OR' (true) or by logical 'AND' (false). Defaults to false. */
    private boolean satisfyAnyPredicates;
    
    /** Registry used in resolving predicates from criteria. */
    private CriterionPredicateRegistry<RoleDescriptor> criterionPredicateRegistry;
    
    /** Flag which determines whether the default predicate registry will be used if no one is supplied explicitly.
     * Defaults to true. */
    private boolean useDefaultPredicateRegistry;
    
    /** Flag indicating whether resolution may be performed solely by applying predicates to the
     * entire metadata collection. Defaults to false. */
    private boolean resolveViaPredicatesOnly;
    
    /**
     * Constructor.
     *
     * @param mdResolver the resolver of EntityDescriptors
     */
    public PredicateRoleDescriptorResolver(@Nonnull MetadataResolver mdResolver) {
        entityDescriptorResolver = Constraint.isNotNull(mdResolver, "Resolver for EntityDescriptors may not be null");
        setId(UUID.randomUUID().toString()); 
        requireValidMetadata = true;
        useDefaultPredicateRegistry = true;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isRequireValidMetadata() {
        return requireValidMetadata;
    }

    /** {@inheritDoc} */
    @Override
    public void setRequireValidMetadata(boolean require) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        requireValidMetadata = require;
    }
    
    /**
     * Get the flag indicating whether resolved credentials may satisfy any predicates 
     * (i.e. connected by logical 'OR') or all predicates (connected by logical 'AND').
     * 
     * <p>Defaults to false.</p>
     * 
     * @return true if must satisfy all, false otherwise
     */
    public boolean isSatisfyAnyPredicates() {
        return satisfyAnyPredicates;
    }

    /**
     * Set the flag indicating whether resolved credentials may satisfy any predicates 
     * (i.e. connected by logical 'OR') or all predicates (connected by logical 'AND').
     * 
     * <p>Defaults to false.</p>
     * 
     * @param flag true if must satisfy all, false otherwise
     */
    public void setSatisfyAnyPredicates(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        satisfyAnyPredicates = flag;
    }

    /**
     * Get the registry used in resolving predicates from criteria.
     * 
     * @return the effective registry instance used
     */
    @NonnullAfterInit public CriterionPredicateRegistry<RoleDescriptor> getCriterionPredicateRegistry() {
        return criterionPredicateRegistry;
    }

    /**
     * Set the registry used in resolving predicates from criteria.
     * 
     * @param registry the registry instance to use
     */
    public void setCriterionPredicateRegistry(@Nullable final CriterionPredicateRegistry<RoleDescriptor> registry) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        criterionPredicateRegistry = registry;
    }

    /**
     * Get the flag which determines whether the default predicate registry will be used 
     * if one is not supplied explicitly.
     * 
     * <p>Defaults to true.</p>
     * 
     * @return true if should use default registry, false otherwise
     */
    public boolean isUseDefaultPredicateRegistry() {
        return useDefaultPredicateRegistry;
    }

    /**
     * Set the flag which determines whether the default predicate registry will be used 
     * if one is not supplied explicitly.
     * 
     * <p>Defaults to true.</p>
     * 
     * @param flag true if should use default registry, false otherwise
     */
    public void setUseDefaultPredicateRegistry(boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        useDefaultPredicateRegistry = flag;
    }
    

    /**
     * Get the flag indicating whether resolution may be performed solely 
     * by applying predicates to the entire metadata collection.
     * 
     * @return true if resolution may be attempted solely via predicates, false if not
     */
    public boolean isResolveViaPredicatesOnly() {
        return resolveViaPredicatesOnly;
    }

    /**
     * Set the flag indicating whether resolution may be performed solely 
     * by applying predicates to the entire metadata collection.
     * 
     * @param flag true if resolution may be attempted solely via predicates, false if not
     */
    public void setResolveViaPredicatesOnly(boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        resolveViaPredicatesOnly = flag;
    }
    
    /**
     * Subclasses should override this method to perform any initialization logic necessary. Default implementation is a
     * no-op.
     * 
     * @throws ComponentInitializationException thrown if there is a problem initializing the provider
     */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (getCriterionPredicateRegistry() == null && isUseDefaultPredicateRegistry()) {
            setCriterionPredicateRegistry(new RoleDescriptorCriterionPredicateRegistry());
        }
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable public RoleDescriptor resolveSingle(CriteriaSet criteria) throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        Iterable<RoleDescriptor> iterable = resolve(criteria);
        if (iterable != null) {
            Iterator<RoleDescriptor> iterator = iterable.iterator();
            if (iterator != null && iterator.hasNext()) {
                return iterator.next();
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public Iterable<RoleDescriptor> resolve(CriteriaSet criteria) throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        Iterable<EntityDescriptor> entityDescriptorsSource = entityDescriptorResolver.resolve(criteria);
        if (!entityDescriptorsSource.iterator().hasNext()) {
            log.debug("Resolved no EntityDescriptors via underlying MetadataResolver, returning empty collection");
            return Collections.emptySet();
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Resolved {} source EntityDescriptors", Iterables.size(entityDescriptorsSource));
            }
        }
        
        Predicate<? super RoleDescriptor> predicate = isRequireValidMetadata() ? IS_VALID_PREDICATE 
                : Predicates.<XMLObject>alwaysTrue();
            
        if (haveRoleCriteria(criteria)) {
            Iterable<RoleDescriptor> candidates = getCandidatesByRoleAndProtocol(entityDescriptorsSource, criteria);
            if (log.isDebugEnabled()) {
                log.debug("Resolved {} RoleDescriptor candidates via role criteria, performing predicate filtering", 
                        Iterables.size(candidates));
            }
            return predicateFilterCandidates(Iterables.filter(candidates, predicate), criteria, false);
        } else if (isResolveViaPredicatesOnly()) {
            Iterable<RoleDescriptor> candidates = getAllCandidates(entityDescriptorsSource);
            if (log.isDebugEnabled()) {
                log.debug("Resolved {} RoleDescriptor total candidates for predicate-only resolution", 
                        Iterables.size(candidates));
            }
            return predicateFilterCandidates(Iterables.filter(candidates, predicate), criteria, true);
        } else {
            log.debug("Found no role criteria and predicate-only resolution is disabled, returning empty collection");
            return Collections.emptySet();
        }
        
    }
    
    /**
     * Determine if have entity role criteria.
     * 
     * @param criteria the current criteria set
     * 
     * @return true if have role criteria, false otherwise
     */
    protected boolean haveRoleCriteria(@Nonnull final CriteriaSet criteria) {
        return criteria.contains(EntityRoleCriterion.class);
    }

    /**
     * Obtain the role descriptors contained by the input entity descriptors which match 
     * the specified role and protocol criteria.
     * 
     * <p>
     * This method should only be called if {@link #haveRoleCriteria(CriteriaSet)} evaluates to true.
     * </p>
     * 
     * @param entityDescriptors the entity descriptors on which to operate
     * @param criteria the current criteria set
     * 
     * @return the role descriptors corresponding to the input entity role and protocol
     */
    protected Iterable<RoleDescriptor> getCandidatesByRoleAndProtocol(
            @Nonnull final Iterable<EntityDescriptor> entityDescriptors, @Nonnull final CriteriaSet criteria) {
        
        EntityRoleCriterion roleCriterion = Constraint.isNotNull(criteria.get(EntityRoleCriterion.class), 
                "EntityRoleCriterion was not supplied");
        
        ProtocolCriterion protocolCriterion = criteria.get(ProtocolCriterion.class);
        
        ArrayList<Iterable<RoleDescriptor>> aggregate = new ArrayList<>();
        for (EntityDescriptor entityDescriptor : entityDescriptors) {
            if (protocolCriterion != null) {
                aggregate.add(entityDescriptor.getRoleDescriptors(roleCriterion.getRole(), 
                        protocolCriterion.getProtocol()));
            } else {
                aggregate.add(entityDescriptor.getRoleDescriptors(roleCriterion.getRole()));
            }
        }
        return Iterables.concat(aggregate);
    }

    /**
     * Obtain all role descriptors contained by the input entity descriptors.
     * 
     * @param entityDescriptors the entity descriptors on which to operate 
     * 
     * @return all role descriptors contained by the input entity descriptors
     */
    protected Iterable<RoleDescriptor> getAllCandidates(
            @Nonnull final Iterable<EntityDescriptor> entityDescriptors) {
        
        ArrayList<Iterable<RoleDescriptor>> aggregate = new ArrayList<>();
        for (EntityDescriptor entityDescriptor : entityDescriptors) {
            aggregate.add(entityDescriptor.getRoleDescriptors());
        }
        return Iterables.concat(aggregate);
    }
    
    /**
     * Filter the supplied candidates by resolving predicates from the supplied criteria and applying
     * the predicates to return a filtered {@link Iterable}.
     * 
     * @param candidates the candidates to evaluate
     * @param criteria the criteria set to evaluate
     * @param onEmptyPredicatesReturnEmpty if true and no predicates are supplied, then return an empty iterable;
     *          otherwise return the original input candidates
     * 
     * @return an iterable of the candidates filtered by the resolved predicates
     * 
     * @throws ResolverException if there is a fatal error during resolution
     */
    protected Iterable<RoleDescriptor> predicateFilterCandidates(@Nonnull final Iterable<RoleDescriptor> candidates,
            @Nonnull final CriteriaSet criteria, final boolean onEmptyPredicatesReturnEmpty)
                    throws ResolverException {
        
        if (!candidates.iterator().hasNext()) {
            log.debug("Candidates iteration was empty, nothing to filter via predicates");
            return Collections.emptySet();
        }
        
        log.debug("Attempting to filter candidate RoleDescriptors via resolved Predicates");
        
        final Set<Predicate<RoleDescriptor>> predicates = ResolverSupport.getPredicates(criteria, 
                EvaluableRoleDescriptorCriterion.class, getCriterionPredicateRegistry());
        
        log.trace("Resolved {} Predicates: {}", predicates.size(), predicates);
        
        boolean satisfyAny;
        final SatisfyAnyCriterion satisfyAnyCriterion = criteria.get(SatisfyAnyCriterion.class);
        if (satisfyAnyCriterion  != null) {
            log.trace("CriteriaSet contained SatisfyAnyCriterion");
            satisfyAny = satisfyAnyCriterion.isSatisfyAny();
        } else {
            log.trace("CriteriaSet did NOT contain SatisfyAnyCriterion");
            satisfyAny = isSatisfyAnyPredicates();
        }
        
        log.trace("Effective satisyAny value: {}", satisfyAny);
        
        return ResolverSupport.getFilteredIterable(candidates, predicates, satisfyAny, onEmptyPredicatesReturnEmpty);
    }

}
