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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.binding.artifact.SAMLSourceIDArtifact;
import org.opensaml.saml.common.binding.artifact.SAMLSourceLocationArtifact;
import org.opensaml.saml.criterion.ArtifactCriterion;
import org.opensaml.saml.ext.saml1md.SourceID;
import org.opensaml.saml.metadata.resolver.index.MetadataIndexKey;
import org.opensaml.saml.metadata.resolver.index.impl.SAMLArtifactMetadataIndex.ArtifactSourceIDMetadataIndexKey;
import org.opensaml.saml.saml1.binding.artifact.SAML1ArtifactType0002;
import org.opensaml.saml.saml2.binding.artifact.SAML2ArtifactType0004;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.security.crypto.JCAConstants;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;


/**
 *
 */
public class SAMLArtifactMetadataIndexTest extends XMLObjectBaseTestCase {
    
    private SAMLArtifactMetadataIndex metadataIndex;
    
    private String entityID = "https://www.example.com/saml";
    private byte[] entityIDSourceID;
    private String sourceLocation = "https://www.example.com/sp/artifactResolve";
            
    private EntityDescriptor descriptor;
    
    private SourceID extSourceID1a, extSourceID1b, extSourceID2;
    
    private MetadataIndexKey entityIDSourceIDKey, extSourceIDKey1a, extSourceIDKey1b, extSourceIDKey2, sourceLocationKey;
    
    private SAMLSourceIDArtifact sourceIDArtifact;
    private SAMLSourceLocationArtifact sourceLocationArtifact;
    
    
    @BeforeMethod
    protected void setUp() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        metadataIndex = new SAMLArtifactMetadataIndex();
        
        MessageDigest sha1Digester = MessageDigest.getInstance(JCAConstants.DIGEST_SHA1);
        entityIDSourceID = sha1Digester.digest(entityID.getBytes("UTF-8"));
        
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        
        byte[] messageHandle = new byte[20];
        secureRandom.nextBytes(messageHandle);
        
        sourceIDArtifact = new SAML2ArtifactType0004(new byte[] {0, 0} , entityIDSourceID, messageHandle);
        
        entityIDSourceIDKey = new SAMLArtifactMetadataIndex.ArtifactSourceIDMetadataIndexKey(entityIDSourceID);
        
        sourceLocationArtifact = new SAML1ArtifactType0002(messageHandle, sourceLocation);
        
        descriptor = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        descriptor.setEntityID(entityID);
        
        //TODO control source location endpoint key
        //sourceLocationKey = TODO
        
        byte[] secondarySourceID;
        
        secondarySourceID = new byte[20];
        secureRandom.nextBytes(secondarySourceID);
        extSourceIDKey1a = new ArtifactSourceIDMetadataIndexKey(secondarySourceID);
        extSourceID1a = buildXMLObject(SourceID.DEFAULT_ELEMENT_NAME);
        extSourceID1a.setValue(new String(Hex.encodeHex(secondarySourceID, true)));
        
        secondarySourceID = new byte[20];
        secureRandom.nextBytes(secondarySourceID);
        extSourceIDKey1b = new ArtifactSourceIDMetadataIndexKey(secondarySourceID);
        extSourceID1b = buildXMLObject(SourceID.DEFAULT_ELEMENT_NAME);
        extSourceID1b.setValue(new String(Hex.encodeHex(secondarySourceID, true)));
        
        secondarySourceID = new byte[20];
        secureRandom.nextBytes(secondarySourceID);
        extSourceIDKey2 = new ArtifactSourceIDMetadataIndexKey(secondarySourceID);
        extSourceID2 = buildXMLObject(SourceID.DEFAULT_ELEMENT_NAME);
        extSourceID2.setValue(new String(Hex.encodeHex(secondarySourceID, true)));
    }
    
    @Test
    public void testGenerateKeysFromDescriptor() {
        //TODO update test for generated source location key when done.
        
        Set<MetadataIndexKey> keys = metadataIndex.generateKeys(descriptor);
        
        Assert.assertEquals(keys.size(), 1);
        Assert.assertTrue(keys.contains(entityIDSourceIDKey));
    }
    
    @Test
    public void testGenerateKeysFromDescriptorWithOneExtension() {
        //TODO update test for generated source location key when done.
        
        RoleDescriptor roleDescriptor1 = buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        Extensions extensions1 = buildXMLObject(Extensions.DEFAULT_ELEMENT_NAME);
        extensions1.getUnknownXMLObjects().add(extSourceID1a);
        roleDescriptor1.setExtensions(extensions1);
        descriptor.getRoleDescriptors().add(roleDescriptor1);
        
        Set<MetadataIndexKey> keys = metadataIndex.generateKeys(descriptor);
        
        Assert.assertEquals(keys.size(), 2);
        Assert.assertTrue(keys.contains(entityIDSourceIDKey));
        Assert.assertTrue(keys.contains(extSourceIDKey1a));
    }
    
    @Test
    public void testGenerateKeysFromDescriptorWithMultipleExtensions() {
        //TODO update test for generated source location key when done.
        
        RoleDescriptor roleDescriptor1 = buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        Extensions extensions1 = buildXMLObject(Extensions.DEFAULT_ELEMENT_NAME);
        extensions1.getUnknownXMLObjects().add(extSourceID1a);
        extensions1.getUnknownXMLObjects().add(extSourceID1b);
        roleDescriptor1.setExtensions(extensions1);
        descriptor.getRoleDescriptors().add(roleDescriptor1);
        
        RoleDescriptor roleDescriptor2 = buildXMLObject(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
        Extensions extensions2 = buildXMLObject(Extensions.DEFAULT_ELEMENT_NAME);
        extensions2.getUnknownXMLObjects().add(extSourceID2);
        roleDescriptor2.setExtensions(extensions2);
        descriptor.getRoleDescriptors().add(roleDescriptor2);
        
        Set<MetadataIndexKey> keys = metadataIndex.generateKeys(descriptor);
        
        Assert.assertEquals(keys.size(), 4);
        Assert.assertTrue(keys.contains(entityIDSourceIDKey));
        Assert.assertTrue(keys.contains(extSourceIDKey1a));
        Assert.assertTrue(keys.contains(extSourceIDKey1b));
        Assert.assertTrue(keys.contains(extSourceIDKey2));
    }
    
    @Test
    public void testGenerateSourceIDKeysFromCriteria() {
        CriteriaSet criteriaSet = new CriteriaSet();
        
        criteriaSet.add(new ArtifactCriterion(sourceIDArtifact));
        
        Set<MetadataIndexKey> keys = metadataIndex.generateKeys(criteriaSet);
        
        Assert.assertEquals(keys.size(), 1);
        Assert.assertTrue(keys.contains(entityIDSourceIDKey));
    }
    
    /* TODO
    @Test
    public void testGenerateSourceLocationKeysFromCriteria() {
        CriteriaSet criteriaSet = new CriteriaSet();
        
        criteriaSet.add(new ArtifactCriterion(sourceLocationArtifact));
        
        Set<MetadataIndexKey> keys = metadataIndex.generateKeys(criteriaSet);
        
        Assert.assertEquals(keys.size(), 1);
        Assert.assertTrue(keys.contains(sourceLocationKey));
    }
    */
    
    @Test
    public void testSourceIDKey() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        MessageDigest sha1Digester = MessageDigest.getInstance(JCAConstants.DIGEST_SHA1);
        
        MetadataIndexKey keySame = new SAMLArtifactMetadataIndex.ArtifactSourceIDMetadataIndexKey(
                sha1Digester.digest(entityID.getBytes("UTF-8")));
                
        sha1Digester.reset();
        MetadataIndexKey keyDifferent = new SAMLArtifactMetadataIndex.ArtifactSourceIDMetadataIndexKey(
                sha1Digester.digest("foobar".getBytes("UTF-8")));
        
        Assert.assertEquals(entityIDSourceIDKey, keySame);
        Assert.assertTrue(entityIDSourceIDKey.hashCode() == keySame.hashCode());
        
        Assert.assertNotEquals(entityIDSourceIDKey, keyDifferent);
    }

}
