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

package org.opensaml.saml.metadata.resolver.index.impl;

import java.net.MalformedURLException;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.net.SimpleURLCanonicalizer;
import net.shibboleth.utilities.java.support.net.URLBuilder;

/**
 * Support methods for metadata indexing.
 */
public final class MetadataIndexSupport {
    
    /** Constructor. */
    private MetadataIndexSupport() { }
    
    /**
     * Canonicalize a location to be indexed.
     * 
     * <p>
     * Query, fragment and username/password are removed.  Then further canonicalization
     * is performed as implemented by {@link SimpleURLCanonicalizer}.
     * </p>
     * 
     * @param uri the location
     * @return the canonicalized location value to index
     * @throws MalformedURLException if URL can not be canonicalized
     */
    public static String canonicalizeLocationURI(@Nonnull final String uri) throws MalformedURLException {
        final URLBuilder urlBuilder = new URLBuilder(uri);
        urlBuilder.setUsername(null);
        urlBuilder.setPassword(null);
        urlBuilder.getQueryParams().clear();
        urlBuilder.setFragment(null);
        return SimpleURLCanonicalizer.canonicalize(urlBuilder.buildURL());
    }

}
