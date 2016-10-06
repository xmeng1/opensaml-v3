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

package org.opensaml.saml.metadata.resolver.index.impl;

import java.net.MalformedURLException;

import org.testng.Assert;
import org.testng.annotations.Test;


public class MetadataIndexSupportTest {
    
    @Test
    public void testCanonicalizeLocationURI() throws MalformedURLException {
        try {
            MetadataIndexSupport.canonicalizeLocationURI(null);
            Assert.fail("Should have failed on null/empty input");
        } catch (MalformedURLException e) {
        }
        try {
            MetadataIndexSupport.canonicalizeLocationURI("");
            Assert.fail("Should have failed on null/empty input");
        } catch (MalformedURLException e) {
        }
        try {
            MetadataIndexSupport.canonicalizeLocationURI("   ");
            Assert.fail("Should have failed on null/empty input");
        } catch (MalformedURLException e) {
        }
        
        Assert.assertEquals(MetadataIndexSupport.canonicalizeLocationURI("https://www.example.com"), "https://www.example.com");
        Assert.assertEquals(MetadataIndexSupport.canonicalizeLocationURI("HTTPS://WWW.EXAMPLE.COM:443/Foo/Bar"), "https://www.example.com/Foo/Bar");
        Assert.assertEquals(MetadataIndexSupport.canonicalizeLocationURI("https://www.example.com/foo/bar?abc=def&dog=cat#fragFrag"), "https://www.example.com/foo/bar");
        Assert.assertEquals(MetadataIndexSupport.canonicalizeLocationURI("https://someuser:somepass@www.example.com:443/foo/bar?abc=def&dog=cat#fragFrag"), "https://www.example.com/foo/bar");
    }
    
    @Test
    public void testTrimURLPathSegment() {
        Assert.assertNull(MetadataIndexSupport.trimURLPathSegment(null));
        Assert.assertNull(MetadataIndexSupport.trimURLPathSegment(""));
        Assert.assertNull(MetadataIndexSupport.trimURLPathSegment("  "));
        Assert.assertNull(MetadataIndexSupport.trimURLPathSegment("foo"));
        
        Assert.assertEquals(MetadataIndexSupport.trimURLPathSegment("/foo/bar/baz/"), "/foo/bar/baz");
        Assert.assertEquals(MetadataIndexSupport.trimURLPathSegment("/foo/bar/baz"), "/foo/bar");
        Assert.assertEquals(MetadataIndexSupport.trimURLPathSegment("/foo/bar"), "/foo");
        Assert.assertEquals(MetadataIndexSupport.trimURLPathSegment("/foo"), "/");
        Assert.assertNull(MetadataIndexSupport.trimURLPathSegment("/"));
    }
    

}
