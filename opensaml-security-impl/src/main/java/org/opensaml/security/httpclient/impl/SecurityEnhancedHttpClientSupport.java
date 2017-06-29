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

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.tls.impl.ThreadLocalX509CredentialKeyManager;

import net.shibboleth.utilities.java.support.httpclient.HttpClientSupport;
import net.shibboleth.utilities.java.support.httpclient.TLSSocketFactoryBuilder;

/**
 * Support class for working with security-enhanced components related to use of {@link HttpClient}.
 */
public final class SecurityEnhancedHttpClientSupport {
    
    /** Constructor. */
    private SecurityEnhancedHttpClientSupport() { }
    
    /**
     * Build an instance of TLS-capable {@link LayeredConnectionSocketFactory} 
     * wrapped by {@link SecurityEnhancedTLSSocketFactory}, configured for 
     * server TLS based on a mandatory {@link TrustEngine} supplied at runtime.
     * 
     * <p>
     * Equivalent to {@link #buildTLSSocketFactory(true, false)}.
     * </p>
     * 
     * @return a new instance of security-enhanced TLS socket factory 
     */
    @Nonnull public static LayeredConnectionSocketFactory buildTLSSocketFactory() {
        return buildTLSSocketFactory(true, false);
    }
    
    /**
     * Build an instance of TLS-capable {@link LayeredConnectionSocketFactory} 
     * wrapped by {@link SecurityEnhancedTLSSocketFactory}, configured for 
     * server TLS based on a mandatory {@link TrustEngine} supplied at runtime,
     * and additionally configured for optional client TLS support via context client TLS credential.
     * 
     * <p>
     * Equivalent to {@link #buildTLSSocketFactory(true, true)}.
     * </p>
     * 
     * @return a new instance of security-enhanced TLS socket factory 
     */
    @Nonnull public static LayeredConnectionSocketFactory buildTLSSocketFactoryWithClientTLS() {
        return buildTLSSocketFactory(true, true);
    }
    
    /**
     * Build an instance of TLS-capable {@link LayeredConnectionSocketFactory} 
     * wrapped by {@link SecurityEnhancedTLSSocketFactory},
     * configured for optional client TLS support via context client TLS credential.
     * 
     * <p>
     * Server TLS will be based on the default JSSE trust mechanism.
     * </p>
     * 
     * <p>
     * Equivalent to {@link #buildTLSSocketFactory(false, true)}.
     * </p>
     * 
     * @return a new instance of security-enhanced TLS socket factory 
     */
    @Nonnull public static LayeredConnectionSocketFactory buildTLSSocketFactoryWithClientTLSOnly() {
        return buildTLSSocketFactory(false, true);
    }
    
    /**
     * Build an instance of TLS-capable {@link LayeredConnectionSocketFactory}.
     * 
     * <p>
     * If either <code>supportTrustEngine</code> or <code>supportClientTLS</code> are true,
     * the returned factory will be a instance of {@link SecurityEnhancedTLSSocketFactory}
     * wrapping an instance of {@link LayeredConnectionSocketFactory}.
     * </p>
     * 
     * <p>
     * If <code>supportTrustEngine</code> is true, then the wrapped factory will be configured
     * with a "no trust" {@link X509TrustManager}, to allow the actual server TLS trust evaluation
     * to be performed by a mandatory {@link TrustEngine} supplied at runtime,
     * as documented in {@link SecurityEnhancedTLSSocketFactory}.
     * </p>
     * 
     * <p>
     * If <code>supportClientTLS</code> is true, then the wrapped factory will be configured
     * with a {@link X509KeyManager} that supports per-request specification of a client TLS
     * credential, as documented in {@link SecurityEnhancedTLSSocketFactory}.
     * </p>
     * 
     * @param supportTrustEngine whether to support server TLS via a context trust engine
     * @param supportClientTLS whether to support client TLS via a context client credential
     * 
     * @return a TLS socket factory
     */
    @Nonnull public static LayeredConnectionSocketFactory buildTLSSocketFactory(final boolean supportTrustEngine, 
            final boolean supportClientTLS) {
        
        TLSSocketFactoryBuilder wrappedFactoryBuilder = new TLSSocketFactoryBuilder();
        
        if (supportTrustEngine || supportClientTLS) {
            wrappedFactoryBuilder.setHostnameVerifier(new AllowAllHostnameVerifier());
            
            if (supportTrustEngine) {
                wrappedFactoryBuilder.setTrustManagers(
                        Collections.<TrustManager>singletonList(HttpClientSupport.buildNoTrustX509TrustManager()));
            }

            if (supportClientTLS) {
                wrappedFactoryBuilder.setKeyManagers(
                        Collections.<KeyManager>singletonList(new ThreadLocalX509CredentialKeyManager()));
            }
            
            return new SecurityEnhancedTLSSocketFactory(wrappedFactoryBuilder.build(), new StrictHostnameVerifier(), 
                    supportTrustEngine);
            
        } else {
            return HttpClientSupport.buildStrictTLSSocketFactory();
        }
        
    }

}
