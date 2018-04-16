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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSource;
import org.opensaml.security.httpclient.HttpClientSecurityConstants;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.HttpClientSecuritySupport;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.X509Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.net.MediaTypeSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Abstract subclass for dynamic metadata resolvers that implement metadata resolution based on HTTP requests.
 */
public abstract class AbstractDynamicHTTPMetadataResolver extends AbstractDynamicMetadataResolver {
    
    /** Default list of supported content MIME types. */
    public static final String[] DEFAULT_CONTENT_TYPES = 
            new String[] {"application/samlmetadata+xml", "application/xml", "text/xml"};
    
    /** MDC attribute representing the current request URI. Will be available during the execution of the 
     * configured {@link ResponseHandler}. */
    public static final String MDC_ATTRIB_CURRENT_REQUEST_URI = 
            AbstractDynamicHTTPMetadataResolver.class.getName() + ".currentRequestURI";
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractDynamicHTTPMetadataResolver.class);
    
    /** HTTP Client used to pull the metadata. */
    @Nonnull private HttpClient httpClient;
    
    /** List of supported MIME types for use in Accept request header and validation of 
     * response Content-Type header.*/
    @NonnullAfterInit private List<String> supportedContentTypes;
    
    /** Generated Accept request header value. */
    @NonnullAfterInit private String supportedContentTypesValue;
    
    /**Supported {@link MediaType} instances, constructed from the {@link #supportedContentTypes} list. */
    @NonnullAfterInit private Set<MediaType> supportedMediaTypes;
    
    /** HttpClient ResponseHandler instance to use. */
    @Nonnull private ResponseHandler<XMLObject> responseHandler;
    
    /** HttpClient credentials provider. 
     * @deprecated use {@link #httpClientSecurityParameters}.
     * */
    @Nullable private CredentialsProvider credentialsProvider;
    
    /** Optional trust engine used in evaluating server TLS credentials.
     * @deprecated use {@link #httpClientSecurityParameters}.
     *  */
    @Nullable private TrustEngine<? super X509Credential> tlsTrustEngine;
    
    /** Optional HttpClient security parameters.*/
    @Nullable private HttpClientSecurityParameters httpClientSecurityParameters;
    
    /**
     * Constructor.
     *
     * @param client the instance of {@link HttpClient} used to fetch remote metadata
     */
    public AbstractDynamicHTTPMetadataResolver(@Nonnull final HttpClient client) {
        this(null, client);
    }
    
    /**
     * Constructor.
     *
     * @param backgroundTaskTimer the {@link Timer} instance used to run resolver background managment tasks
     * @param client the instance of {@link HttpClient} used to fetch remote metadata
     */
    public AbstractDynamicHTTPMetadataResolver(@Nullable final Timer backgroundTaskTimer, 
            @Nonnull final HttpClient client) {
        super(backgroundTaskTimer);
        
        httpClient = Constraint.isNotNull(client, "HttpClient may not be null");
        
        // The default handler
        responseHandler = new BasicMetadataResponseHandler();
    }
    
    /**
     * Sets the optional trust engine used in evaluating server TLS credentials.
     * 
     * <p>
     * See TLS socket factory requirements documented for 
     * {@link #setHttpClientSecurityParameters(HttpClientSecurityParameters)}.
     * </p>
     * 
     * @param engine the trust engine instance to use
     * 
     * @deprecated use {@link #setHttpClientSecurityParameters(HttpClientSecurityParameters)}
     */
    public void setTLSTrustEngine(@Nullable final TrustEngine<? super X509Credential> engine) {
        tlsTrustEngine = engine;
    }
    
    /**
     * Set an instance of {@link CredentialsProvider} used for authentication by the HttpClient instance.
     * 
     * @param provider the credentials provider
     * 
     * @deprecated use {@link #setHttpClientSecurityParameters(HttpClientSecurityParameters)}
     */
    public void setCredentialsProvider(@Nullable final CredentialsProvider provider) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        credentialsProvider = provider;
    }
    
    /**
     * A convenience method to set a (single) username and password used to access metadata. 
     * To disable BASIC authentication pass null for the credentials instance.
     * 
     * <p>
     * An {@link AuthScope} will be generated which specifies any host, port, scheme and realm.
     * </p>
     * 
     * <p>To specify multiple usernames and passwords for multiple host, port, scheme, and realm combinations, instead 
     * provide an instance of {@link CredentialsProvider} via {@link #setCredentialsProvider(CredentialsProvider)}.</p>
     * 
     * @param credentials the username and password credentials
     * 
     * @deprecated use {@link #setHttpClientSecurityParameters(HttpClientSecurityParameters)}
     */
    public void setBasicCredentials(@Nullable final UsernamePasswordCredentials credentials) {
        setBasicCredentialsWithScope(credentials, null);
    }

    /**
     * A convenience method to set a (single) username and password used to access metadata.
     * To disable BASIC authentication pass null for the credentials instance.
     * 
     * <p>
     * If the <code>authScope</code> is null, an {@link AuthScope} will be generated which specifies
     * any host, port, scheme and realm.
     * </p>
     * 
     * <p>To specify multiple usernames and passwords for multiple host, port, scheme, and realm combinations, instead 
     * provide an instance of {@link CredentialsProvider} via {@link #setCredentialsProvider(CredentialsProvider)}.</p>
     * 
     * @param credentials the username and password credentials
     * @param scope the HTTP client auth scope with which to scope the credentials, may be null
     * 
     * @deprecated use {@link #setHttpClientSecurityParameters(HttpClientSecurityParameters)}
     */
    public void setBasicCredentialsWithScope(@Nullable final UsernamePasswordCredentials credentials,
            @Nullable final AuthScope scope) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        if (credentials != null) {
            AuthScope authScope = scope;
            if (authScope == null) {
                authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
            }
            final BasicCredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(authScope, credentials);
            credentialsProvider = provider;
        } else {
            log.debug("{} Either username or password were null, disabling basic auth", getLogPrefix());
            credentialsProvider = null;
        }

    }
    
    /**
     * Get the instance of {@link HttpClientSecurityParameters} which provides various parameters to influence
     * the security behavior of the HttpClient instance.
     * 
     * @return the parameters instance, or null
     */
    @Nullable protected HttpClientSecurityParameters getHttpClientSecurityParameters() {
        return httpClientSecurityParameters;
    }
    
    /**
     * Set an instance of {@link HttpClientSecurityParameters} which provides various parameters to influence
     * the security behavior of the HttpClient instance.
     * 
     * <p>
     * For all TLS-related parameters, must be used in conjunction with an HttpClient instance 
     * which is configured with either a:
     * <ul>
     * <li>
     * a {@link net.shibboleth.utilities.java.support.httpclient.TLSSocketFactory}
     * </li>
     * <li>
     * {@link org.opensaml.security.httpclient.impl.SecurityEnhancedTLSSocketFactory} which wraps
     * an instance of {@link net.shibboleth.utilities.java.support.httpclient.TLSSocketFactory}, with
     * the latter likely configured in a "no trust" configuration.  This variant is required if either a
     * trust engine or a client TLS credential is to be used.
     * </li>
     * For convenience methods for building a 
     * {@link net.shibboleth.utilities.java.support.httpclient.TLSSocketFactory}, 
     * see {@link net.shibboleth.utilities.java.support.httpclient.HttpClientSupport}.
     * </ul>
     * If the appropriate TLS socket factory is not configured and a trust engine is specified,
     * then this will result in no TLS trust evaluation being performed and a 
     * {@link ResolverException} will ultimately be thrown.
     * </p>
     * @param params the security parameters
     */
    public void setHttpClientSecurityParameters(@Nullable final HttpClientSecurityParameters params) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        httpClientSecurityParameters = params;
    }
    
    /**
     * Get the list of supported MIME {@link MediaType} instances used in validation of 
     * the response Content-Type header.
     * 
     * <p>
     * Is generated at init time from {@link #getSupportedContentTypes()}.
     * </p>
     * 
     * @return the supported content types
     */
    @NonnullAfterInit @NotLive @Unmodifiable
    protected Set<MediaType> getSupportedMediaTypes() {
        return supportedMediaTypes;
    }

    /**
     * Get the list of supported MIME types for use in Accept request header and validation of 
     * response Content-Type header.
     * 
     * @return the supported content types
     */
    @NonnullAfterInit @NotLive @Unmodifiable
    public List<String> getSupportedContentTypes() {
        return supportedContentTypes;
    }

    /**
     * Set the list of supported MIME types for use in Accept request header and validation of 
     * response Content-Type header. Values will be effectively lower-cased at runtime.
     * 
     * @param types the new supported content types to set
     */
    public void setSupportedContentTypes(@Nullable final List<String> types) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        if (types == null) {
            supportedContentTypes = Collections.emptyList();
        } else {
            supportedContentTypes = new ArrayList<>(Collections2.transform(
                    StringSupport.normalizeStringCollection(types),
                    new Function<String,String>() {
                        @Override
                        @Nullable public String apply(@Nullable final String input) {
                            return input == null ? null : input.toLowerCase();
                        }
                    }
                    ));
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void initMetadataResolver() throws ComponentInitializationException {
        super.initMetadataResolver();
        
        if (getSupportedContentTypes() == null) {
            setSupportedContentTypes(Arrays.asList(DEFAULT_CONTENT_TYPES));
        }
        
        if (! getSupportedContentTypes().isEmpty()) {
            supportedContentTypesValue = StringSupport.listToStringValue(getSupportedContentTypes(), ", ");
            supportedMediaTypes = new LazySet<>();
            for (final String contentType : getSupportedContentTypes()) {
                supportedMediaTypes.add(MediaType.parse(contentType));
            }
        } else {
            supportedMediaTypes = Collections.emptySet();
        }
        
        log.debug("{} Supported content types are: {}", getLogPrefix(), getSupportedContentTypes());
    }
    
   /** {@inheritDoc} */
    @Override
    protected void doDestroy() {
        httpClient = null;
        credentialsProvider = null;
        tlsTrustEngine = null;
        httpClientSecurityParameters = null;
        
        supportedContentTypes = null;
        supportedContentTypesValue = null;
        supportedMediaTypes = null;
        
        super.doDestroy();
    }
    
    /** {@inheritDoc} */
    @Override
    @Nullable protected XMLObject fetchFromOriginSource(@Nonnull final CriteriaSet criteria) 
            throws IOException {
            
        final HttpUriRequest request = buildHttpRequest(criteria);
        if (request == null) {
            log.debug("{} Could not build request based on input criteria, unable to query", getLogPrefix());
            return null;
        }
        
        final HttpClientContext context = buildHttpClientContext(request);
        
        try {
            MDC.put(MDC_ATTRIB_CURRENT_REQUEST_URI, request.getURI().toString());
            final XMLObject result = httpClient.execute(request, responseHandler, context);
            HttpClientSecuritySupport.checkTLSCredentialEvaluated(context, request.getURI().getScheme());
            return result;
        } finally {
            MDC.remove(MDC_ATTRIB_CURRENT_REQUEST_URI);
        }
    }
    
    /**
     * Check that trust engine evaluation of the server TLS credential was actually performed.
     * 
     * @param context the current HTTP context instance in use
     * @param request the HTTP URI request
     * @throws SSLPeerUnverifiedException thrown if the TLS credential was not actually evaluated by the trust engine
     * 
     * @deprecated use {@link HttpClientSecuritySupport#checkTLSCredentialEvaluated(HttpClientContext, String)}
     */
    @Deprecated
    protected void checkTLSCredentialTrusted(final HttpClientContext context, final HttpUriRequest request) 
            throws SSLPeerUnverifiedException {
        HttpClientSecuritySupport.checkTLSCredentialEvaluated(context, request.getURI().getScheme());
    }
    
    /**
     * Build an appropriate instance of {@link HttpUriRequest} based on the input criteria set.
     * 
     * @param criteria the input criteria set
     * @return the newly constructed request, or null if it can not be built from the supplied criteria
     */
    @Nullable protected HttpUriRequest buildHttpRequest(@Nonnull final CriteriaSet criteria) {
        final String url = buildRequestURL(criteria);
        log.debug("{} Built request URL of: {}", getLogPrefix(), url);
        
        if (url == null) {
            log.debug("{} Could not construct request URL from input criteria, unable to query", getLogPrefix());
            return null;
        }
            
        final HttpGet getMethod = new HttpGet(url);
        
        if (!Strings.isNullOrEmpty(supportedContentTypesValue)) {
            getMethod.addHeader("Accept", supportedContentTypesValue);
        }
        
        // TODO other headers ?
        
        return getMethod;
    }

    /**
     * Build the request URL based on the input criteria set.
     * 
     * @param criteria the input criteria set
     * @return the request URL, or null if it can not be built based on the supplied criteria
     */
    @Nullable protected abstract String buildRequestURL(@Nonnull final CriteriaSet criteria);
    
    /**
     * Build the {@link HttpClientContext} instance which will be used to invoke the {@link HttpClient} request.
     * 
     * @return a new instance of {@link HttpClientContext}
     * 
     * @deprecated use {@link #buildHttpClientContext(HttpUriRequest)}
     */
    protected HttpClientContext buildHttpClientContext() {
        //TODO when we remove this deprecated method, change called method to @Nonnull for request
        DeprecationSupport.warn(ObjectType.METHOD, getClass().getName() + ".buildHttpClientContext()", null, null);
        return buildHttpClientContext(null);
    }
    
    /**
     * Build the {@link HttpClientContext} instance which will be used to invoke the {@link HttpClient} request.
     * 
     * @param request the current HTTP request
     * 
     * @return a new instance of {@link HttpClientContext}
     */
    protected HttpClientContext buildHttpClientContext(@Nullable final HttpUriRequest request) {
        // TODO Really request should be @Nonnull, change when we remove deprecated buildHttpClientContext()
        final HttpClientContext context = HttpClientContext.create();
        
        HttpClientSecuritySupport.marshalSecurityParameters(context, httpClientSecurityParameters, true);
        
        // If these legacy values are present, let them override the above params instance values unconditionally
        if (credentialsProvider != null) {
            context.setCredentialsProvider(credentialsProvider);
        }
        if (tlsTrustEngine != null) {
            context.setAttribute(HttpClientSecurityConstants.CONTEXT_KEY_TRUST_ENGINE, tlsTrustEngine);
        }
        
        if (request != null) {
            HttpClientSecuritySupport.addDefaultTLSTrustEngineCriteria(context, request);
        }
        
        return context;
    }
    
    /**
     * Basic HttpClient response handler for processing metadata fetch requests.
     */
    public class BasicMetadataResponseHandler implements ResponseHandler<XMLObject> {

        /** {@inheritDoc} */
        @Override
        public XMLObject handleResponse(@Nonnull final HttpResponse response) throws IOException {
            
            final int httpStatusCode = response.getStatusLine().getStatusCode();
            
            final String currentRequestURI = MDC.get(MDC_ATTRIB_CURRENT_REQUEST_URI);
            
            // TODO should we be seeing/doing this? Probably not if we don't do conditional GET.
            // But we will if we do pre-emptive refreshing of metadata in background thread.
            if (httpStatusCode == HttpStatus.SC_NOT_MODIFIED) {
                log.debug("{} Metadata document from '{}' has not changed since last retrieval", 
                        getLogPrefix(), currentRequestURI);
                return null;
            }

            if (httpStatusCode != HttpStatus.SC_OK) {
                log.warn("{} Non-ok status code '{}' returned from remote metadata source: {}", 
                        getLogPrefix(), httpStatusCode, currentRequestURI);
                return null;
            }
            
            try {
                validateHttpResponse(response);
            } catch (final ResolverException e) {
                log.error("{} Problem validating dynamic metadata HTTP response", getLogPrefix(), e);
                return null;
            }
            
            try {
                final InputStream ins = response.getEntity().getContent();
                final byte[] source = ByteStreams.toByteArray(ins);
                try (ByteArrayInputStream bais = new ByteArrayInputStream(source)) {
                    final XMLObject xmlObject = unmarshallMetadata(bais);
                    xmlObject.getObjectMetadata().put(new XMLObjectSource(source));
                    return xmlObject;
                }
            } catch (final IOException | UnmarshallingException e) {
                log.error("{} Error unmarshalling HTTP response stream", getLogPrefix(), e);
                return null;
            }
                
        }
        
        /**
         * Validate the received HTTP response instance, such as checking for supported content types.
         * 
         * @param response the received response
         * @throws ResolverException if the response was not valid, or if there is a fatal error validating the response
         */
        protected void validateHttpResponse(@Nonnull final HttpResponse response) throws ResolverException {
            if (!getSupportedMediaTypes().isEmpty()) {
                String contentTypeValue = null;
                final Header contentType = response.getEntity().getContentType();
                if (contentType != null && contentType.getValue() != null) {
                    contentTypeValue = StringSupport.trimOrNull(contentType.getValue());
                }
                log.debug("{} Saw raw Content-Type from response header '{}'", getLogPrefix(), contentTypeValue);
                
                if (!MediaTypeSupport.validateContentType(contentTypeValue, getSupportedMediaTypes(), true, false)) {
                    throw new ResolverException("HTTP response specified an unsupported Content-Type MIME type: " 
                            + contentTypeValue);
                }
            }
        }
            
    }

}