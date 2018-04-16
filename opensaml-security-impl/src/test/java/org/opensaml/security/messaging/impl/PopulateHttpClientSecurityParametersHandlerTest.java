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

package org.opensaml.security.messaging.impl;

import java.util.Collections;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.security.httpclient.HttpClientSecurityConfigurationCriterion;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.HttpClientSecurityParametersResolver;
import org.opensaml.security.messaging.HttpClientSecurityContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/** Unit test for {@link PopulateSignatureValidationParameters}. */
public class PopulateHttpClientSecurityParametersHandlerTest extends OpenSAMLInitBaseTestCase {

    private MessageContext messageContext;
    
    private PopulateHttpClientSecurityParametersHandler handler;
    
    @BeforeMethod public void setUp() {
        messageContext = new MessageContext<>();
        handler = new PopulateHttpClientSecurityParametersHandler();
    }
    
    @Test(expectedExceptions=ComponentInitializationException.class)
    public void testConfig() throws ComponentInitializationException {
        handler.initialize();
    }
    
    @Test(expectedExceptions=ConstraintViolationException.class)
    public void testNoContext() throws Exception {
        handler.setHttpClientSecurityParametersResolver(new MockResolver(false));
        handler.initialize();
        
        handler.invoke(null);
    }
    
    @Test(expectedExceptions=MessageHandlerException.class)
    public void testResolverError() throws Exception {
        handler.setHttpClientSecurityParametersResolver(new MockResolver(true));
        handler.initialize();
        
        handler.invoke(messageContext);
    }    

    @Test public void testSuccess() throws Exception {
        handler.setHttpClientSecurityParametersResolver(new MockResolver(false));
        handler.initialize();
        
        handler.invoke(messageContext);
        Assert.assertNotNull(messageContext.getSubcontext(HttpClientSecurityContext.class).getSecurityParameters());
    }    
    
    private class MockResolver implements HttpClientSecurityParametersResolver {

        private boolean throwException;
        
        public MockResolver(final boolean shouldThrow) {
            throwException = shouldThrow;
        }
        
        /** {@inheritDoc} */
        @Override
        public Iterable<HttpClientSecurityParameters> resolve(CriteriaSet criteria) throws ResolverException {
            return Collections.singletonList(resolveSingle(criteria));
        }

        /** {@inheritDoc} */
        @Override
        public HttpClientSecurityParameters resolveSingle(CriteriaSet criteria) throws ResolverException {
            if (throwException) {
                throw new ResolverException();
            }
            
            Constraint.isNotNull(criteria.get(HttpClientSecurityConfigurationCriterion.class), "Criterion was null");
            return new HttpClientSecurityParameters();
        }
        
    }
    
}