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

package org.opensaml.security.criteria;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.Criterion;

/**
 * An implementation of {@link Criterion} which specifies key length criteria.
 */
public final class KeyLengthCriterion implements Criterion {
    
    /** Key length of resolved credentials. */
    private Integer keyLength;
    
    /**
     * Constructor.
     *
     * @param length key length 
     */
    public KeyLengthCriterion(@Nonnull final Integer length) {
        setKeyLength(length);
    }

    /**
     * Get the key length.
     * 
     * @return Returns the keyLength.
     */
    @Nonnull public Integer getKeyLength() {
        return keyLength;
    }

    /**
     * Set the key length.
     * 
     * @param length The keyLength to set.
     */
    public void setKeyLength(@Nonnull final Integer length) {
        Constraint.isNotNull(length, "Key length criteria value cannot be null");

        keyLength = length;
    }
    
    /** {@inheritDoc} */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("KeyLengthCriterion [keyLength=");
        builder.append(keyLength);
        builder.append("]");
        return builder.toString();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return keyLength.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof KeyLengthCriterion) {
            return keyLength.equals(((KeyLengthCriterion) obj).keyLength);
        }

        return false;
    }

}
