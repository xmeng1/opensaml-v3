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

import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;

import java.nio.charset.StandardCharsets;

/**
 * Handles conversion of String values to bytes and back.
 *
 * @author Marvin S. Addison
 */
public class StringTranscoder implements Transcoder<String> {

    /** Max size is maximum default memcached value size, 1MB. */
    private static final int MAX_SIZE = 1024 * 1024;


    @Override
    public boolean asyncDecode(CachedData d) {
        return false;
    }

    @Override
    public CachedData encode(final String o) {
        return new CachedData(0, o.getBytes(StandardCharsets.UTF_8), MAX_SIZE);
    }

    @Override
    public String decode(final CachedData d) {
        return new String(d.getData(), StandardCharsets.UTF_8);
    }

    @Override
    public int getMaxSize() {
        return MAX_SIZE;
    }
}
