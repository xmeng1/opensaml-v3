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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.ext.saml2mdquery.AttributeQueryDescriptorType;
import org.opensaml.saml.metadata.criteria.role.EvaluableRoleDescriptorCriterion;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 *
 */
public class PredicateRoleDescriptorResolverTest extends XMLObjectBaseTestCase {
    
    @Test
    public void testNoEntitiesResolveSingle() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.<EntityDescriptor>newArrayList()));
        roleResolver.initialize();
        
        RoleDescriptor roleDescriptor = roleResolver.resolveSingle(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME)));
        Assert.assertNull(roleDescriptor, "Resolved RoleDescriptor was null");
    }
    
    @Test
    public void testNoEntitiesResolveMulti() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.<EntityDescriptor>newArrayList()));
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME)));
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");
        Assert.assertFalse(roleDescriptors.iterator().hasNext());
    }
    
    @Test
    public void testSingleEntityResolveSingleNoProtocol() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor())));
        roleResolver.initialize();
        
        RoleDescriptor roleDescriptor = roleResolver.resolveSingle(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME)));
        Assert.assertNotNull(roleDescriptor, "Resolved RoleDescriptor was null");
        Assert.assertEquals(SPSSODescriptor.DEFAULT_ELEMENT_NAME, roleDescriptor.getElementQName(),
                "Saw incorrect role type");
    }
    
    @Test
    public void testSingleEntityResolveSingleWithFalsePredicate() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor())));
        roleResolver.initialize();
        
        RoleDescriptor roleDescriptor = roleResolver.resolveSingle(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME),
                new BooleanPredicateCriterion(false)
                ));
        Assert.assertNull(roleDescriptor, "Resolved RoleDescriptor was not null");
    }
    
    @Test
    public void testSingleEntityResolveMultiNoProtocol() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor())));
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME)));
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");

        int count = 0;
        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            Assert.assertEquals(SPSSODescriptor.DEFAULT_ELEMENT_NAME, roleDescriptor.getElementQName(),
                    "Saw incorrect role type");
            count++;
        }

        Assert.assertEquals(2, count, "Resolved unexpected number of RoleDescriptors");
    }
    
    @Test
    public void testSingleEntityResolveMultiWithFalsePredicate() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor())));
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME),
                new BooleanPredicateCriterion(false)
                ));
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");

        int count = 0;
        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            Assert.assertEquals(SPSSODescriptor.DEFAULT_ELEMENT_NAME, roleDescriptor.getElementQName(),
                    "Saw incorrect role type");
            count++;
        }

        Assert.assertEquals(0, count, "Resolved unexpected number of RoleDescriptors");
    }
    
    @Test
    public void testSingleEntityResolveSingleWithProtocol() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor())));
        roleResolver.initialize();
        
        RoleDescriptor roleDescriptor = roleResolver.resolveSingle(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME),
                new ProtocolCriterion(SAMLConstants.SAML20P_NS)));
        
        Assert.assertNotNull(roleDescriptor, "Resolved RoleDescriptor was null");
        Assert.assertEquals(SPSSODescriptor.DEFAULT_ELEMENT_NAME, roleDescriptor.getElementQName(),
                "Saw incorrect role type");
        Assert.assertTrue(roleDescriptor.getSupportedProtocols().contains(SAMLConstants.SAML20P_NS),
                "Returned RoleDescriptor didn't support specified protocol");
    }
    
    @Test
    public void testSingleEntityResolveMultiWithProtocol() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor())));
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME),
                new ProtocolCriterion(SAMLConstants.SAML20P_NS)));
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");

        int count = 0;
        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            Assert.assertEquals(SPSSODescriptor.DEFAULT_ELEMENT_NAME, roleDescriptor.getElementQName(),
                    "Saw incorrect role type");
            Assert.assertTrue(roleDescriptor.getSupportedProtocols().contains(SAMLConstants.SAML20P_NS),
                    "Returned RoleDescriptor didn't support specified protocol");
            count++;
        }

        Assert.assertEquals(1, count, "Resolved unexpected number of RoleDescriptors");
    }
    
    @Test
    public void testSingleEntityResolveMultiNoRoleCriteria() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor())));
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet());
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");

        int count = 0;
        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            Assert.assertEquals(SPSSODescriptor.DEFAULT_ELEMENT_NAME, roleDescriptor.getElementQName(),
                    "Saw incorrect role type");
            count++;
        }

        Assert.assertEquals(0, count, "Resolved unexpected number of RoleDescriptors");
    }
    
    @Test
    public void testSingleEntityResolveMultiNoRoleCriteriaWithFalsePredicate() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor())));
        roleResolver.setResolveViaPredicatesOnly(true);
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet(
                new BooleanPredicateCriterion(false)
                ));
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");

        int count = 0;
        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            count++;
        }

        Assert.assertEquals(0, count, "Resolved unexpected number of RoleDescriptors");
    }
    
    @Test
    public void testSingleEntityResolveMultiNoRoleCriteriaWithTruePredicate() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor())));
        roleResolver.setResolveViaPredicatesOnly(true);
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet(
                new BooleanPredicateCriterion(true)
                ));
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");

        int count = 0;
        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            count++;
        }

        Assert.assertEquals(3, count, "Resolved unexpected number of RoleDescriptors");
    }
    
    @Test
    public void testMultiEntityResolveSingleWithProtocol() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor(), buildTestDescriptor())));
        roleResolver.initialize();
        
        RoleDescriptor roleDescriptor = roleResolver.resolveSingle(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME),
                new ProtocolCriterion(SAMLConstants.SAML20P_NS)));
        
        Assert.assertNotNull(roleDescriptor, "Resolved RoleDescriptor was null");
        Assert.assertEquals(SPSSODescriptor.DEFAULT_ELEMENT_NAME, roleDescriptor.getElementQName(),
                "Saw incorrect role type");
        Assert.assertTrue(roleDescriptor.getSupportedProtocols().contains(SAMLConstants.SAML20P_NS),
                "Returned RoleDescriptor didn't support specified protocol");
    }
    
    @Test
    public void testMultiEntityResolveMultiWithProtocol() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor(), buildTestDescriptor())));
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME),
                new ProtocolCriterion(SAMLConstants.SAML20P_NS)));
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");

        int count = 0;
        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            Assert.assertEquals(SPSSODescriptor.DEFAULT_ELEMENT_NAME, roleDescriptor.getElementQName(),
                    "Saw incorrect role type");
            Assert.assertTrue(roleDescriptor.getSupportedProtocols().contains(SAMLConstants.SAML20P_NS),
                    "Returned RoleDescriptor didn't support specified protocol");
            count++;
        }

        Assert.assertEquals(2, count, "Resolved unexpected number of RoleDescriptors");
    }
    
    @Test
    public void testMultiEntityResolveSingleNoProtocol() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor(), buildTestDescriptor())));
        roleResolver.initialize();
        
        RoleDescriptor roleDescriptor = roleResolver.resolveSingle(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME)));
        Assert.assertNotNull(roleDescriptor, "Resolved RoleDescriptor was null");
        Assert.assertEquals(SPSSODescriptor.DEFAULT_ELEMENT_NAME, roleDescriptor.getElementQName(),
                "Saw incorrect role type");
    }
    
    @Test
    public void testMultiEntityResolveMultiNoProtocol() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor(), buildTestDescriptor())));
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME)));
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");

        int count = 0;
        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            Assert.assertEquals(SPSSODescriptor.DEFAULT_ELEMENT_NAME, roleDescriptor.getElementQName(),
                    "Saw incorrect role type");
            count++;
        }

        Assert.assertEquals(4, count, "Resolved unexpected number of RoleDescriptors");
    }
    
    @Test
    public void testMultiEntityResolveMultiNoRoleCriteria() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor(), buildTestDescriptor())));
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet(       
                new BooleanPredicateCriterion(true)
                ));
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");

        int count = 0;
        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            Assert.assertEquals(SPSSODescriptor.DEFAULT_ELEMENT_NAME, roleDescriptor.getElementQName(),
                    "Saw incorrect role type");
            count++;
        }

        Assert.assertEquals(0, count, "Resolved unexpected number of RoleDescriptors");
    }
    
    @Test
    public void testMultiEntityResolveMultiNoRoleCriteriaWithFalsePredicate() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor(), buildTestDescriptor())));
        roleResolver.setResolveViaPredicatesOnly(true);
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet(
                new BooleanPredicateCriterion(false)
                ));
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");

        int count = 0;
        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            count++;
        }

        Assert.assertEquals(0, count, "Resolved unexpected number of RoleDescriptors");
    }
    
    @Test
    public void testMultiEntityResolveMultiNoRoleCriteriaWithTruePredicate() throws ResolverException, ComponentInitializationException {
        PredicateRoleDescriptorResolver roleResolver = 
                new PredicateRoleDescriptorResolver(new StaticMetadataResolver(Lists.newArrayList(buildTestDescriptor(), buildTestDescriptor())));
        roleResolver.setResolveViaPredicatesOnly(true);
        roleResolver.initialize();
        
        Iterable<RoleDescriptor> roleDescriptors = roleResolver.resolve(new CriteriaSet(
                new BooleanPredicateCriterion(true)
                ));
        Assert.assertNotNull(roleDescriptors, "Resolved RoleDescriptor iterable was null");

        int count = 0;
        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            count++;
        }

        Assert.assertEquals(6, count, "Resolved unexpected number of RoleDescriptors");
    }

    
    // Helper methods
    
    private EntityDescriptor buildTestDescriptor() {
        EntityDescriptor entityDescriptor = buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        
        SPSSODescriptor spssoDescriptor1 = buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        spssoDescriptor1.addSupportedProtocol(SAMLConstants.SAML11P_NS);
        entityDescriptor.getRoleDescriptors().add(spssoDescriptor1);
        
        AttributeQueryDescriptorType aqDescriptor = buildXMLObject(AttributeQueryDescriptorType.TYPE_NAME);
        aqDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);
        entityDescriptor.getRoleDescriptors().add(aqDescriptor);
        
        SPSSODescriptor spssoDescriptor2 = buildXMLObject(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        spssoDescriptor2.addSupportedProtocol(SAMLConstants.SAML20P_NS);
        entityDescriptor.getRoleDescriptors().add(spssoDescriptor2);
        
        return entityDescriptor;
    }


    public static class StaticMetadataResolver implements MetadataResolver {
        
        private List<EntityDescriptor> entityDescriptors;
        
        public StaticMetadataResolver(List<EntityDescriptor> descriptors) {
            entityDescriptors = descriptors;
        }
            
        @Nullable public String getId() { return "foo"; }

        @Nullable
        public EntityDescriptor resolveSingle(CriteriaSet criteria) throws ResolverException {
            return entityDescriptors.get(0);
        }

        @Nonnull
        public Iterable<EntityDescriptor> resolve(CriteriaSet criteria) throws ResolverException {
            return entityDescriptors;
        }

        public boolean isRequireValidMetadata() {
            return false;
        }

        public void setRequireValidMetadata(boolean requireValidMetadata) { }

        public MetadataFilter getMetadataFilter() {
            return null;
        }

        public void setMetadataFilter(MetadataFilter newFilter) { }
    }
    
    public static class BooleanPredicateCriterion implements EvaluableRoleDescriptorCriterion {
        
        private boolean result;
        
        public BooleanPredicateCriterion(boolean flag) {
            result = flag;
        }

        /** {@inheritDoc} */
        public boolean apply(RoleDescriptor input) {
            return result;
        }
        
    }
    
}
