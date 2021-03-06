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

package org.opensaml.saml.saml2.core.impl;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.common.AbstractSAMLObjectUnmarshaller;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.opensaml.xmlsec.signature.Signature;
import org.w3c.dom.Attr;

import com.google.common.base.Strings;

/**
 * A thread-safe Unmarshaller for {@link org.opensaml.saml.saml2.core.RequestAbstractType} objects.
 */
public abstract class RequestAbstractTypeUnmarshaller extends AbstractSAMLObjectUnmarshaller {

    /** {@inheritDoc} */
    protected void processChildElement(XMLObject parentSAMLObject, XMLObject childSAMLObject)
            throws UnmarshallingException {
        final RequestAbstractType req = (RequestAbstractType) parentSAMLObject;
    
        if (childSAMLObject instanceof Issuer) {
            req.setIssuer((Issuer) childSAMLObject);
        } else if (childSAMLObject instanceof Signature) {
            req.setSignature((Signature) childSAMLObject);
        } else if (childSAMLObject instanceof Extensions) {
            req.setExtensions((Extensions) childSAMLObject);
        } else {
            super.processChildElement(parentSAMLObject, childSAMLObject);
        }
    }

    /** {@inheritDoc} */
    protected void processAttribute(XMLObject samlObject, Attr attribute) throws UnmarshallingException {
        final RequestAbstractType req = (RequestAbstractType) samlObject;

        if (attribute.getNamespaceURI() == null) {
            if (attribute.getLocalName().equals(RequestAbstractType.VERSION_ATTRIB_NAME)) {
                req.setVersion(SAMLVersion.valueOf(attribute.getValue()));
            } else if (attribute.getLocalName().equals(RequestAbstractType.ID_ATTRIB_NAME)) {
                req.setID(attribute.getValue());
                attribute.getOwnerElement().setIdAttributeNode(attribute, true);
            } else if (attribute.getLocalName().equals(RequestAbstractType.ISSUE_INSTANT_ATTRIB_NAME)
                    && !Strings.isNullOrEmpty(attribute.getValue())) {
                req.setIssueInstant(new DateTime(attribute.getValue(), ISOChronology.getInstanceUTC()));
            } else if (attribute.getLocalName().equals(RequestAbstractType.DESTINATION_ATTRIB_NAME)) {
                req.setDestination(attribute.getValue());
            } else if (attribute.getLocalName().equals(RequestAbstractType.CONSENT_ATTRIB_NAME)) {
                req.setConsent(attribute.getValue());
            } else {
                super.processAttribute(samlObject, attribute);
            }
        } else {
            super.processAttribute(samlObject, attribute);
        }
    }
    
}