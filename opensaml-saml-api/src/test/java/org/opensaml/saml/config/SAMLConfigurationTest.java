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

package org.opensaml.saml.config;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/**
 *
 */
public class SAMLConfigurationTest {
    
    @Test
    public void testBindingURLSchemes() {
        List<String> schemes = null;
        SAMLConfiguration config = new SAMLConfiguration();
        
        // Test default values
        schemes = config.getAllowedBindingURLSchemes();
        Assert.assertEquals(schemes.size(), 2);
        Assert.assertTrue(schemes.contains("http"));
        Assert.assertTrue(schemes.contains("https"));
        
        // Test normalization 
        config.setAllowedBindingURLSchemes(Lists.newArrayList("   HTTP  ",  null, "  HTTPS  ", "FooBar", "     "));
        schemes = config.getAllowedBindingURLSchemes();
        Assert.assertEquals(schemes.size(), 3);
        Assert.assertTrue(schemes.contains("http"));
        Assert.assertTrue(schemes.contains("https"));
        Assert.assertTrue(schemes.contains("foobar"));
    }
    

}
