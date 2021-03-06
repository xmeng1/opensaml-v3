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

package org.opensaml.security.credential.criteria.impl;

import java.security.MessageDigest;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.logic.AbstractTriStatePredicate;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.apache.commons.codec.binary.Hex;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.security.x509.X509DigestCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An implementation of {@link net.shibboleth.utilities.java.support.resolver.Criterion} which specifies
 * criteria based on the digest of an X.509 certificate.
 */
public final class EvaluableX509DigestCredentialCriterion extends AbstractTriStatePredicate<Credential> 
        implements EvaluableCredentialCriterion {

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(EvaluableX509DigestCredentialCriterion.class);
    
    /** Digest algorithm. */
    private final String algorithm;
    
    /** X.509 certificate digest. */
    private final byte[] x509digest;

    /**
     * Constructor.
     * 
     * @param criteria the criteria which is the basis for evaluation
     */
    public EvaluableX509DigestCredentialCriterion(@Nonnull final X509DigestCriterion criteria) {
        algorithm = Constraint.isNotNull(criteria, "Criterion instance cannot be null").getAlgorithm();
        x509digest = criteria.getDigest();
    }
    
    /**
     * Constructor.
     *
     * @param alg algorithm of digest computation
     * @param digest certificate digest
     */
    public EvaluableX509DigestCredentialCriterion(@Nonnull final String alg, @Nonnull final byte[] digest) {
        x509digest = Constraint.isNotEmpty(digest, "X.509 digest cannot be null or empty");
        String trimmed = StringSupport.trimOrNull(alg);
        Constraint.isNotNull(trimmed, "Certificate digest algorithm cannot be null or empty");
        algorithm = trimmed;
    }

    /** {@inheritDoc} */
    @Nullable public boolean apply(@Nullable final Credential target) {
        if (target == null) {
            log.error("Credential target was null");
            return isNullInputSatisfies();
        } else if (!(target instanceof X509Credential)) {
            log.info("Credential is not an X509Credential, does not satisfy X.509 digest criteria");
            return false;
        }

        X509Certificate entityCert = ((X509Credential) target).getEntityCertificate();
        if (entityCert == null) {
            log.info("X509Credential did not contain an entity certificate, does not satisfy criteria");
            return false;
        }
        
        try {
            MessageDigest hasher = MessageDigest.getInstance(algorithm);
            byte[] hashed = hasher.digest(entityCert.getEncoded());
            return Arrays.equals(hashed, x509digest);
        } catch (CertificateEncodingException e) {
            log.error("Unable to encode certificate for digest operation", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("Unable to obtain a digest implementation for algorithm {" + algorithm + "}" , e);
        }
        
        return isUnevaluableSatisfies();
    }
    
    /** {@inheritDoc} */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EvaluableX509DigestCredentialCriterion [algorithm=");
        builder.append(algorithm);
        builder.append(", x509digest=");
        builder.append(Hex.encodeHexString(x509digest));
        builder.append("]");
        return builder.toString();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        int result = 17;
        result = result*37 + algorithm.hashCode();
        result = result*37 + x509digest.hashCode();
        return result;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof EvaluableX509DigestCredentialCriterion) {
            EvaluableX509DigestCredentialCriterion other = (EvaluableX509DigestCredentialCriterion) obj;
            return algorithm.equals(other.algorithm) && Arrays.equals(x509digest, other.x509digest);
        }

        return false;
    }
    
}