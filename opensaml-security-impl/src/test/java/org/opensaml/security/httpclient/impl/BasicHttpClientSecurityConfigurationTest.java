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
import java.security.cert.CertificateException;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.security.x509.X509Support;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 *
 */
public class BasicHttpClientSecurityConfigurationTest {
    
    private X509Credential x509Credential;
    
    @BeforeMethod
    protected void setUp() throws CertificateException, URISyntaxException {
        x509Credential = CredentialSupport.getSimpleCredential(
                X509Support.decodeCertificate(new File(this.getClass().getResource("/data/certificate.pem").toURI())), null);
    }
    
    @Test
    public void testBasic() {
        BasicHttpClientSecurityConfiguration config = new BasicHttpClientSecurityConfiguration();
        config.setClientTLSCredential(x509Credential);
        config.setCredentialsProvider(new BasicCredentialsProvider());
        config.setHostnameVerifier(new StrictHostnameVerifier());
        config.setTLSCipherSuites(Lists.newArrayList("test"));
        config.setTLSProtocols(Lists.newArrayList("test"));
        config.setTLSTrustEngine(new MockTrustEngine());
        
        Assert.assertNotNull(config.getClientTLSCredential());
        Assert.assertNotNull(config.getCredentialsProvider());
        Assert.assertNotNull(config.getHostnameVerifier());
        Assert.assertNotNull(config.getTLSCipherSuites());
        Assert.assertNotNull(config.getTLSProtocols());
        Assert.assertNotNull(config.getTLSTrustEngine());
    }
    
    @Test
    public void testEmptyLists() {
        BasicHttpClientSecurityConfiguration config = new BasicHttpClientSecurityConfiguration();
        config.setTLSCipherSuites(Lists.<String>newArrayList());
        config.setTLSProtocols(Lists.<String>newArrayList());
        
        Assert.assertNull(config.getTLSCipherSuites());
        Assert.assertNull(config.getTLSProtocols());
    }
    
    @Test
    public void testCredentialsProvider() {
        BasicHttpClientSecurityConfiguration config = new BasicHttpClientSecurityConfiguration();
        config.setBasicCredentials(new UsernamePasswordCredentials("test", "test"));
        
        Assert.assertNotNull(config.getCredentialsProvider());
    }
    
    
    // Helpers
    
    public static class MockTrustEngine implements TrustEngine<X509Credential>  {
        public boolean validate(X509Credential token, CriteriaSet trustBasisCriteria) throws SecurityException {
            return false;
        }
    }

}
