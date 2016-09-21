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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Unit test for {@link StorageRecordTranscoder} class.
 */
public class StorageRecordTranscoderTest {

    private StorageRecordTranscoder transcoder = new StorageRecordTranscoder();

    @DataProvider
    public Object[][] testRecords() {
        return new Object[][] {
                new Object[] {new MemcachedStorageRecord("Whither the weather", null)},
                new Object[] {new MemcachedStorageRecord("x", Long.MAX_VALUE)},
                new Object[] {new MemcachedStorageRecord("床前明月光，疑是地上霜. 举头望明月，低头思故乡.", 2515878896L)},
        };
    }

    @Test(dataProvider = "testRecords")
    public void testEncodeDecode(final MemcachedStorageRecord expected) {
        final MemcachedStorageRecord actual = transcoder.decode(transcoder.encode(expected));
        assertEquals(actual.getValue(), expected.getValue());
        assertEquals(actual.getExpiration(), expected.getExpiration());
        assertEquals(actual.getVersion(), expected.getVersion());
    }
}