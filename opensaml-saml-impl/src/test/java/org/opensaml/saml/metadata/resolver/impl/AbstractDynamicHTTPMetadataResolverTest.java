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

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.util.XMLObjectSource;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.Criterion;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

public class AbstractDynamicHTTPMetadataResolverTest extends XMLObjectBaseTestCase {
    
    private MockDynamicHTTPMetadataResolver resolver;
    
    private HttpClientBuilder httpClientBuilder;
    
    private EntityDescriptor entityDescriptor;
    
    private byte[] entityDescriptorBytes;
    
    @BeforeMethod
    public void setUp() throws Exception {
        httpClientBuilder = new HttpClientBuilder();
        
        HttpClient httpClient = httpClientBuilder.buildClient();
        
        entityDescriptor = buildXMLObject(EntityDescriptor.DEFAULT_ELEMENT_NAME);
        entityDescriptor.setEntityID("https://foo1.example.org/idp/shibboleth");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLObjectSupport.marshallToOutputStream(entityDescriptor, baos);
        baos.flush();
        baos.close();
        entityDescriptorBytes = baos.toByteArray();
        
        resolver = new MockDynamicHTTPMetadataResolver(httpClient);
        resolver.setId("myDynamicResolver");
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
    public void testBasicRequest() throws ResolverException {
        // Test uses MDQ protocol
        String baseURL = "http://shibboleth.net:9000";
        String entityID = "https://foo1.example.org/idp/shibboleth";
        String requestURL = new MetadataQueryProtocolRequestURLBuilder(baseURL).apply(entityID);
        
        CriteriaSet criteriaSet = new CriteriaSet(new EntityIdCriterion(entityID), new RequestURLCriterion(requestURL));
        
        EntityDescriptor ed = resolver.resolveSingle(criteriaSet);
        Assert.assertNotNull(ed);
        Assert.assertEquals(ed.getEntityID(), entityID);
        Assert.assertNull(ed.getDOM());
    }
    
    @Test
    public void testResponseHandlerXMLObjectSource() throws Exception {
        ResponseHandler<XMLObject> responseHandler = resolver.new BasicMetadataResponseHandler();
        
        BasicHttpResponse httpResponse = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), HttpStatus.SC_OK, "OK");
        ByteArrayEntity entity = new ByteArrayEntity(entityDescriptorBytes);
        entity.setContentType(new BasicHeader(HttpHeaders.CONTENT_TYPE, "text/xml"));
        httpResponse.setEntity(entity);
        
        XMLObject result = responseHandler.handleResponse(httpResponse);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof EntityDescriptor);
        Assert.assertTrue(result.getObjectMetadata().containsKey(XMLObjectSource.class));
        Assert.assertEquals(result.getObjectMetadata().get(XMLObjectSource.class).size(), 1);
    }
    
    @Test
    public void testResponseHandlerBadStatusCode() throws Exception {
        ResponseHandler<XMLObject> responseHandler = resolver.new BasicMetadataResponseHandler();
        
        BasicHttpResponse httpResponse = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal Error");
        
        XMLObject result = responseHandler.handleResponse(httpResponse);
        Assert.assertNull(result);
    }
    
    @Test
    public void testResponseHandlerUnsupportedContentType() throws Exception {
        ResponseHandler<XMLObject> responseHandler = resolver.new BasicMetadataResponseHandler();
        
        BasicHttpResponse httpResponse = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), HttpStatus.SC_OK, "OK");
        ByteArrayEntity entity = new ByteArrayEntity(entityDescriptorBytes);
        entity.setContentType(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/foobar"));
        httpResponse.setEntity(entity);
        
        XMLObject result = responseHandler.handleResponse(httpResponse);
        Assert.assertNull(result);
    }
    
    
    
    // Helpers
    
    public static class MockDynamicHTTPMetadataResolver extends AbstractDynamicHTTPMetadataResolver {
        
        /**
         * Constructor.
         *
         * @param backgroundTaskTimer
         * @param client
         */
        public MockDynamicHTTPMetadataResolver(HttpClient client) {
            super(null, client);
        }

        /** {@inheritDoc} */
        protected String buildRequestURL(CriteriaSet criteria) {
            return criteria.get(RequestURLCriterion.class).requestURL;
        }
        
    }

    public static class RequestURLCriterion implements Criterion {
        public String requestURL;
        public RequestURLCriterion(String url) {
            requestURL = url;
        }
    }
    
}
