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

package org.opensaml.saml.metadata.criteria.role.impl;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.metadata.criteria.role.EvaluableRoleDescriptorCriterion;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;

import com.google.common.base.MoreObjects;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Implementation of {@link EvaluableRoleDescriptorCriterion} which evaluates that a role descriptor's
 * type or element name ({@link QName} is the specified role.
 */
public class EvaluableEntityRoleDescriptorCriterion implements EvaluableRoleDescriptorCriterion {
    
    /** The entity role QName being evaluated. */
    @Nonnull @NotEmpty private QName role;
    
    /**
     * Constructor.
     *
     * @param criterion the entity role criterion
     */
    public EvaluableEntityRoleDescriptorCriterion(@Nonnull final EntityRoleCriterion criterion) {
        Constraint.isNotNull(criterion, "EntityRoleCriterion was null");
        role = Constraint.isNotNull(criterion.getRole(), "Criterion entity role was null");
    }
    
    /**
     * Constructor.
     *
     * @param entityRole the entity role QName
     */
    public EvaluableEntityRoleDescriptorCriterion(@Nonnull final QName entityRole) {
        role = Constraint.isNotNull(entityRole, "Entity role QName was null");
    }
    
    /** {@inheritDoc} */
    public boolean apply(RoleDescriptor input) {
        if (input == null) {
            return false;
        }
        
        QName schemaType = input.getSchemaType();
        if (schemaType != null && Objects.equals(role, schemaType)) {
            return true;
        }
        
        return Objects.equals(role, input.getElementQName());
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
        
        if (other instanceof EvaluableEntityRoleDescriptorCriterion) {
            return Objects.equals(this.role, ((EvaluableEntityRoleDescriptorCriterion)other).role);
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
