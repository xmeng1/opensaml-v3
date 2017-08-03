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

package org.opensaml.saml.common.profile.logic;

import java.util.Arrays;
import java.util.Collections;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.metadata.resolver.filter.MetadataNodeProcessor;
import org.opensaml.saml.metadata.resolver.filter.impl.EntitiesDescriptorNameProcessor;
import org.opensaml.saml.metadata.resolver.filter.impl.NodeProcessingMetadataFilter;
import org.opensaml.saml.metadata.resolver.impl.ResourceBackedMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link EntityGroupNamePredicate}.
 */
public class EntityGroupNamePredicateTest extends XMLObjectBaseTestCase {

    private NodeProcessingMetadataFilter filter;
    
    private ResourceBackedMetadataResolver metadataProvider;
    
    @BeforeClass
    protected void setUp() throws Exception {
        
        final Resource resource =
                new ClassPathResource("/org/opensaml/saml/metadata/resolver/filter/impl/EntitiesDescriptor-Name-metadata.xml");
        
        filter = new NodeProcessingMetadataFilter();
        filter.setNodeProcessors(Collections.<MetadataNodeProcessor>singletonList(new EntitiesDescriptorNameProcessor()));
        filter.initialize();
        
        metadataProvider = new ResourceBackedMetadataResolver(null, ResourceHelper.of(resource));
        metadataProvider.setId("test");
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setMetadataFilter(filter);
        metadataProvider.initialize();
    }
    
    @AfterClass
    protected void tearDown() {
        metadataProvider.destroy();
        filter.destroy();
    }

    @Test
    public void testNoMatch() throws Exception {

        final EntityGroupNamePredicate condition =
                new EntityGroupNamePredicate(Collections.singletonList("GroupBad"), metadataProvider);
                
        final EntityDescriptor entity =
                metadataProvider.resolveSingle(new CriteriaSet(new EntityIdCriterion("https://idp-top.example.org")));
        Assert.assertNotNull(entity);
        
        Assert.assertFalse(condition.apply(entity));
    }
    
    @Test
    public void testGroupMatch() throws Exception {

        final EntityGroupNamePredicate condition =
                new EntityGroupNamePredicate(Collections.singletonList("GroupTop"), metadataProvider);
                
        final EntityDescriptor entity =
                metadataProvider.resolveSingle(new CriteriaSet(new EntityIdCriterion("https://idp-top.example.org")));
        Assert.assertNotNull(entity);
        
        Assert.assertTrue(condition.apply(entity));
    }

    @Test
    public void testGroupsMatch() throws Exception {

        final EntityGroupNamePredicate condition =
                new EntityGroupNamePredicate(Arrays.asList("GroupBad", "GroupSub2"), metadataProvider);
                
        final EntityDescriptor entity =
                metadataProvider.resolveSingle(new CriteriaSet(new EntityIdCriterion("https://idp-sub2a.example.org")));
        Assert.assertNotNull(entity);
        
        Assert.assertTrue(condition.apply(entity));
    }

    @Test
    public void testAffiliationMatch() throws Exception {

        final EntityGroupNamePredicate condition =
                new EntityGroupNamePredicate(Collections.singletonList("https://affiliation.example.org"), metadataProvider);
                
        final EntityDescriptor entity =
                metadataProvider.resolveSingle(new CriteriaSet(new EntityIdCriterion("https://idp-sub2a.example.org")));
        Assert.assertNotNull(entity);
        
        Assert.assertTrue(condition.apply(entity));
    }

    @Test
    public void testAffiliationNoMatch() throws Exception {

        final EntityGroupNamePredicate condition =
                new EntityGroupNamePredicate(Collections.singletonList("https://affiliation.example.org"), metadataProvider);
                
        final EntityDescriptor entity =
                metadataProvider.resolveSingle(new CriteriaSet(new EntityIdCriterion("https://idp-top.example.org")));
        Assert.assertNotNull(entity);
        
        Assert.assertFalse(condition.apply(entity));
    }

}