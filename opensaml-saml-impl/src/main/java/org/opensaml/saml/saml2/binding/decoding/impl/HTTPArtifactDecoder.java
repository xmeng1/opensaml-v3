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

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.servlet.BaseHttpServletRequestXMLMessageDecoder;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.BindingDescriptor;
import org.opensaml.saml.common.binding.EndpointResolver;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.binding.artifact.SAMLSourceLocationArtifact;
import org.opensaml.saml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.saml.common.binding.impl.DefaultEndpointResolver;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.config.SAMLConfigurationSupport;
import org.opensaml.saml.criterion.ArtifactCriterion;
import org.opensaml.saml.criterion.EndpointCriterion;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.metadata.resolver.RoleDescriptorResolver;
import org.opensaml.saml.saml2.binding.artifact.SAML2Artifact;
import org.opensaml.saml.saml2.binding.artifact.SAML2ArtifactBuilderFactory;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.metadata.ArtifactResolutionService;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.security.SecurityException;
import org.opensaml.soap.client.SOAPClient;
import org.opensaml.soap.common.SOAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.SecureRandomIdentifierGenerationStrategy;

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
    
    /** SOAP client. */
    private SOAPClient soapClient;
    
    /** Identifier generation strategy. */
    private IdentifierGenerationStrategy idStrategy;

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (roleDescriptorResolver == null) {
            throw new ComponentInitializationException("RoleDescriptorResolver cannot be null");
        }
        
        if (peerEntityRole == null) {
            throw new ComponentInitializationException("Peer entity role cannot be null");
        }
        
        if (soapClient == null) {
            throw new ComponentInitializationException("SOAPClient cannot be null");
        }
        
        if (idStrategy == null) {
            idStrategy = new SecureRandomIdentifierGenerationStrategy();
        }
        
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
        
    }
    
    /** {@inheritDoc} */
    protected void doDestroy() {
        super.doDestroy();
        bindingDescriptor = null;
        artifactBuilderFactory = null;
        artifactEndpointResolver = null;
        roleDescriptorResolver = null;
        peerEntityRole = null;
        soapClient = null;
        idStrategy = null;
    }

    /**
     * Get the identifier generation strategy.
     * 
     * @return Returns the identifier generation strategy
     */
    @NonnullAfterInit public IdentifierGenerationStrategy getIdentifierGenerationStrategy() {
        return idStrategy;
    }

    /**
     * Set the identifier generation strategy.
     * 
     * @param strategy the identifier generation strategy
     */
    public void setIdentifierGenerationStrategy(@Nullable final IdentifierGenerationStrategy strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        idStrategy = strategy;
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
     * Must be capable of resolving descriptors based on {@link ArtifactCriterion}.
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
     * Must be capable of resolving descriptors based on {@link ArtifactCriterion}.
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

    /**
     * Get the SOAP client instance.
     * 
     * @return the SOAP client
     */
    @NonnullAfterInit public SOAPClient getSOAPClient() {
        return soapClient;
    }

    /**
     * Set the SOAP client instance.
     * 
     * @param client the SOAP client
     */
    public void setSOAPClient(@Nonnull final SOAPClient client) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        soapClient = client;
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
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
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
    private void processArtifact(MessageContext messageContext, HttpServletRequest request) 
            throws MessageDecodingException {
        String encodedArtifact = StringSupport.trimOrNull(request.getParameter("SAMLart"));
        if (encodedArtifact == null) {
            log.error("URL SAMLart parameter was missing or did not contain a value.");
            throw new MessageDecodingException("URL SAMLart parameter was missing or did not contain a value.");
        }
        
        try {
            SAML2Artifact artifact = parseArtifact(encodedArtifact);
            
            RoleDescriptor peerRoleDescriptor = resolvePeerRoleDescriptor(artifact);
            if (peerRoleDescriptor == null) {
                throw new MessageDecodingException("Failed to resolve peer RoleDescriptor based on inbound artifact");
            }

            ArtifactResolutionService ars = resolveArtifactEndpoint(artifact, peerRoleDescriptor);

            SAMLObject inboundMessage = dereferenceArtifact(artifact, peerRoleDescriptor, ars);

            messageContext.setMessage(inboundMessage);
        } catch (MessageDecodingException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageDecodingException("Fatal error decoding or resolving inbound artifact", e);
        }
    }
    
    /**
     * @param artifact
     * @param peerRoleDescriptor 
     * @param artifactResolveEndpointURL
     * @return
     */
    private SAMLObject dereferenceArtifact(SAML2Artifact artifact, RoleDescriptor peerRoleDescriptor, ArtifactResolutionService ars) 
            throws MessageDecodingException {
        
        MessageContext<SAMLObject> outbound = new MessageContext<>();
        outbound.setMessage(buildArtifactResolveRequestMessage(artifact, ars.getLocation(), peerRoleDescriptor));
        //TODO more population of context
        //  - signing params
        //  - client TLS params
        //  - setting up stuff for handling response
        //TODO what components needed to support signing and client TLS, and how do we get them?
        //TODO probably support optional static injected creds and params, as well as injected resolution strategies
        
        InOutOperationContext<SAMLObject, SAMLObject> opContext = new InOutOperationContext<>(null, outbound);
        
        try {
            log.trace("Executing ArtifactResolve over SOAP 1.1 binding to endpoint: {}", ars.getLocation());
            soapClient.send(ars.getLocation(), opContext);
            return opContext.getInboundMessageContext().getMessage();
        } catch (SOAPException | SecurityException e) {
            throw new MessageDecodingException("Error dereferencing artifact", e);
        }
    }

    /**
     * @param artifact
     * @param endpoint 
     * @param peerRoleDescriptor 
     * @return
     */
    private ArtifactResolve buildArtifactResolveRequestMessage(SAML2Artifact artifact, String endpoint, RoleDescriptor peerRoleDescriptor) {
        ArtifactResolve request = 
                (ArtifactResolve) XMLObjectSupport.buildXMLObject(ArtifactResolve.DEFAULT_ELEMENT_NAME);
        
        Artifact requestArtifact = (Artifact) XMLObjectSupport.buildXMLObject(Artifact.DEFAULT_ELEMENT_NAME);
        requestArtifact.setArtifact(Base64Support.encode(artifact.getArtifactBytes(), false));
        request.setArtifact(requestArtifact);
        
        request.setID(idStrategy.generateIdentifier(true));
        request.setDestination(endpoint);
        request.setIssueInstant(new DateTime(ISOChronology.getInstanceUTC()));
        request.setIssuer(buildIssuer(peerRoleDescriptor));
        
        return request;
    }

    /**
     * @param peerRoleDescriptor 
     * @return
     */
    private Issuer buildIssuer(RoleDescriptor peerRoleDescriptor) {
        Issuer issuer = (Issuer) XMLObjectSupport.buildXMLObject(Issuer.DEFAULT_ELEMENT_NAME);
        //TODO how do we get our own entityID?
        //     probably support optional static injected self entityID as well as injected resolution strategy
        //issuer.setValue("TODO");
        return issuer;
    }

    /**
     * @param artifact
     * @param peerRoleDescriptor
     * @return
     */
    private ArtifactResolutionService resolveArtifactEndpoint(SAML2Artifact artifact, RoleDescriptor peerRoleDescriptor) throws MessageDecodingException {
        RoleDescriptorCriterion roleDescriptorCriterion = new RoleDescriptorCriterion(peerRoleDescriptor);

        ArtifactResolutionService arsTemplate = 
                (ArtifactResolutionService) XMLObjectSupport.buildXMLObject(
                        ArtifactResolutionService.DEFAULT_ELEMENT_NAME);
        
        arsTemplate.setBinding(SAMLConstants.SAML2_SOAP11_BINDING_URI);
        
        if (artifact instanceof SAMLSourceLocationArtifact) {
            arsTemplate.setLocation(((SAMLSourceLocationArtifact)artifact).getSourceLocation());
        }
        
        Integer endpointIndex = SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(artifact.getEndpointIndex());
        arsTemplate.setIndex(endpointIndex);
        
        EndpointCriterion<ArtifactResolutionService> endpointCriterion = 
                new EndpointCriterion<>(arsTemplate, false);

        CriteriaSet criteriaSet = new CriteriaSet(roleDescriptorCriterion, endpointCriterion);

        try {
            ArtifactResolutionService ars = artifactEndpointResolver.resolveSingle(criteriaSet);
            if (ars != null) {
                return ars;
            } else {
                throw new MessageDecodingException("Unable to resolve ArtifactResolutionService endpoint");
            }
        } catch (ResolverException e) {
            throw new MessageDecodingException("Unable to resolve ArtifactResolutionService endpoint");
        }
    }

    /**
     * @param artifact
     * @return
     */
    private RoleDescriptor resolvePeerRoleDescriptor(SAML2Artifact artifact) throws MessageDecodingException {

        CriteriaSet criteriaSet = new CriteriaSet(
                new ArtifactCriterion(artifact),
                new ProtocolCriterion(SAMLConstants.SAML20P_NS),
                new EntityRoleCriterion(getPeerEntityRole()));
        try {
            return roleDescriptorResolver.resolveSingle(criteriaSet);
        } catch (ResolverException e) {
            throw new MessageDecodingException("Error resolving peer entity RoleDescriptor", e);
        }
    }

    /**
     * @param encodedArtifact
     * @return
     */
    private SAML2Artifact parseArtifact(String encodedArtifact) throws MessageDecodingException {
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