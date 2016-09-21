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

package org.opensaml.saml.common;

import net.shibboleth.utilities.java.support.xml.XMLAssertTestNG;

import org.custommonkey.xmlunit.Diff;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.metadata.Organization;
import org.opensaml.saml.saml2.metadata.OrganizationDisplayName;
import org.opensaml.saml.saml2.metadata.OrganizationName;
import org.opensaml.saml.saml2.metadata.OrganizationURL;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

/**
 * Round trip messaging test case.
 */
public class RoundTripTest extends XMLObjectBaseTestCase {
    
    /** Organization to marshall */
    private Organization organization;
    
    /** Organization Marshaller */
    private Marshaller orgMarshaller;
    
    /** Organization Unmarshaller */
    private Unmarshaller orgUnmarshaller;
    
    @BeforeMethod
    protected void setUp() throws Exception {
        SAMLObjectBuilder<Organization> orgBuilder = (SAMLObjectBuilder<Organization>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<Organization>getBuilderOrThrow(
                        Organization.DEFAULT_ELEMENT_NAME);
        organization = orgBuilder.buildObject();            

        SAMLObjectBuilder<OrganizationName> orgNameBuilder = (SAMLObjectBuilder<OrganizationName>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<OrganizationName>getBuilderOrThrow(
                        OrganizationName.DEFAULT_ELEMENT_NAME);
        OrganizationName newOrgName = orgNameBuilder.buildObject();
        newOrgName.setValue("OrgFullName");
        newOrgName.setXMLLang("en");
        organization.getOrganizationNames().add(newOrgName);

        SAMLObjectBuilder<OrganizationDisplayName> orgDisplayNameBuilder = (SAMLObjectBuilder<OrganizationDisplayName>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<OrganizationDisplayName>getBuilderOrThrow(
                        OrganizationDisplayName.DEFAULT_ELEMENT_NAME);
        OrganizationDisplayName newOrgDisplayName = orgDisplayNameBuilder.buildObject();
        newOrgDisplayName.setValue("OrgDisplayName");
        newOrgDisplayName.setXMLLang("en");
        organization.getDisplayNames().add(newOrgDisplayName);

        SAMLObjectBuilder<OrganizationURL> orgURLBuilder = (SAMLObjectBuilder<OrganizationURL>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<OrganizationURL>getBuilderOrThrow(
                        OrganizationURL.DEFAULT_ELEMENT_NAME);
        OrganizationURL newOrgURL = orgURLBuilder.buildObject();    
        newOrgURL.setValue("http://org.url.edu");
        newOrgURL.setXMLLang("en");
        organization.getURLs().add(newOrgURL);
        
        MarshallerFactory marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
        orgMarshaller = marshallerFactory.getMarshaller(organization);
        
        UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        orgUnmarshaller = unmarshallerFactory.getUnmarshaller(organization.getElementQName());
    }

    /**
     * Tests marshalling and unmarshalling the same object a three times.
     * 
     * @throws MarshallingException thrown if the object can't be marshalled
     * @throws UnmarshallingException thrown if hte object can't be unmarshalled
     */
    @Test
    public void testRoundTrip() throws MarshallingException, UnmarshallingException{

        //Marshall the element
        Element orgElement1 =  orgMarshaller.marshall(organization);
        
        // Unmarshall it
        Organization org2 = (Organization) orgUnmarshaller.unmarshall(orgElement1);
        
        // Drop DOM and remarshall
        org2.releaseDOM();
        org2.releaseChildrenDOM(true);
        Element orgElement2 = orgMarshaller.marshall(org2);
        XMLAssertTestNG.assertXMLIdentical(new Diff(orgElement1.getOwnerDocument(), orgElement2.getOwnerDocument()), true);
        
        // Unmarshall again
        Organization org3 = (Organization) orgUnmarshaller.unmarshall(orgElement2);
        
        // Drop DOM and remarshall
        org3.releaseDOM();
        org3.releaseChildrenDOM(true);
        Element orgElement3 = orgMarshaller.marshall(org3);
        XMLAssertTestNG.assertXMLIdentical(new Diff(orgElement1.getOwnerDocument(), orgElement3.getOwnerDocument()), true);
    }
}