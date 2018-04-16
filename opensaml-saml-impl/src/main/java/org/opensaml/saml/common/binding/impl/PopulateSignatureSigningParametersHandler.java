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

package org.opensaml.saml.common.binding.impl;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.handler.AbstractMessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.xmlsec.SecurityConfigurationSupport;
import org.opensaml.xmlsec.SignatureSigningConfiguration;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.SignatureSigningParametersResolver;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.criterion.SignatureSigningConfigurationCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Handler that resolves and populates {@link SignatureSigningParameters} on a {@link SecurityParametersContext}
 * created/accessed via a lookup function, by default as an immediate child context of the target
 * {@link MessageContext}.
 */
public class PopulateSignatureSigningParametersHandler extends AbstractMessageHandler {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateSignatureSigningParametersHandler.class);
    
    /** Strategy used to look up the {@link SecurityParametersContext} to set the parameters for. */
    @Nonnull private Function<MessageContext,SecurityParametersContext> securityParametersContextLookupStrategy;

    /** Strategy used to look up an existing {@link SecurityParametersContext} to copy. */
    @Nullable private Function<MessageContext,SecurityParametersContext> existingParametersContextLookupStrategy;
    
    /** Strategy used to look up a per-request {@link SignatureSigningConfiguration} list. */
    @NonnullAfterInit
    private Function<MessageContext,List<SignatureSigningConfiguration>> configurationLookupStrategy;

    /** Strategy used to look up a SAML metadata context. */
    @Nullable private Function<MessageContext,SAMLMetadataContext> metadataContextLookupStrategy;
    
    /** Resolver for parameters to store into context. */
    @NonnullAfterInit private SignatureSigningParametersResolver resolver;
    
    /**
     * Constructor.
     */
    public PopulateSignatureSigningParametersHandler() {
        // Create context by default.
        securityParametersContextLookupStrategy = new ChildContextLookup<>(SecurityParametersContext.class, true);

        // Default: msg context -> SAMLPeerEntityContext -> SAMLMetadataContext
        metadataContextLookupStrategy = Functions.compose(
                new ChildContextLookup<SAMLPeerEntityContext,SAMLMetadataContext>(SAMLMetadataContext.class),
                new ChildContextLookup<MessageContext,SAMLPeerEntityContext>(SAMLPeerEntityContext.class));
    }

    /**
     * Set the strategy used to look up the {@link SecurityParametersContext} to set the parameters for.
     * 
     * @param strategy lookup strategy
     */
    public void setSecurityParametersContextLookupStrategy(
            @Nonnull final Function<MessageContext,SecurityParametersContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        securityParametersContextLookupStrategy = Constraint.isNotNull(strategy,
                "SecurityParametersContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to look up an existing {@link SecurityParametersContext} to copy instead
     * of actually resolving the parameters to set.
     * 
     * @param strategy lookup strategy
     */
    public void setExistingParametersContextLookupStrategy(
            @Nullable final Function<MessageContext,SecurityParametersContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        existingParametersContextLookupStrategy = strategy;
    }
    
    /**
     * Set lookup strategy for {@link SAMLMetadataContext} for input to resolution.
     * 
     * @param strategy  lookup strategy
     */
    public void setMetadataContextLookupStrategy(
            @Nullable final Function<MessageContext,SAMLMetadataContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        metadataContextLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to look up a per-request {@link SignatureSigningConfiguration} list.
     * 
     * @param strategy lookup strategy
     */
    public void setConfigurationLookupStrategy(
            @Nonnull final Function<MessageContext,List<SignatureSigningConfiguration>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        configurationLookupStrategy = Constraint.isNotNull(strategy,
                "SignatureSigningConfiguration lookup strategy cannot be null");
    }
    
    /**
     * Set the resolver to use for the parameters to store into the context.
     * 
     * @param newResolver   resolver to use
     */
    public void setSignatureSigningParametersResolver(
            @Nonnull final SignatureSigningParametersResolver newResolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        resolver = Constraint.isNotNull(newResolver, "SignatureSigningParametersResolver cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (resolver == null) {
            throw new ComponentInitializationException("SignatureSigningParametersResolver cannot be null");
        } else if (configurationLookupStrategy == null) {
            configurationLookupStrategy = new Function<MessageContext,List<SignatureSigningConfiguration>>() {
                public List<SignatureSigningConfiguration> apply(final MessageContext input) {
                    return Collections.singletonList(
                            SecurityConfigurationSupport.getGlobalSignatureSigningConfiguration());
                }
            };
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreInvoke(@Nonnull final MessageContext messageContext) throws MessageHandlerException {
        
        if (super.doPreInvoke(messageContext)) {
            log.debug("{} Signing enabled", getLogPrefix());
            return true;
        } else {
            log.debug("{} Signing not enabled", getLogPrefix());
            return false;
        }
    }
    
// Checkstyle: CyclomaticComplexity|ReturnCount OFF
    /** {@inheritDoc} */
    @Override
    protected void doInvoke(@Nonnull final MessageContext messageContext) throws MessageHandlerException {

        log.debug("{} Resolving SignatureSigningParameters for request", getLogPrefix());
        
        final SecurityParametersContext paramsCtx =
                securityParametersContextLookupStrategy.apply(messageContext);
        if (paramsCtx == null) {
            log.debug("{} No SecurityParametersContext returned by lookup strategy", getLogPrefix());
            throw new MessageHandlerException("No SecurityParametersContext returned by lookup strategy");
        }
        
        if (existingParametersContextLookupStrategy != null) {
            final SecurityParametersContext existingCtx =
                    existingParametersContextLookupStrategy.apply(messageContext);
            if (existingCtx != null && existingCtx.getSignatureSigningParameters() != null) {
                log.debug("{} Found existing SecurityParametersContext to copy from", getLogPrefix());
                paramsCtx.setSignatureSigningParameters(existingCtx.getSignatureSigningParameters());
                return;
            }
        }
        
        final List<SignatureSigningConfiguration> configs = configurationLookupStrategy.apply(messageContext);
        if (configs == null || configs.isEmpty()) {
            log.error("{} No SignatureSigningConfiguration returned by lookup strategy", getLogPrefix());
            throw new MessageHandlerException("No SignatureSigningConfiguration returned by lookup strategy");
        }
        
        final CriteriaSet criteria = new CriteriaSet(new SignatureSigningConfigurationCriterion(configs));
        
        if (metadataContextLookupStrategy != null) {
            final SAMLMetadataContext metadataCtx = metadataContextLookupStrategy.apply(messageContext);
            if (metadataCtx != null && metadataCtx.getRoleDescriptor() != null) {
                log.debug("{} Adding metadata to resolution criteria for signing/digest algorithms", getLogPrefix());
                criteria.add(new RoleDescriptorCriterion(metadataCtx.getRoleDescriptor()));
            }
        }
        
        try {
            final SignatureSigningParameters params = resolver.resolveSingle(criteria);
            paramsCtx.setSignatureSigningParameters(params);
            log.debug("{} {} SignatureSigningParameters", getLogPrefix(),
                    params != null ? "Resolved" : "Failed to resolve");
        } catch (final ResolverException e) {
            log.error("{} Error resolving SignatureSigningParameters", getLogPrefix(), e);
            throw new MessageHandlerException("Error resolving SignatureSigningParameters", e);
        }
    }
// Checkstyle: CyclomaticComplexity|ReturnCount ON
    
}