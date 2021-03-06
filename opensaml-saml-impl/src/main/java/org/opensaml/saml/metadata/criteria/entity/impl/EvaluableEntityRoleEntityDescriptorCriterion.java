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

package org.opensaml.saml.metadata.criteria.entity.impl;

import java.util.Objects;

import javax.xml.namespace.QName;

import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.metadata.criteria.entity.EvaluableEntityDescriptorCriterion;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import com.google.common.base.MoreObjects;

import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Implementation of {@link EvaluableEntityDescriptorCriterion} which evaluates that an entity descriptor
 * contains a certain role.
 */
public class EvaluableEntityRoleEntityDescriptorCriterion implements EvaluableEntityDescriptorCriterion {
    
    /** Entity role. */
    private QName role;

    
    /**
     * Constructor.
     *
     * @param criterion the entity role criterion
     */
    public EvaluableEntityRoleEntityDescriptorCriterion(EntityRoleCriterion criterion) {
        Constraint.isNotNull(criterion, "EntityRoleCriterion was null");
        role = Constraint.isNotNull(criterion.getRole(), "Criterion role QName was null");
    }
    
    /**
     * Constructor.
     *
     * @param entityRole the entity
     */
    public EvaluableEntityRoleEntityDescriptorCriterion(QName entityRole) {
        role = Constraint.isNotNull(entityRole, "Entity Role QName was null");
    }

    /** {@inheritDoc} */
    public boolean apply(EntityDescriptor entityDescriptor) {
        if (entityDescriptor == null) {
            return false;
        }
        
        return ! entityDescriptor.getRoleDescriptors(role).isEmpty();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return role.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        if (other instanceof EvaluableEntityRoleEntityDescriptorCriterion) {
            return Objects.equals(this.role, ((EvaluableEntityRoleEntityDescriptorCriterion)other).role);
        }
        
        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("role", role)
                .toString();
    }
    
}
