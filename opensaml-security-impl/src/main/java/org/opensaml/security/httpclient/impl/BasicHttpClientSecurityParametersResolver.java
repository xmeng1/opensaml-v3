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

package org.opensaml.security.httpclient.impl;

import java.security.Key;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.httpclient.HttpClientSecurityConfiguration;
import org.opensaml.security.httpclient.HttpClientSecurityConfigurationCriterion;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.HttpClientSecurityParametersResolver;
import org.opensaml.security.httpclient.TLSCriteriaSetCriterion;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.X509Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.ObjectSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Basic implementation of {@link HttpClientSecurityParametersResolver}.
 * 
 * <p>
 * The following {@link net.shibboleth.utilities.java.support.resolver.Criterion} inputs are supported:
 * <ul>
 * <li>{@link HttpClientSecurityConfigurationCriterion} - required</li> 
 * </ul>
 * </p>
 */
public class BasicHttpClientSecurityParametersResolver implements HttpClientSecurityParametersResolver {
    
    /** Logger. */
    private Logger log = LoggerFactory.getLogger(BasicHttpClientSecurityParametersResolver.class);

    /** {@inheritDoc} */
    public Iterable<HttpClientSecurityParameters> resolve(@Nonnull final CriteriaSet criteria) 
            throws ResolverException {
        
        final HttpClientSecurityParameters params = resolveSingle(criteria);
        if (params != null) {
            return Collections.singletonList(params);
        } else {
            return Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    public HttpClientSecurityParameters resolveSingle(@Nonnull final CriteriaSet criteria) throws ResolverException {
        Constraint.isNotNull(criteria, "CriteriaSet was null");
        Constraint.isNotNull(criteria.get(HttpClientSecurityConfigurationCriterion.class), 
                "Resolver requires an instance of HttpClientSecurityConfigurationCriterion");
        
        final HttpClientSecurityParameters params = new HttpClientSecurityParameters();
        
        resolveAndPopulateParams(params, criteria);
        
        if (validate(params)) {
            logResult(params);
            return params;
        } else {
            return null;
        }
    }
    
    /**
     * Resolve and populate all parametersu.
     * 
     * @param params the parameters instance to populate
     * @param criteria the criteria to process
     */
    protected void resolveAndPopulateParams(@Nonnull final HttpClientSecurityParameters params, 
            @Nonnull final CriteriaSet criteria) {
        
        final List<HttpClientSecurityConfiguration> configs = 
                criteria.get(HttpClientSecurityConfigurationCriterion.class).getConfigurations();
        
        for (final HttpClientSecurityConfiguration config : configs) {
            params.setClientTLSCredential(ObjectSupport.firstNonNull(params.getClientTLSCredential(), 
                    config.getClientTLSCredential()));
            params.setCredentialsProvider(ObjectSupport.firstNonNull(params.getCredentialsProvider(), 
                    config.getCredentialsProvider()));
            params.setHostnameVerifier(ObjectSupport.firstNonNull(params.getHostnameVerifier(), 
                    config.getHostnameVerifier()));
            params.setTLSCipherSuites(ObjectSupport.firstNonNull(params.getTLSCipherSuites(), 
                    config.getTLSCipherSuites()));
            params.setTLSProtocols(ObjectSupport.firstNonNull(params.getTLSProtocols(), 
                    config.getTLSProtocols()));
            params.setTLSTrustEngine(ObjectSupport.<TrustEngine<? super X509Credential>>firstNonNull(
                    params.getTLSTrustEngine(), config.getTLSTrustEngine()));
        }
        
        if (criteria.contains(TLSCriteriaSetCriterion.class)) {
            params.setTLSCriteriaSet(criteria.get(TLSCriteriaSetCriterion.class).getCriteria());
        }
    }

    /**
     * Validate that the {@link HttpClientSecurityParameters} instance has all the required properties populated.
     * 
     * @param params the parameters instance to evaluate
     * 
     * @return true if parameters instance passes validation, false otherwise
     */
    protected boolean validate(@Nonnull final HttpClientSecurityParameters params) {
        // Default impl is no required data.
        return true;
    }
    
    /**
     * Log the resolved parameters.
     * 
     * @param params the resolved param
     */
    protected void logResult(@Nonnull final HttpClientSecurityParameters params) {
        if (log.isDebugEnabled()) {
            log.debug("Resolved HttpClientSecurityParameters:");
            
            final Key clientTLSKey = CredentialSupport.extractSigningKey(params.getClientTLSCredential());
            if (clientTLSKey != null) {
                log.debug("\tClient TLS credential with key algorithm: {}", clientTLSKey.getAlgorithm());
            } else {
                log.debug("\tClient TLS credential: null"); 
            }
            
            log.debug("\tHostnameVerifier: {}", params.getHostnameVerifier() != null ? "present" : "null");
            log.debug("\tTLS TrustEngine: {}", params.getTLSTrustEngine() != null ? "present" : "null");
            log.debug("\tTLS CriteriaSet: {}", params.getTLSCriteriaSet() != null ? "present" : "null");
            
            log.debug("\tTLS cipher suites: {}", params.getTLSCipherSuites()); 
            log.debug("\tTLS protocols: {}", params.getTLSProtocols()); 
            
            log.debug("\tAuthCache: {}", params.getAuthCache() != null ? "present" : "null");
            log.debug("\tCredentialsProvider: {}", params.getCredentialsProvider() != null ? "present" : "null");
        }
    }

}
