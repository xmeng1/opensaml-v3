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

package org.opensaml.storage.impl.memcached;

import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.opensaml.storage.StorageCapabilities;

/**
 * Provides a description of memcached capabilities. Note that only value size is configurable since memcached supports
 * increasing the maximum slab size via the <code>-I</code> flag. {@link MemcachedStorageService} supports
 * arbitrarily large context names and keys by hashing values longer than 250 bytes, which is the maximum size allowed.
 *
 * @author Marvin S. Addison
 */
public class MemcachedStorageCapabilities implements StorageCapabilities {

    /** Memcached supports 1M slabs (i.e. values) by default and issues warning on increase. */
    private static long defaultMaxValue = 1024 * 1024;

    /** Maximum size of memcached values. */
    @Positive
    private final long valueSize;


    /** Constructor. */
    public MemcachedStorageCapabilities() {
        this(defaultMaxValue);
    }

    /**
     * Constructor.
     *
     * @param maxValueSize maximum value size
     */
    public MemcachedStorageCapabilities(@Positive final long maxValueSize) {
        Constraint.isGreaterThan(0, maxValueSize, "Maximum value size must be a positive integer");
        valueSize = maxValueSize;
    }

    @Override
    public int getContextSize(){
        return Integer.MAX_VALUE;
    }

    @Override
    public int getKeySize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getValueSize() {
        return valueSize;
    }
}
