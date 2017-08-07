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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.opensaml.core.xml.XMLObject;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Simple implementation of {@link XMLObjectLoadSaveManager} which uses an in-memory map.
 *
 * @param <T> the specific base XML object type being managed
 */
@NotThreadSafe
public class MapLoadSaveManager<T extends XMLObject> implements XMLObjectLoadSaveManager<T> {
    
    /** The backing map. */
    private Map<String,T> backingMap;
    
    /** Constructor. */
    public MapLoadSaveManager() {
        this(new HashMap<String,T>());
    }

    /**
     * Constructor.
     *
     * @param map the backing map 
     */
    public MapLoadSaveManager(@Nonnull final Map<String, T> map) {
        backingMap = Constraint.isNotNull(map, "Backing map was null");
    }

    /** {@inheritDoc} */
    public Set<String> listKeys() throws IOException {
        return backingMap.keySet();
    }

    /** {@inheritDoc} */
    public Iterable<Pair<String, T>> listAll() throws IOException {
        ArrayList<Pair<String,T>> list = new ArrayList<>();
        for (String key : listKeys()) {
            list.add(new Pair<>(key, load(key)));
        }
        return list;
    }

    /** {@inheritDoc} */
    public boolean exists(final String key) throws IOException {
        return backingMap.containsKey(key);
    }

    /** {@inheritDoc} */
    public T load(final String key) throws IOException {
        return backingMap.get(key);
    }

    /** {@inheritDoc} */
    public void save(final String key, final T xmlObject) throws IOException {
        save(key, xmlObject, false);
    }

    /** {@inheritDoc} */
    public void save(final String key, final T xmlObject, final boolean overwrite) throws IOException {
        if (!overwrite && exists(key)) {
            throw new IOException(String.format("Value already exists for key '%s'", key));
        } else {
            backingMap.put(key, xmlObject);
        }
    }

    /** {@inheritDoc} */
    public boolean remove(final String key) throws IOException {
        return backingMap.remove(key) != null;
    }

    /** {@inheritDoc} */
    public boolean updateKey(final String currentKey, final String newKey) throws IOException {
        T value = load(currentKey);
        if (value != null) {
            save(newKey, value, false);
            remove(currentKey);
            return true;
        } else {
            return false;
        }
    }

}
