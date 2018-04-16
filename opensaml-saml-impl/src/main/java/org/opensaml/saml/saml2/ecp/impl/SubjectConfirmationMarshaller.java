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

package org.opensaml.saml.saml2.ecp.impl;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.AbstractSAMLObjectMarshaller;
import org.opensaml.saml.saml2.ecp.SubjectConfirmation;
import org.w3c.dom.Element;

/**
 * A thread-safe Marshaller for {@link SubjectConfirmation} objects.
 */
public class SubjectConfirmationMarshaller extends AbstractSAMLObjectMarshaller {

    /** {@inheritDoc} */
    protected void marshallAttributes(final XMLObject samlObject, final Element domElement)
            throws MarshallingException {
        final SubjectConfirmation sc = (SubjectConfirmation) samlObject;

        if (sc.isSOAP11MustUnderstandXSBoolean() != null) {
            XMLObjectSupport.marshallAttribute(SubjectConfirmation.SOAP11_MUST_UNDERSTAND_ATTR_NAME, 
                    sc.isSOAP11MustUnderstandXSBoolean().toString(), domElement, false);
        }
        
        if (sc.getSOAP11Actor() != null) {
            XMLObjectSupport.marshallAttribute(SubjectConfirmation.SOAP11_ACTOR_ATTR_NAME, 
                    sc.getSOAP11Actor(), domElement, false);
        }
        
        if (sc.getMethod() != null) {
            domElement.setAttributeNS(null, SubjectConfirmation.METHOD_ATTRIB_NAME, sc.getMethod());
        }
    }
}