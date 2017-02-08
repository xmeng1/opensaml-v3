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

import java.util.Objects;

import com.google.common.base.MoreObjects;

import net.shibboleth.utilities.java.support.resolver.Criterion;

/**
 * A criterion for specifying whether encryption is to be treated as optional.
 * 
 * @since 3.3.0
 */
public class EncryptionOptionalCriterion implements Criterion {
    
    /** The encryptionOptional criterion value. */
    private Boolean encryptionOptional;
    
    /**
     * Constructor.
     */
    public EncryptionOptionalCriterion() {
        encryptionOptional = Boolean.FALSE;
    }
    
    /**
     * Constructor.
     *
     * @param value the encryptionOptional flag value
     */
    public EncryptionOptionalCriterion(boolean value) {
        encryptionOptional = value;
    }

    /**
     * Get the encryptionOptional value.
     * 
     * @return true or false
     */
    public boolean isEncryptionOptional() {
        return encryptionOptional;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return encryptionOptional.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        if (other instanceof EncryptionOptionalCriterion) {
            return Objects.equals(encryptionOptional, ((EncryptionOptionalCriterion)other).encryptionOptional);
        }
        
        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(encryptionOptional).toString();
    }

}
