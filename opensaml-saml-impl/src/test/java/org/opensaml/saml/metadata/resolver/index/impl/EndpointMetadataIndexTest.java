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

package org.opensaml.saml.metadata.resolver.index.impl;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.criterion.EndpointCriterion;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.StartsWithLocationCriterion;
import org.opensaml.saml.metadata.resolver.index.MetadataIndexKey;
import org.opensaml.saml.saml2.metadata.ArtifactResolutionService;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 *
 */
public class EndpointMetadataIndexTest extends XMLObjectBaseTestCase {
    
    private String entityID = "https://www.example.com/saml";
    private EntityDescriptor descriptor;
    
    private SPSSODescriptor spRoleDescriptor;
    private IDPSSODescriptor idpRoleDescriptor;
    
    private AssertionConsumerService endpoint1;
    private ArtifactResolutionService endpoint2, idpEndpoint;
    
    private String location1 = "https://www.example.com/saml/someEndpoint1";
    private String location2 = "https://www.example.com/saml/someEndpoint2";
    private String responseLocation1 = "https://www.example.com/saml/someResponseEndpoint1";
    private String idpLocation = "https://idp.example.com/saml/someEndpoint";
    
    private MetadataIndexKey endpointKey1, endpointKey2, responseEndpointKey1, idpEndpointKey;
    
    @BeforeMethod
    protected void setUp() {
        descriptor = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        descriptor.setEntityID(entityID);
        
        spRoleDescriptor = buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        descriptor.getRoleDescriptors().add(spRoleDescriptor);
        
        idpRoleDescriptor = buildXMLObject(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
        descriptor.getRoleDescriptors().add(idpRoleDescriptor);
        
        endpoint1 = buildXMLObject(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        endpoint1.setLocation(location1);
        endpoint1.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        
        endpoint2 = buildXMLObject(ArtifactResolutionService.DEFAULT_ELEMENT_NAME);
        endpoint2.setLocation(location2);
        endpoint2.setBinding(SAMLConstants.SAML2_SOAP11_BINDING_URI);
        
        idpEndpoint = buildXMLObject(ArtifactResolutionService.DEFAULT_ELEMENT_NAME);
        idpEndpoint.setLocation(idpLocation);
        idpEndpoint.setBinding(SAMLConstants.SAML2_SOAP11_BINDING_URI);
        
        spRoleDescriptor.getAssertionConsumerServices().add(endpoint1);
        spRoleDescriptor.getArtifactResolutionServices().add(endpoint2);
        
        
        endpointKey1 = new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, location1, false);
        endpointKey2 = new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, ArtifactResolutionService.DEFAULT_ELEMENT_NAME, location2, false);
        idpEndpointKey = new EndpointMetadataIndex.EndpointMetadataIndexKey(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, ArtifactResolutionService.DEFAULT_ELEMENT_NAME, idpLocation, false);
        responseEndpointKey1 = new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, responseLocation1, true);
    }
    
    @Test
    public void testGenerateKeysFromCriteria() {
        EndpointMetadataIndex metadataIndex = new EndpointMetadataIndex();
        CriteriaSet criteriaSet = new CriteriaSet();
        Set<MetadataIndexKey> keys = null;
        Endpoint endpoint = null;
        
        // Insufficient input
        criteriaSet.clear();
        criteriaSet.add(new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
        keys = metadataIndex.generateKeys(criteriaSet);
        Assert.assertNull(keys);
        
        // Location only
        criteriaSet.clear();
        criteriaSet.add(new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
        endpoint = buildXMLObject(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        endpoint.setLocation(location1);
        criteriaSet.add(new EndpointCriterion<Endpoint>(endpoint));
        keys = metadataIndex.generateKeys(criteriaSet);
        Assert.assertNotNull(keys);
        Assert.assertEquals(keys.size(), 1);
        Assert.assertTrue(keys.contains(endpointKey1));
        
        // Location + response location
        criteriaSet.clear();
        criteriaSet.add(new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
        endpoint = buildXMLObject(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        endpoint.setLocation(location1);
        endpoint.setResponseLocation(responseLocation1);
        criteriaSet.add(new EndpointCriterion<Endpoint>(endpoint));
        keys = metadataIndex.generateKeys(criteriaSet);
        Assert.assertNotNull(keys);
        Assert.assertEquals(keys.size(), 2);
        Assert.assertTrue(keys.contains(endpointKey1));
        Assert.assertTrue(keys.contains(responseEndpointKey1));
        
        // This tests the generation of path trimmed variants, i.e. the CAS use case.
        criteriaSet.clear();
        criteriaSet.add(new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
        endpoint = buildXMLObject(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        endpoint.setLocation("https://www.example.com/cas/someEndpoint1/foo/bar/");
        criteriaSet.add(new EndpointCriterion<Endpoint>(endpoint));
        criteriaSet.add(new StartsWithLocationCriterion());
        keys = metadataIndex.generateKeys(criteriaSet);
        Assert.assertNotNull(keys);
        Assert.assertEquals(keys.size(), 10);
        Assert.assertTrue(keys.contains(new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, 
                "https://www.example.com/cas/someEndpoint1/foo/bar/", false)));
        Assert.assertTrue(keys.contains(new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, 
                "https://www.example.com/cas/someEndpoint1/foo/bar", false)));
        Assert.assertTrue(keys.contains(new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, 
                "https://www.example.com/cas/someEndpoint1/foo/", false)));
        Assert.assertTrue(keys.contains(new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, 
                "https://www.example.com/cas/someEndpoint1/foo", false)));
        Assert.assertTrue(keys.contains(new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, 
                "https://www.example.com/cas/someEndpoint1/", false)));
        Assert.assertTrue(keys.contains(new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, 
                "https://www.example.com/cas/someEndpoint1", false)));
        Assert.assertTrue(keys.contains(new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, 
                "https://www.example.com/cas/", false)));
        Assert.assertTrue(keys.contains(new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, 
                "https://www.example.com/cas", false)));
        Assert.assertTrue(keys.contains(new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, 
                "https://www.example.com/", false)));
        Assert.assertTrue(keys.contains(new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, 
                "https://www.example.com", false)));
    }
    
    @Test
    public void testGenerateKeysFromDescriptorAlwaysFalseSelector() {
        EndpointMetadataIndex metadataIndex = new EndpointMetadataIndex(Predicates.<Endpoint>alwaysFalse());
        
        Set<MetadataIndexKey> keys = metadataIndex.generateKeys(descriptor);
        
        Assert.assertEquals(keys.size(), 0);
    }
    
    @Test
    public void testGenerateKeysFromDescriptorDefaultCtor() {
        EndpointMetadataIndex metadataIndex = new EndpointMetadataIndex();
        
        Set<MetadataIndexKey> keys = metadataIndex.generateKeys(descriptor);
        
        Assert.assertEquals(keys.size(), 2);
        Assert.assertTrue(keys.contains(endpointKey1));
        Assert.assertTrue(keys.contains(endpointKey2));
    }
    
    @Test
    public void testGenerateKeysFromDescriptorTwoRoles() {
        idpRoleDescriptor.getArtifactResolutionServices().add(idpEndpoint);
        
        EndpointMetadataIndex metadataIndex = new EndpointMetadataIndex();
        
        Set<MetadataIndexKey> keys = metadataIndex.generateKeys(descriptor);
        
        Assert.assertEquals(keys.size(), 3);
        Assert.assertTrue(keys.contains(endpointKey1));
        Assert.assertTrue(keys.contains(endpointKey2));
        Assert.assertTrue(keys.contains(idpEndpointKey));
    }
    
    @Test
    public void testGenerateKeysFromDescriptorWithResponseLocation() {
        endpoint1.setResponseLocation(responseLocation1);
        
        EndpointMetadataIndex metadataIndex = new EndpointMetadataIndex();
        
        Set<MetadataIndexKey> keys = metadataIndex.generateKeys(descriptor);
        
        Assert.assertEquals(keys.size(), 3);
        Assert.assertTrue(keys.contains(endpointKey1));
        Assert.assertTrue(keys.contains(endpointKey2));
        Assert.assertTrue(keys.contains(responseEndpointKey1));
    }
    
    @Test
    public void testGenerateKeysFromDescriptorIndexOnlyACS() {
        Map<QName, Set<QName>> indexableEndpoints = new HashMap<>();
        indexableEndpoints.put(SPSSODescriptor.DEFAULT_ELEMENT_NAME, Sets.newHashSet(AssertionConsumerService.DEFAULT_ELEMENT_NAME));
        EndpointMetadataIndex metadataIndex = 
                new EndpointMetadataIndex(new EndpointMetadataIndex.DefaultEndpointSelectionPredicate(indexableEndpoints));
        
        Set<MetadataIndexKey> keys = metadataIndex.generateKeys(descriptor);
        
        Assert.assertEquals(keys.size(), 1);
        Assert.assertTrue(keys.contains(endpointKey1));
    }
    
    @Test
    public void testDefaultEndpointSelectionPredicate() {
        Predicate<Endpoint> predicate;
        Map<QName, Set<QName>> indexableEndpoints;
        
        AssertionConsumerService endpoint = buildXMLObject(AssertionConsumerService.DEFAULT_ELEMENT_NAME);
        endpoint.setLocation(location1);
        endpoint.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        SPSSODescriptor role = buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        role.getAssertionConsumerServices().add(endpoint);
        
        predicate = new EndpointMetadataIndex.DefaultEndpointSelectionPredicate();
        Assert.assertFalse(predicate.apply(endpoint));
        
        indexableEndpoints = Collections.emptyMap();
        predicate = new EndpointMetadataIndex.DefaultEndpointSelectionPredicate(indexableEndpoints);
        Assert.assertFalse(predicate.apply(endpoint));
        
        indexableEndpoints = new HashMap<>();
        indexableEndpoints.put(SPSSODescriptor.DEFAULT_ELEMENT_NAME, Sets.newHashSet(ArtifactResolutionService.DEFAULT_ELEMENT_NAME));
        predicate = new EndpointMetadataIndex.DefaultEndpointSelectionPredicate(indexableEndpoints);
        Assert.assertFalse(predicate.apply(endpoint));
        
        indexableEndpoints = new HashMap<>();
        indexableEndpoints.put(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, Sets.newHashSet(ArtifactResolutionService.DEFAULT_ELEMENT_NAME));
        predicate = new EndpointMetadataIndex.DefaultEndpointSelectionPredicate(indexableEndpoints);
        Assert.assertFalse(predicate.apply(endpoint));
        
        indexableEndpoints = new HashMap<>();
        indexableEndpoints.put(SPSSODescriptor.DEFAULT_ELEMENT_NAME, Sets.newHashSet(AssertionConsumerService.DEFAULT_ELEMENT_NAME));
        predicate = new EndpointMetadataIndex.DefaultEndpointSelectionPredicate(indexableEndpoints);
        Assert.assertTrue(predicate.apply(endpoint));
    }
    
    @Test
    public void testEndpointKey() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        MetadataIndexKey keySame = 
                new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, location1, false);
                
        MetadataIndexKey keySameCanonicalized = 
                new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, "HTTPS://WWW.EXAMPLE.COM:443/saml/someEndpoint1?foo=x&bar=y#someFragment", false);
        
        MetadataIndexKey keyDifferentRole = 
                new EndpointMetadataIndex.EndpointMetadataIndexKey(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, location1, false);
                
        MetadataIndexKey keyDifferentEndpointType = 
                new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, SingleSignOnService.DEFAULT_ELEMENT_NAME, location1, false);
        
        MetadataIndexKey keyDifferentLocation = 
                new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, location2, false);
        
        MetadataIndexKey keyDifferentIsResponse = 
                new EndpointMetadataIndex.EndpointMetadataIndexKey(SPSSODescriptor.DEFAULT_ELEMENT_NAME, AssertionConsumerService.DEFAULT_ELEMENT_NAME, location1, true);
        
        Assert.assertEquals(endpointKey1, keySame);
        Assert.assertTrue(endpointKey1.hashCode() == keySame.hashCode());
        
        Assert.assertEquals(endpointKey1, keySameCanonicalized);
        Assert.assertTrue(endpointKey1.hashCode() == keySameCanonicalized.hashCode());
        
        Assert.assertNotEquals(endpointKey1, keyDifferentRole);
        Assert.assertNotEquals(endpointKey1, keyDifferentEndpointType);
        Assert.assertNotEquals(endpointKey1, keyDifferentLocation);
        Assert.assertNotEquals(endpointKey1, keyDifferentIsResponse);
    }

}
