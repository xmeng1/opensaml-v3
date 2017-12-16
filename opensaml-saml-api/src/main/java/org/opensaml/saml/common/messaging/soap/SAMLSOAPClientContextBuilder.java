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

package org.opensaml.saml.common.messaging.soap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.messaging.MessageException;
import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.RecursiveTypedParentContextLookup;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;
import org.opensaml.saml.common.messaging.context.SAMLSelfEntityContext;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.security.messaging.HttpClientSecurityContext;

import com.google.common.base.Function;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

//TODO when impl finished, document required vs optional data and derivation rules

/**
 * Builder {@link InOutOperationContext} instances for SAML SOAP client use cases.
 * 
 * @param <InboundMessageType> the inbound message type
 * @param <OutboundMessageType> the outbound message type
 */
public class SAMLSOAPClientContextBuilder<InboundMessageType extends SAMLObject, 
        OutboundMessageType extends SAMLObject> {
    
    /** The outbound message. **/
    private OutboundMessageType outboundMessage;
    
    /** The SAML self entityID. **/
    private String selfEntityID;
    
    /** The SAML peer entityID. **/
    private String peerEntityID;
    
    /** The SAML peer entity roles. **/
    private QName peerEntityRole;
    
    /** The SAML peer EntityDescriptor. **/
    private EntityDescriptor peerEntityDescriptor;
    
    /** The SAML peer RoleDescriptor. **/
    private RoleDescriptor peerRoleDescriptor;
    
    /** TLS CriteriaSet strategy. */
    private Function<MessageContext<?>, CriteriaSet> tlsCriteriaSetStrategy;
    
    /**
     * Get the outbound message.
     * 
     * @return the outbound message
     */
    @Nullable public OutboundMessageType getOutboundMessage() {
        return outboundMessage;
    }

    /**
     * Set the outbound message.
     * 
     * @param message the outbound message
     * @return this builder instance
     */
    @Nonnull public SAMLSOAPClientContextBuilder<InboundMessageType, OutboundMessageType> setOutboundMessage(
            final OutboundMessageType message) {
        outboundMessage = message;
        return this;
    }

    /**
     * Get the SAML self entityID.
     * 
     * @return the SAML self entityID
     */
    @Nullable public String getSelfEntityID() {
        return selfEntityID;
    }

    /**
     * Set the SAML self entityID.
     * 
     * @param entityID the SAML self entityID.
     * @return this builder instance
     */
    @Nonnull public SAMLSOAPClientContextBuilder<InboundMessageType, OutboundMessageType> setSelfEntityID(
            final String entityID) {
        selfEntityID = entityID;
        return this;
    }

    /**
     * Get the SAML peer entityID.
     * 
     * @return the SAML peer entityID
     */
    @Nullable public String getPeerEntityID() {
        if (peerEntityID != null) {
            return peerEntityID;
        } else if (getPeerEntityDescriptor() != null) {
            return getPeerEntityDescriptor().getEntityID();
        } else {
            return null;
        }
    }

    /**
     * Set the SAML peer entityID.
     * 
     * @param entityID the SAML peer entityID
     * @return this builder instance
     */
    @Nonnull public SAMLSOAPClientContextBuilder<InboundMessageType, OutboundMessageType> setPeerEntityID(
            final String entityID) {
        peerEntityID = entityID;
        return this;
    }

    /**
     * Get the SAML peer role.
     * 
     * @return the SAML peer role
     */
    @Nullable public QName getPeerEntityRole() {
        if (peerEntityRole != null) {
            return peerEntityRole;
        } else if (getPeerRoleDescriptor() != null) {
            if (getPeerRoleDescriptor().getSchemaType() != null) {
                return getPeerRoleDescriptor().getSchemaType();
            } else {
                return getPeerRoleDescriptor().getElementQName();
            }
        } else {
             return null;
        }
    }

    /**
     * Set the SAML peer role.
     * 
     * @param role the SAML peer role
     * @return this builder instance
     */
    @Nonnull public SAMLSOAPClientContextBuilder<InboundMessageType, OutboundMessageType> setPeerEntityRole(
            final QName role) {
        peerEntityRole = role;
        return this;
    }

    /**
     * Get the SAML peer EntityDscriptor.
     * 
     * @return the SAML peer EntityDescriptor
     */
    @Nullable public EntityDescriptor getPeerEntityDescriptor() {
        if (peerEntityDescriptor != null) {
            return peerEntityDescriptor;
        } else if (getPeerRoleDescriptor() != null) {
            final XMLObject roleParent = getPeerRoleDescriptor().getParent();
            if (roleParent instanceof EntityDescriptor) {
                return (EntityDescriptor) roleParent;
            }
        } 
        return null;
    }

    /**
     * Set the SAML peer EntityDescriptor.
     * 
     * @param entityDescriptor the SAML peer EntityDescriptor
     * @return this builder instance
     */
    @Nonnull public SAMLSOAPClientContextBuilder<InboundMessageType, OutboundMessageType> setPeerEntityDescriptor(
            final EntityDescriptor entityDescriptor) {
        peerEntityDescriptor = entityDescriptor;
        return this;
    }

    /**
     * Get the SAML peer RoleDescriptor.
     * 
     * @return the SAML peer RoleDescriptor
     */
    @Nullable public RoleDescriptor getPeerRoleDescriptor() {
        return peerRoleDescriptor;
    }

    /**
     * Set the SAML peer RoleDescriptor.
     * 
     * @param roleDescriptor the SAML peer RoleDescriptor.
     * @return this builder instance
     */
    @Nonnull public SAMLSOAPClientContextBuilder<InboundMessageType, OutboundMessageType> setPeerRoleDescriptor(
            final RoleDescriptor roleDescriptor) {
        peerRoleDescriptor = roleDescriptor;
        return this;
    }

    /**
     * Get the TLS CriteriaSet strategy.
     * 
     * @return Returns the tlsCriteriaSetStrategy.
     */
    @Nullable public Function<MessageContext<?>, CriteriaSet> getTLSCriteriaSetStrategy() {
        if (tlsCriteriaSetStrategy != null) {
            return tlsCriteriaSetStrategy;
        } else {
            return new DefaultTLSCriteriaSetStrategy();
        }
    }

    /**
     * Set the TLS CriteriaSet strategy.
     * 
     * @param strategy the strategy
     * @return this builder instance 
     */
    @Nonnull public SAMLSOAPClientContextBuilder<InboundMessageType, OutboundMessageType> 
            setTLSCriteriaSetStrategy(@Nullable final Function<MessageContext<?>, CriteriaSet> strategy) {
        tlsCriteriaSetStrategy = strategy;
        return this;
    }

    /**
     * Build the new operation context.
     * 
     * @return the operation context
     * 
     * @throws MessageException if any required data is not supplied and can not be derived from other supplied data
     */
    public InOutOperationContext<InboundMessageType, OutboundMessageType> build() throws MessageException {
        if (getOutboundMessage() == null) {
            errorMissingData("Outbound message");
        }
        final MessageContext<OutboundMessageType> outboundContext = new MessageContext<OutboundMessageType>();
        outboundContext.setMessage(getOutboundMessage());
        
        final Function<MessageContext<?>, CriteriaSet> tlsStrategy = getTLSCriteriaSetStrategy();
        if (tlsStrategy != null) {
            outboundContext.getSubcontext(HttpClientSecurityContext.class, true)
                .setTLSCriteriaSetStrategy(tlsStrategy);
        }
        
        final InOutOperationContext<InboundMessageType, OutboundMessageType> opContext = 
                new InOutOperationContext<>(null, outboundContext);
        
        // This is just so it's easy to change.
        final BaseContext parent = opContext;
        
        //TODO is this required always?
        final String selfID = getSelfEntityID();
        if (selfID != null) {
            final SAMLSelfEntityContext selfContext = parent.getSubcontext(SAMLSelfEntityContext.class, true);
            selfContext.setEntityId(selfID);
        }
        
        // Both of these required, either supplied or derived
        final String peerID = getPeerEntityID();
        if (peerID == null) {
            errorMissingData("Peer entityID");
        }
        final QName peerRoleName = getPeerEntityRole();
        if (peerRoleName == null) {
            errorMissingData("Peer role");
        }
        final SAMLPeerEntityContext peerContext = parent.getSubcontext(SAMLPeerEntityContext.class, true);
        peerContext.setEntityId(peerID);
        peerContext.setRole(peerRoleName);
        
        //  Both optional, could be resolved in SOAP handling pipeline by handler(s)
        final SAMLMetadataContext metadataContext = peerContext.getSubcontext(SAMLMetadataContext.class, true);
        metadataContext.setEntityDescriptor(getPeerEntityDescriptor());
        metadataContext.setRoleDescriptor(getPeerRoleDescriptor());
        
        return opContext;
    }

    /**
     * Convenience method to report out an error due to missing required data.
     * 
     * @param details the error details
     * @throws MessageException the error to be reported out
     */
    private void errorMissingData(@Nonnull final String details) throws MessageException {
        throw new MessageException("Required context data was not supplied or derivable: " + details);
    }
    
    /** Default TLS CriteriaSet strategy function. */
    public static class DefaultTLSCriteriaSetStrategy implements Function<MessageContext<?>, CriteriaSet> {

        /** {@inheritDoc} */
        public CriteriaSet apply(@Nullable final MessageContext<?> messageContext) {
            final CriteriaSet criteria = new CriteriaSet();
            criteria.add(new UsageCriterion(UsageType.SIGNING));
            
            if (messageContext == null) {
                return criteria;
            }
            
            // This should be consistent with what build() does above.
            final BaseContext parent = new RecursiveTypedParentContextLookup<>(InOutOperationContext.class)
                    .apply(messageContext);
            if (parent == null) {
                return criteria;
            }
            
            final SAMLProtocolContext protocolContext = parent.getSubcontext(SAMLProtocolContext.class);
            if (protocolContext != null && protocolContext.getProtocol() != null) {
                criteria.add(new ProtocolCriterion(protocolContext.getProtocol()));
            }
            
            final SAMLPeerEntityContext peerContext = parent.getSubcontext(SAMLPeerEntityContext.class);
            if (peerContext == null) {
                return criteria;
            }
            
            final SAMLMetadataContext metadataContext = peerContext.getSubcontext(SAMLMetadataContext.class);
            if (metadataContext != null && metadataContext.getRoleDescriptor() != null) {
                criteria.add(new RoleDescriptorCriterion(metadataContext.getRoleDescriptor()));
                return criteria;
            } else {
                if (peerContext.getEntityId() != null) {
                    criteria.add(new EntityIdCriterion(peerContext.getEntityId()));
                }
                if (peerContext.getRole() != null) {
                    criteria.add(new EntityRoleCriterion(peerContext.getRole()));
                }
                return criteria;
            }
        }
        
    }

}
