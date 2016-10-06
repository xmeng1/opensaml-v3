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

package org.opensaml.saml.criterion;

import java.util.Objects;

import com.google.common.base.MoreObjects;

import net.shibboleth.utilities.java.support.resolver.Criterion;

/**
 * A criterion which allows to specify at runtime whether location paths being evaluated
 * may be evaluated on the basis of a "starts with" match.
 */
public class StartsWithLocationCriterion implements Criterion {
    
    /** The matchStartsWith criterion value. */
    private Boolean matchStartsWith;
    
    /**
     * Constructor.
     */
    public StartsWithLocationCriterion() {
        matchStartsWith = Boolean.TRUE;
    }
    
    /**
     * Constructor.
     *
     * @param value the matchStartsWith flag value
     */
    public StartsWithLocationCriterion(boolean value) {
        matchStartsWith = value;
    }

    /**
     * Get the startswith value.
     * 
     * @return true or false
     */
    public boolean isMatchStartsWith() {
        return matchStartsWith;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return matchStartsWith.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        if (other instanceof StartsWithLocationCriterion) {
            return Objects.equals(matchStartsWith, ((StartsWithLocationCriterion)other).matchStartsWith);
        }
        
        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(matchStartsWith).toString();
    }

}
