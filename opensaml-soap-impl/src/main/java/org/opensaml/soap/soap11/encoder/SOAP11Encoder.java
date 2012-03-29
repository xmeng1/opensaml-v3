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

package org.opensaml.soap.soap11.encoder;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.XMLObjectProviderRegistrySupport;
import org.opensaml.soap.common.SOAPObjectBuilder;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.ws.message.MessageContext;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.message.handler.BaseHandlerChainAwareMessageEncoder;
import org.opensaml.ws.transport.OutTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Basic SOAP 1.1 encoder.
 */
public class SOAP11Encoder extends BaseHandlerChainAwareMessageEncoder {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SOAP11Encoder.class);
    
    /** SOAP Envelope builder. */
    private SOAPObjectBuilder<Envelope> envBuilder;
    
    /** SOAP Body builder. */
    private SOAPObjectBuilder<Body> bodyBuilder;
    

    /** Constructor. */
    @SuppressWarnings("unchecked")
    public SOAP11Encoder() {
        super();
        XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        envBuilder = (SOAPObjectBuilder<Envelope>) builderFactory.getBuilder(Envelope.DEFAULT_ELEMENT_NAME);
        bodyBuilder = (SOAPObjectBuilder<Body>) builderFactory.getBuilder(Body.DEFAULT_ELEMENT_NAME);
    }
    
    /** {@inheritDoc} */
    public boolean providesMessageConfidentiality(MessageContext messageContext) throws MessageEncodingException {
        return messageContext.getOutboundMessageTransport().isConfidential();
    }

    /** {@inheritDoc} */
    public boolean providesMessageIntegrity(MessageContext messageContext) throws MessageEncodingException {
        return messageContext.getOutboundMessageTransport().isIntegrityProtected();
    }
    
    /** {@inheritDoc} */
    protected void prepareMessageContext(MessageContext messageContext) throws MessageEncodingException {
        if (messageContext.getOutboundMessage() == null) {
            messageContext.setOutboundMessage(buildSOAPEnvelope(messageContext));
        }
    }
    
    /** {@inheritDoc} */
    protected void encodeToTransport(MessageContext messageContext) throws MessageEncodingException {
        Element envelopeElem = marshallMessage(messageContext.getOutboundMessage());
        
        preprocessTransport(messageContext);
        
        OutTransport outTransport = messageContext.getOutboundMessageTransport();
        SerializeSupport.writeNode(envelopeElem, outTransport.getOutgoingStream());
    }
    
    /**
     * Perform any processing or fixup on the message context's outbound transport, prior to encoding the actual
     * message.
     * 
     * <p>
     * The default implementation does nothing. Subclasses should override to implement transport-specific 
     * behavior.
     * </p>
     * 
     * @param messageContext the current message context being processed
     * 
     * @throws MessageEncodingException thrown if there is a problem preprocessing the transport
     */
    protected void preprocessTransport(MessageContext messageContext) throws MessageEncodingException {
    }

    /**
     * Builds the SOAP envelope and body skeleton to be encoded.
     * 
     * @param messageContext the message context being processed
     * 
     * @return the minimal SOAP message envelope skeleton
     */
    protected Envelope buildSOAPEnvelope(MessageContext messageContext) {
        log.debug("Building SOAP envelope");

        Envelope envelope = envBuilder.buildObject();

        Body body = bodyBuilder.buildObject();
        envelope.setBody(body);

        return envelope;
    }
}