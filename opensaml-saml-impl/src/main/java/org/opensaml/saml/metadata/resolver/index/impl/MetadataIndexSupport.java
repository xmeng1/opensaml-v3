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
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.net.SimpleURLCanonicalizer;
import net.shibboleth.utilities.java.support.net.URLBuilder;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

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
     * @param input the location
     * @return the canonicalized location value to index
     * @throws MalformedURLException if URL can not be canonicalized
     */
    @Nonnull public static String canonicalizeLocationURI(@Nonnull final String input) throws MalformedURLException {
        final String uri = StringSupport.trimOrNull(input);
        if (uri == null) {
            throw new MalformedURLException("URL input was null or empty");
        }
        final URLBuilder urlBuilder = new URLBuilder(uri);
        urlBuilder.setUsername(null);
        urlBuilder.setPassword(null);
        urlBuilder.getQueryParams().clear();
        urlBuilder.setFragment(null);
        return SimpleURLCanonicalizer.canonicalize(urlBuilder.buildURL());
    }
    
    /**
     * Trim the right-most segment from the specified URL path.
     * 
     * <p>
     * The input should be the path only, with no query or fragment.
     * </p>
     * 
     * <p>
     * Paths ending in "/" return the input with the trailing slash omitted, except for the
     * special case of input == "/", which returns null.
     * Paths not ending in "/" return the input with the right-most
     * segment removed, and including a trailing slash.
     * </p>
     * 
     * @param input the path to evaluate
     * @return the trimmed path, or null
     */
    @Nullable public static String trimURLPathSegment(@Nullable final String input) {
        final String path = StringSupport.trimOrNull(input);
        if (path == null || "/".equals(path)) {
            return null;
        } else {
            final int idx = path.lastIndexOf("/");
            if (idx > 0) {
                if (path.endsWith("/")) {
                    return path.substring(0, idx);
                } else {
                    return path.substring(0, idx+1);
                }
            } else if (idx == 0) {
                return "/";
            } else {
                return null;
            }
        }
    }

}
