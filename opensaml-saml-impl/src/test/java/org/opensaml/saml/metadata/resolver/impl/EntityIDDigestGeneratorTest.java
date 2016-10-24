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

import java.security.NoSuchAlgorithmException;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.security.crypto.JCAConstants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import junit.framework.Assert;
import net.shibboleth.utilities.java.support.codec.StringDigester;
import net.shibboleth.utilities.java.support.codec.StringDigester.OutputFormat;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 *
 */
public class EntityIDDigestGeneratorTest {
    
    private String controlValue;
    
    private String controlValueSHA1Hex;
    
    private CriteriaSet criteria;
    
    private EntityIDDigestGenerator generator;
    
    @BeforeMethod
    public void setUp() {
        controlValue = "urn:test:foobar";
        controlValueSHA1Hex = "d278c9975472a6b4827b1a8723192b4e99aa969c";
        criteria = new CriteriaSet();
    }
    
    @Test
    public void testBasic() throws NoSuchAlgorithmException {
        generator = new EntityIDDigestGenerator();
        
        Assert.assertNull(generator.apply(null));
        
        criteria.clear();
        Assert.assertNull(generator.apply(criteria));
        
        criteria.add(new EntityIdCriterion(controlValue));
        
        Assert.assertEquals(controlValueSHA1Hex, generator.apply(criteria));
        
        generator = new EntityIDDigestGenerator(null, "metadata-", ".xml", null);
        
        Assert.assertEquals("metadata-" + controlValueSHA1Hex + ".xml", generator.apply(criteria));
        
        generator = new EntityIDDigestGenerator(null, "metadata", "xml", ".");
        
        Assert.assertEquals("metadata." + controlValueSHA1Hex + ".xml", generator.apply(criteria));
        
        StringDigester digester = new StringDigester(JCAConstants.DIGEST_SHA1, OutputFormat.HEX_UPPER);
        
        generator = new EntityIDDigestGenerator(digester, null, null, null);
        
        Assert.assertEquals(controlValueSHA1Hex.toUpperCase(), generator.apply(criteria));
        
        generator = new EntityIDDigestGenerator(digester, "metadata", "xml", ".");
        
        Assert.assertEquals("metadata." + controlValueSHA1Hex.toUpperCase() + ".xml", generator.apply(criteria));
    }

}
