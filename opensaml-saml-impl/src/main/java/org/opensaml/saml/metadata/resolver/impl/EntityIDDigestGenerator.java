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

import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.security.crypto.JCAConstants;

import com.google.common.base.Function;

import net.shibboleth.utilities.java.support.codec.StringDigester;
import net.shibboleth.utilities.java.support.codec.StringDigester.OutputFormat;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 * Strategy for processing input criteria to extract the entityID from an {@link EntityIdCriterion} 
 * and produce the digest of the value.
 * 
 * <p>
 *  By default the digest strategy used is the SHA-1 algorithm, with output in lower-case hexadecimal.
 *  By default, prefix, suffix and value separator are null.
 *  </p>
 */
public class EntityIDDigestGenerator implements Function<CriteriaSet, String> {
    
    /** String digester for the EntityDescriptor's entityID. */
    @Nonnull private StringDigester digester;
    
    /** Prefix to prepend to the digested value. */
    @Nullable private String prefix;
    
    /** Suffix to append to the digested value. */
    @Nullable private String suffix;
    
    /** Common separator between prefix, digested and suffix values. */
    @Nullable private String separator;
    
    /** Constructor. */
    public EntityIDDigestGenerator() {
        this(null, null, null, null);
    }
    
    /** Constructor.
     * 
     * 
     * @param valueDigester optional digeser for the entityID value
     * @param keyPrefix optional prefix for the digested value
     * @param keySuffix optional suffix for the digested value
     * @param valueSeparator optional separator between the prefix, digest and suffix values
     */
    public EntityIDDigestGenerator(@Nullable final StringDigester valueDigester,  @Nullable final String keyPrefix, 
            @Nullable final String keySuffix, @Nullable final String valueSeparator) {
        
        prefix = StringSupport.trimOrNull(keyPrefix);
        suffix = StringSupport.trimOrNull(keySuffix);
        separator = StringSupport.trimOrNull(valueSeparator);
        
        digester = valueDigester;
        if (digester == null) {
            try {
                digester = new StringDigester(JCAConstants.DIGEST_SHA1, OutputFormat.HEX_LOWER);
            } catch (final NoSuchAlgorithmException e) {
                // this can't really happen b/c SHA-1 is required to be supported on all JREs.
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String apply(final CriteriaSet input) {
        if (input == null) {
            return null;
        }
        
        final EntityIdCriterion entityIDCrit = input.get(EntityIdCriterion.class);
        if (entityIDCrit == null) { 
            return null;
        }
        
        final String entityID = StringSupport.trimOrNull(entityIDCrit.getEntityId());
        if (entityID == null) {
            return null;
        }
        
        final String digested = digester.apply(entityID);
        
        if (prefix == null && suffix == null) {
            return digested;
        } else {
            final StringBuffer buffer = new StringBuffer();
            if (prefix != null) {
                buffer.append(prefix);
                if (separator != null) {
                    buffer.append(separator);
                }
            }
            buffer.append(digested);
            if (suffix != null) {
                if (separator != null) {
                    buffer.append(separator);
                }
                buffer.append(suffix);
            }
            return buffer.toString();
        }
    }
    
}