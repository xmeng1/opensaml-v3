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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.persist.MapLoadSaveManager;
import org.opensaml.core.xml.persist.XMLObjectLoadSaveManager;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.metadata.resolver.filter.impl.SignatureValidationFilter;
import org.opensaml.saml.metadata.resolver.impl.AbstractDynamicMetadataResolver.DynamicEntityBackingStore;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.security.crypto.JCAConstants;
import org.opensaml.security.crypto.KeySupport;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureSupport;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

public class AbstractDynamicMetadataResolverTest extends XMLObjectBaseTestCase {
    
    private Map<String, EntityDescriptor> sourceMap;
    
    private XMLObjectLoadSaveManager<EntityDescriptor> persistentCacheManager;
    private Map<String,EntityDescriptor> persistentCacheMap;
    private Function<EntityDescriptor, String> persistentCacheKeyGenerator;
    
    private MockDynamicResolver resolver;
    
    private String id1, id2, id3;
    private EntityDescriptor ed1, ed2, ed3;
    
    private Credential signingCred;
    private SignatureSigningParameters signingParams;
    private SignatureTrustEngine signatureTrustEngine;
    private SignatureValidationFilter signatureValidationFilter;
    
    @BeforeClass
    protected void setUpSigningSupport() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPair kp = KeySupport.generateKeyPair(JCAConstants.KEY_ALGO_RSA, 1024, null);
        signingCred = CredentialSupport.getSimpleCredential(kp.getPublic(), kp.getPrivate());
        
        signingParams = new SignatureSigningParameters();
        signingParams.setSigningCredential(signingCred);
        signingParams.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        signingParams.setSignatureCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        signingParams.setSignatureReferenceDigestMethod(SignatureConstants.ALGO_ID_DIGEST_SHA256);
        signingParams.setKeyInfoGenerator(DefaultSecurityConfigurationBootstrap.buildBasicKeyInfoGeneratorManager().getDefaultManager().getFactory(signingCred).newInstance());
        
        signatureTrustEngine = new ExplicitKeySignatureTrustEngine(
                new StaticCredentialResolver(signingCred), 
                DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver());
        
        signatureValidationFilter = new SignatureValidationFilter(signatureTrustEngine);
    }
    
    @BeforeMethod
    protected void setUpEntityData() throws MarshallingException, IOException, SecurityException, SignatureException {
        ByteArrayOutputStream baos = null;
        
        id1 = "urn:test:entity:1";
        ed1 = buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        ed1.setEntityID(id1);
        SignatureSupport.signObject(ed1, signingParams);
        Assert.assertTrue(ed1.isSigned());
        baos = new ByteArrayOutputStream();
        XMLObjectSupport.marshallToOutputStream(ed1, baos);
        baos.flush();
        baos.close();
        Assert.assertNotNull(ed1.getDOM());
        
        id2 = "urn:test:entity:2";
        ed2 = buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        ed2.setEntityID(id2);
        SignatureSupport.signObject(ed2, signingParams);
        Assert.assertTrue(ed2.isSigned());
        baos = new ByteArrayOutputStream();
        XMLObjectSupport.marshallToOutputStream(ed2, baos);
        baos.flush();
        baos.close();
        Assert.assertNotNull(ed2.getDOM());
        
        id3 = "urn:test:entity:3";
        ed3 = buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        ed3.setEntityID(id3);
        SignatureSupport.signObject(ed3, signingParams);
        Assert.assertTrue(ed3.isSigned());
        baos = new ByteArrayOutputStream();
        XMLObjectSupport.marshallToOutputStream(ed3, baos);
        baos.flush();
        baos.close();
        Assert.assertNotNull(ed3.getDOM());
        
    }
    
    @BeforeMethod
    protected void setUp() {
        sourceMap = new HashMap<>();
        persistentCacheMap = new HashMap<>();
        persistentCacheManager = new MapLoadSaveManager<>(persistentCacheMap);
        
        resolver = new MockDynamicResolver(sourceMap);
        resolver.setId("test123");
        resolver.setParserPool(XMLObjectProviderRegistrySupport.getParserPool());
    }
    
    @AfterMethod
    protected void tearDown() {
        if (resolver != null) {
            resolver.destroy();
        }
    }
    
    @Test
    public void testNoEntities() throws ComponentInitializationException, ResolverException {
        resolver.initialize();
        
        DynamicEntityBackingStore backingStore = resolver.getBackingStore();
        
        Assert.assertNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id1))));
        
        Assert.assertTrue(backingStore.getIndexedDescriptors().isEmpty());
    }
    
    @Test
    public void testBasicResolution() throws ComponentInitializationException, ResolverException {
        sourceMap.put(id1, ed1);
        sourceMap.put(id2, ed2);
        sourceMap.put(id3, ed3);
        
        resolver.initialize();
        
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id1))));
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id2))));
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id3))));
        
        DynamicEntityBackingStore backingStore = resolver.getBackingStore();
        
        Assert.assertTrue(backingStore.getIndexedDescriptors().containsKey(id1));
        Assert.assertEquals(backingStore.getIndexedDescriptors().get(id1).size(), 1);
        
        Assert.assertTrue(backingStore.getIndexedDescriptors().containsKey(id2));
        Assert.assertEquals(backingStore.getIndexedDescriptors().get(id2).size(), 1);
        
        Assert.assertTrue(backingStore.getIndexedDescriptors().containsKey(id3));
        Assert.assertEquals(backingStore.getIndexedDescriptors().get(id3).size(), 1);
    }
    
    @Test
    public void testDOMDropFromFetch() throws ComponentInitializationException, ResolverException {
        sourceMap.put(id1, ed1);
        
        resolver.initialize();
        
        EntityDescriptor result = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id1)));
        
        Assert.assertNotNull(result);
        Assert.assertNull(result.getDOM());
    }
    
    public void testBasicResolutionWithPersistentCache() throws ComponentInitializationException, ResolverException {
        sourceMap.put(id1, ed1);
        sourceMap.put(id2, ed2);
        sourceMap.put(id3, ed3);
        
        resolver.setPersistentCacheManager(persistentCacheManager);
        resolver.initialize();
        
        Assert.assertTrue(resolver.isPersistentCachingEnabled());
        
        Assert.assertEquals(persistentCacheMap.size(), 0);
        
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id1))));
        
        Assert.assertEquals(persistentCacheMap.size(), 1);
        
        String cacheKey = resolver.getPersistentCacheKeyGenerator().apply(ed1);
        Assert.assertTrue(persistentCacheMap.containsKey(cacheKey));
        Assert.assertSame(persistentCacheMap.get(cacheKey), ed1);
        
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id2))));
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id3))));
        
        Assert.assertEquals(persistentCacheMap.size(), 3);
        
        cacheKey = resolver.getPersistentCacheKeyGenerator().apply(ed2);
        Assert.assertTrue(persistentCacheMap.containsKey(cacheKey));
        Assert.assertSame(persistentCacheMap.get(cacheKey), ed2);
        
        cacheKey = resolver.getPersistentCacheKeyGenerator().apply(ed3);
        Assert.assertTrue(persistentCacheMap.containsKey(cacheKey));
        Assert.assertSame(persistentCacheMap.get(cacheKey), ed3);
        
    }
    
    @Test
    public void testWithPersistentCacheAndSignatureValidation() throws ComponentInitializationException, ResolverException {
        sourceMap.put(id1, ed1);
        sourceMap.put(id2, ed2);
        sourceMap.put(id3, ed3);
        
        resolver.setPersistentCacheManager(persistentCacheManager);
        resolver.setMetadataFilter(signatureValidationFilter);
        resolver.initialize();
        
        Assert.assertTrue(resolver.isPersistentCachingEnabled());
        
        Assert.assertEquals(persistentCacheMap.size(), 0);
        
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id1))));
        
        Assert.assertEquals(persistentCacheMap.size(), 1);
        
        String cacheKey = resolver.getPersistentCacheKeyGenerator().apply(ed1);
        Assert.assertTrue(persistentCacheMap.containsKey(cacheKey));
        Assert.assertSame(persistentCacheMap.get(cacheKey), ed1);
        
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id2))));
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id3))));
        
        Assert.assertEquals(persistentCacheMap.size(), 3);
        
        cacheKey = resolver.getPersistentCacheKeyGenerator().apply(ed2);
        Assert.assertTrue(persistentCacheMap.containsKey(cacheKey));
        Assert.assertSame(persistentCacheMap.get(cacheKey), ed2);
        
        cacheKey = resolver.getPersistentCacheKeyGenerator().apply(ed3);
        Assert.assertTrue(persistentCacheMap.containsKey(cacheKey));
        Assert.assertSame(persistentCacheMap.get(cacheKey), ed3);
        
    }
    
    @Test
    public void testInitFromPersistentCache() throws ComponentInitializationException, ResolverException, IOException {
        persistentCacheKeyGenerator = new AbstractDynamicMetadataResolver.DefaultCacheKeyGenerator();
        persistentCacheManager.save(persistentCacheKeyGenerator.apply(ed1), ed1);
        persistentCacheManager.save(persistentCacheKeyGenerator.apply(ed2), ed2);
        persistentCacheManager.save(persistentCacheKeyGenerator.apply(ed3), ed3);
        
        resolver.setPersistentCacheManager(persistentCacheManager);
        resolver.setPersistentCacheKeyGenerator(persistentCacheKeyGenerator);
        resolver.setInitializeFromPersistentCacheInBackground(false);
        
        resolver.initialize();
        
        DynamicEntityBackingStore backingStore = resolver.getBackingStore();
        
        // These will be there before any resolve() calls, loaded from the persistent cache
        Assert.assertTrue(backingStore.getIndexedDescriptors().containsKey(id1));
        Assert.assertEquals(backingStore.getIndexedDescriptors().get(id1).size(), 1);
        
        Assert.assertTrue(backingStore.getIndexedDescriptors().containsKey(id2));
        Assert.assertEquals(backingStore.getIndexedDescriptors().get(id2).size(), 1);
        
        Assert.assertTrue(backingStore.getIndexedDescriptors().containsKey(id3));
        Assert.assertEquals(backingStore.getIndexedDescriptors().get(id3).size(), 1);
        
        Assert.assertTrue(sourceMap.isEmpty());
        
        for (String entityID : Lists.newArrayList(id1, id2, id3)) {
            EntityDescriptor ed = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityID)));
            Assert.assertNotNull(ed);
            Assert.assertNull(ed.getDOM());
        }
    }
    
    @Test
    public void testInitFromPersistentCacheWithPredicate() throws ComponentInitializationException, ResolverException, IOException {
        persistentCacheKeyGenerator = new AbstractDynamicMetadataResolver.DefaultCacheKeyGenerator();
        persistentCacheManager.save(persistentCacheKeyGenerator.apply(ed1), ed1);
        persistentCacheManager.save(persistentCacheKeyGenerator.apply(ed2), ed2);
        persistentCacheManager.save(persistentCacheKeyGenerator.apply(ed3), ed3);
        
        resolver.setPersistentCacheManager(persistentCacheManager);
        resolver.setPersistentCacheKeyGenerator(persistentCacheKeyGenerator);
        resolver.setInitializeFromPersistentCacheInBackground(false);
        
        // Only load id1 from the cache
        resolver.setInitializationFromCachePredicate(
                new Predicate<EntityDescriptor>() {
                    public boolean apply(EntityDescriptor input) {
                        if (input == null) {
                            return false;
                        }
                        return Objects.equal(id1, input.getEntityID());
                    }
                });
        
        resolver.initialize();
        
        DynamicEntityBackingStore backingStore = resolver.getBackingStore();
        
        // This will be there before any resolve() calls, loaded from the persistent cache
        Assert.assertTrue(backingStore.getIndexedDescriptors().containsKey(id1));
        Assert.assertEquals(backingStore.getIndexedDescriptors().get(id1).size(), 1);
        
        // These were filtered out by the predicate
        Assert.assertFalse(backingStore.getIndexedDescriptors().containsKey(id2));
        Assert.assertFalse(backingStore.getIndexedDescriptors().containsKey(id3));
        
        
        Assert.assertTrue(sourceMap.isEmpty());
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id1))));
        Assert.assertNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id2))));
        Assert.assertNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id3))));
    }
    
    @Test
    public void testInitFromPersistentCacheWithDifferingKeys() throws ComponentInitializationException, ResolverException, IOException {
        persistentCacheKeyGenerator = new AbstractDynamicMetadataResolver.DefaultCacheKeyGenerator();
        persistentCacheManager.save("one", ed1);
        persistentCacheManager.save("two", ed2);
        persistentCacheManager.save("three", ed3);
        
        resolver.setPersistentCacheManager(persistentCacheManager);
        resolver.setPersistentCacheKeyGenerator(persistentCacheKeyGenerator);
        resolver.setInitializeFromPersistentCacheInBackground(false);
        
        resolver.initialize();
        
        // Keys should have been updated
        Assert.assertFalse(persistentCacheMap.containsKey("one"));
        Assert.assertTrue(persistentCacheMap.containsKey(persistentCacheKeyGenerator.apply(ed1)));
        
        Assert.assertFalse(persistentCacheMap.containsKey("two"));
        Assert.assertTrue(persistentCacheMap.containsKey(persistentCacheKeyGenerator.apply(ed2)));
        
        Assert.assertFalse(persistentCacheMap.containsKey("three"));
        Assert.assertTrue(persistentCacheMap.containsKey(persistentCacheKeyGenerator.apply(ed3)));
        
        DynamicEntityBackingStore backingStore = resolver.getBackingStore();
        
        // These will be there before any resolve() calls, loaded from the persistent cache
        Assert.assertTrue(backingStore.getIndexedDescriptors().containsKey(id1));
        Assert.assertEquals(backingStore.getIndexedDescriptors().get(id1).size(), 1);
        
        Assert.assertTrue(backingStore.getIndexedDescriptors().containsKey(id2));
        Assert.assertEquals(backingStore.getIndexedDescriptors().get(id2).size(), 1);
        
        Assert.assertTrue(backingStore.getIndexedDescriptors().containsKey(id3));
        Assert.assertEquals(backingStore.getIndexedDescriptors().get(id3).size(), 1);
        
        Assert.assertTrue(sourceMap.isEmpty());
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id1))));
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id2))));
        Assert.assertNotNull(resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(id3))));
    }
    
    // Helper classes
    
    private static class MockDynamicResolver extends AbstractDynamicMetadataResolver {
        
        private Map<String,EntityDescriptor> originSourceMap;

        public MockDynamicResolver(Map<String, EntityDescriptor> map) {
            this(map, null);
        }
        
        public MockDynamicResolver(Map<String, EntityDescriptor> map, Timer backgroundTaskTimer) {
            super(backgroundTaskTimer);
            originSourceMap = map;
        }

        protected XMLObject fetchFromOriginSource(CriteriaSet criteria) throws IOException {
            EntityIdCriterion entityIdCriterion = criteria.get(EntityIdCriterion.class);
            if (entityIdCriterion != null) {
                return originSourceMap.get(entityIdCriterion.getEntityId());
            } else {
                return null;
            }
        }
        
    }
    
}
