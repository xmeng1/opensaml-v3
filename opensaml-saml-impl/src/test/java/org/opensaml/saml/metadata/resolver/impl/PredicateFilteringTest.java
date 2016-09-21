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

package org.opensaml.saml.metadata.resolver.impl;

import java.io.File;
import java.net.URL;
import java.util.Set;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.metadata.criteria.entity.impl.EvaluableEntityRoleEntityDescriptorCriterion;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.CriterionPredicateRegistry;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Test metadata predicate-based filtering code implemented in {@link AbstractMetadataResolver} 
 * and {@link AbstractBatchMetadataResolver}, using {@link FilesystemMetadataResolver}.
 */
public class PredicateFilteringTest extends XMLObjectBaseTestCase {
    
    private FilesystemMetadataResolver metadataProvider;
    
    private File mdFile;

    private String entityID;

    private CriteriaSet criteriaSet;
    
    @BeforeMethod
    protected void setUp() throws Exception {
        entityID = "urn:mace:incommon:washington.edu";

        URL mdURL = FilesystemMetadataResolverTest.class
                .getResource("/org/opensaml/saml/saml2/metadata/InCommon-metadata.xml");
        mdFile = new File(mdURL.toURI());

        metadataProvider = new FilesystemMetadataResolver(mdFile);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        
        criteriaSet = new CriteriaSet();
    }
    
    @Test
    public void testResolveByEntityIDAndFilterByRole() throws ResolverException, ComponentInitializationException {
        CriterionPredicateRegistry<EntityDescriptor> registry = new CriterionPredicateRegistry<>();
        registry.register(EntityRoleCriterion.class, EvaluableEntityRoleEntityDescriptorCriterion.class);
        metadataProvider.setCriterionPredicateRegistry(registry);
        metadataProvider.initialize();
        
        Set<EntityDescriptor> descriptors;
        
        // This entityID has only IDPSSODescriptor
        
        criteriaSet.clear();
        criteriaSet.add(new EntityIdCriterion(entityID));
        criteriaSet.add(new EntityRoleCriterion(IDPSSODescriptor.DEFAULT_ELEMENT_NAME));
        descriptors = Sets.newHashSet(metadataProvider.resolve(criteriaSet));
        Assert.assertEquals(descriptors.size(), 1);
        
        criteriaSet.clear();
        criteriaSet.add(new EntityIdCriterion(entityID));
        criteriaSet.add(new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
        descriptors = Sets.newHashSet(metadataProvider.resolve(criteriaSet));
        Assert.assertEquals(descriptors.size(), 0);
        
    }
    
    @Test
    public void testResolveByRoleViaPredicatesOnly() throws ResolverException, ComponentInitializationException {
        CriterionPredicateRegistry<EntityDescriptor> registry = new CriterionPredicateRegistry<>();
        registry.register(EntityRoleCriterion.class, EvaluableEntityRoleEntityDescriptorCriterion.class);
        metadataProvider.setCriterionPredicateRegistry(registry);
        metadataProvider.setResolveViaPredicatesOnly(true);
        metadataProvider.initialize();
        
        Set<EntityDescriptor> descriptors;
        
        criteriaSet.clear();
        criteriaSet.add(new EntityRoleCriterion(IDPSSODescriptor.DEFAULT_ELEMENT_NAME));
        descriptors = Sets.newHashSet(metadataProvider.resolve(criteriaSet));
        
        Assert.assertEquals(descriptors.size(), 15);
        for (EntityDescriptor descriptor : descriptors) {
            Assert.assertTrue(descriptor.getRoleDescriptors(IDPSSODescriptor.DEFAULT_ELEMENT_NAME).size() > 0);
        }
        
        criteriaSet.clear();
        criteriaSet.add(new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
        descriptors = Sets.newHashSet(metadataProvider.resolve(criteriaSet));
        
        Assert.assertEquals(descriptors.size(), 16);
        for (EntityDescriptor descriptor : descriptors) {
            Assert.assertTrue(descriptor.getRoleDescriptors(SPSSODescriptor.DEFAULT_ELEMENT_NAME).size() > 0);
        }
    }
    
    @Test
    public void testResolveByRoleWithoutViaPredicatesOnly() throws ResolverException, ComponentInitializationException {
        CriterionPredicateRegistry<EntityDescriptor> registry = new CriterionPredicateRegistry<>();
        registry.register(EntityRoleCriterion.class, EvaluableEntityRoleEntityDescriptorCriterion.class);
        metadataProvider.setCriterionPredicateRegistry(registry);
        metadataProvider.setResolveViaPredicatesOnly(false);
        metadataProvider.initialize();
        
        Set<EntityDescriptor> descriptors;
        
        criteriaSet.clear();
        criteriaSet.add(new EntityRoleCriterion(IDPSSODescriptor.DEFAULT_ELEMENT_NAME));
        descriptors = Sets.newHashSet(metadataProvider.resolve(criteriaSet));
        
        Assert.assertEquals(descriptors.size(), 0);
        
        criteriaSet.clear();
        criteriaSet.add(new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
        descriptors = Sets.newHashSet(metadataProvider.resolve(criteriaSet));
        
        Assert.assertEquals(descriptors.size(), 0);
    }

}
