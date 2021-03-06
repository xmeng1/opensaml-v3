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

package org.opensaml.xmlsec.criterion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.Criterion;

import org.opensaml.xmlsec.SignatureValidationConfiguration;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

/**
 * Criterion which holds one or more instances of {@link SignatureValidationConfiguration}.
 */
public class SignatureValidationConfigurationCriterion implements Criterion {
    
    /** The list of configuration instances. */
    @Nonnull @NonnullElements private List<SignatureValidationConfiguration> configs;
    
    /**
     * Constructor.
     *
     * @param configurations list of configuration instances
     */
    public SignatureValidationConfigurationCriterion(@Nonnull @NonnullElements @NotEmpty
            List<SignatureValidationConfiguration> configurations) {
        Constraint.isNotNull(configurations, "List of configurations cannot be null");
        configs = new ArrayList<>(Collections2.filter(configurations, Predicates.notNull()));
        Constraint.isGreaterThanOrEqual(1, configs.size(), "At least one configuration is required");
        
    }
    
    /**
     * Constructor.
     *
     * @param configurations varargs array of configuration instances
     */
    public SignatureValidationConfigurationCriterion(@Nonnull @NonnullElements @NotEmpty
            SignatureValidationConfiguration... configurations) {
        Constraint.isNotNull(configurations, "List of configurations cannot be null");
        configs = new ArrayList<>(Collections2.filter(Arrays.asList(configurations), Predicates.notNull()));
        Constraint.isGreaterThanOrEqual(1, configs.size(), "At least one configuration is required");
    }
    
    /**
     * Get the list of configuration instances.
     * @return the list of configuration instances
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable @NotEmpty
    public List<SignatureValidationConfiguration> getConfigurations() {
        return ImmutableList.copyOf(configs);
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SignatureValidationConfigurationCriterion [configs=");
        builder.append(configs);
        builder.append("]");
        return builder.toString();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return configs.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof SignatureValidationConfigurationCriterion) {
            return configs.equals(((SignatureValidationConfigurationCriterion) obj).getConfigurations());
        }

        return false;
    }

}