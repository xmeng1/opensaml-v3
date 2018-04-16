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

package org.opensaml.saml.common.binding.impl;

import java.util.Collections;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.profile.RequestContextBuilder;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.ParentProfileRequestContextLookup;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.SignatureSigningParametersResolver;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.criterion.SignatureSigningConfigurationCriterion;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Functions;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/** Unit test for {@link PopulateSignatureSigningParametersHandler}. */
public class PopulateSignatureSigningParametersHandlerTest extends OpenSAMLInitBaseTestCase {

    private ProfileRequestContext prc;
    
    private PopulateSignatureSigningParametersHandler handler;
    
    @BeforeMethod public void setUp() {
        prc = new RequestContextBuilder().buildProfileRequestContext();
        handler = new PopulateSignatureSigningParametersHandler();
    }
    
    @Test(expectedExceptions=ComponentInitializationException.class)
    public void testConfig() throws ComponentInitializationException {
        handler.initialize();
    }
    
    @Test(expectedExceptions=ConstraintViolationException.class)
    public void testNoContext() throws Exception {
        handler.setSignatureSigningParametersResolver(new MockResolver(false));
        handler.initialize();
        
        prc.setOutboundMessageContext(null);
        
        handler.invoke(prc.getOutboundMessageContext());
    }
    
    @Test(expectedExceptions=MessageHandlerException.class)
    public void testResolverError() throws Exception {
        handler.setSignatureSigningParametersResolver(new MockResolver(true));
        handler.initialize();
        
        handler.invoke(prc.getOutboundMessageContext());
    }    

    @Test
    public void testSuccess() throws Exception {
        handler.setSignatureSigningParametersResolver(new MockResolver(false));
        handler.initialize();
        
        handler.invoke(prc.getOutboundMessageContext());
        Assert.assertNotNull(prc.getOutboundMessageContext().getSubcontext(
                SecurityParametersContext.class).getSignatureSigningParameters());
    }    

    @Test
    public void testCopy() throws Exception {
        // Test copy from PRC to MessageContext
        handler.setSignatureSigningParametersResolver(new MockResolver(true));
        handler.setExistingParametersContextLookupStrategy(
                Functions.compose(new ChildContextLookup(SecurityParametersContext.class),
                        new ParentProfileRequestContextLookup()));
        handler.setSecurityParametersContextLookupStrategy(
                new ChildContextLookup<MessageContext,SecurityParametersContext>(SecurityParametersContext.class, true));
        handler.initialize();
        
        prc.getSubcontext(SecurityParametersContext.class, true).setSignatureSigningParameters(new SignatureSigningParameters());
        
        handler.invoke(prc.getOutboundMessageContext());
        Assert.assertSame(prc.getSubcontext(SecurityParametersContext.class).getSignatureSigningParameters(),
                prc.getOutboundMessageContext().getSubcontext(SecurityParametersContext.class).getSignatureSigningParameters());
    }    
    
    private class MockResolver implements SignatureSigningParametersResolver {

        private boolean throwException;
        
        public MockResolver(final boolean shouldThrow) {
            throwException = shouldThrow;
        }
        
        /** {@inheritDoc} */
        @Override
        public Iterable<SignatureSigningParameters> resolve(CriteriaSet criteria) throws ResolverException {
            return Collections.singletonList(resolveSingle(criteria));
        }

        /** {@inheritDoc} */
        @Override
        public SignatureSigningParameters resolveSingle(CriteriaSet criteria) throws ResolverException {
            if (throwException) {
                throw new ResolverException();
            }
            
            Constraint.isNotNull(criteria.get(SignatureSigningConfigurationCriterion.class), "Criterion was null");
            return new SignatureSigningParameters();
        }
        
    }
    
}