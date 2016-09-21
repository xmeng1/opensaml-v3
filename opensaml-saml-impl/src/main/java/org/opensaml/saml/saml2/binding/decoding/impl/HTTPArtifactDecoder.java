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

package org.opensaml.saml.saml2.binding.decoding.impl;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.servlet.BaseHttpServletRequestXMLMessageDecoder;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.BindingDescriptor;
import org.opensaml.saml.common.binding.EndpointResolver;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.saml.common.binding.impl.DefaultEndpointResolver;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.config.SAMLConfigurationSupport;
import org.opensaml.saml.criterion.ArtifactSourceIDCriterion;
import org.opensaml.saml.criterion.EndpointCriterion;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.metadata.resolver.RoleDescriptorResolver;
import org.opensaml.saml.saml2.binding.artifact.AbstractSAML2Artifact;
import org.opensaml.saml.saml2.binding.artifact.SAML2ArtifactBuilderFactory;
import org.opensaml.saml.saml2.binding.artifact.SAML2ArtifactType0004;
import org.opensaml.saml.saml2.metadata.ArtifactResolutionService;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/** 
 * SAML 2 Artifact Binding decoder, support both HTTP GET and POST.
 * 
 * <strong>NOTE: This decoder is not yet implemented.</strong>
 * */
public class HTTPArtifactDecoder extends BaseHttpServletRequestXMLMessageDecoder<SAMLObject> 
        implements SAMLMessageDecoder {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(HTTPArtifactDecoder.class);

    /** Optional {@link BindingDescriptor} to inject into {@link SAMLBindingContext} created. */
    @Nullable private BindingDescriptor bindingDescriptor;
    
    /** SAML 2 artifact builder factory. */
    @NonnullAfterInit private SAML2ArtifactBuilderFactory artifactBuilderFactory;
    
    /** Resolver for ArtifactResolutionService endpoints. **/
    @NonnullAfterInit private EndpointResolver<ArtifactResolutionService> artifactEndpointResolver;
    
    /** Role descriptor resolver. */
    @NonnullAfterInit private RoleDescriptorResolver roleDescriptorResolver;
    
    /** The peer entity role QName. */
    @NonnullAfterInit private QName peerEntityRole;

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (artifactBuilderFactory == null) {
            artifactBuilderFactory = SAMLConfigurationSupport.getSAML2ArtifactBuilderFactory();
            if (artifactBuilderFactory == null) {
                throw new ComponentInitializationException("Could not obtain a required instance " 
                        + "of SAML2ArtifactBuilderFactory");
            }
        }
        
        if (artifactEndpointResolver == null) {
            artifactEndpointResolver = new DefaultEndpointResolver<>();
        }
        
        if (roleDescriptorResolver == null) {
            //TODO default this?  Need new impl that doesn't require EntityIdCriterion
        }
        
        if (peerEntityRole == null) {
            throw new ComponentInitializationException("Peer entity role cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    protected void doDestroy() {
        super.doDestroy();
        bindingDescriptor = null;
        artifactBuilderFactory = null;
        artifactEndpointResolver = null;
        roleDescriptorResolver = null;
        peerEntityRole = null;
    }

    /**
     * Get the peer entity role {@link QName}.
     * 
     * @return the peer entity role
     */
    @NonnullAfterInit public QName getPeerEntityRole() {
        return peerEntityRole;
    }

    /**
     * Set the peer entity role {@link QName}.
     * 
     * @param role  the peer entity role
     */
    public void setPeerEntityRole(@Nonnull final QName role) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        peerEntityRole = role;
    }

    /**
     * Get the artifact endpoint resolver.
     * 
     * @return the endpoint resolver
     */
    @NonnullAfterInit public EndpointResolver<ArtifactResolutionService> getArtifactEndpointResolver() {
        return artifactEndpointResolver;
    }

    /**
     * Set the artifact endpoint resolver.
     * 
     * @param resolver the new resolver
     */
    public void setArtifactEndpointResolver(@Nullable final EndpointResolver<ArtifactResolutionService> resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        artifactEndpointResolver = resolver;
    }

    /**
     * Get the role descriptor resolver.
     * 
     * <p>
     * Must be capable of resolving descriptors based on {@link ArtifactSourceIDCriterion}.
     * </p>
     * 
     * @return the role descriptor resolver
     */
    @NonnullAfterInit public RoleDescriptorResolver getRoleDescriptorResolver() {
        return roleDescriptorResolver;
    }

    /**
     * Set the role descriptor resolver.
     * 
     * <p>
     * Must be capable of resolving descriptors based on {@link ArtifactSourceIDCriterion}.
     * </p>
     * 
     * @param resolver the role descriptor resolver
     */
    public void setRoleDescriptorResolver(@Nullable final RoleDescriptorResolver resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        roleDescriptorResolver = resolver;
    }

    /**
     * Get the SAML 2 artifact builder factory.
     * 
     * @return the artifact builder factory in use
     */
    @NonnullAfterInit public SAML2ArtifactBuilderFactory getArtifactBuilderFactory() {
        return artifactBuilderFactory;
    }

    /**
     * Set the SAML 2 artifact builder factory.
     * 
     * @param factory the artifact builder factory
     */
    public void setArtifactBuilderFactory(@Nullable final SAML2ArtifactBuilderFactory factory) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        artifactBuilderFactory = factory;
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getBindingURI() {
        return SAMLConstants.SAML2_ARTIFACT_BINDING_URI;
    }

    /**
     * Get an optional {@link BindingDescriptor} to inject into {@link SAMLBindingContext} created.
     * 
     * @return binding descriptor
     */
    @Nullable public BindingDescriptor getBindingDescriptor() {
        return bindingDescriptor;
    }
    
    /**
     * Set an optional {@link BindingDescriptor} to inject into {@link SAMLBindingContext} created.
     * 
     * @param descriptor a binding descriptor
     */
    public void setBindingDescriptor(@Nullable final BindingDescriptor descriptor) {
        bindingDescriptor = descriptor;
    }
    
    /** {@inheritDoc} */
    protected void doDecode() throws MessageDecodingException {
        MessageContext<SAMLObject> messageContext = new MessageContext<>();
        HttpServletRequest request = getHttpServletRequest();

        String relayState = StringSupport.trim(request.getParameter("RelayState"));
        log.debug("Decoded SAML relay state of: {}", relayState);
        SAMLBindingSupport.setRelayState(messageContext, relayState);
        
        processArtifact(messageContext, request);

        populateBindingContext(messageContext);
        
        setMessageContext(messageContext);
    }
    
    /**
     * Process the incoming artifact by decoding the artifacts, dereferencing it from the artifact issuer and 
     * storing the resulting protocol message in the message context.
     * 
     * @param messageContext the message context being processed
     * @param request the HTTP servlet request
     * 
     * @throws MessageDecodingException thrown if there is a problem decoding or dereferencing the artifact
     */
    protected void processArtifact(MessageContext messageContext, HttpServletRequest request) 
            throws MessageDecodingException {
        String encodedArtifact = StringSupport.trimOrNull(request.getParameter("SAMLart"));
        if (encodedArtifact == null) {
            log.error("URL SAMLart parameter was missing or did not contain a value.");
            throw new MessageDecodingException("URL SAMLart parameter was missing or did not contain a value.");
        }
        
        try {
            AbstractSAML2Artifact artifact = parseArtifact(encodedArtifact);

            ArtifactResolutionService ars = resolveArtifactEndpoint(artifact);

            SAMLObject inboundMessage = dereferenceArtifact(artifact, ars);

            messageContext.setMessage(inboundMessage);
        } catch (MessageDecodingException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageDecodingException("Fatal error decoding or resolving inbound artifact", e);
        }
    }
    
    /**
     * @param artifact
     * @param artifactResolveEndpointURL
     * @return
     */
    protected SAMLObject dereferenceArtifact(AbstractSAML2Artifact artifact, ArtifactResolutionService ars) 
            throws MessageDecodingException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param artifact
     * @return
     */
    protected ArtifactResolutionService resolveArtifactEndpoint(AbstractSAML2Artifact artifact) throws MessageDecodingException {
        try {
            RoleDescriptor roleDescriptor = resolveRoleDescriptor(artifact);
            if (roleDescriptor == null) {
                throw new MessageDecodingException("Failed to resolve peer RoleDescriptor based on inbound artifact");
            }
            RoleDescriptorCriterion roleDescriptorCriterion = new RoleDescriptorCriterion(roleDescriptor);
            
            ArtifactResolutionService arsTemplate = 
                    (ArtifactResolutionService) XMLObjectSupport.buildXMLObject(
                            ArtifactResolutionService.DEFAULT_ELEMENT_NAME);
            Integer endpointIndex = SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(artifact.getEndpointIndex());
            arsTemplate.setIndex(endpointIndex);
            arsTemplate.setBinding(SAMLConstants.SAML2_SOAP11_BINDING_URI);
            EndpointCriterion<ArtifactResolutionService> endpointCriterion = 
                    new EndpointCriterion<>(arsTemplate, false);

            CriteriaSet criteriaSet = new CriteriaSet(roleDescriptorCriterion, endpointCriterion);

            ArtifactResolutionService ars = artifactEndpointResolver.resolveSingle(criteriaSet);
            if (ars != null) {
                return ars;
            } else {
                throw new MessageDecodingException("Unable to resolve ArtifactResolutionService endpoint");
            }
        } catch (ResolverException e) {
            throw new MessageDecodingException("Error resolving ArtifactResolutionService to use", e);
        }
    }

    /**
     * @param artifact
     * @return
     */
    protected RoleDescriptor resolveRoleDescriptor(AbstractSAML2Artifact artifact) throws MessageDecodingException {
        //TODO move check and casting up to higher level?
        //TODO move to more extensible model than hardcoded support for type 4?
        if (artifact instanceof SAML2ArtifactType0004) {
            SAML2ArtifactType0004 type4Artifact = (SAML2ArtifactType0004) artifact;
            ArtifactSourceIDCriterion sourceIDCriterion = new ArtifactSourceIDCriterion(type4Artifact.getSourceID());
            
            CriteriaSet criteriaSet = new CriteriaSet(sourceIDCriterion,
                    new ProtocolCriterion(SAMLConstants.SAML20P_NS),
                    new EntityRoleCriterion(getPeerEntityRole()));
            try {
                return roleDescriptorResolver.resolveSingle(criteriaSet);
            } catch (ResolverException e) {
                throw new MessageDecodingException("Error resolving peer entity RoleDescriptor", e);
            }
        } else {
            throw new MessageDecodingException("Saw unsupported artifact type: " + artifact.getClass().getName());
        }
    }

    /**
     * @param encodedArtifact
     * @return
     */
    protected AbstractSAML2Artifact parseArtifact(String encodedArtifact) throws MessageDecodingException {
        return artifactBuilderFactory.buildArtifact(encodedArtifact);
    }

    /**
     * Populate the context which carries information specific to this binding.
     * 
     * @param messageContext the current message context
     */
    protected void populateBindingContext(MessageContext<SAMLObject> messageContext) {
        SAMLBindingContext bindingContext = messageContext.getSubcontext(SAMLBindingContext.class, true);
        bindingContext.setBindingUri(getBindingURI());
        bindingContext.setBindingDescriptor(bindingDescriptor);
        bindingContext.setHasBindingSignature(false);
        bindingContext.setIntendedDestinationEndpointURIRequired(false);
    }

}