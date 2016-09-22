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

package org.opensaml.saml.metadata.resolver.impl;

import java.io.IOException;
import java.util.Timer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.persist.XMLObjectLoadSaveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 * Resolver which dynamically resolves metadata from a local source managed by an instance
 * of {@link XMLObjectLoadSaveManager}.
 */
public class LocalDynamicMetadataResolver extends AbstractDynamicMetadataResolver {
    
    /** Logger. */
    private Logger log = LoggerFactory.getLogger(LocalDynamicMetadataResolver.class);
    
    /** The manager for the local store of metadata. */
    private XMLObjectLoadSaveManager<XMLObject> sourceManager;
    
    /** Function for generating the String key used with the cache manager. */
    private Function<CriteriaSet, String> sourceKeyGenerator;

    /**
     * Constructor.
     *
     * @param backgroundTaskTimer
     */
    public LocalDynamicMetadataResolver(@Nonnull final XMLObjectLoadSaveManager<XMLObject> manager) {
        this(manager, null, null);
    }
    
    /**
     * Constructor.
     *
     * @param manager
     * @param keyGenerator
     * @param backgroundTaskTimer
     */
    public LocalDynamicMetadataResolver(@Nonnull final XMLObjectLoadSaveManager<XMLObject> manager, 
            @Nullable final Function<CriteriaSet, String> keyGenerator,
            @Nullable final Timer backgroundTaskTimer) {
        
        super(backgroundTaskTimer);
        
        sourceManager = Constraint.isNotNull(manager, "Local source manager was null");
        
        sourceKeyGenerator = keyGenerator;
        if (sourceKeyGenerator == null) {
            sourceKeyGenerator = new DefaultSourceKeyGenerator();
        }
    }

    /** {@inheritDoc} */
    protected XMLObject fetchFromOriginSource(CriteriaSet criteria) throws IOException {
        String key = sourceKeyGenerator.apply(criteria);
        if (key != null) {
            log.trace("Attempting to load from local source manager with generated key '{}'", key);
            XMLObject result = sourceManager.load(key);
            if (result != null) {
                log.trace("Successfully loaded target from local source manager source with key '{}' of type: ",
                        key, result.getElementQName());
            } else {
                log.trace("Found no target in local source manager with key '{}'", key);
            }
            return result;
        } else {
            log.trace("Could not generate source key from criteria, can not resolve");
            return null;
        }
    }
    
    /**
     * Default strategy for generating source keys from input criteria.
     */
    public static class DefaultSourceKeyGenerator implements Function<CriteriaSet, String> {

        /** {@inheritDoc} */
        public String apply(CriteriaSet input) {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
}
