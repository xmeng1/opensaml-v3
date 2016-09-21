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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.io.Files;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

/**
 * Implementation of {@link XMLObjectLoadSaveManager} which uses a local filesystem to load and store serialized XML.
 * 
 * <p>
 * The primary required configuration is a base directory path under which files of serialized XML will be located.
 * The file name to use is simply the specified String index key, which is treated as an immediate child
 * file name of the base directory.
 * Callers are required to ensure that index keys are acceptable as file names on the platform(s) 
 * on which this manager is used.
 * </p>
 *
 * @param <T> the specific base XML object type being managed
 */
@NotThreadSafe
public class FilesystemLoadSaveManager<T extends XMLObject> implements XMLObjectLoadSaveManager<T> {
    
    /** Logger. */
    private Logger log = LoggerFactory.getLogger(FilesystemLoadSaveManager.class);
    
    /** The base directory used for storing individual serialized XML files. */
    private File baseDirectory;
    
    /** Parser pool instance for deserializing XML from the filesystem. */
    private ParserPool parserPool;
    
    /** File file used in filtering files in {@link #listKeys()} and {@link #listAll()}. */
    private FileFilter fileFilter;
    
    /**
     * Constructor.
     *
     * @param baseDir the base directory
     */
    public FilesystemLoadSaveManager(@Nonnull final String baseDir) {
        this(new File(Constraint.isNotNull(StringSupport.trimOrNull(baseDir), 
                "Base directory string instance was null or empty")),
                null);
    }

    /**
     * Constructor.
     *
     * @param baseDir the base directory
     */
    public FilesystemLoadSaveManager(@Nonnull final File baseDir) {
        this(baseDir, null);
    }
    
    /**
     * Constructor.
     *
     * @param baseDir the base directory
     * @param pp the parser pool instance to use
     */
    public FilesystemLoadSaveManager(@Nonnull final String baseDir, @Nullable final ParserPool pp) {
        this(new File(Constraint.isNotNull(StringSupport.trimOrNull(baseDir), 
                "Base directory string instance was null or empty")),
                pp);
    }

    /**
     * Constructor.
     *
     * @param baseDir the base directory
     * @param pp the parser pool instance to use
     */
    public FilesystemLoadSaveManager(@Nonnull final File baseDir, @Nullable final ParserPool pp) {
        baseDirectory = Constraint.isNotNull(baseDir, "Base directory File instance was null");
        parserPool = pp;
        if (parserPool == null) {
            parserPool = Constraint.isNotNull(XMLObjectProviderRegistrySupport.getParserPool(),
                    "Specified ParserPool was null and global ParserPool was not available");
        }
        fileFilter = new DefaultFileFilter();
    }

    /** {@inheritDoc} */
    public Set<String> listKeys() throws IOException {
        File[] files = baseDirectory.listFiles(fileFilter);
        HashSet<String> keys = new HashSet<>();
        for (File file : files) {
            keys.add(file.getName());
        }
        return Collections.unmodifiableSet(keys);
    }

    /** {@inheritDoc} */
    public Iterable<Pair<String, T>> listAll() throws IOException {
        return new FileIterable(listKeys());
    }

    /** {@inheritDoc} */
    public boolean exists(String key) throws IOException {
        return buildFile(key).exists();
    }

    /** {@inheritDoc} */
    public T load(String key) throws IOException {
        File file = buildFile(key);
        try (FileInputStream fis = new FileInputStream(file)) {
            try {
                XMLObject xmlObject = XMLObjectSupport.unmarshallFromInputStream(parserPool, fis);
                //TODO via ctor, etc, does caller need to supply a Class so we can can test and throw an IOException, 
                // rather than an unchecked ClassCastException?
                return (T) xmlObject;
            } catch (XMLParserException|UnmarshallingException e) {
                throw new IOException(String.format("Error loading file from path: %s", file.getAbsolutePath()), e);
            }
        }
    }

    /** {@inheritDoc} */
    public void save(String key, T xmlObject) throws IOException {
        save(key, xmlObject, false);
    }

    /** {@inheritDoc} */
    public void save(String key, T xmlObject, boolean overwrite) throws IOException {
        if (!overwrite && exists(key)) {
            throw new IOException(
                    String.format("Target file already exists for key '%s' and overwrite not indicated", key));
        }
        
        //TODO possibly look at objectMetadata for source byte[] and write that rather than marshall+serialize?
        
        File file = buildFile(key);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            try {
                XMLObjectSupport.marshallToOutputStream(xmlObject, fos);
            } catch (MarshallingException e) {
                throw new IOException(String.format("Error saving target file: %s", file.getAbsolutePath()), e);
            }
        }
        
    }

    /** {@inheritDoc} */
    public boolean remove(String key) throws IOException {
        File file = buildFile(key);
        if (file.exists()) {
            boolean success = file.delete();
            if (success) {
                return true;
            } else {
                throw new IOException(String.format("Error removing target file: %s", file.getAbsolutePath()));
            }
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    public boolean updateKey(String currentKey, String newKey) throws IOException {
        File currentFile = buildFile(currentKey);
        File newFile = buildFile(newKey);
        if (!currentFile.exists()) {
            return false;
        } else if (newFile.exists()) {
            throw new IOException(String.format("Specified new key already exists: %s", newKey));
        } else {
            Files.move(currentFile, newFile);
            return true;
        }
    }
    
    /**
     * Build the target file name from the specified index key and the configured base directory.
     * 
     * @param key the target file name index key
     * @return the constructed File instance for the target file
     * @throws IOException if there is a fatal error constructing or evaluating the candidate target path
     */
    protected File buildFile(String key) throws IOException {
        File path = new File(baseDirectory, 
                Constraint.isNotNull(StringSupport.trimOrNull(key), "Input key was null or empty"));
        if (path.exists() && !path.isFile()) {
            throw new IOException(String.format("Path exists based on specified key, but is not a file: %s", 
                    path.getAbsolutePath()));
        }
        return path;
    }
    
    /**
     * Default filter used to filter data returned in {@link FilesystemLoadSaveManager#listKeys()}
     * and {@link FilesystemLoadSaveManager#listAll()}.
     */
    public static class DefaultFileFilter implements FileFilter {

        /** {@inheritDoc} */
        public boolean accept(File pathname) {
            if (pathname == null) {
                return false;
            }
            return pathname.isFile();
        }
        
    }
    
    /**
     * Iterable which provides lazy iteration over the managed files.
     */
    private class FileIterable implements Iterable<Pair<String, T>> {
        
        /** Snapshot of filesystem keys at time of construction. */
        private Set<String> keys;

        /**
         * Constructor.
         *
         * @param filenames Snapshot of filesystem keys at time of construction
         */
        public FileIterable(@Nonnull final Collection<String> filenames) {
            keys = new HashSet<>();
            keys.addAll(Collections2.filter(filenames, Predicates.notNull()));
        }

        /** {@inheritDoc} */
        public Iterator<Pair<String, T>> iterator() {
            return new FileIterator(keys);
        }
        
    }
    
    /**
     * Iterator which provides lazy iteration over the managed files.
     */
    private class FileIterator implements Iterator<Pair<String, T>> {
        
        /** Iterator for the keys. */
        private Iterator<String> keysIter;
        
        /** Current value to return from next(). */
        private Pair<String, T> current;
        
        /**
         * Constructor.
         *
         * @param filenames Snapshot of filesystem keys at time of construction
         */
        public FileIterator(@Nonnull final Collection<String> filenames) {
            Set<String> keys = new HashSet<>();
            keys.addAll(Collections2.filter(filenames, Predicates.notNull()));
            keysIter = keys.iterator();
        }

        /** {@inheritDoc} */
        public boolean hasNext() {
            if (current != null) {
                return true;
            }
            
            current = getNext();
            
            return current != null;
        }

        /** {@inheritDoc} */
        public Pair<String, T> next() {
            if (current != null) {
                Pair<String, T> temp = current;
                current = null;
                return temp;
            } else {
                Pair<String, T> temp = getNext();
                if (temp != null) {
                    return temp;
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        /** {@inheritDoc} */
        public void remove() {
            //TODO can we support?  Probably.
            throw new UnsupportedOperationException();
        }
        
        /**
         * Internal support to get the next item for iteration.
         * 
         * @return the next item for iteration, or null if no more items
         */
        private Pair<String, T> getNext() {
            while (keysIter.hasNext()) {
                String key = keysIter.next();
                try {
                    T xmlObject = load(key);
                    return new Pair<>(key, xmlObject);
                } catch (IOException e) {
                    log.warn("Error loading target file with key '{}'", key, e);
                    //TODO should swallow the error, or throw out as unchecked?
                }
            }
            return null;
        }
        
    }

}
