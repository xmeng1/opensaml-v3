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

package org.opensaml.saml.common.profile.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.saml.metadata.EntityGroupName;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.AffiliateMember;
import org.opensaml.saml.saml2.metadata.AffiliationDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

/**
 * Predicate to determine whether one of a set of names matches any of an entity's containing
 * {@link org.opensaml.saml.saml2.metadata.EntitiesDescriptor} groups. 
 */
public class EntityGroupNamePredicate implements Predicate<EntityDescriptor> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(EntityGroupNamePredicate.class);
    
    /** Groups to match on. */
    @Nonnull @NonnullElements private final Set<String> groupNames;
    
    /** A supplemental resolver to allow for {@link AffiliationDescriptor} lookup. */
    @Nullable private MetadataResolver metadataResolver;
    
    /** Pre-created criteria sets for metadata lookup. */
    @Nullable @NonnullElements private Collection<CriteriaSet> criteriaSets;
    
    /**
     * Constructor.
     * 
     * @param names the group names to test for
     */
    public EntityGroupNamePredicate(@Nonnull @NonnullElements final Collection<String> names) {
        this(names, null);
    }
    
    /**
     * Constructor.
     * 
     * @param names the group names to test for
     * @param resolver metadata resolver for affiliation support
     * 
     * @since 3.4.0
     */
    public EntityGroupNamePredicate(@Nonnull @NonnullElements final Collection<String> names,
            @Nullable final MetadataResolver resolver) {
        
        Constraint.isNotNull(names, "Group name collection cannot be null");
        groupNames = new HashSet<>(names.size());
        for (final String name : names) {
            final String trimmed = StringSupport.trimOrNull(name);
            if (trimmed != null) {
                groupNames.add(trimmed);
            }
        }
        
        metadataResolver = resolver;
        if (resolver != null) {
            criteriaSets = new ArrayList<>(groupNames.size());
            for (final String name : groupNames) {
                criteriaSets.add(new CriteriaSet(new EntityIdCriterion(name)));
            }
        }
    }

    /**
     * Get the group name criteria.
     * 
     * @return  the group name criteria
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Set<String> getGroupNames() {
        return ImmutableSet.copyOf(groupNames);
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    public boolean apply(@Nullable final EntityDescriptor input) {
        
        if (input == null) {
            log.debug("Input was null, condition is false");
            return false;
        }
        
        for (final EntityGroupName group : input.getObjectMetadata().get(EntityGroupName.class)) {
            if (groupNames.contains(group.getName())) {
                log.debug("Found matching group '{}' attached to entity '{}'", group.getName(), input.getEntityID());
                return true;
            }
        }
        
        if (metadataResolver != null) {
            for (final CriteriaSet criteria : criteriaSets) {
                try {
                    final EntityDescriptor affiliation = metadataResolver.resolveSingle(criteria);
                    if (affiliation != null && affiliation.getAffiliationDescriptor() != null) {
                        for (final AffiliateMember member : affiliation.getAffiliationDescriptor().getMembers()) {
                            if (member.getID().equals(input.getEntityID())) {
                                log.debug("Found AffiliationDescriptor '{}' membership for entity '{}'",
                                        affiliation.getEntityID(), input.getEntityID());
                                return true;
                            }
                        }
                    }
                } catch (final ResolverException e) {
                    log.warn("Metadata lookup for AffiliationDescriptor failed", e);
                }
            }
        }
        
        log.debug("No group match found for entity '{}'", input.getEntityID());
        return false;
    }
// Checkstyle: CyclomaticComplexity ON
    
}