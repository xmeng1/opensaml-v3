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

package org.opensaml.saml.saml2.binding.artifact;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.BasicEndpointSelector;
import org.opensaml.saml.common.messaging.context.SamlLocalEntityContext;
import org.opensaml.saml.common.messaging.context.SamlMetadataContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.ArtifactResolutionService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IndexedEndpoint;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.provider.MetadataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SAML 2, type 0x0004, artifact builder.
 */
public class SAML2ArtifactType0004Builder implements SAML2ArtifactBuilder<SAML2ArtifactType0004> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SAML2ArtifactType0004Builder.class);

    /** {@inheritDoc} */
    public SAML2ArtifactType0004 buildArtifact(byte[] artifact) {
        return SAML2ArtifactType0004.parseArtifact(artifact);
    }

    /** {@inheritDoc} */
    public SAML2ArtifactType0004 buildArtifact(MessageContext<SAMLObject> requestContext) {
        try {
            IndexedEndpoint acsEndpoint = (IndexedEndpoint) getAcsEndpoint(requestContext);
            if (acsEndpoint == null) {
                return null;
            }

            byte[] endpointIndex = intToByteArray(acsEndpoint.getIndex());
            byte[] trimmedIndex = new byte[2];
            trimmedIndex[0] = endpointIndex[2];
            trimmedIndex[1] = endpointIndex[3];

            MessageDigest sha1Digester = MessageDigest.getInstance("SHA-1");
            byte[] source = sha1Digester.digest(getLocalEntityId(requestContext).getBytes());

            SecureRandom handleGenerator = SecureRandom.getInstance("SHA1PRNG");
            byte[] assertionHandle;
            assertionHandle = new byte[20];
            handleGenerator.nextBytes(assertionHandle);

            return new SAML2ArtifactType0004(trimmedIndex, source, assertionHandle);
        } catch (NoSuchAlgorithmException e) {
            log.error("JVM does not support required cryptography algorithms: SHA-1/SHA1PRNG.", e);
            throw new InternalError("JVM does not support required cryptography algorithms: SHA-1/SHA1PRNG.");
        }
    }

    /**
     * Gets the source location used to for the artifacts created by this encoder.
     * 
     * @param requestContext current request context
     * 
     * @return source location used to for the artifacts created by this encoder
     */
    protected Endpoint getAcsEndpoint(MessageContext<SAMLObject> requestContext) {
        BasicEndpointSelector selector = new BasicEndpointSelector();
        selector.setEndpointType(ArtifactResolutionService.DEFAULT_ELEMENT_NAME);
        selector.getSupportedIssuerBindings().add(SAMLConstants.SAML2_SOAP11_BINDING_URI);
        selector.setMetadataProvider(getMetadataProvider(requestContext));
        selector.setEntityMetadata(getLocalEntityMetadata(requestContext));
        selector.setEntityRoleMetadata(getLocalEntityRoleMetadata(requestContext));

        Endpoint acsEndpoint = selector.selectEndpoint();

        if (acsEndpoint == null) {
            log.error("No artifact resolution service endpoint defined for the entity "
                    + getOutboundMessageIssuer(requestContext));
            return null;
        }

        return acsEndpoint;
    }
    
    /**
     * Get the local entityId.
     * 
     * @param requestContext the message context
     * 
     * @return the local entityId
     */
    private String getLocalEntityId(MessageContext<SAMLObject> requestContext) {
        SamlLocalEntityContext localContext = requestContext.getSubcontext(SamlLocalEntityContext.class, false);
        Constraint.isNotNull(localContext, "Message context did not contain a LocalEntityContext");
        Constraint.isNotNull(localContext.getEntityId(), "LocalEntityContext contained a null entityId");
        return localContext.getEntityId();
    }
    
    /**
     * Get the outbound message issuer.
     * 
     * @param requestContext  the message context
     * @return the outbound message issuer
     */
    private String getOutboundMessageIssuer(MessageContext<SAMLObject> requestContext) {
        BasicMessageMetadataContext basicContext = 
                requestContext.getSubcontext(BasicMessageMetadataContext.class, false);
        Constraint.isNotNull(basicContext, "Message context did not contain a BasicMessageMetadataContext");
        return basicContext.getMessageIssuer();
    }
    
    /**
     * Get the local entity role metadata.
     * 
     * @param requestContext the message context
     * @return local entity role metadata
     */
    private RoleDescriptor getLocalEntityRoleMetadata(MessageContext<SAMLObject> requestContext) {
        SamlLocalEntityContext localContext = requestContext.getSubcontext(SamlLocalEntityContext.class, false);
        Constraint.isNotNull(localContext, "Message context did not contain a LocalEntityContext");
        SamlMetadataContext mdContext = localContext.getSubcontext(SamlMetadataContext.class, false);
        Constraint.isNotNull(mdContext, "LocalEntityContext did not contain a SamlMetadataContext");
        return mdContext.getRoleDescriptor();
        
    }

    /**
     * Get the local entity metadata.
     * 
     * @param requestContext the message context
     * @return the local entity metadata
     */
    private EntityDescriptor getLocalEntityMetadata(MessageContext<SAMLObject> requestContext) {
        SamlLocalEntityContext localContext = requestContext.getSubcontext(SamlLocalEntityContext.class, false);
        Constraint.isNotNull(localContext, "Message context did not contain a LocalEntityContext");
        SamlMetadataContext mdContext = localContext.getSubcontext(SamlMetadataContext.class, false);
        Constraint.isNotNull(mdContext, "LocalEntityContext did not contain a SamlMetadataContext");
        return mdContext.getEntityDescriptor();
    }

    /**
     * @param requestContext
     * @return
     */
    private MetadataProvider getMetadataProvider(MessageContext<SAMLObject> requestContext) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Converts an integer into an unsigned 4-byte array.
     * 
     * @param integer integer to convert
     * 
     * @return 4-byte array representing integer
     */
    private byte[] intToByteArray(int integer) {
        byte[] intBytes = new byte[4];
        intBytes[0] = (byte) ((integer & 0xff000000) >>> 24);
        intBytes[1] = (byte) ((integer & 0x00ff0000) >>> 16);
        intBytes[2] = (byte) ((integer & 0x0000ff00) >>> 8);
        intBytes[3] = (byte) ((integer & 0x000000ff));

        return intBytes;
    }
}