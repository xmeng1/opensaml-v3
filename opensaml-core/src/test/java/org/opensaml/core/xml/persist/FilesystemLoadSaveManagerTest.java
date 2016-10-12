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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.xml.namespace.QName;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.XMLRuntimeException;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.mock.SimpleXMLObject;
import org.opensaml.core.xml.util.XMLObjectSource;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

public class FilesystemLoadSaveManagerTest extends XMLObjectBaseTestCase {
    
    private Logger log = LoggerFactory.getLogger(FilesystemLoadSaveManagerTest.class);
    
    private File baseDir;
    
    private FilesystemLoadSaveManager<SimpleXMLObject> manager;
    
    @BeforeMethod
    public void setUp() throws IOException {
        baseDir = new File(System.getProperty("java.io.tmpdir"), "load-save-manager-test");
        baseDir.deleteOnExit();
        log.debug("Using base directory: {}", baseDir.getAbsolutePath());
        resetBaseDir();
        Assert.assertTrue(baseDir.mkdirs());
        
        manager = new FilesystemLoadSaveManager<>(baseDir);
    }
    
    @AfterMethod
    public void tearDown() throws IOException {
        resetBaseDir();
    }
    
    @Test
    public void emptyDir() throws IOException {
        testState(Sets.<String>newHashSet());
    }
    
    @DataProvider
    public Object[][] saveLoadUpdateRemoveParams() {
        return new Object[][] {
                new Object[] { Boolean.FALSE},
                new Object[] { Boolean.TRUE },
        };
    }
    
    @Test(dataProvider="saveLoadUpdateRemoveParams")
    public void saveLoadUpdateRemove(Boolean buildWithObjectSourceByteArray) throws IOException {
        testState(Sets.<String>newHashSet());
        
        Assert.assertNull(manager.load("bogus"));
        
        manager.save("foo", (SimpleXMLObject) buildXMLObject(SimpleXMLObject.ELEMENT_NAME, buildWithObjectSourceByteArray));
        testState(Sets.newHashSet("foo"));
        
        manager.save("bar", (SimpleXMLObject) buildXMLObject(SimpleXMLObject.ELEMENT_NAME, buildWithObjectSourceByteArray));
        manager.save("baz", (SimpleXMLObject) buildXMLObject(SimpleXMLObject.ELEMENT_NAME, buildWithObjectSourceByteArray));
        testState(Sets.newHashSet("foo", "bar", "baz"));
        
        // Duplicate with overwrite
        manager.save("bar", (SimpleXMLObject) buildXMLObject(SimpleXMLObject.ELEMENT_NAME, buildWithObjectSourceByteArray), true);
        testState(Sets.newHashSet("foo", "bar", "baz"));
        
        // Duplicate without overwrite
        try {
            manager.save("bar", (SimpleXMLObject) buildXMLObject(SimpleXMLObject.ELEMENT_NAME, buildWithObjectSourceByteArray), false);
            Assert.fail("Should have failed on duplicate save without overwrite");
        } catch (IOException e) {
            // expected, do nothing
        }
        testState(Sets.newHashSet("foo", "bar", "baz"));
        
        Assert.assertTrue(manager.updateKey("foo", "foo2"));
        testState(Sets.newHashSet("foo2", "bar", "baz"));
        
        // Doesn't exist anymore
        Assert.assertFalse(manager.updateKey("foo", "foo2"));
        testState(Sets.newHashSet("foo2", "bar", "baz"));
        
        // Can't update to an existing name
        try {
            manager.updateKey("bar", "baz");
            Assert.fail("updateKey should have filed to due existing new key name");
        } catch (IOException e) {
            // expected, do nothing
        }
        testState(Sets.newHashSet("foo2", "bar", "baz"));
        
        // Doesn't exist anymore
        Assert.assertFalse(manager.remove("foo"));
        testState(Sets.newHashSet("foo2", "bar", "baz"));
        
        Assert.assertTrue(manager.remove("foo2"));
        testState(Sets.newHashSet("bar", "baz"));
        
        Assert.assertTrue(manager.remove("bar"));
        Assert.assertTrue(manager.remove("baz"));
        testState(Sets.<String>newHashSet());
    }

    @Test
    public void buildTargetFileFromKey() throws IOException {
        File target = manager.buildFile("abc");
        Assert.assertEquals(target, new File(baseDir, "abc"));
    }
    
    @Test(expectedExceptions=IOException.class)
    public void targetExistsButIsNotAFile() throws IOException {
        File target = new File(baseDir, "abc");
        Assert.assertFalse(target.exists());
        target.mkdir();
        try {
            manager.buildFile("abc");
        } finally {
            if (target.exists()) {
                Files.delete(target.toPath());
            }
        }
    }
    
    @Test(expectedExceptions=ConstraintViolationException.class)
    public void targetKeyIsNull() throws IOException {
        manager.buildFile(null);
    }
    
    @Test(expectedExceptions=ConstraintViolationException.class)
    public void targetKeyIsEmpty() throws IOException {
        manager.buildFile("  ");
    }
    
    @Test
    public void ctorCreateDirectory() throws IOException {
        resetBaseDir();
        Assert.assertFalse(baseDir.exists());
        new FilesystemLoadSaveManager<>(baseDir);
        Assert.assertTrue(baseDir.exists());
    }
    
    @Test
    public void ctorPathTrimming() throws IOException {
        new FilesystemLoadSaveManager<>(String.format("    %s     ", baseDir.getAbsolutePath()));
        File target = manager.buildFile("abc");
        Assert.assertEquals(target.getParentFile(), baseDir);
        Assert.assertEquals(target.getParent(), baseDir.getAbsolutePath());
        Assert.assertFalse(target.getParent().startsWith(" "));
        Assert.assertFalse(target.getParent().endsWith(" "));
    }
    
    @Test(expectedExceptions=ConstraintViolationException.class)
    public void ctorEmptyPathString() {
        new FilesystemLoadSaveManager<>("  ");
    }
    
    @Test(expectedExceptions=ConstraintViolationException.class)
    public void ctorNullFile() {
        new FilesystemLoadSaveManager<>((File)null);
    }
    
    @Test(expectedExceptions=ConstraintViolationException.class)
    public void ctorRelativeDir() {
        new FilesystemLoadSaveManager<>("my/relative/dir");
    }
    
    @Test(expectedExceptions=ConstraintViolationException.class)
    public void ctorBaseDirPathExistsButNotADirectory() throws IOException {
        resetBaseDir();
        Files.createFile(baseDir.toPath());
        new FilesystemLoadSaveManager<>(baseDir);
    }
    
    @Test
    public void iterator() throws IOException {
        Iterator<Pair<String,SimpleXMLObject>> iterator = null;
        
        iterator = manager.listAll().iterator();
        Assert.assertFalse(iterator.hasNext());
        try {
            iterator.next();
            Assert.fail("Should have failed due to no more elements");
        } catch (NoSuchElementException e) {
            //expected, do nothing
        }
        
        manager.save("foo", (SimpleXMLObject) buildXMLObject(SimpleXMLObject.ELEMENT_NAME));
        iterator = manager.listAll().iterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertNotNull(iterator.next());
        Assert.assertFalse(iterator.hasNext());
        try {
            iterator.next();
            Assert.fail("Should have failed due to no more elements");
        } catch (NoSuchElementException e) {
            //expected, do nothing
        }
        
        manager.save("bar", (SimpleXMLObject) buildXMLObject(SimpleXMLObject.ELEMENT_NAME));
        manager.save("baz", (SimpleXMLObject) buildXMLObject(SimpleXMLObject.ELEMENT_NAME));
        iterator = manager.listAll().iterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertNotNull(iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertNotNull(iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertNotNull(iterator.next());
        Assert.assertFalse(iterator.hasNext());
        try {
            iterator.next();
            Assert.fail("Should have failed due to no more elements");
        } catch (NoSuchElementException e) {
            //expected, do nothing
        }
        
        manager.remove("foo");
        iterator = manager.listAll().iterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertNotNull(iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertNotNull(iterator.next());
        Assert.assertFalse(iterator.hasNext());
        try {
            iterator.next();
            Assert.fail("Should have failed due to no more elements");
        } catch (NoSuchElementException e) {
            //expected, do nothing
        }
        
        manager.remove("bar");
        manager.remove("baz");
        iterator = manager.listAll().iterator();
        Assert.assertFalse(iterator.hasNext());
        try {
            iterator.next();
            Assert.fail("Should have failed due to no more elements");
        } catch (NoSuchElementException e) {
            //expected, do nothing
        }
        
        // Test when file is removed after iterator is created
        manager.save("foo", (SimpleXMLObject) buildXMLObject(SimpleXMLObject.ELEMENT_NAME));
        Assert.assertTrue(manager.exists("foo"));
        Assert.assertNotNull(manager.load("foo"));
        iterator = manager.listAll().iterator();
        manager.remove("foo");
        Assert.assertFalse(iterator.hasNext());
        
    }
    
    
    
    // Helpers
    
    private void testState(Set<String> expectedKeys) throws IOException {
        Assert.assertEquals(manager.listKeys().isEmpty(), expectedKeys.isEmpty() ? true : false);
        Assert.assertEquals(manager.listKeys(), expectedKeys);
        for (String expectedKey : expectedKeys) {
            Assert.assertTrue(manager.exists(expectedKey));
            SimpleXMLObject sxo = manager.load(expectedKey);
            Assert.assertNotNull(sxo);
            Assert.assertEquals(sxo.getObjectMetadata().get(XMLObjectSource.class).size(), 1);
        }
        
        Assert.assertEquals(manager.listAll().iterator().hasNext(), expectedKeys.isEmpty() ? false: true);
        
        int sawCount = 0;
        for (Pair<String,SimpleXMLObject> entry : manager.listAll()) {
            sawCount++;
            Assert.assertTrue(expectedKeys.contains(entry.getFirst()));
            Assert.assertNotNull(entry.getSecond());
        }
        Assert.assertEquals(sawCount, expectedKeys.size());
    }
    
    private void resetBaseDir() throws IOException {
        if (baseDir.exists()) {
            if (baseDir.isDirectory()) {
                for (File child : baseDir.listFiles()) {
                    Files.delete(child.toPath());
                }
            }
            Files.delete(baseDir.toPath());
        }
    }
    
    // It's hard to actually test that we're writing the existing byte[], but by doing this
    // we can at least visually inspect the logs for save() ops and see that it logs as expected.
    protected <T extends XMLObject> T buildXMLObject(QName name, boolean withObjectSource) {
        T xmlObject = super.buildXMLObject(name);
        if (withObjectSource) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                XMLObjectSupport.marshallToOutputStream(xmlObject, baos);
                xmlObject.getObjectMetadata().put(new XMLObjectSource(baos.toByteArray()));
            } catch (MarshallingException | IOException e) {
                throw new XMLRuntimeException("Error marshalling XMLObject", e);
            }
        }
        return xmlObject;
    }

    
}
