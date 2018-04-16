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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpHost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.protocol.HttpContext;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.security.httpclient.HttpClientSecurityConstants;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.TrustedNamesCriterion;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.security.x509.tls.impl.ThreadLocalX509CredentialContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 * An security-enhanced implementation of HttpClient's TLS-capable {@link LayeredConnectionSocketFactory}.
 * 
 * <p>
 * This implementation wraps an existing TLS socket factory instance, decorating it with additional support for:
 * <ul>
 *     <li>Verifying the server TLS certificate and chain via a
 *         {@link TrustEngine}<code>&lt;</code>{@link org.opensaml.security.credential.Credential}<code>&gt;</code>
 *         and {@link CriteriaSet} supplied by the HttpClient caller via the {@link HttpContext}.</li>
 *         
 *     <li>Loading and clearing a thread-local instance of {@link X509Credential} used for client TLS.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The context keys used by this component are as follows, defined in {@link HttpClientSecurityConstants}:
 * <ul>
 *   <li>{@link HttpClientSecurityConstants#CONTEXT_KEY_TRUST_ENGINE}: The trust engine instance used. 
 *        Supplied by the HttpClient caller. Must be an instance of
 *        {@link TrustEngine}<code>&lt;</code>{@link org.opensaml.security.credential.Credential}<code>&gt;</code>.</li>
 *   <li>{@link HttpClientSecurityConstants#CONTEXT_KEY_CRITERIA_SET}: The criteria set instance used. 
 *        Supplied by the HttpClient caller. Must be an instance of {@link CriteriaSet}. </li>
 *   <li>{@link HttpClientSecurityConstants#CONTEXT_KEY_SERVER_TLS_CREDENTIAL_TRUSTED}: The result of the 
 *       trust evaluation, if it was performed.  Populated by this component.  Will be a {@link Boolean}, 
 *       where <code>true</code> means the server TLS was evaluated as trusted, <code>false</code> means 
 *       the credential was evaluated as untrusted.  A null or missing value means that trust engine 
 *       evaluation was not performed.</li>
 *   <li>{@link HttpClientSecurityConstants#CONTEXT_KEY_CLIENT_TLS_CREDENTIAL}: The client TLS credential used.
 *        Supplied by the HttpClient caller. Must be an instance of {@link X509Credential}.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * If the trust engine context attribute is not populated by the caller and {@link #isTrustEngineRequired()} 
 * is <code>true</code> (the default), then an {@link SSLPeerUnverifiedException} is thrown.
 * </p>
 * 
 * <p>
 * If the trust engine context attribute is not populated by the caller and {@link #isTrustEngineRequired()}
 * is <code>false</code>, then no trust evaluation is performed. This allows use of this implementation 
 * with use cases where, given a particular HttpClient instance, sometimes TLS trust engine evaluation is to 
 * be performed, and sometimes not. The caller is then responsible for ensuring they supply a trust engine or not,
 * as appropriate.
 * </p>
 * 
 * <p>
 * Since this implementation may typically be used with and wrap a "no trust" SSL socket factory,
 * an optional instance of {@link X509HostnameVerifier} may also be supplied.  If supplied, hostname 
 * verification will be performed against the new {@link SSLSocket} via 
 * {@link X509HostnameVerifier#verify(String, SSLSocket)}.
 * </p>
 * 
 * <p>
 * If the client TLS credential context attribute is not populated by the caller, then client TLS is not attempted.
 * </p>
 * 
 * <p>
 * Client TLS support requires use of a compatible {@link javax.net.ssl.KeyManager} implementation configured in the 
 * {@link javax.net.ssl.SSLContext} of the wrapped {@link LayeredConnectionSocketFactory}, such as
 * {@link org.opensaml.security.x509.tls.impl.ThreadLocalX509CredentialKeyManager}.
 * </p>
 * 
 */
public class SecurityEnhancedTLSSocketFactory implements LayeredConnectionSocketFactory {
    
    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(SecurityEnhancedTLSSocketFactory.class);
    
    /** The HttpClient socket factory instance wrapped by this implementation. */
    @Nonnull private LayeredConnectionSocketFactory wrappedFactory;
    
    /** The hostname verifier evaluated by this implementation. */
    @Nullable private X509HostnameVerifier hostnameVerifier;
    
    /** Flag indicating whether a context trust engine attribute is required for TLS server validation. 
     * Default: true. */
    private boolean engineRequired = true;
    
    /**
     * Constructor. 
     * 
     * <p>No hostname verifier is configured in this implementation. (Does not affect whether hostname 
     * is or is not evaluated by the wrapped socket factory).</p>
     * 
     * @param factory the underlying HttpClient socket factory wrapped by this implementation.
     * @param trustEngineRequired flag indicating whether a context trust engine attribute is required 
     *         for TLS server validation.
     */
    public SecurityEnhancedTLSSocketFactory(@Nonnull final LayeredConnectionSocketFactory factory,
            final boolean trustEngineRequired) {
        this(factory, null, trustEngineRequired);
    }
    
    /**
     * Constructor. 
     * 
     * <p>No hostname verifier is configured in this implementation. (Does not affect whether hostname 
     * is or is not evaluated by the wrapped socket factory).</p>
     * 
     * @param factory the underlying HttpClient socket factory wrapped by this implementation.
     */
    public SecurityEnhancedTLSSocketFactory(@Nonnull final LayeredConnectionSocketFactory factory) {
        this(factory, null, true);
    }

    /**
     * Constructor. 
     * 
     * @param factory the underlying HttpClient socket factory wrapped by this implementation.
     * @param verifier the hostname verifier evaluated by this implementation
     */
    public SecurityEnhancedTLSSocketFactory(@Nonnull final LayeredConnectionSocketFactory factory, 
            @Nullable final X509HostnameVerifier verifier) {
        this(factory, verifier, true);
    }
    /**
     * Constructor. 
     * 
     * @param factory the underlying HttpClient socket factory wrapped by this implementation.
     * @param verifier the hostname verifier evaluated by this implementation
     * @param trustEngineRequired flag indicating whether a context trust engine attribute is required 
     *         for TLS server validation.
     */
    public SecurityEnhancedTLSSocketFactory(@Nonnull final LayeredConnectionSocketFactory factory, 
            @Nullable final X509HostnameVerifier verifier, final boolean trustEngineRequired) {
        wrappedFactory = Constraint.isNotNull(factory, "Socket factory was null");
        hostnameVerifier = verifier;
        engineRequired = trustEngineRequired;
    }

    /**
     * Get the flag indicating whether a context trust engine attribute is required for TLS server validation. 
     * 
     * <p>Default: true.</p>
     * 
     * @return true if trust engine is required, false if not
     */
    public boolean isTrustEngineRequired() {
        return engineRequired;
    }

    /** {@inheritDoc} */
    public Socket createSocket(final HttpContext context) throws IOException {
        log.trace("In createSocket");
        return wrappedFactory.createSocket(context);
    }

// CheckStyle: ParameterNumber OFF
    /** {@inheritDoc} */
    public Socket connectSocket(final int connectTimeout, final Socket sock, final HttpHost host,
            final InetSocketAddress remoteAddress, final InetSocketAddress localAddress,
            final HttpContext context) throws IOException {
        
        log.trace("In connectSocket");
        try {
            setup(context);
            final Socket socket = 
                    wrappedFactory.connectSocket(connectTimeout, sock, host, remoteAddress, localAddress, context);
            performTrustEval(socket, host.getHostName(), context);
            performHostnameVerification(socket, host.getHostName(), context);
            return socket;
        } finally {
            teardown(context);
        }
    }
// CheckStyle: ParameterNumber ON

    /** {@inheritDoc} */
    public Socket createLayeredSocket(final Socket socket, final String target, final int port,
            final HttpContext context) throws IOException {
        log.trace("In createLayeredSocket");
        try {
            setup(context);
            final Socket layeredSocket = wrappedFactory.createLayeredSocket(socket, target, port, context);
            performTrustEval(layeredSocket, target, context);
            performHostnameVerification(layeredSocket, target, context);
            return layeredSocket;
        } finally {
            teardown(context);
        }
    }
    
    /**
     * Perform trust evaluation by extracting the server TLS {@link X509Credential} from the 
     * {@link SSLSession} and evaluating it via a
     * {@link TrustEngine}<code>&lt;</code>{@link org.opensaml.security.credential.Credential}<code>&gt;</code>
     * and {@link CriteriaSet} supplied by the caller via the {@link HttpContext}.
     * 
     * @param socket the socket instance being processed
     * @param context the HttpClient context being processed
     * 
     * @throws IOException if the server TLS credential is untrusted, or if there is a fatal error
     *           attempting trust evaluation.
     *           
     * @deprecated use {@link #performTrustEval(Socket, String, HttpContext)}
     */
    @Deprecated
    protected void performTrustEval(@Nonnull final Socket socket, @Nonnull final HttpContext context) 
            throws IOException {
        //TODO when we remove this deprecated method, change called method to @Nonnull for hostname
        DeprecationSupport.warn(ObjectType.METHOD, getClass().getName() + ".performTrustEval(Socket, HttpContext)",
                null, null);
        performTrustEval(socket, null, context);
    }
    
// Checkstyle: ReturnCount OFF
    /**
     * Perform trust evaluation by extracting the server TLS {@link X509Credential} from the 
     * {@link SSLSession} and evaluating it via a
     * {@link TrustEngine}<code>&lt;</code>{@link org.opensaml.security.credential.Credential}<code>&gt;</code>
     * and {@link CriteriaSet} supplied by the caller via the {@link HttpContext}.
     * 
     * @param socket the socket instance being processed
     * @param hostname the hostname being processed
     * @param context the HttpClient context being processed
     * 
     * @throws IOException if the server TLS credential is untrusted, or if there is a fatal error
     *           attempting trust evaluation.
     */
    protected void performTrustEval(@Nonnull final Socket socket, @Nullable final String hostname, 
            @Nonnull final HttpContext context) throws IOException {
        //TODO Really hostname should be @Nonnull, change when we remove deprecated performTrustEval(...)
        
        if (!(socket instanceof SSLSocket)) {
            log.debug("Socket was not an instance of SSLSocket, skipping trust eval");
            return;
        }
        final SSLSocket sslSocket = (SSLSocket) socket;
        
        log.debug("Attempting to evaluate server TLS credential against supplied TrustEngine and CriteriaSet");
        
        @SuppressWarnings("unchecked") final
        TrustEngine<? super X509Credential> trustEngine = (TrustEngine<? super X509Credential>) context.getAttribute(
                HttpClientSecurityConstants.CONTEXT_KEY_TRUST_ENGINE);
        if (trustEngine == null) {
            if (isTrustEngineRequired()) {
                log.warn("The required trust engine was not supplied by the caller, failing socket TLS creation");
                throw new SSLPeerUnverifiedException("The required trust engine was not supplied by the caller");
            } else  {
                log.debug("No trust engine supplied by caller, skipping trust eval");
                return;
            }
        } else {
            log.trace("Saw trust engine of type: {}", trustEngine.getClass().getName());
        }
        
        CriteriaSet criteriaSet = (CriteriaSet) context.getAttribute(
                HttpClientSecurityConstants.CONTEXT_KEY_CRITERIA_SET);
        if (criteriaSet == null) {
            log.debug("No criteria set supplied by caller, building new criteria set with signing " 
                    + "and trusted names criteria");
            criteriaSet = new CriteriaSet(new UsageCriterion(UsageType.SIGNING));
            if (hostname != null) {
                criteriaSet.add(new TrustedNamesCriterion(Collections.singleton(hostname)));
            }
        } else {
            log.trace("Saw CriteriaSet: {}", criteriaSet);
        }

        final X509Credential credential = extractCredential(sslSocket);
        
        try {
            if (trustEngine.validate(credential, criteriaSet)) {
                log.debug("Credential evaluated as trusted");
                context.setAttribute(HttpClientSecurityConstants.CONTEXT_KEY_SERVER_TLS_CREDENTIAL_TRUSTED, 
                        Boolean.TRUE);
            } else {
                log.debug("Credential evaluated as untrusted");
                context.setAttribute(HttpClientSecurityConstants.CONTEXT_KEY_SERVER_TLS_CREDENTIAL_TRUSTED, 
                        Boolean.FALSE);
                throw new SSLPeerUnverifiedException("Trust engine could not establish trust of server TLS credential");
            }
        } catch (final SecurityException e) {
            log.error("Trust engine error evaluating credential", e);
            throw new IOException("Trust engine error evaluating credential", e);
        }
        
    }
// Checkstyle: ReturnCount ON

    /**
     * Extract the server TLS {@link X509Credential} from the supplied {@link SSLSocket}.
     * 
     * @param sslSocket the SSL socket instance to process
     * @return an X509Credential representing the server TLS entity certificate as well as the 
     *          supplied supporting intermediate certificate chain (if any)
     * @throws IOException if credential data can not be extracted from the socket
     */
    @Nonnull protected X509Credential extractCredential(@Nonnull final SSLSocket sslSocket) throws IOException {
        final SSLSession session = sslSocket.getSession();
        final Certificate[] peerCertificates = session.getPeerCertificates();
        if (peerCertificates == null || peerCertificates.length < 1) {
            throw new SSLPeerUnverifiedException("SSLSession peer certificates array was null or empty");
        }
        
        final ArrayList<X509Certificate> certChain = new ArrayList<>();
        for (final Certificate cert : peerCertificates) {
            certChain.add((X509Certificate) cert);
        }
        
        final X509Certificate entityCert = certChain.get(0);
        
        final BasicX509Credential credential = new BasicX509Credential(entityCert);
        credential.setEntityCertificateChain(certChain);
        
        return credential;
    }
    
    /**
     * Perform hostname verification on the connection represented by the supplied socket.
     * 
     * @param socket the socket instance being processed
     * @param hostname the hostname against which to verify
     * @param context the current HttpClient context instance
     * @throws IOException if an I/O error occurs or the verification process fails
     */
    protected void performHostnameVerification(final Socket socket, final String hostname, final HttpContext context)
            throws IOException {
        if (hostnameVerifier != null && socket instanceof SSLSocket) {
            hostnameVerifier.verify(hostname, (SSLSocket) socket);
        }
    }
    
    /**
     * Load the {@link ThreadLocalX509CredentialContext} with the client TLS credential obtained from 
     * the {@link HttpContext}.
     * 
     * @param context the HttpContext instance
     */
    protected void setup(@Nullable final HttpContext context) {
        log.trace("Attempting to setup thread-local client TLS X509Credential");
        if (context == null) {
            log.trace("HttpContext was null, skipping thread-local setup");
            return;
        }
        if (!ThreadLocalX509CredentialContext.haveCurrent()) {
            final X509Credential credential = 
                    (X509Credential) context.getAttribute(
                            HttpClientSecurityConstants.CONTEXT_KEY_CLIENT_TLS_CREDENTIAL);
            if (credential != null) {
                log.trace("Loading ThreadLocalX509CredentialContext with client TLS credential: {}", credential);
                ThreadLocalX509CredentialContext.loadCurrent(credential);
            } else {
                log.trace("HttpContext did not contain a client TLS credential, nothing to do");
            }
        } else {
            log.trace("ThreadLocalX509CredentialContext was already loaded with client TLS credential, skipping setup");
        }
    }
    
    /**
     * Clear the {@link ThreadLocalX509CredentialContext} of the client TLS credential obtained from 
     * the {@link HttpContext}.
     * 
     * @param context the HttpContext instance
     */
    protected void teardown(@Nullable final HttpContext context) {
        log.trace("Clearing thread-local client TLS X509Credential");
        ThreadLocalX509CredentialContext.clearCurrent();
    }

}
