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

package org.opensaml.security.ldap.impl;

import java.security.GeneralSecurityException;

import javax.annotation.Nullable;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.ldaptive.ssl.CredentialConfig;
import org.ldaptive.ssl.SSLContextInitializer;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.trust.impl.TrustEngineX509TrustManager;
import org.opensaml.security.x509.X509Credential;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 * Ldaptive {@link CredentialConfig} implementation that supplies a {@link TrustEngine}-driven
 * {@link TrustManager} to the eventual TLS context.
 */
public class TrustEngineCredentialConfig implements CredentialConfig {
    
    /** The trust engine to use. */
    @Nullable private TrustEngine<? super X509Credential> tlsTrustEngine;

    /** Optional criteria set used in evaluating server TLS credentials. */
    @Nullable private CriteriaSet tlsCriteriaSet;
    
    /**
     * Sets the optional trust engine used in evaluating server TLS credentials.
     * 
     * @param engine the trust engine instance to use
     */
    public void setTLSTrustEngine(@Nullable final TrustEngine<? super X509Credential> engine) {
        tlsTrustEngine = engine;
    }
    
    /**
     * Set the optional criteria set used in evaluating server TLS credentials.
     * 
     * @param criteriaSet the new criteria set instance to use
     */
    public void setTLSCriteriaSet(@Nullable final CriteriaSet criteriaSet) {
        tlsCriteriaSet = criteriaSet;
    }
    
    /** {@inheritDoc} */
    public SSLContextInitializer createSSLContextInitializer() throws GeneralSecurityException {
        // CheckStyle: AnonInnerLength OFF
        return new SSLContextInitializer() {
            
            public SSLContext initSSLContext(final String protocol) throws GeneralSecurityException {
                final SSLContext ctx = SSLContext.getInstance(protocol);
                ctx.init(getKeyManagers(), getTrustManagers(), null);
                return ctx;
            }

            public TrustManager[] getTrustManagers() throws GeneralSecurityException {
                final TrustEngineX509TrustManager trustManager = new TrustEngineX509TrustManager();
                trustManager.setTLSTrustEngine(tlsTrustEngine);
                trustManager.setTLSCriteriaSet(tlsCriteriaSet);
                return new TrustManager[] { trustManager };
            }

            public void setTrustManagers(final TrustManager... managers) {
                throw new UnsupportedOperationException("setTrustManagers not supported");
            }

            public KeyManager[] getKeyManagers() throws GeneralSecurityException {
                return null;
            }
        };
        // CheckStyle: AnonInnerLength ON
    }

}