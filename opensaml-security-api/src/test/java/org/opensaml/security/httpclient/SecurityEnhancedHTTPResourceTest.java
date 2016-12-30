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

package org.opensaml.security.httpclient;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import net.shibboleth.ext.spring.resource.HTTPResource;
import net.shibboleth.ext.spring.resource.ResourceTestHelper;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.springframework.core.io.ClassPathResource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test for HTTPResource with security support added.
 */
public class SecurityEnhancedHTTPResourceTest {

    private final String existsURL =
            "https://git.shibboleth.net/view/?p=spring-extensions.git;a=blob_plain;f=src/test/resources/data/document.xml;h=e8ec7c0d20c7a6b8193e1868398cda0c28df45ed;hb=HEAD";

    private HttpClient client;
    private HttpClientSecurityParameters params;
    private HttpClientSecurityContextHandler handler;

    @BeforeClass public void setupClient() throws Exception {
        client = (new HttpClientBuilder()).buildClient();
    }
    
    @BeforeMethod public void setUp() throws ComponentInitializationException {
        params = new HttpClientSecurityParameters();
        handler = new HttpClientSecurityContextHandler();
        handler.setHttpClientSecurityParameters(params);
        handler.initialize();
    }

    @Test public void testNoSecurityAdded() throws IOException, ComponentInitializationException {
        final HTTPResource existsResource = new HTTPResource(client, existsURL);
        existsResource.setHttpClientContextHandler(handler);
        
        Assert.assertTrue(ResourceTestHelper.compare(existsResource, new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
    }

    @Test public void testHostnameRejected() throws IOException, ComponentInitializationException {
        final HTTPResource existsResource = new HTTPResource(client, existsURL);
        existsResource.setHttpClientContextHandler(handler);
        
        params.setHostnameVerifier(new X509HostnameVerifier() {
            public boolean verify(String arg0, SSLSession arg1) {
                return false;
            }
            public void verify(String host, SSLSocket ssl) throws IOException {
                throw new IOException("Rejecting hostname for test");
            }
            public void verify(String host, X509Certificate cert) throws SSLException {
                throw new SSLException("Rejecting hostname for test");
            }
            public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
                throw new SSLException("Rejecting hostname for test");
            }
        });
        
        Assert.assertFalse(existsResource.exists());
    }

    @Test public void testBadSSLProtocol() throws IOException, ComponentInitializationException {
        final HTTPResource existsResource = new HTTPResource(client, existsURL);
        existsResource.setHttpClientContextHandler(handler);
        
        params.setTLSProtocols(Collections.singletonList("SSLv3"));
        
        Assert.assertFalse(existsResource.exists());
    }

}