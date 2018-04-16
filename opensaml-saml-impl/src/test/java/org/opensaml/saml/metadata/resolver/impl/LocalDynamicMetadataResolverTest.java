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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.persist.MapLoadSaveManager;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.security.crypto.JCAConstants;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.codec.StringDigester;
import net.shibboleth.utilities.java.support.codec.StringDigester.OutputFormat;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 *
 */
public class LocalDynamicMetadataResolverTest extends XMLObjectBaseTestCase {
    
    private LocalDynamicMetadataResolver resolver;
    
    private MapLoadSaveManager<XMLObject> sourceManager;
    
    private String entityID1, entityID2;
    
    private EntityDescriptor entity1, entity2;
    
    private StringDigester sha1Digester;
    
    @BeforeMethod
    public void setUp() throws NoSuchAlgorithmException, ComponentInitializationException {
        sha1Digester = new StringDigester(JCAConstants.DIGEST_SHA1, OutputFormat.HEX_LOWER);
        
        sourceManager = new MapLoadSaveManager<>();
        
        entityID1 = "urn:test:entity1";
        entityID2 = "urn:test:entity2";
        
        entity1 = buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        entity1.setEntityID(entityID1);
        entity2 = buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        entity2.setEntityID(entityID2);
        
        resolver = new LocalDynamicMetadataResolver(sourceManager);
        resolver.setId("abc123");
        resolver.setParserPool(parserPool);
        resolver.initialize();
    }
    
    @AfterMethod
    public void tearDown() {
        if (resolver != null) {
            resolver.destroy();
        }
    }
    
    @Test
    public void testEmptySource() throws ResolverException {
        Assert.assertNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityID1))));
        Assert.assertNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityID2))));
    }

    @Test
    public void testBasicResolveLifecycle() throws ResolverException, IOException {
        sourceManager.save(sha1Digester.apply(entityID1), entity1);
        
        Assert.assertSame(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityID1))), entity1);
        
        // Not there yet
        Assert.assertNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityID2))));
        
        // Add it
        sourceManager.save(sha1Digester.apply(entityID2), entity2);
        
        // Now should be resolveable
        Assert.assertSame(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityID2))), entity2);
        
        // Remove source data
        sourceManager.remove(sha1Digester.apply(entityID1));
        sourceManager.remove(sha1Digester.apply(entityID2));
        Assert.assertNull(sourceManager.load(sha1Digester.apply(entityID1)));
        Assert.assertNull(sourceManager.load(sha1Digester.apply(entityID2)));
        
        // Should still be live b/c already resolved
        Assert.assertSame(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityID1))), entity1);
        Assert.assertSame(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityID2))), entity2);
    }
    
    @Test
    public void testCtorSourceKeyGenerator() throws ComponentInitializationException, IOException, ResolverException {
        resolver.destroy();
        
        resolver = new LocalDynamicMetadataResolver(null, sourceManager, new IdentityEntityIDGenerator());
        resolver.setId("abc123");
        resolver.setParserPool(parserPool);
        resolver.initialize();
        
        sourceManager.save(entityID1, entity1);
        
        Assert.assertSame(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityID1))), entity1);
    }
    
    @Test(expectedExceptions=ConstraintViolationException.class)
    public void testCtorMissingManager() {
        resolver = new LocalDynamicMetadataResolver(null);
    }
    
}
