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

package org.opensaml.saml.common.binding;

import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

public class SAMLBindingSupportTest {
    
    @Test
    public void testConvertSAML2ArtifactEndpointIndex() {
        Assert.assertEquals(SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(new byte[] {0x00, 0x00}), 0);
        Assert.assertEquals(SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(new byte[] {0x00, 0x01}), 1);
        Assert.assertEquals(SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(new byte[] {0x00, 0x02}), 2);
        
        Assert.assertEquals(SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(new byte[] {0x00, 0x10}), 16);
        
        Assert.assertEquals(SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(new byte[] {0x00, (byte) 0xFE}), 254);
        Assert.assertEquals(SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(new byte[] {0x00, (byte) 0xFF}), 255);
        
        // This is the largest unsigned value supported
        Assert.assertEquals(SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(new byte[] {(byte) 0x7F, (byte) 0xFF}), 32767);
        
        try {
            SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(new byte[] {(byte) 0x80, (byte) 0x00});
            Assert.fail("Should have failed on input resulting in negative int");
        } catch (ConstraintViolationException e) {
            // expected
        }
        
        try {
            SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(null);
            Assert.fail("Should have failed on null input");
        } catch (ConstraintViolationException e) {
            // expected
        }
        
        try {
            SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(new byte[] {0x02});
            Assert.fail("Should have failed on too short input");
        } catch (ConstraintViolationException e) {
            // expected
        }
        
        try {
            SAMLBindingSupport.convertSAML2ArtifactEndpointIndex(new byte[] {0x00, 0x00, 0x02});
            Assert.fail("Should have failed on too long input");
        } catch (ConstraintViolationException e) {
            // expected
        }
    }

}
