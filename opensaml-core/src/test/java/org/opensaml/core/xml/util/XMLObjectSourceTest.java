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

package org.opensaml.core.xml.util;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
public class XMLObjectSourceTest {
    
    @Test
    public void testGetterAndHashCodeAndEquals() {
        byte[] source1 = new byte[] { 0x00, 0x01, 0x02 };
        byte[] source2 = new byte[] { 0x00, 0x01, 0x02 };
        byte[] source3 = new byte[] { 0x10, 0x10, 0x10 };
        
        XMLObjectSource xmlSource1 = new XMLObjectSource(source1);
        XMLObjectSource xmlSource2 = new XMLObjectSource(source2);
        XMLObjectSource xmlSource3 = new XMLObjectSource(source3);
        
        Assert.assertTrue(Arrays.equals(source1, xmlSource1.getObjectSource()));
        Assert.assertEquals(xmlSource1, xmlSource1);
        Assert.assertTrue(xmlSource1.hashCode() == xmlSource1.hashCode());
        
        Assert.assertTrue(Arrays.equals(source2, xmlSource2.getObjectSource()));
        Assert.assertEquals(xmlSource1, xmlSource2);
        Assert.assertTrue(xmlSource1.hashCode() == xmlSource2.hashCode());
        
        Assert.assertTrue(Arrays.equals(source3, xmlSource3.getObjectSource()));
        Assert.assertNotEquals(xmlSource1, xmlSource3);
    }
    
    @Test
    public void testBadCtorParams() {
        try {
            new XMLObjectSource(null);
            Assert.fail("Should have failed ctor with null arg");
        } catch (Exception e){
            // expected, do nothing
        }
        
        try {
            new XMLObjectSource(new byte[] { } );
            Assert.fail("Should have failed ctor with 0 lenght array");
        } catch (Exception e){
            // expected, do nothing
        }
        
    }

}
