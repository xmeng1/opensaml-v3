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

package org.opensaml.saml.saml1.core.impl;

import javax.annotation.Nonnull;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.common.AbstractSAMLObjectUnmarshaller;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.RequestAbstractType;
import org.opensaml.saml.saml1.core.RespondWith;
import org.opensaml.xmlsec.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import com.google.common.base.Strings;

/**
 * A thread safe Unmarshaller for {@link org.opensaml.saml.saml1.core.RequestAbstractType} objects.
 */
public abstract class RequestAbstractTypeUnmarshaller extends AbstractSAMLObjectUnmarshaller {

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RequestAbstractType.class);

    /** {@inheritDoc} */
    public XMLObject unmarshall(Element domElement) throws UnmarshallingException {
        // After regular unmarshalling, check the minor version and set ID-ness if not SAML 1.0
        final RequestAbstractType request = (RequestAbstractType) super.unmarshall(domElement);
        if (request.getVersion() != SAMLVersion.VERSION_10 && !Strings.isNullOrEmpty(request.getID())) {
            domElement.setIdAttributeNS(null, RequestAbstractType.ID_ATTRIB_NAME, true);
        }
        return request;
    }

    /** {@inheritDoc} */
    protected void processChildElement(XMLObject parentSAMLObject, XMLObject childSAMLObject)
            throws UnmarshallingException {
        final RequestAbstractType request = (RequestAbstractType) parentSAMLObject;

        if (childSAMLObject instanceof Signature) {
            request.setSignature((Signature) childSAMLObject);
        } else if (childSAMLObject instanceof RespondWith) {
            request.getRespondWiths().add((RespondWith) childSAMLObject);
        } else {
            super.processChildElement(parentSAMLObject, childSAMLObject);
        }
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    protected void processAttribute(XMLObject samlElement, Attr attribute) throws UnmarshallingException {
        final RequestAbstractType request = (RequestAbstractType) samlElement;

        if (attribute.getNamespaceURI() == null) {
            if (RequestAbstractType.ID_ATTRIB_NAME.equals(attribute.getLocalName())) {
                request.setID(attribute.getValue());
            } else if (RequestAbstractType.ISSUEINSTANT_ATTRIB_NAME.equals(attribute.getLocalName())
                    && !Strings.isNullOrEmpty(attribute.getValue())) {
                DateTime cal = new DateTime(attribute.getValue(), ISOChronology.getInstanceUTC());
                request.setIssueInstant(cal);
            } else if (attribute.getLocalName().equals(RequestAbstractType.MAJORVERSION_ATTRIB_NAME)) {
                int major;
                try {
                    major = Integer.parseInt(attribute.getValue());
                    if (major != 1) {
                        throw new UnmarshallingException("MajorVersion was invalid, must be 1");
                    }
                } catch (final NumberFormatException n) {
                    log.error("Failed to parse major version string", n);
                    throw new UnmarshallingException(n);
                }
            } else if (RequestAbstractType.MINORVERSION_ATTRIB_NAME.equals(attribute.getLocalName())) {
                int minor;
                try {
                    minor = Integer.parseInt(attribute.getValue());
                } catch (final NumberFormatException n) {
                    log.error("Unable to parse minor version string", n);
                    throw new UnmarshallingException(n);
                }
                if (minor == 0) {
                    request.setVersion(SAMLVersion.VERSION_10);
                } else if (minor == 1) {
                    request.setVersion(SAMLVersion.VERSION_11);
                }
            } else {
                super.processAttribute(samlElement, attribute);
            }
        } else {
            super.processAttribute(samlElement, attribute);
        }
        
    }
// Checkstyle: CyclomaticComplexity OFF
    
}