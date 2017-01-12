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

package org.opensaml.security.trust.impl;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.X509TrustManager;

import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.X509Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 * {@link X509TrustManager} implementation that relies on a {@link TrustEngine}.
 */
public class TrustEngineX509TrustManager implements X509TrustManager {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(TrustEngineX509TrustManager.class);
    
    /** The trust engine to use. */
    @Nullable private TrustEngine<? super X509Credential> tlsTrustEngine;

    /** Optional criteria set used in evaluating server TLS credentials. */
    @Nullable private CriteriaSet tlsCriteriaSet;
    
    /**
     * Set the trust engine used in evaluating server TLS credentials.
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
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        checkServerTrusted(arg0, arg1);
    }

    /** {@inheritDoc} */
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        if (arg0 == null || arg0.length < 1) {
            throw new CertificateException("Peer certificate array was null or empty");
        } else if (tlsTrustEngine == null) {
            throw new CertificateException("TrustEngine was null");
        }

        final ArrayList<X509Certificate> certChain = new ArrayList<>();
        for (final Certificate cert : arg0) {
            certChain.add((X509Certificate) cert);
        }

        final BasicX509Credential credential = new BasicX509Credential(arg0[0]);
        credential.setEntityCertificateChain(certChain);
        
        final CriteriaSet criteriaSet;
        if (tlsCriteriaSet != null) {
            criteriaSet = tlsCriteriaSet;
        } else {
            criteriaSet = new CriteriaSet(new UsageCriterion(UsageType.SIGNING));
        }
        
        try {
            if (tlsTrustEngine.validate(credential, criteriaSet)) {
                log.debug("Credential evaluated as trusted");
            } else {
                log.debug("Credential evaluated as untrusted");
                throw new CertificateException("Trust engine could not establish trust of TLS credential");
            }
        } catch (final SecurityException e) {
            log.error("Trust engine error evaluating credential", e);
            throw new CertificateException("Trust engine error evaluating credential", e);
        }
    }

    /** {@inheritDoc} */
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[] {};
    }

}