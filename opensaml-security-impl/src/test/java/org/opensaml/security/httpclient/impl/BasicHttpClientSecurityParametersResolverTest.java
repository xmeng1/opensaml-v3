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

package org.opensaml.security.httpclient.impl;

import java.io.File;
import java.net.URISyntaxException;
import java.security.KeyException;
import java.security.cert.CertificateException;

import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.crypto.KeySupport;
import org.opensaml.security.httpclient.HttpClientSecurityConfigurationCriterion;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.security.x509.X509Support;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 *
 */
public class BasicHttpClientSecurityParametersResolverTest {
    
    private BasicHttpClientSecurityParametersResolver resolver;
    
    private X509Credential x509Credential1, x509Credential2, x509Credential3;
    
    @BeforeMethod
    protected void setUp() throws CertificateException, URISyntaxException, KeyException {
        resolver = new BasicHttpClientSecurityParametersResolver();
        x509Credential1 = CredentialSupport.getSimpleCredential(
                X509Support.decodeCertificate(new File(this.getClass().getResource("/data/certificate.pem").toURI())), 
                KeySupport.decodePrivateKey(new File(this.getClass().getResource("/data/rsa-privkey-nopass.pem").toURI()), null)
                );
        x509Credential2 = CredentialSupport.getSimpleCredential(
                X509Support.decodeCertificate(new File(this.getClass().getResource("/data/certificate.pem").toURI())), 
                KeySupport.decodePrivateKey(new File(this.getClass().getResource("/data/rsa-privkey-nopass.pem").toURI()), null)
                );
        x509Credential3 = CredentialSupport.getSimpleCredential(
                X509Support.decodeCertificate(new File(this.getClass().getResource("/data/certificate.pem").toURI())), 
                KeySupport.decodePrivateKey(new File(this.getClass().getResource("/data/rsa-privkey-nopass.pem").toURI()), null)
                );
    }
    
    @Test
    public void testSingleConfigFullyPopulated() throws ResolverException {
        CriteriaSet criteria = new CriteriaSet(new HttpClientSecurityConfigurationCriterion(
                buildBaseConfiguration(x509Credential1)));
        
        HttpClientSecurityParameters params = resolver.resolveSingle(criteria);
        
        Assert.assertNotNull(params);;
        Assert.assertNotNull(params.getAuthCache());
        Assert.assertNotNull(params.getClientTLSCredential());
        Assert.assertNotNull(params.getCredentialsProvider());
        Assert.assertNotNull(params.getHostnameVerifier());
        Assert.assertNotNull(params.getTLSCipherSuites());
        Assert.assertNotNull(params.getTLSCriteriaSet());
        Assert.assertNotNull(params.getTLSProtocols());
        Assert.assertNotNull(params.getTLSTrustEngine());
    }
    
    @Test
    public void testSingleConfigEmpty() throws ResolverException {
        CriteriaSet criteria = new CriteriaSet(new HttpClientSecurityConfigurationCriterion(
                new BasicHttpClientSecurityConfiguration()));
        
        HttpClientSecurityParameters params = resolver.resolveSingle(criteria);
        
        Assert.assertNotNull(params);;
        Assert.assertNull(params.getAuthCache());
        Assert.assertNull(params.getClientTLSCredential());
        Assert.assertNull(params.getCredentialsProvider());
        Assert.assertNull(params.getHostnameVerifier());
        Assert.assertNull(params.getTLSCipherSuites());
        Assert.assertNull(params.getTLSCriteriaSet());
        Assert.assertNull(params.getTLSProtocols());
        Assert.assertNull(params.getTLSTrustEngine());
    }
    
    @Test
    public void testMultipleConfigsSimple() throws ResolverException {
        CriteriaSet criteria;
        HttpClientSecurityParameters params;
        
        criteria = new CriteriaSet(new HttpClientSecurityConfigurationCriterion(
                buildBaseConfiguration(x509Credential1),
                new BasicHttpClientSecurityConfiguration(),
                new BasicHttpClientSecurityConfiguration()));
        
        params = resolver.resolveSingle(criteria);
        
        Assert.assertNotNull(params);;
        Assert.assertNotNull(params.getAuthCache());
        Assert.assertNotNull(params.getClientTLSCredential());
        Assert.assertNotNull(params.getCredentialsProvider());
        Assert.assertNotNull(params.getHostnameVerifier());
        Assert.assertNotNull(params.getTLSCipherSuites());
        Assert.assertNotNull(params.getTLSCriteriaSet());
        Assert.assertNotNull(params.getTLSProtocols());
        Assert.assertNotNull(params.getTLSTrustEngine());
        
        criteria = new CriteriaSet(new HttpClientSecurityConfigurationCriterion(
                new BasicHttpClientSecurityConfiguration(),
                buildBaseConfiguration(x509Credential1),
                new BasicHttpClientSecurityConfiguration()));
        
        params = resolver.resolveSingle(criteria);
        
        Assert.assertNotNull(params);;
        Assert.assertNotNull(params.getAuthCache());
        Assert.assertNotNull(params.getClientTLSCredential());
        Assert.assertNotNull(params.getCredentialsProvider());
        Assert.assertNotNull(params.getHostnameVerifier());
        Assert.assertNotNull(params.getTLSCipherSuites());
        Assert.assertNotNull(params.getTLSCriteriaSet());
        Assert.assertNotNull(params.getTLSProtocols());
        Assert.assertNotNull(params.getTLSTrustEngine());
        
        criteria = new CriteriaSet(new HttpClientSecurityConfigurationCriterion(
                new BasicHttpClientSecurityConfiguration(),
                new BasicHttpClientSecurityConfiguration(),
                buildBaseConfiguration(x509Credential1)));
        
        params = resolver.resolveSingle(criteria);
        
        Assert.assertNotNull(params);;
        Assert.assertNotNull(params.getAuthCache());
        Assert.assertNotNull(params.getClientTLSCredential());
        Assert.assertNotNull(params.getCredentialsProvider());
        Assert.assertNotNull(params.getHostnameVerifier());
        Assert.assertNotNull(params.getTLSCipherSuites());
        Assert.assertNotNull(params.getTLSCriteriaSet());
        Assert.assertNotNull(params.getTLSProtocols());
        Assert.assertNotNull(params.getTLSTrustEngine());
    }
    
    @Test
    public void testMultipleConfigsLayered() throws ResolverException {
        CriteriaSet criteria;
        HttpClientSecurityParameters params;
        
        BasicHttpClientSecurityConfiguration config1 = buildBaseConfiguration(x509Credential1);
        BasicHttpClientSecurityConfiguration config2 = buildBaseConfiguration(x509Credential2);
        BasicHttpClientSecurityConfiguration config3 = buildBaseConfiguration(x509Credential3);
        
        criteria = new CriteriaSet(new HttpClientSecurityConfigurationCriterion(
                config1,
                config2,
                config3));
        
        params = resolver.resolveSingle(criteria);
        
        Assert.assertNotNull(params);;
        Assert.assertNotNull(params.getClientTLSCredential());
        Assert.assertSame(params.getClientTLSCredential(), config1.getClientTLSCredential());
        
        config1.setClientTLSCredential(null);
        
        criteria = new CriteriaSet(new HttpClientSecurityConfigurationCriterion(
                config1,
                config2,
                config3));
        
        params = resolver.resolveSingle(criteria);
        
        Assert.assertNotNull(params);;
        Assert.assertNotNull(params.getClientTLSCredential());
        Assert.assertSame(params.getClientTLSCredential(), config2.getClientTLSCredential());
        
        config2.setClientTLSCredential(null);
        
        criteria = new CriteriaSet(new HttpClientSecurityConfigurationCriterion(
                config1,
                config2,
                config3));
        
        params = resolver.resolveSingle(criteria);
        
        Assert.assertNotNull(params);;
        Assert.assertNotNull(params.getClientTLSCredential());
        Assert.assertSame(params.getClientTLSCredential(), config3.getClientTLSCredential());
    }
    
    
    // Helpers
    
    private BasicHttpClientSecurityConfiguration buildBaseConfiguration(X509Credential x509Credential)  {
        BasicHttpClientSecurityConfiguration config = new BasicHttpClientSecurityConfiguration();
        config.setAuthCache(new BasicAuthCache());
        config.setClientTLSCredential(x509Credential);
        config.setCredentialsProvider(new BasicCredentialsProvider());
        config.setHostnameVerifier(new StrictHostnameVerifier());
        config.setTLSCipherSuites(Lists.newArrayList("test"));
        config.setTLSCriteriaSet(new CriteriaSet());
        config.setTLSProtocols(Lists.newArrayList("test"));
        config.setTLSTrustEngine(new MockTrustEngine());
        return config;
    }
    
    public static class MockTrustEngine implements TrustEngine<X509Credential>  {
        public boolean validate(X509Credential token, CriteriaSet trustBasisCriteria) throws SecurityException {
            return false;
        }
    }

}
