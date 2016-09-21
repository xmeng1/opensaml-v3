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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Unit test for {@link MemcachedStorageRecord} class.
 */
public class MemcachedStorageRecordTest {

    @Test
    public void testNumericExpiration() {
        final MemcachedStorageRecord record = new MemcachedStorageRecord("r1", 5031757792L);
        assertEquals(record.getExpiration().longValue(), 5031757792L);
        assertEquals(record.getExpiry(), 5031757);
    }

    @Test
    public void testNullExpiration() {
        final MemcachedStorageRecord record = new MemcachedStorageRecord("r2", null);
        assertNull(record.getExpiration());
        assertEquals(record.getExpiry(), 0);
    }
}