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

/**
 * 
 */

package org.opensaml.soap.soap11.impl;

import java.util.Map.Entry;

import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.xml.AttributeSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.AbstractXMLObjectMarshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.soap.soap11.Envelope;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 * A thread-safe marshaller for {@link org.opensaml.soap.soap11.Envelope}s.
 */
public class EnvelopeMarshaller extends AbstractXMLObjectMarshaller {

    /** {@inheritDoc} */
    protected void marshallAttributes(XMLObject xmlObject, Element domElement) throws MarshallingException {
        Envelope envelope = (Envelope) xmlObject;

        Attr attribute;
        for (Entry<QName, String> entry : envelope.getUnknownAttributes().entrySet()) {
            attribute = AttributeSupport.constructAttribute(domElement.getOwnerDocument(), entry.getKey());
            attribute.setValue(entry.getValue());
            domElement.setAttributeNodeNS(attribute);
            if (XMLObjectProviderRegistrySupport.isIDAttribute(entry.getKey()) 
                    || envelope.getUnknownAttributes().isIDAttribute(entry.getKey())) {
                attribute.getOwnerElement().setIdAttributeNode(attribute, true);
            }
        }
    }

    /** {@inheritDoc} */
    protected void marshallElementContent(XMLObject xmlObject, Element domElement) throws MarshallingException {
        // nothing to do, not element content
    }
}