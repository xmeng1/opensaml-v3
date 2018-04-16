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

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import org.opensaml.storage.StorageRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Storage record implementation for use with {@link MemcachedStorageService}.
 *
 * @author Marvin S. Addison
 */
public class MemcachedStorageRecord extends StorageRecord {

    /**
     * Creates a new instance with specific record version.
     *
     * @param val Stored value.
     * @param exp Expiration instant in milliseconds, null for infinite expiration.
     */
    public MemcachedStorageRecord(@Nonnull @NotEmpty final String val, @Nullable final Long exp) {
        super(val, exp);
    }

    /**
     * Converts a {@link org.opensaml.storage.StorageRecord#getExpiration()} value in milliseconds to the corresponding
     * value in seconds.
     * 
     * @param exp the expiration value
     *
     * @return 0 if given expiration is null, otherwise <code>exp/1000</code>.
     */
    public static int expiry(final Long exp) {
        return exp == null ? 0 : (int) (exp / 1000);
    }

    /**
     * Gets the expiration date as an integer representing seconds since the Unix epoch, 1970-01-01T00:00:00.
     * The value provided by this method is suitable for representing the memcached entry expiration.
     *
     * @return 0 if expiration is null, otherwise <code>getExpiration()/1000</code>.
     */
    public int getExpiry() {
        return expiry(getExpiration());
    }

    /**
     * Sets the record version.
     * @param version Record version; must be positive.
     */
    @Override
    protected void setVersion(@Positive final long version) {
        super.setVersion(version);
    }
}
