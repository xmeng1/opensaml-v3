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

import java.util.Collections;
import java.util.Set;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.metadata.resolver.index.MetadataIndex;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 *
 */
public class MetadataIndexManagerTest extends XMLObjectBaseTestCase {
    
    private EntityDescriptor a, b, c;
    private SimpleStringCriterion critAEntity, critBEntity, critCEntity;
    private EntityRoleCriterion roleCritSP = new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
    private EntityRoleCriterion roleCritIDP = new EntityRoleCriterion(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
    
    private CriteriaSet criteriaSet;
    
    private Optional<Set<EntityDescriptor>> result;
    
    @BeforeMethod
    public void setUp() {
        a = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        a.setEntityID("urn:test:a");
        a.getRoleDescriptors().add((RoleDescriptor) XMLObjectSupport.buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
        critAEntity = new SimpleStringCriterion(a.getEntityID().toUpperCase());
        
        b = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        b.setEntityID("urn:test:b");
        b.getRoleDescriptors().add((RoleDescriptor) XMLObjectSupport.buildXMLObject(IDPSSODescriptor.DEFAULT_ELEMENT_NAME));
        b.getRoleDescriptors().add((RoleDescriptor) XMLObjectSupport.buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
        critBEntity = new SimpleStringCriterion(b.getEntityID().toUpperCase());
        
        c = (EntityDescriptor) XMLObjectSupport.buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        c.setEntityID("urn:test:c");
        c.getRoleDescriptors().add((RoleDescriptor) XMLObjectSupport.buildXMLObject(IDPSSODescriptor.DEFAULT_ELEMENT_NAME));
        critCEntity = new SimpleStringCriterion(c.getEntityID().toUpperCase());
        
        criteriaSet = new CriteriaSet();
    }
    
    @Test
    public void testSingleIndexWithUniqueResults() {
        FunctionDrivenMetadataIndex functionIndex = 
                new FunctionDrivenMetadataIndex(new UppercaseEntityIdDescriptorFunction(), 
                        new SimpleStringCriteriaFunction());
        
        MetadataIndexManager manager = new MetadataIndexManager(Collections.<MetadataIndex>singleton(functionIndex));
        
        criteriaSet.clear();
        criteriaSet.add(critAEntity);
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().isEmpty());
        
        criteriaSet.clear();
        criteriaSet.add(critBEntity);
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().isEmpty());
        
        criteriaSet.clear();
        criteriaSet.add(critCEntity);
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().isEmpty());
        
        manager.indexEntityDescriptor(a);
        manager.indexEntityDescriptor(b);
        manager.indexEntityDescriptor(c);
        
        criteriaSet.clear();
        criteriaSet.add(critAEntity);
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().isEmpty());
        Assert.assertEquals(result.get().size(), 1);
        Assert.assertTrue(result.get().contains(a));
        
        criteriaSet.clear();
        criteriaSet.add(critBEntity);
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().isEmpty());
        Assert.assertEquals(result.get().size(), 1);
        Assert.assertTrue(result.get().contains(b));
        
        criteriaSet.clear();
        criteriaSet.add(critCEntity);
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().isEmpty());
        Assert.assertEquals(result.get().size(), 1);
        Assert.assertTrue(result.get().contains(c));
    }
    
    @Test
    public void testSingleIndexWithNoResult() {
        FunctionDrivenMetadataIndex functionIndex = 
                new FunctionDrivenMetadataIndex(new UppercaseEntityIdDescriptorFunction(), 
                        new SimpleStringCriteriaFunction());
        
        MetadataIndexManager manager = new MetadataIndexManager(Collections.<MetadataIndex>singleton(functionIndex));
        
        manager.indexEntityDescriptor(a);
        manager.indexEntityDescriptor(b);
        manager.indexEntityDescriptor(c);
        
        criteriaSet.clear();
        criteriaSet.add(new SimpleStringCriterion("foobar"));
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().isEmpty());
    }
    
    @Test
    public void testSingleIndexWithMultipleResults() {
        RoleMetadataIndex roleIndex = new RoleMetadataIndex();
        
        MetadataIndexManager manager = new MetadataIndexManager(Collections.<MetadataIndex>singleton(roleIndex));
        
        manager.indexEntityDescriptor(a);
        manager.indexEntityDescriptor(b);
        manager.indexEntityDescriptor(c);
        
        criteriaSet.clear();
        criteriaSet.add(roleCritSP);
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().isEmpty());
        Assert.assertEquals(result.get().size(), 2);
        Assert.assertTrue(result.get().contains(a));
        Assert.assertTrue(result.get().contains(b));
        
        criteriaSet.clear();
        criteriaSet.add(roleCritIDP);
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().isEmpty());
        Assert.assertEquals(result.get().size(), 2);
        Assert.assertTrue(result.get().contains(b));
        Assert.assertTrue(result.get().contains(c));
        
    }

    @Test
    public void testMultipleIndexesWithSingleResult() {
        RoleMetadataIndex roleIndex = new RoleMetadataIndex();
        
        FunctionDrivenMetadataIndex functionIndex = 
                new FunctionDrivenMetadataIndex(new UppercaseEntityIdDescriptorFunction(), 
                        new SimpleStringCriteriaFunction());
        
        MetadataIndexManager manager = new MetadataIndexManager(Sets.newHashSet(roleIndex, functionIndex));
        
        manager.indexEntityDescriptor(a);
        manager.indexEntityDescriptor(b);
        manager.indexEntityDescriptor(c);
        
        criteriaSet.clear();
        criteriaSet.add(roleCritSP);
        criteriaSet.add(critBEntity);
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().isEmpty());
        Assert.assertEquals(result.get().size(), 1);
        Assert.assertTrue(result.get().contains(b));
    }

    @Test
    public void testMultipleIndexesWithNoResult() {
        RoleMetadataIndex roleIndex = new RoleMetadataIndex();
        
        FunctionDrivenMetadataIndex functionIndex = 
                new FunctionDrivenMetadataIndex(new UppercaseEntityIdDescriptorFunction(), 
                        new SimpleStringCriteriaFunction());
        
        MetadataIndexManager manager = new MetadataIndexManager(Sets.newHashSet(roleIndex, functionIndex));
        
        manager.indexEntityDescriptor(a);
        manager.indexEntityDescriptor(b);
        manager.indexEntityDescriptor(c);
        
        criteriaSet.clear();
        criteriaSet.add(roleCritSP);
        criteriaSet.add(new SimpleStringCriterion("foobar"));
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().isEmpty());
    }

    @Test
    public void testMultipleIndexesWithMultipleResults() {
        RoleMetadataIndex roleIndex = new RoleMetadataIndex();
        
        FunctionDrivenMetadataIndex functionIndex = 
                new FunctionDrivenMetadataIndex(new ConstantEntityDescriptorFunction("All"),
                        new SimpleStringCriteriaFunction());
        
        MetadataIndexManager manager = new MetadataIndexManager(Sets.newHashSet(roleIndex, functionIndex));
        
        manager.indexEntityDescriptor(a);
        manager.indexEntityDescriptor(b);
        manager.indexEntityDescriptor(c);
        
        // This is just a sanity check on the artificial constant function index, which indexes everything
        criteriaSet.clear();
        criteriaSet.add(new SimpleStringCriterion("All"));
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().isEmpty());
        Assert.assertEquals(result.get().size(), 3);
        Assert.assertTrue(result.get().contains(a));
        Assert.assertTrue(result.get().contains(b));
        Assert.assertTrue(result.get().contains(c));
        
        criteriaSet.clear();
        criteriaSet.add(roleCritIDP);
        criteriaSet.add(new SimpleStringCriterion("All"));
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertTrue(result.isPresent());
        Assert.assertFalse(result.get().isEmpty());
        Assert.assertEquals(result.get().size(), 2);
        Assert.assertTrue(result.get().contains(b));
        Assert.assertTrue(result.get().contains(c));
    }
    
    @Test
    public void testNoIndexes() {
        MetadataIndexManager manager = new MetadataIndexManager(Sets.<MetadataIndex>newHashSet());
        
        manager.indexEntityDescriptor(a);
        
        criteriaSet.clear();
        criteriaSet.add(critAEntity);
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertFalse(result.isPresent());
    }
    
    @Test
    public void testNoApplicableCriteria() {
        FunctionDrivenMetadataIndex functionIndex = 
                new FunctionDrivenMetadataIndex(new UppercaseEntityIdDescriptorFunction(), 
                        new SimpleStringCriteriaFunction());
        
        MetadataIndexManager manager = new MetadataIndexManager(Collections.<MetadataIndex>singleton(functionIndex));
        
        manager.indexEntityDescriptor(a);
        
        criteriaSet.clear();
        // This criterion isn't understood by the configured index
        criteriaSet.add(new EntityIdCriterion("urn:test:a"));
        result = manager.lookupEntityDescriptors(criteriaSet);
        Assert.assertFalse(result.isPresent());
    }
    
}
