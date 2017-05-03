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

package org.opensaml.saml.saml2.metadata.impl;

import javax.xml.namespace.QName;

import org.opensaml.core.xml.XMLObjectProviderBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSBooleanValue;
import org.opensaml.core.xml.schema.impl.XSAnyBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test case for creating, marshalling, and unmarshalling
 * {@link org.opensaml.saml.saml2.metadata.impl.RequestedAttributeImpl}.
 */
public class RequestedAttributeTest extends XMLObjectProviderBaseTestCase {

    /** Expected Name attribute value */
    protected String expectedName;

    /** Expected NameFormat attribute value */
    protected String expectedNameFormat;

    /** Expected FriendlyName attribute value */
    protected String expectedFriendlyName;

    /** Excpected isRequired attribute value */
    protected XSBooleanValue expectedIsRequired;
    
    /** Expected saml2:AttributeValue values (element content).*/
    protected String expectedAttributeValue0, expectedAttributeValue1;

    /**
     * Constructor
     */
    public RequestedAttributeTest() {
        singleElementFile = "/org/opensaml/saml/saml2/metadata/impl/RequestedAttribute.xml";
        singleElementOptionalAttributesFile = "/org/opensaml/saml/saml2/metadata/impl/RequestedAttributeOptionalAttributes.xml";
        childElementsFile = "/org/opensaml/saml/saml2/metadata/impl/RequestedAttributeChildElements.xml";
    }

    @BeforeMethod
    protected void setUp() throws Exception {
        expectedName = "attribName";
        expectedNameFormat = "urn:string";
        expectedFriendlyName = "Attribute Name";
        expectedIsRequired = new XSBooleanValue(Boolean.TRUE, false);
        expectedAttributeValue0 = "SomeAttributeValue0";
        expectedAttributeValue1 = "SomeAttributeValue1";
    }

    /** {@inheritDoc} */
    @Test
    public void testSingleElementUnmarshall() {
        RequestedAttribute attribute = (RequestedAttribute) unmarshallElement(singleElementFile);

        String name = attribute.getName();
        Assert.assertEquals(name, expectedName, "Name was " + name + ", expected " + expectedName);

    }

    /** {@inheritDoc} */
    @Test
    public void testSingleElementOptionalAttributesUnmarshall() {
        RequestedAttribute requestedAttribute = (RequestedAttribute) unmarshallElement(singleElementOptionalAttributesFile);

        String name = requestedAttribute.getName();
        Assert.assertEquals(name, expectedName, "Name was " + name + ", expected " + expectedName);

        String nameFormat = requestedAttribute.getNameFormat();
        Assert.assertEquals(nameFormat, expectedNameFormat,
                "NameFormat was " + nameFormat + ", expected " + expectedNameFormat);

        String friendlyName = requestedAttribute.getFriendlyName();
        Assert.assertEquals(friendlyName, expectedFriendlyName,
                "FriendlyName was " + friendlyName + ", expected " + expectedFriendlyName);

        boolean isRequired = requestedAttribute.isRequired().booleanValue();
        Assert.assertEquals(requestedAttribute.isRequiredXSBoolean(), expectedIsRequired,
                "Is Required was " + isRequired + ", expected " + expectedIsRequired);
    }
    
    /** {@inheritDoc} */
    @Test
    public void testChildElementsUnmarshall() {
        RequestedAttribute attribute = (RequestedAttribute) unmarshallElement(childElementsFile);

        String name = attribute.getName();
        Assert.assertEquals(name, expectedName, "Name was " + name + ", expected " + expectedName);
        
        Assert.assertEquals(attribute.getAttributeValues().size(), 2);
        
        Assert.assertTrue(attribute.getAttributeValues().get(0) instanceof XSAny);
        XSAny value0 = (XSAny) attribute.getAttributeValues().get(0);
        Assert.assertEquals(value0.getTextContent(), expectedAttributeValue0);
        
        Assert.assertTrue(attribute.getAttributeValues().get(1) instanceof XSAny);
        XSAny value1 = (XSAny) attribute.getAttributeValues().get(1);
        Assert.assertEquals(value1.getTextContent(), expectedAttributeValue1);
    }


    /** {@inheritDoc} */
    @Test
    public void testSingleElementMarshall() {
        QName qname = new QName(SAMLConstants.SAML20MD_NS, RequestedAttribute.DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML20MD_PREFIX);
        RequestedAttribute requestedAttribute = (RequestedAttribute) buildXMLObject(qname);

        requestedAttribute.setName(expectedName);

        assertXMLEquals(expectedDOM, requestedAttribute);
    }

    /** {@inheritDoc} */
    @Test
    public void testSingleElementOptionalAttributesMarshall() {
        RequestedAttribute requestedAttribute = (new RequestedAttributeBuilder()).buildObject();

        requestedAttribute.setName(expectedName);
        requestedAttribute.setNameFormat(expectedNameFormat);
        requestedAttribute.setFriendlyName(expectedFriendlyName);
        requestedAttribute.setIsRequired(expectedIsRequired);

        assertXMLEquals(expectedOptionalAttributesDOM, requestedAttribute);
    }
    
    /** {@inheritDoc} */
    @Test
    public void testChildElementsMarshall() {
        QName qname = new QName(SAMLConstants.SAML20MD_NS, RequestedAttribute.DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML20MD_PREFIX);
        RequestedAttribute requestedAttribute = (RequestedAttribute) buildXMLObject(qname);

        requestedAttribute.setName(expectedName);
        
        XSAnyBuilder valueBuilder = (XSAnyBuilder) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSAny.TYPE_NAME);

        XSAny val0 = valueBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        val0.setTextContent(expectedAttributeValue0);
        requestedAttribute.getAttributeValues().add(val0);
        
        XSAny val1 = valueBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        val1.setTextContent(expectedAttributeValue1);
        requestedAttribute.getAttributeValues().add(val1);

        assertXMLEquals(expectedChildElementsDOM, requestedAttribute);
    }
    
    /**
     * Test the proper behavior of the XSBooleanValue attributes.
     */
    @Test
    public void testXSBooleanAttributes() {
        RequestedAttribute attrib = 
            (RequestedAttribute) buildXMLObject(RequestedAttribute.DEFAULT_ELEMENT_NAME);
        
        // isRequired attribute
        attrib.setIsRequired(Boolean.TRUE);
        Assert.assertEquals(attrib.isRequired(), Boolean.TRUE, "Unexpected value for boolean attribute found");
        Assert.assertNotNull(attrib.isRequiredXSBoolean(), "XSBooleanValue was null");
        Assert.assertEquals(attrib.isRequiredXSBoolean(), new XSBooleanValue(Boolean.TRUE, false),
                "XSBooleanValue was unexpected value");
        Assert.assertEquals(attrib.isRequiredXSBoolean().toString(), "true", "XSBooleanValue string was unexpected value");
        
        attrib.setIsRequired(Boolean.FALSE);
        Assert.assertEquals(attrib.isRequired(), Boolean.FALSE, "Unexpected value for boolean attribute found");
        Assert.assertNotNull(attrib.isRequiredXSBoolean(), "XSBooleanValue was null");
        Assert.assertEquals(attrib.isRequiredXSBoolean(), new XSBooleanValue(Boolean.FALSE, false),
                "XSBooleanValue was unexpected value");
        Assert.assertEquals(attrib.isRequiredXSBoolean().toString(), "false", "XSBooleanValue string was unexpected value");
        
        attrib.setIsRequired((Boolean) null);
        Assert.assertEquals(attrib.isRequired(), Boolean.FALSE, "Unexpected default value for boolean attribute found");
        Assert.assertNull(attrib.isRequiredXSBoolean(), "XSBooleanValue was not null");
    }
}