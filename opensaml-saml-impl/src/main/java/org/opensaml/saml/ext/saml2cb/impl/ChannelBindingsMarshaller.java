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

package org.opensaml.saml.ext.saml2cb.impl;

import org.opensaml.saml.ext.saml2cb.ChannelBindings;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.impl.XSBase64BinaryMarshaller;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.w3c.dom.Element;

/**
 * A thread-safe Marshaller for {@link ChannelBindings} objects.
 */
public class ChannelBindingsMarshaller extends XSBase64BinaryMarshaller {

    /** {@inheritDoc} */
    protected void marshallAttributes(final XMLObject xmlObject, final Element domElement) throws MarshallingException {
        final ChannelBindings cb = (ChannelBindings) xmlObject;

        if (cb.getType() != null) {
            domElement.setAttributeNS(null, ChannelBindings.TYPE_ATTRIB_NAME, cb.getType());
        }

        if (cb.isSOAP11MustUnderstandXSBoolean() != null) {
            XMLObjectSupport.marshallAttribute(ChannelBindings.SOAP11_MUST_UNDERSTAND_ATTR_NAME, 
                    cb.isSOAP11MustUnderstandXSBoolean().toString(), domElement, false);
        }
        
        if (cb.getSOAP11Actor() != null) {
            XMLObjectSupport.marshallAttribute(ChannelBindings.SOAP11_ACTOR_ATTR_NAME, 
                    cb.getSOAP11Actor(), domElement, false);
        }
    }

}