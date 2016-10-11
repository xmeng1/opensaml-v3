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

package org.opensaml.core.xml.persist;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.opensaml.core.xml.AbstractXMLObject;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.mock.SimpleXMLObject;
import org.opensaml.core.xml.util.XMLObjectSource;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link AbstractXMLObject}.
 */
public class AbstractXMLObjectTest extends XMLObjectBaseTestCase {
    
    /**
     * Tests of DOM and XMLObjectSource drop.
     * 
     * @throws MarshallingException
     * @throws IOException
     */
    @Test
    public void testDOMAndXMLObjectSourceDropOnMutateContent() throws MarshallingException, IOException {
        SimpleXMLObject sxo = null;
        ByteArrayOutputStream baos  = null;
        
        sxo = buildXMLObject(simpleXMLObjectQName);
        baos = new ByteArrayOutputStream();
        XMLObjectSupport.marshallToOutputStream(sxo, baos);
        baos.flush();
        baos.close();
        sxo.getObjectMetadata().put(new XMLObjectSource(baos.toByteArray()));
        
        Assert.assertTrue(sxo.getObjectMetadata().containsKey(XMLObjectSource.class));
        Assert.assertFalse(sxo.getObjectMetadata().get(XMLObjectSource.class).isEmpty());
        Assert.assertNotNull(sxo.getDOM());
        
        sxo.setValue("some value");
        
        Assert.assertFalse(sxo.getObjectMetadata().containsKey(XMLObjectSource.class));
        Assert.assertTrue(sxo.getObjectMetadata().get(XMLObjectSource.class).isEmpty());
        Assert.assertNull(sxo.getDOM());
    }
    
    /**
     * Tests of DOM and XMLObjectSource drop.
     * 
     * @throws MarshallingException
     * @throws IOException
     */
    @Test
    public void testDOMAndXMLObjectSourceDropOnMutateAttribute() throws MarshallingException, IOException {
        SimpleXMLObject sxo = null;
        ByteArrayOutputStream baos  = null;
        
        sxo = buildXMLObject(simpleXMLObjectQName);
        baos = new ByteArrayOutputStream();
        XMLObjectSupport.marshallToOutputStream(sxo, baos);
        baos.flush();
        baos.close();
        sxo.getObjectMetadata().put(new XMLObjectSource(baos.toByteArray()));
        
        Assert.assertTrue(sxo.getObjectMetadata().containsKey(XMLObjectSource.class));
        Assert.assertFalse(sxo.getObjectMetadata().get(XMLObjectSource.class).isEmpty());
        Assert.assertNotNull(sxo.getDOM());
        
        sxo.getUnknownAttributes().put(new QName("urn:test:ns", "foo"), "foobar");
        
        Assert.assertFalse(sxo.getObjectMetadata().containsKey(XMLObjectSource.class));
        Assert.assertTrue(sxo.getObjectMetadata().get(XMLObjectSource.class).isEmpty());
        Assert.assertNull(sxo.getDOM());
    }

    /**
     * Tests of DOM and XMLObjectSource drop.
     * 
     * @throws MarshallingException
     * @throws IOException
     */
    @Test
    public void testDOMAndXMLObjectSourceDropOnMutateChildElements() throws MarshallingException, IOException {
        SimpleXMLObject sxo = null;
        ByteArrayOutputStream baos  = null;
        
        sxo = buildXMLObject(simpleXMLObjectQName);
        baos = new ByteArrayOutputStream();
        XMLObjectSupport.marshallToOutputStream(sxo, baos);
        baos.flush();
        baos.close();
        sxo.getObjectMetadata().put(new XMLObjectSource(baos.toByteArray()));
        
        Assert.assertTrue(sxo.getObjectMetadata().containsKey(XMLObjectSource.class));
        Assert.assertFalse(sxo.getObjectMetadata().get(XMLObjectSource.class).isEmpty());
        Assert.assertNotNull(sxo.getDOM());
        
        sxo.getUnknownXMLObjects().add(buildXMLObject(simpleXMLObjectQName));
        
        Assert.assertFalse(sxo.getObjectMetadata().containsKey(XMLObjectSource.class));
        Assert.assertTrue(sxo.getObjectMetadata().get(XMLObjectSource.class).isEmpty());
        Assert.assertNull(sxo.getDOM());
    }

}
