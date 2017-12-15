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

package org.opensaml.security.messaging.impl;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.handler.AbstractMessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.security.httpclient.HttpClientSecurityConfiguration;
import org.opensaml.security.httpclient.HttpClientSecurityConfigurationCriterion;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.HttpClientSecurityParametersResolver;
import org.opensaml.security.httpclient.impl.BasicHttpClientSecurityConfiguration;
import org.opensaml.security.messaging.HttpClientSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Handler that resolves and populates {@link HttpClientSecurityParameters} on a {@link HttpClientSecurityContext}
 * created/accessed via a lookup function, by default as an immediate child context of the target
 * {@link MessageContext}.
 */
public class PopulateHttpClientSecurityParametersHandler extends AbstractMessageHandler {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateHttpClientSecurityParametersHandler.class);
    
    /** Strategy used to look up the {@link HttpClientSecurityContext} to set the parameters for. */
    @Nonnull private Function<MessageContext,HttpClientSecurityContext> securityParametersContextLookupStrategy;

    /** Strategy used to look up an existing {@link HttpClientSecurityContext} to copy. */
    @Nullable private Function<MessageContext,HttpClientSecurityContext> existingParametersContextLookupStrategy;
    
    /** Strategy used to look up a per-request {@link HttpClientSecurityConfiguration} list. */
    @NonnullAfterInit
    private Function<MessageContext,List<HttpClientSecurityConfiguration>> configurationLookupStrategy;

    /** Resolver for parameters to store into context. */
    @NonnullAfterInit private HttpClientSecurityParametersResolver resolver;
    
    /**
     * Constructor.
     */
    public PopulateHttpClientSecurityParametersHandler() {
        // Create context by default.
        securityParametersContextLookupStrategy = new ChildContextLookup<>(HttpClientSecurityContext.class, true);
    }

    /**
     * Set the strategy used to look up the {@link HttpClientSecurityContext} to set the parameters for.
     * 
     * @param strategy lookup strategy
     */
    public void setSecurityParametersContextLookupStrategy(
            @Nonnull final Function<MessageContext,HttpClientSecurityContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        securityParametersContextLookupStrategy = Constraint.isNotNull(strategy,
                "HttpClientSecurityContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to look up an existing {@link HttpClientSecurityContext} to copy instead
     * of actually resolving the parameters to set.
     * 
     * @param strategy lookup strategy
     */
    public void setExistingParametersContextLookupStrategy(
            @Nullable final Function<MessageContext,HttpClientSecurityContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        existingParametersContextLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to look up a per-request {@link HttpClientSecurityConfiguration} list.
     * 
     * @param strategy lookup strategy
     */
    public void setConfigurationLookupStrategy(
            @Nonnull final Function<MessageContext,List<HttpClientSecurityConfiguration>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        configurationLookupStrategy = Constraint.isNotNull(strategy,
                "HttpClientSecurityConfiguration lookup strategy cannot be null");
    }
    
    /**
     * Set the resolver to use for the parameters to store into the context.
     * 
     * @param newResolver   resolver to use
     */
    public void setHttpClientSecurityParametersResolver(
            @Nonnull final HttpClientSecurityParametersResolver newResolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        resolver = Constraint.isNotNull(newResolver, "HttpClientSecurityParametersResolver cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (resolver == null) {
            throw new ComponentInitializationException("HttpClientSecurityParametersResolver cannot be null");
        } else if (configurationLookupStrategy == null) {
            configurationLookupStrategy = new Function<MessageContext,List<HttpClientSecurityConfiguration>>() {
                public List<HttpClientSecurityConfiguration> apply(final MessageContext input) {
                    // TODO should we have a library global default somewhere? Probably not.  Only TLS TrustEngine
                    // is semi-required (depending on usage), and that can't be defaulted anyway.
                    //
                    // Just return an empty instance to satisfy contract requirements.
                    return Collections.<HttpClientSecurityConfiguration>singletonList(
                            new BasicHttpClientSecurityConfiguration());
                }
            };
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreInvoke(@Nonnull final MessageContext messageContext) throws MessageHandlerException {
        
        if (super.doPreInvoke(messageContext)) {
            log.debug("{} HttpClient security enabled", getLogPrefix());
            return true;
        } else {
            log.debug("{} HttpClient security not enabled", getLogPrefix());
            return false;
        }
    }
    
// Checkstyle: CyclomaticComplexity|ReturnCount OFF
    /** {@inheritDoc} */
    @Override
    protected void doInvoke(@Nonnull final MessageContext messageContext) throws MessageHandlerException {

        log.debug("{} Resolving HttpClientSecurityParameters for request", getLogPrefix());
        
        final HttpClientSecurityContext paramsCtx =
                securityParametersContextLookupStrategy.apply(messageContext);
        if (paramsCtx == null) {
            log.debug("{} No HttpClientSecurityContext returned by lookup strategy", getLogPrefix());
            throw new MessageHandlerException("No HttpClientSecurityContext returned by lookup strategy");
        }
        
        if (existingParametersContextLookupStrategy != null) {
            final HttpClientSecurityContext existingCtx =
                    existingParametersContextLookupStrategy.apply(messageContext);
            if (existingCtx != null && existingCtx.getSecurityParameters() != null) {
                log.debug("{} Found existing HttpClientSecurityContext to copy from", getLogPrefix());
                paramsCtx.setSecurityParameters(existingCtx.getSecurityParameters());
                return;
            }
        }
        
        final List<HttpClientSecurityConfiguration> configs = configurationLookupStrategy.apply(messageContext);
        if (configs == null || configs.isEmpty()) {
            log.error("{} No HttpClientSecurityConfiguration returned by lookup strategy", getLogPrefix());
            throw new MessageHandlerException("No HttpClientSecurityConfiguration returned by lookup strategy");
        }
        
        final CriteriaSet criteria = new CriteriaSet(new HttpClientSecurityConfigurationCriterion(configs));
        
        try {
            final HttpClientSecurityParameters params = resolver.resolveSingle(criteria);
            paramsCtx.setSecurityParameters(params);
            log.debug("{} {} HttpClientSecurityParameters", getLogPrefix(),
                    params != null ? "Resolved" : "Failed to resolve");
        } catch (final ResolverException e) {
            log.error("{} Error resolving HttpClientSecurityParameters", getLogPrefix(), e);
            throw new MessageHandlerException("Error resolving HttpClientSecurityParameters", e);
        }
    }
// Checkstyle: CyclomaticComplexity|ReturnCount ON
    
}