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

package org.opensaml.security.httpclient;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.X509Credential;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 * The security configuration information to use when performing HTTP client requests.
 */
public interface HttpClientSecurityConfiguration {
    
    /**
     * Get an instance of {@link CredentialsProvider} used for authentication by the HttpClient instance.
     * 
     * @return the credentials provider, or null
     */
    @Nullable public CredentialsProvider getCredentialsProvider();
    
    /**
     * Get an instance of {@link AuthCache} used for authentication by the HttpClient instance.
     * 
     * @return the cache, or null
     * 
     * @since 3.4.0
     */
    @Nullable public AuthCache getAuthCache();
    
    /**
     * Sets the optional trust engine used in evaluating server TLS credentials.
     * 
     * @return the trust engine instance to use, or null
     */
    @Nullable public TrustEngine<? super X509Credential> getTLSTrustEngine();
    
    /**
     * Get the optional criteria set used in evaluating server TLS credentials.
     * 
     * @return the criteria set instance to use
     */
    @Nullable public CriteriaSet getTLSCriteriaSet();
    
    /**
     * Get the optional list of TLS protocols. 
     * 
     * @return the TLS protocols, or null
     */
    @Nullable public List<String> getTLSProtocols();
    
    /**
     * Get the optional list of TLS cipher suites.
     * 
     * @return the list of TLS cipher suites, or null
     */
    @Nullable public List<String> getTLSCipherSuites();
    
    /**
     * Get the optional hostname verifier.
     * 
     * @return the hostname verifier, or null
     */
    @Nullable public X509HostnameVerifier getHostnameVerifier();
    
    /**
     * Get the optional client TLS credential.
     * 
     * @return the client TLS credential, or null
     */
    @Nullable public X509Credential getClientTLSCredential();

}
