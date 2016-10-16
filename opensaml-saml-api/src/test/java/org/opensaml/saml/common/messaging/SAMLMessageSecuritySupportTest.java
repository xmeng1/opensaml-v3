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

package org.opensaml.saml.common.messaging;

import org.junit.Assert;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.saml.config.SAMLConfiguration;
import org.testng.annotations.Test;

/**
 *
 */
public class SAMLMessageSecuritySupportTest {
    
    @Test
    public void testCheckURLScheme() {
        synchronized (ConfigurationService.class) {
            SAMLConfiguration globalConfig = ConfigurationService.get(SAMLConfiguration.class);
            if (globalConfig == null) {
                globalConfig = new SAMLConfiguration();
                ConfigurationService.register(SAMLConfiguration.class, globalConfig);
            }
        }

        Assert.assertTrue(SAMLMessageSecuritySupport.checkURLScheme("http"));
        Assert.assertTrue(SAMLMessageSecuritySupport.checkURLScheme("https"));
        Assert.assertFalse(SAMLMessageSecuritySupport.checkURLScheme("foobar"));

        // check normalization
        Assert.assertTrue(SAMLMessageSecuritySupport.checkURLScheme("  HTTPS  "));
        Assert.assertFalse(SAMLMessageSecuritySupport.checkURLScheme("  "));
        Assert.assertFalse(SAMLMessageSecuritySupport.checkURLScheme(null));
    }

}
