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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;

import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.joda.time.DateTime;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.security.httpclient.impl.SecurityEnhancedTLSSocketFactory;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.trust.impl.ExplicitKeyTrustEngine;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.PKIXValidationInformation;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.security.x509.X509Support;
import org.opensaml.security.x509.impl.BasicPKIXValidationInformation;
import org.opensaml.security.x509.impl.BasicX509CredentialNameEvaluator;
import org.opensaml.security.x509.impl.CertPathPKIXTrustEvaluator;
import org.opensaml.security.x509.impl.PKIXX509CredentialTrustEngine;
import org.opensaml.security.x509.impl.StaticPKIXValidationInformationResolver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Resources;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.httpclient.HttpClientSupport;
import net.shibboleth.utilities.java.support.repository.RepositorySupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;


/**
 * Test case for {@link FileBackedHTTPMetadataResolver}.
 */
public class FileBackedHTTPMetadataResolverTest extends XMLObjectBaseTestCase {
    
    private static final String DATA_PATH = "/org/opensaml/saml/metadata/resolver/impl/";
    
    private HttpClientBuilder httpClientBuilder;

    private String relativeMDResource;
    private String metadataURL;
    private String badMDURL;
    private String backupFilePath;
    private FileBackedHTTPMetadataResolver metadataProvider;
    private String entityID;
    private CriteriaSet criteriaSet;

    @BeforeMethod
    protected void setUp() throws Exception {
        httpClientBuilder = new HttpClientBuilder();
        
        relativeMDResource = "org/opensaml/saml/metadata/resolver/impl/08ced64cddc9f1578598b2cf71ae747b11d11472.xml";
        metadataURL = RepositorySupport.buildHTTPSResourceURL("java-opensaml", String.format("opensaml-saml-impl/src/test/resources/%s", relativeMDResource));
        
        entityID = "https://www.example.org/sp";
        badMDURL = "http://www.opensaml.org/foo/bar/baz/samlmd";
        backupFilePath = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") 
                + "filebacked-http-metadata.xml";
        
        criteriaSet = new CriteriaSet(new EntityIdCriterion(entityID));
    }

    @AfterMethod
    protected void tearDown() throws IOException {
        Path nioBackupFilePath = Paths.get(backupFilePath);
        Files.deleteIfExists(nioBackupFilePath);
    }
    
    /**
     * Tests the basic success case.
     * @throws ComponentInitializationException 
     * @throws ResolverException 
     */
    @Test
    public void testGetEntityDescriptor() throws Exception {
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        metadataProvider.initialize();
        
        Assert.assertFalse(metadataProvider.isInitializedFromBackupFile());
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNotNull(descriptor, "Retrieved entity descriptor was null");
        Assert.assertEquals(descriptor.getEntityID(), entityID, "Entity's ID does not match requested ID");
    }
    
    /**
     * Test fail-fast = true with known bad metadata URL.
     */
    @Test
    public void testFailFastBadURL() throws Exception {
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), badMDURL, backupFilePath);
        
        metadataProvider.setFailFastInitialization(true);
        metadataProvider.setParserPool(parserPool);
        
        try {
            metadataProvider.initialize();
            Assert.fail("metadata provider claims to have parsed known invalid data");
        } catch (ComponentInitializationException e) {
            //expected, do nothing
        }
    }
    
    /**
     * Test fail-fast = false with known bad metadata URL.
     */
    @Test
    public void testNoFailFastBadURL() throws Exception {
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), badMDURL, backupFilePath);
        
        metadataProvider.setFailFastInitialization(false);
        metadataProvider.setId("test");
        metadataProvider.setParserPool(parserPool);
        
        try {
            metadataProvider.initialize();
        } catch (ComponentInitializationException e) {
            Assert.fail("Provider failed init with fail-fast=false");
        }
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNull(descriptor);
    }
    
    /**
     *  Test fail-fast = true and bad backup file
     */
    @Test
    public void testFailFastBadBackupFile() throws Exception {
        try {
            // Use a known existing directory as backup file path, which is an invalid argument.
            metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, System.getProperty("java.io.tmpdir"));
        } catch (ResolverException e) {
            Assert.fail("Provider failed bad backup file in constructor");
            
        }
        metadataProvider.setFailFastInitialization(true);
        metadataProvider.setParserPool(parserPool);
        
        try {
            metadataProvider.initialize();
            Assert.fail("Provider passed init with bad backup file, fail-fast=true");
        } catch (ComponentInitializationException e) {
            // expected do nothing
        }
    }
    
    /**
     *  Test case of fail-fast = false and bad backup file
     *  
     * @throws ResolverException
     */
    @Test
    public void testNoFailFastBadBackupFile() throws Exception {
        try {
            // Use a known existing directory as backup file path, which is an invalid argument.
            metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, System.getProperty("java.io.tmpdir"));
        } catch (ResolverException e) {
            Assert.fail("Provider failed bad backup file in constructor");
            
        }
        metadataProvider.setFailFastInitialization(false);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        
        try {
            metadataProvider.initialize();
            Assert.assertFalse(metadataProvider.isInitializedFromBackupFile());
        } catch (ComponentInitializationException e) {
            Assert.fail("Provider failed init with bad backup file, fail-fast=false");
        }
        
        Assert.assertNotNull(metadataProvider.resolveSingle(criteriaSet), "Metadata retrieved from backing file was null");
    }
    
    /**
     * Tests initialization from backup file, followed shortly by real refresh via HTTP.
     * @throws ComponentInitializationException 
     * 
     * @throws ResolverException, ComponentInitializationException
     */
    @Test
    public void testInitFromBackupFile() throws Exception {
        File backupFile = new File(backupFilePath);
        try (FileOutputStream backupFileOutputStream = new FileOutputStream(backupFile)) {
            Resources.copy(Resources.getResource(relativeMDResource), backupFileOutputStream);
        }
        
        Assert.assertTrue(backupFile.exists(), "Backup file was not created");
        Assert.assertTrue(backupFile.length() > 0, "Backup file contains no data");
        
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setFailFastInitialization(true);
        metadataProvider.setId("test");
        metadataProvider.setBackupFileInitNextRefreshDelay(1000);
        metadataProvider.initialize();
        
        Assert.assertTrue(metadataProvider.isInitializedFromBackupFile());
        
        DateTime initRefresh = metadataProvider.getLastRefresh();
        DateTime initUpdate = metadataProvider.getLastUpdate();
        
        Assert.assertNotNull(metadataProvider.resolveSingle(criteriaSet), "Metadata inited from backing file was null");
        
        // Sleep past the artificial next refresh delay on init from backup file.
        Thread.sleep(metadataProvider.getBackupFileInitNextRefreshDelay() + 5000);
        
        Assert.assertTrue(initRefresh.isBefore(metadataProvider.getLastRefresh()));
        Assert.assertTrue(initUpdate.isBefore(metadataProvider.getLastUpdate()));
        
        Assert.assertNotNull(metadataProvider.resolveSingle(criteriaSet), "Metadata retrieved from HTTP refreshed metadata was null");
    }
    
    /**
     * Tests that backup file is not loaded on a refresh when already have cached metadata.
     * @throws ComponentInitializationException 
     * 
     * @throws ResolverException, ComponentInitializationException
     */
    @Test
    public void testNoBackupFileLoadWhenMetadataCached() throws Exception {
        File backupFile = new File(backupFilePath);
        try (FileOutputStream backupFileOutputStream = new FileOutputStream(backupFile)) {
            Resources.copy(Resources.getResource(relativeMDResource), backupFileOutputStream);
        }
        
        Assert.assertTrue(backupFile.exists(), "Backup file was not created");
        Assert.assertTrue(backupFile.length() > 0, "Backup file contains no data");
        
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), badMDURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setFailFastInitialization(true);
        metadataProvider.setId("test");
        metadataProvider.initialize();
        
        Assert.assertTrue(metadataProvider.isInitializedFromBackupFile());
        
        DateTime initRefresh = metadataProvider.getLastRefresh();
        DateTime initUpdate = metadataProvider.getLastUpdate();
        
        Assert.assertNotNull(metadataProvider.resolveSingle(criteriaSet), "Metadata retrieved from backing file was null");
        
        Thread.sleep(1000);
        
        // Manually do a refresh here, for testing via log examination that backing file not loaded due to existing cached metadata
        metadataProvider.refresh();
        
        // We should see refresh attempt, but no update.
        Assert.assertTrue(initRefresh.isBefore(metadataProvider.getLastRefresh()));
        Assert.assertEquals(initUpdate, metadataProvider.getLastUpdate());
        
        Assert.assertNotNull(metadataProvider.resolveSingle(criteriaSet), "Metadata retrieved from cached metadata was null");
    }
    
    @Test
    public void testTrustEngineSocketFactoryNoHTTPSNoTrustEngine() throws Exception  {
        // Make sure resolver works when TrustEngine socket factory is configured but just using an HTTP URL.
        httpClientBuilder.setTLSSocketFactory(buildTrustEngineSocketFactory());
        
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        metadataProvider.initialize();
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNotNull(descriptor, "Retrieved entity descriptor was null");
        Assert.assertEquals(descriptor.getEntityID(), entityID, "Entity's ID does not match requested ID");
    }
    
    @Test
    public void testTrustEngineSocketFactoryNoHTTPSWithTrustEngine() throws Exception  {
        // Make sure resolver works when TrustEngine socket factory is configured but just using an HTTP URL.
        httpClientBuilder.setTLSSocketFactory(buildTrustEngineSocketFactory());
        
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        metadataProvider.setTLSTrustEngine(buildExplicitKeyTrustEngine("repo-entity.crt"));
        metadataProvider.initialize();
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNotNull(descriptor, "Retrieved entity descriptor was null");
        Assert.assertEquals(descriptor.getEntityID(), entityID, "Entity's ID does not match requested ID");
    }
    
    @Test
    public void testHTTPSNoTrustEngine() throws Exception  {
        httpClientBuilder.setTLSSocketFactory(buildTrustEngineSocketFactory());
        
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath); 
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        metadataProvider.initialize();
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNotNull(descriptor, "Retrieved entity descriptor was null");
        Assert.assertEquals(descriptor.getEntityID(), entityID, "Entity's ID does not match requested ID");
    }
    
    @Test
    public void testHTTPSTrustEngineExplicitKey() throws Exception  {
        httpClientBuilder.setTLSSocketFactory(buildTrustEngineSocketFactory());
        
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        metadataProvider.setTLSTrustEngine(buildExplicitKeyTrustEngine("repo-entity.crt"));
        metadataProvider.initialize();
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNotNull(descriptor, "Retrieved entity descriptor was null");
        Assert.assertEquals(descriptor.getEntityID(), entityID, "Entity's ID does not match requested ID");
    }
    

    @Test(expectedExceptions=ComponentInitializationException.class)
    public void testHTTPSTrustEngineInvalidKey() throws Exception  {
        httpClientBuilder.setTLSSocketFactory(buildTrustEngineSocketFactory());
        
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        metadataProvider.setTLSTrustEngine(buildExplicitKeyTrustEngine("badKey.crt"));
        metadataProvider.initialize();
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNotNull(descriptor, "Retrieved entity descriptor was null");
        Assert.assertEquals(descriptor.getEntityID(), entityID, "Entity's ID does not match requested ID");
    }
    
    @Test
    public void testHTTPSTrustEngineValidPKIX() throws Exception  {
        httpClientBuilder.setTLSSocketFactory(buildTrustEngineSocketFactory());
        
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        metadataProvider.setTLSTrustEngine(buildPKIXTrustEngine("repo-rootCA.crt", null, false));
        metadataProvider.initialize();
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNotNull(descriptor, "Retrieved entity descriptor was null");
        Assert.assertEquals(descriptor.getEntityID(), entityID, "Entity's ID does not match requested ID");
    }
    
    @Test
    public void testHTTPSTrustEngineValidPKIXExplicitName() throws Exception  {
        httpClientBuilder.setTLSSocketFactory(buildTrustEngineSocketFactory());
        
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        metadataProvider.setTLSTrustEngine(buildPKIXTrustEngine("repo-rootCA.crt", "*.shibboleth.net", true));
        metadataProvider.initialize();
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNotNull(descriptor, "Retrieved entity descriptor was null");
        Assert.assertEquals(descriptor.getEntityID(), entityID, "Entity's ID does not match requested ID");
    }
    
    @Test(expectedExceptions=ComponentInitializationException.class)
    public void testHTTPSTrustEngineInvalidPKIX() throws Exception  {
        httpClientBuilder.setTLSSocketFactory(buildTrustEngineSocketFactory());
        
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        metadataProvider.setTLSTrustEngine(buildPKIXTrustEngine("badCA.crt", null, false));
        metadataProvider.initialize();
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNotNull(descriptor, "Retrieved entity descriptor was null");
        Assert.assertEquals(descriptor.getEntityID(), entityID, "Entity's ID does not match requested ID");
    }
    
    @Test(expectedExceptions=ComponentInitializationException.class)
    public void testHTTPSTrustEngineValidPKIXInvalidName() throws Exception  {
        httpClientBuilder.setTLSSocketFactory(buildTrustEngineSocketFactory());
        
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        metadataProvider.setTLSTrustEngine(buildPKIXTrustEngine("repo-rootCA.crt", "foobar.shibboleth.net", true));
        metadataProvider.initialize();
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNotNull(descriptor, "Retrieved entity descriptor was null");
        Assert.assertEquals(descriptor.getEntityID(), entityID, "Entity's ID does not match requested ID");
    }
    
    @Test(expectedExceptions=ComponentInitializationException.class)
    public void testHTTPSTrustEngineWrongSocketFactory() throws Exception  {
        // Trust engine set, but appropriate socket factory not set
        metadataProvider = new FileBackedHTTPMetadataResolver(httpClientBuilder.buildClient(), metadataURL, backupFilePath);
        metadataProvider.setParserPool(parserPool);
        metadataProvider.setId("test");
        metadataProvider.setTLSTrustEngine(buildExplicitKeyTrustEngine("repo-entity.crt"));
        metadataProvider.initialize();
        
        EntityDescriptor descriptor = metadataProvider.resolveSingle(criteriaSet);
        Assert.assertNotNull(descriptor, "Retrieved entity descriptor was null");
        Assert.assertEquals(descriptor.getEntityID(), entityID, "Entity's ID does not match requested ID");
    }
    
    
    // Helpers
    
    private LayeredConnectionSocketFactory buildTrustEngineSocketFactory() {
        return new SecurityEnhancedTLSSocketFactory(
                HttpClientSupport.buildNoTrustTLSSocketFactory(),
                SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER
                );
    }

    private TrustEngine<? super X509Credential> buildExplicitKeyTrustEngine(String cert) throws URISyntaxException, CertificateException {
        File certFile = new File(this.getClass().getResource(DATA_PATH + cert).toURI());
        X509Certificate entityCert = X509Support.decodeCertificate(certFile);
        X509Credential entityCredential = new BasicX509Credential(entityCert);
        return new ExplicitKeyTrustEngine(new StaticCredentialResolver(entityCredential));
    }
    
    private TrustEngine<? super X509Credential> buildPKIXTrustEngine(String cert, String name, boolean nameCheckEnabled) throws URISyntaxException, CertificateException {
        File certFile = new File(this.getClass().getResource(DATA_PATH + cert).toURI());
        X509Certificate rootCert = X509Support.decodeCertificate(certFile);
        PKIXValidationInformation info = new BasicPKIXValidationInformation(Collections.singletonList(rootCert), null, 5);
        Set<String> trustedNames = (Set<String>) (name != null ? Collections.singleton(name) : Collections.emptySet());
        StaticPKIXValidationInformationResolver resolver = new StaticPKIXValidationInformationResolver(Collections.singletonList(info), trustedNames);
        return new PKIXX509CredentialTrustEngine(resolver, 
                new CertPathPKIXTrustEvaluator(),
                (nameCheckEnabled ? new BasicX509CredentialNameEvaluator() : null));
    }

}