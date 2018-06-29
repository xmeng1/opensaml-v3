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

package org.opensaml.saml.ext.reqattr.impl;

import javax.xml.namespace.QName;

import org.opensaml.core.xml.XMLObjectProviderBaseTestCase;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.reqattr.RequestedAttributes;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.opensaml.saml.saml2.metadata.impl.RequestedAttributeBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test case for creating, marshalling, and unmarshalling
 * {@link org.opensaml.saml.saml2.metadata.impl.RequestedAttributeImpl}.
 */
public class RequestedAttributesTest extends XMLObjectProviderBaseTestCase {

    /** Expected Name attribute value */
    protected final String expectedNames[] = {"attribName1", "attribName2", "attribName3"};

    /**
     * Constructor
     */
    public RequestedAttributesTest() {
        singleElementFile = "/org/opensaml/saml/ext/reqattr/impl/RequestedAttributes.xml";
        childElementsFile = "/org/opensaml/saml/ext/reqattr/impl/RequestedAttributesChildElements.xml";
    }

    /** {@inheritDoc} */
    @Override
    @Test
    public void testSingleElementUnmarshall() {
        final RequestedAttributes requestedAttributes = (RequestedAttributes) unmarshallElement(singleElementFile);

        Assert.assertNotNull(requestedAttributes, "Requested Attribute Simple parse");

    }

    /** {@inheritDoc} */
    @Override
    @Test
    public void testChildElementsUnmarshall() {
        final RequestedAttributes requestedAttributes = (RequestedAttributes) unmarshallElement(childElementsFile);

        Assert.assertEquals(requestedAttributes.getRequestedAttributes().size(), expectedNames.length);
        
        for (int i = 0; i < expectedNames.length; i++) {
            Assert.assertEquals(requestedAttributes.getRequestedAttributes().get(i).getName(), expectedNames[i]);
        }
    }


    /** {@inheritDoc} */
    @Override
    @Test
    public void testSingleElementMarshall() {
        final QName qname = new QName(SAMLConstants.SAML20PREQ_ATTR_NS, RequestedAttributes.DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML20PREQ_ATTRR_PREFIX);
        final RequestedAttributes requestedAttributes = (RequestedAttributes) buildXMLObject(qname);

        assertXMLEquals(expectedDOM, requestedAttributes);
    }

    /** {@inheritDoc} */
    @Override
    @Test
    public void testChildElementsMarshall() {
        final RequestedAttributes requestedAttributes = (new RequestedAttributesBuilder()).buildObject();
        final RequestedAttributeBuilder childBuilder = new RequestedAttributeBuilder();
        
        for (int i = 0; i < expectedNames.length; i++) {
            final RequestedAttribute ra = childBuilder.buildObject();
            ra.setName(expectedNames[i]);
            requestedAttributes.getRequestedAttributes().add(ra);
        }

        assertXMLEquals(expectedChildElementsDOM, requestedAttributes);
    }
    
}