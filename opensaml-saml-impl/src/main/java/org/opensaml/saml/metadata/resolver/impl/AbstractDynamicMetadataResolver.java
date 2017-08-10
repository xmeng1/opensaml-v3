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
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.metrics.MetricsSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.persist.XMLObjectLoadSaveManager;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.core.xml.util.XMLObjectSupport.CloneOutputOption;
import org.opensaml.saml.metadata.resolver.DynamicMetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.FilterException;
import org.opensaml.saml.saml2.common.SAML2Support;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.security.crypto.JCAConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.Timer.Context;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;

import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.codec.StringDigester;
import net.shibboleth.utilities.java.support.codec.StringDigester.OutputFormat;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

/**
 * Abstract subclass for metadata resolvers that resolve metadata dynamically, as needed and on demand.
 */
public abstract class AbstractDynamicMetadataResolver extends AbstractMetadataResolver 
        implements DynamicMetadataResolver {
    
    /** Metric name for the timer for {@link #fetchFromOriginSource(CriteriaSet)}. */
    public static final String METRIC_TIMER_FETCH_FROM_ORIGIN_SOURCE = "timer.fetchFromOriginSource";
    
    /** Metric name for the timer for {@link #resolve(CriteriaSet)}. */
    public static final String METRIC_TIMER_RESOLVE = "timer.resolve";
    
    /** Metric name for the ratio gauge of fetches to resolve requests. */
    public static final String METRIC_RATIOGAUGE_FETCH_TO_RESOLVE = "ratioGauge.fetchToResolve";
    
    /** Metric name for the gauge of the number of live entityIDs. */
    public static final String METRIC_GAUGE_NUM_LIVE_ENTITYIDS = "gauge.numLiveEntityIDs";
    
    /** Metric name for the gauge of the persistent cache initialization metrics. */
    public static final String METRIC_GAUGE_PERSISTENT_CACHE_INIT = "gauge.persistentCacheInitialization";
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractDynamicMetadataResolver.class);
    
    /** Base name for Metrics instrumentation names. */
    @NonnullAfterInit private String metricsBaseName;
    
    /** Metrics Timer for {@link #resolve(CriteriaSet)}. */
    @Nullable private com.codahale.metrics.Timer timerResolve;
    
    /** Metrics Timer for {@link #fetchFromOriginSource(CriteriaSet)}. */
    @Nullable private com.codahale.metrics.Timer timerFetchFromOriginSource;
    
    /** Metrics RatioGauge for count of origin fetches to resolves.*/
    @Nullable private RatioGauge ratioGaugeFetchToResolve;
    
    /** Metrics Gauge for the number of live entityIDs.*/
    @Nullable private Gauge<Integer> gaugeNumLiveEntityIDs;
    
    /** Metrics Gauge for the persistent cache initialization.*/
    @Nullable private Gauge<PersistentCacheInitializationMetrics> gaugePersistentCacheInit;
    
    /** Timer used to schedule background metadata update tasks. */
    private Timer taskTimer;
    
    /** Whether we created our own task timer during object construction. */
    private boolean createdOwnTaskTimer;
    
    /** Minimum cache duration. */
    @Duration @Positive private Long minCacheDuration;
    
    /** Maximum cache duration. */
    @Duration @Positive private Long maxCacheDuration;
    
    /** Factor used to compute when the next refresh interval will occur. Default value: 0.75 */
    @Positive private Float refreshDelayFactor;
    
    /** The maximum idle time in milliseconds for which the resolver will keep data for a given entityID, 
     * before it is removed. */
    @Duration @Positive private Long maxIdleEntityData;
    
    /** Flag indicating whether idle entity data should be removed. */
    private boolean removeIdleEntityData;
    
    /** The interval in milliseconds at which the cleanup task should run. */
    @Duration @Positive private Long cleanupTaskInterval;
    
    /** The backing store cleanup sweeper background task. */
    private BackingStoreCleanupSweeper cleanupTask;
    
    /** The manager for the persistent cache store for resolved metadata. */
    private XMLObjectLoadSaveManager<EntityDescriptor> persistentCacheManager;
    
    /** Function for generating the String key used with the cache manager. */
    private Function<EntityDescriptor, String> persistentCacheKeyGenerator;
    
    /** Flag indicating whether should initialize from the persistent cache in the background. */
    private boolean initializeFromPersistentCacheInBackground;
    
    /** The delay in milliseconds after which to schedule the background initialization from the persistent cache. */
    @Duration @Positive private Long backgroundInitializationFromCacheDelay;
    
    /** Predicate which determines whether a given entity should be loaded from the persistent cache
     * at resolver initialization time. */
    private Predicate<EntityDescriptor> initializationFromCachePredicate;
    
    /** Object tracking metrics related to the persistent cache initialization. */
    @NonnullAfterInit private PersistentCacheInitializationMetrics persistentCacheInitMetrics;
    
    /** Flag used to track state of whether currently initializing or not. */
    private boolean initializing;
    
    /**
     * Constructor.
     *
     * @param backgroundTaskTimer the {@link Timer} instance used to run resolver background management tasks
     */
    public AbstractDynamicMetadataResolver(@Nullable final Timer backgroundTaskTimer) {
        super();
        
        if (backgroundTaskTimer == null) {
            taskTimer = new Timer(true);
            createdOwnTaskTimer = true;
        } else {
            taskTimer = backgroundTaskTimer;
        }
        
        // Default to 10 minutes.
        minCacheDuration = 10*60*1000L;
        
        // Default to 8 hours.
        maxCacheDuration = 8*60*60*1000L;
        
        refreshDelayFactor = 0.75f;
        
        // Default to 30 minutes.
        cleanupTaskInterval = 30*60*1000L;
        
        // Default to 8 hours.
        maxIdleEntityData = 8*60*60*1000L;
        
        // Default to removing idle metadata
        removeIdleEntityData = true;
        
        // Default to initializing from the the persistent cache in the background
        initializeFromPersistentCacheInBackground = true;
        
        // Default to 2 seconds.
        backgroundInitializationFromCacheDelay = 2*1000L;
    }
    
    /**
     * Get the flag indicating whether should initialize from the persistent cache in the background.
     * 
     * <p>Defaults to: true.</p>
     * 
     * @return true if should init from the cache in background, false otherwise
     */
    public boolean isInitializeFromPersistentCacheInBackground() {
        return initializeFromPersistentCacheInBackground;
    }

    /**
     * Set the flag indicating whether should initialize from the persistent cache in the background.
     * 
     * <p>Defaults to: true.</p>
     * 
     * @param flag true if should init from the cache in the background, false otherwise
     */
    public void setInitializeFromPersistentCacheInBackground(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        initializeFromPersistentCacheInBackground = flag;
    }

    /**
     * Get the delay in milliseconds after which to schedule the background initialization from the persistent cache.
     * 
     * <p>Defaults to: 2 seconds.</p>
     * 
     * @return the delay in milliseconds
     * 
     * @since 3.3.0
     */
    @Nonnull public Long getBackgroundInitializationFromCacheDelay() {
        return backgroundInitializationFromCacheDelay;
    }

    /**
     * Set the delay in milliseconds after which to schedule the background initialization from the persistent cache.
     * 
     * <p>Defaults to: 2 seconds.</p>
     * 
     * @param delay the delay in milliseconds
     * 
     * @since 3.3.0
     */
    public void setBackgroundInitializationFromCacheDelay(@Nonnull final Long delay) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        backgroundInitializationFromCacheDelay = delay;
    }

    /**
     * Get the manager for the persistent cache store for resolved metadata.
     * 
     * @return the cache manager if configured, or null
     */
    @Nullable public XMLObjectLoadSaveManager<EntityDescriptor> getPersistentCacheManager() {
        return persistentCacheManager;
    }

    /**
     * Set the manager for the persistent cache store for resolved metadata.
     * 
     * @param manager the cache manager, may be null
     */
    public void setPersistentCacheManager(@Nullable final XMLObjectLoadSaveManager<EntityDescriptor> manager) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        persistentCacheManager = manager;
    }
    
    /**
     * Get the flag indicating whether persistent caching of the resolved metadata is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isPersistentCachingEnabled() {
        return getPersistentCacheManager() != null;
    }

    /**
     * Get the function for generating the String key used with the persistent cache manager. 
     * 
     * @return the key generator or null
     */
    @NonnullAfterInit public Function<EntityDescriptor, String> getPersistentCacheKeyGenerator() {
        return persistentCacheKeyGenerator;
    }

    /**
     * Set the function for generating the String key used with the persistent cache manager. 
     * 
     * @param generator the new generator to set, may be null
     */
    public void setPersistentCacheKeyGenerator(@Nullable final Function<EntityDescriptor, String> generator) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        persistentCacheKeyGenerator = generator;
    }

    /**
     * Get the predicate which determines whether a given entity should be loaded from the persistent cache
     * at resolver initialization time.
     * 
     * @return the cache initialization predicate
     */
    @NonnullAfterInit public Predicate<EntityDescriptor> getInitializationFromCachePredicate() {
        return initializationFromCachePredicate;
    }

    /**
     * Set the predicate which determines whether a given entity should be loaded from the persistent cache
     * at resolver initialization time.
     * 
     * @param predicate the cache initialization predicate
     */
    public void setInitializationFromCachePredicate(@Nullable final Predicate<EntityDescriptor> predicate) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        initializationFromCachePredicate = predicate;
    }

    /**
     *  Get the minimum cache duration for metadata.
     *  
     *  <p>Defaults to: 10 minutes.</p>
     *  
     * @return the minimum cache duration, in milliseconds
     */
    @Nonnull public Long getMinCacheDuration() {
        return minCacheDuration;
    }

    /**
     *  Set the minimum cache duration for metadata.
     *  
     *  <p>Defaults to: 10 minutes.</p>
     *  
     * @param duration the minimum cache duration, in milliseconds
     */
    public void setMinCacheDuration(@Nonnull final Long duration) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        minCacheDuration = Constraint.isNotNull(duration, "Minimum cache duration may not be null");
    }

    /**
     *  Get the maximum cache duration for metadata.
     *  
     *  <p>Defaults to: 8 hours.</p>
     *  
     * @return the maximum cache duration, in milliseconds
     */
    @Nonnull public Long getMaxCacheDuration() {
        return maxCacheDuration;
    }

    /**
     *  Set the maximum cache duration for metadata.
     *  
     *  <p>Defaults to: 8 hours.</p>
     *  
     * @param duration the maximum cache duration, in milliseconds
     */
    public void setMaxCacheDuration(@Nonnull final Long duration) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        maxCacheDuration = Constraint.isNotNull(duration, "Maximum cache duration may not be null");
    }
    
    /**
     * Gets the delay factor used to compute the next refresh time.
     * 
     * <p>Defaults to:  0.75.</p>
     * 
     * @return delay factor used to compute the next refresh time
     */
    public Float getRefreshDelayFactor() {
        return refreshDelayFactor;
    }

    /**
     * Sets the delay factor used to compute the next refresh time. The delay must be between 0.0 and 1.0, exclusive.
     * 
     * <p>Defaults to:  0.75.</p>
     * 
     * @param factor delay factor used to compute the next refresh time
     */
    public void setRefreshDelayFactor(final Float factor) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        if (factor <= 0 || factor >= 1) {
            throw new IllegalArgumentException("Refresh delay factor must be a number between 0.0 and 1.0, exclusive");
        }

        refreshDelayFactor = factor;
    }

    /**
     * Get the flag indicating whether idle entity data should be removed. 
     * 
     * @return true if idle entity data should be removed, false otherwise
     */
    public boolean isRemoveIdleEntityData() {
        return removeIdleEntityData;
    }

    /**
     * Set the flag indicating whether idle entity data should be removed. 
     * 
     * @param flag true if idle entity data should be removed, false otherwise
     */
    public void setRemoveIdleEntityData(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        removeIdleEntityData = flag;
    }

    /**
     * Get the maximum idle time in milliseconds for which the resolver will keep data for a given entityID, 
     * before it is removed.
     * 
     * <p>Defaults to: 8 hours.</p>
     * 
     * @return return the maximum idle time in milliseconds
     */
    @Nonnull public Long getMaxIdleEntityData() {
        return maxIdleEntityData;
    }

    /**
     * Set the maximum idle time in milliseconds for which the resolver will keep data for a given entityID, 
     * before it is removed.
     * 
     * <p>Defaults to: 8 hours.</p>
     * 
     * @param max the maximum entity data idle time, in milliseconds
     */
    public void setMaxIdleEntityData(@Nonnull final Long max) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        maxIdleEntityData = Constraint.isNotNull(max, "Max idle entity data may not be null");
    }

    /**
     * Get the interval in milliseconds at which the cleanup task should run.
     * 
     * <p>Defaults to: 30 minutes.</p>
     * 
     * @return return the interval, in milliseconds
     */
    @Nonnull public Long getCleanupTaskInterval() {
        return cleanupTaskInterval;
    }

    /**
     * Set the interval in milliseconds at which the cleanup task should run.
     * 
     * <p>Defaults to: 30 minutes.</p>
     * 
     * @param interval the interval to set, in milliseconds
     */
    public void setCleanupTaskInterval(@Nonnull final Long interval) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        cleanupTaskInterval = Constraint.isNotNull(interval, "Cleanup task interval may not be null");
    }

    /**
     * Get the base name for Metrics instrumentation.
     * 
     * @return the Metrics base name
     */
    @NonnullAfterInit public String getMetricsBaseName() {
        return metricsBaseName;
    }
    
    /**
     * Set the base name for Metrics instrumentation.
     * 
     * @param baseName the Metrics base name
     */
    public void setMetricsBaseName(@Nullable final String baseName) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        metricsBaseName = StringSupport.trimOrNull(baseName);
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public Iterable<EntityDescriptor> resolve(@Nonnull final CriteriaSet criteria) throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        final Context contextResolve = MetricsSupport.startTimer(timerResolve);
        try {
            final EntityIdCriterion entityIdCriterion = criteria.get(EntityIdCriterion.class);
            if (entityIdCriterion == null || Strings.isNullOrEmpty(entityIdCriterion.getEntityId())) {
                log.info("{} Entity Id was not supplied in criteria set, skipping resolution", getLogPrefix());
                return Collections.emptySet();
            }

            final String entityID = StringSupport.trimOrNull(criteria.get(EntityIdCriterion.class).getEntityId());
            log.debug("{} Attempting to resolve metadata for entityID: {}", getLogPrefix(), entityID);

            final EntityManagementData mgmtData = getBackingStore().getManagementData(entityID);
            final Lock readLock = mgmtData.getReadWriteLock().readLock();
            Iterable<EntityDescriptor> candidates = null;
            try {
                readLock.lock();

                final List<EntityDescriptor> descriptors = lookupEntityID(entityID);
                if (descriptors.isEmpty()) {
                    log.debug("{} Did not find requested metadata in backing store, attempting to resolve dynamically", 
                            getLogPrefix());
                } else {
                    if (shouldAttemptRefresh(mgmtData)) {
                        log.debug("{} Metadata was indicated to be refreshed based on refresh trigger time", 
                                getLogPrefix());
                    } else {
                        log.debug("{} Found requested metadata in backing store", getLogPrefix());
                        candidates = descriptors;
                    }
                }
            } finally {
                readLock.unlock();
            }

            if (candidates == null) {
                candidates = resolveFromOriginSource(criteria);
            }

            return predicateFilterCandidates(candidates, criteria, false);
        } finally {
            MetricsSupport.stopTimer(contextResolve);
        }
    }
    
    /**
     * Fetch metadata from an origin source based on the input criteria, store it in the backing store 
     * and then return it.
     * 
     * @param criteria the input criteria set
     * @return the resolved metadata
     * @throws ResolverException  if there is a fatal error attempting to resolve the metadata
     */
    @Nonnull @NonnullElements protected Iterable<EntityDescriptor> resolveFromOriginSource(
            @Nonnull final CriteriaSet criteria) throws ResolverException {
        
        final String entityID = StringSupport.trimOrNull(criteria.get(EntityIdCriterion.class).getEntityId());
        final EntityManagementData mgmtData = getBackingStore().getManagementData(entityID);
        final Lock writeLock = mgmtData.getReadWriteLock().writeLock(); 
        
        try {
            writeLock.lock();
            
            // It's possible that multiple threads fall into here and attempt to preemptively refresh. 
            // This check should ensure that only 1 actually successfully does it, b/c the refresh
            // trigger time will be updated as seen by the subsequent ones. 
            final List<EntityDescriptor> descriptors = lookupEntityID(entityID);
            if (!descriptors.isEmpty() && !shouldAttemptRefresh(mgmtData)) {
                log.debug("{} Metadata was resolved and stored by another thread " 
                        + "while this thread was waiting on the write lock", getLogPrefix());
                return descriptors;
            } else {
                log.debug("{} Resolving metadata dynamically for entity ID: {}", getLogPrefix(), entityID);
            }
            
            final Context contextFetchFromOriginSource = MetricsSupport.startTimer(timerFetchFromOriginSource);
            XMLObject root = null;
            try {
                root = fetchFromOriginSource(criteria);
            } finally {
                MetricsSupport.stopTimer(contextFetchFromOriginSource);
            }
            
            if (root == null) {
                log.debug("{} No metadata was fetched from the origin source", getLogPrefix());
            } else {
                try {
                    processNewMetadata(root, entityID);
                } catch (final FilterException e) {
                    log.error("{} Metadata filtering problem processing new metadata", getLogPrefix(), e);
                }
            }
            
            return lookupEntityID(entityID);
            
        } catch (final IOException e) {
            log.error("{} Error fetching metadata from origin source", getLogPrefix(), e);
            return lookupEntityID(entityID);
        } finally {
            writeLock.unlock();
        }
        
    }

    /**
     * Fetch the metadata from the origin source.
     * 
     * @param criteria the input criteria set
     * @return the resolved metadata root XMLObject, or null if metadata could not be fetched
     * @throws IOException if there is a fatal error fetching metadata from the origin source
     */
    @Nullable protected abstract XMLObject fetchFromOriginSource(@Nonnull final CriteriaSet criteria) 
            throws IOException;

    /** {@inheritDoc} */
    @Override
    @Nonnull @NonnullElements protected List<EntityDescriptor> lookupEntityID(@Nonnull final String entityID) 
            throws ResolverException {
        getBackingStore().getManagementData(entityID).recordEntityAccess();
        return super.lookupEntityID(entityID);
    }

    /**
     * Process the specified new metadata document, including metadata filtering, and store the 
     * processed metadata in the backing store.
     * 
     * <p>
     * Equivalent to {@link #processNewMetadata(XMLObject, String, false)}.
     * </p>
     * 
     * @param root the root of the new metadata document being processed
     * @param expectedEntityID the expected entityID of the resolved metadata
     * 
     * @throws FilterException if there is a problem filtering the metadata
     */
    @Nonnull protected void processNewMetadata(@Nonnull final XMLObject root, @Nonnull final String expectedEntityID) 
            throws FilterException {
        try {
            processNewMetadata(root, expectedEntityID, false);
        } catch (final ResolverException e) {
            //TODO this is kludgy, but necessary until we can change the API to add an exception to the method signature
            throw new FilterException(e);
        }
    }
    
    /**
     * Process the specified new metadata document, including metadata filtering, and store the 
     * processed metadata in the backing store.
     * 
     * <p>
     * In order to be processed successfully, the metadata (after filtering) must be an instance of
     * {@link EntityDescriptor} and its <code>entityID</code> value must match the value supplied
     * as the required <code>expectedEntityID</code> argument.
     * </p>
     * 
     * @param root the root of the new metadata document being processed
     * @param expectedEntityID the expected entityID of the resolved metadata
     * @param fromPersistentCache whether the entity data was loaded from the persistent cache
     * 
     * @throws FilterException if there is a problem filtering the metadata
     * @throws ResolverException if there is a problem processing the metadata
     */
    //CheckStyle: ReturnCount|CyclomaticComplexity OFF
    @Nonnull protected void processNewMetadata(@Nonnull final XMLObject root, @Nonnull final String expectedEntityID,
            final boolean fromPersistentCache) throws FilterException, ResolverException {
        
        final XMLObject filteredMetadata = filterMetadata(prepareForFiltering(root));
        
        if (filteredMetadata == null) {
            log.info("{} Metadata filtering process produced a null document, resulting in an empty data set", 
                    getLogPrefix());
            releaseMetadataDOM(root);
            if (fromPersistentCache) {
                throw new FilterException("Metadata filtering process produced a null XMLObject");
            } else {
                return;
            }
        }
        
        if (filteredMetadata instanceof EntityDescriptor) {
            final EntityDescriptor entityDescriptor = (EntityDescriptor) filteredMetadata;
            if (!Objects.equals(entityDescriptor.getEntityID(), expectedEntityID)) {
                log.warn("{} New metadata's entityID '{}' does not match expected entityID '{}', will not process", 
                        getLogPrefix(), entityDescriptor.getEntityID(), expectedEntityID);
                if (fromPersistentCache) {
                    throw new ResolverException("New metadata's entityID does not match expected entityID");
                } else {
                    return; 
                }
            }
            
            preProcessEntityDescriptor(entityDescriptor, getBackingStore());
            
            log.info("{} Successfully loaded new EntityDescriptor with entityID '{}' from {}",
                    getLogPrefix(), entityDescriptor.getEntityID(), 
                    fromPersistentCache ? "persistent cache" : "origin source");
            
            // Note: we store in the cache the original input XMLObject, not the filtered one
            if (isPersistentCachingEnabled() && !fromPersistentCache && (root instanceof EntityDescriptor)) {
                final EntityDescriptor origDescriptor = (EntityDescriptor) root;
                final String key = getPersistentCacheKeyGenerator().apply(origDescriptor);
                log.trace("{} Storing resolved EntityDescriptor '{}' in persistent cache with key '{}'", 
                        getLogPrefix(), origDescriptor.getEntityID(), key);
                if (key == null) {
                    log.warn("{} Could not generate cache storage key for EntityDescriptor '{}', skipping caching", 
                            getLogPrefix(), origDescriptor.getEntityID());
                } else {
                    try {
                        getPersistentCacheManager().save(key, origDescriptor, true);
                    } catch (final IOException e) {
                        log.warn("{} Error saving EntityDescriptor '{}' to cache store with key {}'", 
                                getLogPrefix(), origDescriptor.getEntityID(), key);
                    }
                }
            }
            
        } else {
            log.warn("{} Document root was not an EntityDescriptor: {}", getLogPrefix(), root.getClass().getName());
        }
        
        releaseMetadataDOM(filteredMetadata);
        releaseMetadataDOM(root);
    
    }
    //CheckStyle: ReturnCount|CyclomaticComplexity ON
    
    /**
     * Prepare the object for filtering:  If persistent caching is enabled, return a clone of the object
     * in case the configured filter mutates the object.
     * 
     * @param input the XMLObject on which to operate
     * @return the XMLObject instance to be filtered
     */
    @Nonnull protected XMLObject prepareForFiltering(@Nonnull final XMLObject input) {
        if (getMetadataFilter() != null && isPersistentCachingEnabled()) {
            // For this case, we want to filter a clone of the input root object, since filters can mutate
            // the XMLObject and this will cause DOM to be dropped. This will muck with the persistent cache if
            //   1) the root doesn't expose its source byte[] via object metadata, and
            //   2) the object can't be successfully round-tripped (e.g signatures).
            try {
                return XMLObjectSupport.cloneXMLObject(input, CloneOutputOption.RootDOMInNewDocument);
            } catch (final MarshallingException | UnmarshallingException e) {
                log.warn("{} Error cloning XMLObject, will use input root object as filter target", getLogPrefix(), e);
                return input;
            }
        } else {
            return input;
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void preProcessEntityDescriptor(@Nonnull final EntityDescriptor entityDescriptor, 
            @Nonnull final EntityBackingStore backingStore) {
        
        final String entityID = StringSupport.trimOrNull(entityDescriptor.getEntityID());
        
        removeByEntityID(entityID, backingStore);
        
        super.preProcessEntityDescriptor(entityDescriptor, backingStore);
        
        final DynamicEntityBackingStore dynamicBackingStore = (DynamicEntityBackingStore) backingStore;
        final EntityManagementData mgmtData = dynamicBackingStore.getManagementData(entityID);
        
        final DateTime now = new DateTime(ISOChronology.getInstanceUTC());
        log.debug("{} For metadata expiration and refresh computation, 'now' is : {}", getLogPrefix(), now);
        
        mgmtData.setLastUpdateTime(now);
        
        mgmtData.setExpirationTime(computeExpirationTime(entityDescriptor, now));
        log.debug("{} Computed metadata expiration time: {}", getLogPrefix(), mgmtData.getExpirationTime());
        
        mgmtData.setRefreshTriggerTime(computeRefreshTriggerTime(mgmtData.getExpirationTime(), now));
        log.debug("{} Computed refresh trigger time: {}", getLogPrefix(), mgmtData.getRefreshTriggerTime());
    }

    /**
     * Compute the effective expiration time for the specified metadata.
     * 
     * @param entityDescriptor the EntityDescriptor instance to evaluate
     * @param now the current date time instant
     * @return the effective expiration time for the metadata
     */
    @Nonnull protected DateTime computeExpirationTime(@Nonnull final EntityDescriptor entityDescriptor,
            @Nonnull final DateTime now) {
        
        final DateTime lowerBound = now.toDateTime(ISOChronology.getInstanceUTC()).plus(getMinCacheDuration());
        
        DateTime expiration = SAML2Support.getEarliestExpiration(entityDescriptor, 
                now.plus(getMaxCacheDuration()), now);
        if (expiration.isBefore(lowerBound)) {
            expiration = lowerBound;
        }
        
        return expiration;
    }
    
    /**
     * Compute the refresh trigger time.
     * 
     * @param expirationTime the time at which the metadata effectively expires
     * @param nowDateTime the current date time instant
     * 
     * @return the time after which refresh attempt(s) should be made
     */
    @Nonnull protected DateTime computeRefreshTriggerTime(@Nullable final DateTime expirationTime,
            @Nonnull final DateTime nowDateTime) {
        
        final DateTime nowDateTimeUTC = nowDateTime.toDateTime(ISOChronology.getInstanceUTC());
        final long now = nowDateTimeUTC.getMillis();

        long expireInstant = 0;
        if (expirationTime != null) {
            expireInstant = expirationTime.toDateTime(ISOChronology.getInstanceUTC()).getMillis();
        }
        long refreshDelay = (long) ((expireInstant - now) * getRefreshDelayFactor());

        // if the expiration time was null or the calculated refresh delay was less than the floor
        // use the floor
        if (refreshDelay < getMinCacheDuration()) {
            refreshDelay = getMinCacheDuration();
        }

        return nowDateTimeUTC.plus(refreshDelay);
    }
    
    /**
     * Determine whether should attempt to refresh the metadata, based on stored refresh trigger time.
     * 
     * @param mgmtData the entity'd management data
     * @return true if should attempt refresh, false otherwise
     */
    protected boolean shouldAttemptRefresh(@Nonnull final EntityManagementData mgmtData) {
        final DateTime now = new DateTime(ISOChronology.getInstanceUTC());
        return now.isAfter(mgmtData.getRefreshTriggerTime());
        
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected DynamicEntityBackingStore createNewBackingStore() {
        return new DynamicEntityBackingStore();
    }
    
    /** {@inheritDoc} */
    @Override
    @NonnullAfterInit protected DynamicEntityBackingStore getBackingStore() {
        return (DynamicEntityBackingStore) super.getBackingStore();
    }
    
    /** {@inheritDoc} */
    @Override
    protected void initMetadataResolver() throws ComponentInitializationException {
        try {
            initializing = true;
            
            super.initMetadataResolver();
            
            initializeMetricsInstrumentation();
            
            setBackingStore(createNewBackingStore());
            
            if (getPersistentCacheKeyGenerator() == null) {
                setPersistentCacheKeyGenerator(new DefaultCacheKeyGenerator());
            }
            
            if (getInitializationFromCachePredicate() == null) {
                setInitializationFromCachePredicate(Predicates.<EntityDescriptor>alwaysTrue());
            }
            
            persistentCacheInitMetrics = new PersistentCacheInitializationMetrics();
            if (isPersistentCachingEnabled()) {
                persistentCacheInitMetrics.enabled = true;
                if (isInitializeFromPersistentCacheInBackground()) {
                    log.debug("{} Initializing from the persistent cache in the background in {} ms", 
                            getLogPrefix(), getBackgroundInitializationFromCacheDelay());
                    final TimerTask initTask = new TimerTask() {
                        public void run() {
                            initializeFromPersistentCache();
                        }
                    };
                    taskTimer.schedule(initTask, getBackgroundInitializationFromCacheDelay());
                } else {
                    log.debug("{} Initializing from the persistent cache in the foreground", getLogPrefix());
                    initializeFromPersistentCache();
                }
            }
            
            cleanupTask = new BackingStoreCleanupSweeper();
            // Start with a delay of 1 minute, run at the user-specified interval
            taskTimer.schedule(cleanupTask, 1*60*1000, getCleanupTaskInterval());

        } finally {
            initializing = false;
        }
    }

    /**
     * Initialize the Metrics-based instrumentation.
     */
    private void initializeMetricsInstrumentation() {
        if (getMetricsBaseName() == null) {
            setMetricsBaseName(MetricRegistry.name(this.getClass(), getId()));
        }
        
        final MetricRegistry metricRegistry = MetricsSupport.getMetricRegistry();
        if (metricRegistry != null) {
            timerResolve = metricRegistry.timer(
                    MetricRegistry.name(getMetricsBaseName(), METRIC_TIMER_RESOLVE));
            timerFetchFromOriginSource = metricRegistry.timer(
                    MetricRegistry.name(getMetricsBaseName(), METRIC_TIMER_FETCH_FROM_ORIGIN_SOURCE));

            // Note that these gauges must use the support method to register in a synchronized fashion,
            // and also must store off the instances for later use in destroy.
            ratioGaugeFetchToResolve = MetricsSupport.register(
                    MetricRegistry.name(getMetricsBaseName(), METRIC_RATIOGAUGE_FETCH_TO_RESOLVE), 
                    new RatioGauge() {
                        protected Ratio getRatio() {
                            return Ratio.of(timerFetchFromOriginSource.getCount(), 
                                    timerResolve.getCount());
                        }},
                    true);
            
            gaugeNumLiveEntityIDs = MetricsSupport.register(
                    MetricRegistry.name(getMetricsBaseName(), METRIC_GAUGE_NUM_LIVE_ENTITYIDS),
                    new Gauge<Integer>() {
                        public Integer getValue() {
                            return getBackingStore().getIndexedDescriptors().keySet().size();
                        }},
                    true);
            
            gaugePersistentCacheInit = MetricsSupport.register(
                    MetricRegistry.name(getMetricsBaseName(), METRIC_GAUGE_PERSISTENT_CACHE_INIT),
                    new Gauge<PersistentCacheInitializationMetrics>() {
                        public PersistentCacheInitializationMetrics getValue() {
                            return persistentCacheInitMetrics;
                        }},
                    true);
        }
    }
    
    /**
     * Initialize the resolver with data from the persistent cache manager, if enabled.
     */
    protected void initializeFromPersistentCache() {
        if (!isPersistentCachingEnabled()) {
            log.trace("{} Persistent caching is not enabled, skipping init from cache", getLogPrefix());
            return;
        } else {
            log.trace("{} Attempting to load and process entities from the persistent cache", getLogPrefix());
        }
        
        final long start = System.nanoTime();
        try {
            for (final Pair<String, EntityDescriptor> cacheEntry: getPersistentCacheManager().listAll()) {
                persistentCacheInitMetrics.entriesTotal++;
                final EntityDescriptor descriptor = cacheEntry.getSecond();
                final String currentKey = cacheEntry.getFirst();
                log.trace("{} Loaded EntityDescriptor from cache store with entityID '{}' and storage key '{}'", 
                        getLogPrefix(), descriptor.getEntityID(), currentKey);
                
                final String entityID = StringSupport.trimOrNull(descriptor.getEntityID());
                final EntityManagementData mgmtData = getBackingStore().getManagementData(entityID);
                final Lock writeLock = mgmtData.getReadWriteLock().writeLock(); 
                
                try {
                    writeLock.lock();
                    
                    // This can happen if we init from the persistent cache in a background thread,
                    // and metadata for this entityID was resolved before we hit this cache entry.
                    if (!lookupIndexedEntityID(entityID).isEmpty()) {
                        log.trace("{} Metadata for entityID '{}' found in persistent cache was already live, " 
                                + "ignoring cached entry", getLogPrefix(), entityID);
                        persistentCacheInitMetrics.entriesSkippedAlreadyLive++;
                        continue;
                    }
                
                    processPersistentCacheEntry(currentKey, descriptor);
                    
                } finally {
                    writeLock.unlock();
                }
            }
        } catch (final IOException e) {
            log.warn("{} Error loading EntityDescriptors from cache", getLogPrefix(), e);
        } finally {
            persistentCacheInitMetrics.processingTime = System.nanoTime() - start; 
            log.debug("{} Persistent cache initialization metrics: {}", getLogPrefix(), persistentCacheInitMetrics);
        }
    }

    /**
     * Process an entry loaded from the persistent cache.
     * 
     * @param currentKey the current persistent cache key
     * @param descriptor the entity descriptor to process
     */
    protected void processPersistentCacheEntry(@Nonnull final String currentKey, 
            @Nonnull final EntityDescriptor descriptor) {
        
        if (isValid(descriptor)) {
            if (getInitializationFromCachePredicate().apply(descriptor)) {
                try {
                    processNewMetadata(descriptor, descriptor.getEntityID(), true);
                    log.trace("{} Successfully processed EntityDescriptor with entityID '{}' from cache", 
                            getLogPrefix(), descriptor.getEntityID());
                    persistentCacheInitMetrics.entriesLoaded++;
                } catch (final FilterException | ResolverException e) {
                    log.warn("{} Error processing EntityDescriptor '{}' from cache with storage key '{}'", 
                            getLogPrefix(), descriptor.getEntityID(), currentKey, e);
                    persistentCacheInitMetrics.entriesSkippedProcessingException++;
                }
            } else {
                log.trace("{} Cache initialization predicate indicated to not process EntityDescriptor " 
                        + "with entityID '{}' and cache storage key '{}'",
                        getLogPrefix(), descriptor.getEntityID(), currentKey);
                persistentCacheInitMetrics.entriesSkippedFailedPredicate++;
            }
            
            // Update storage key if necessary, e.g. if cache key generator impl has changed.
            final String expectedKey = getPersistentCacheKeyGenerator().apply(descriptor);
            try {
                if (!Objects.equals(currentKey, expectedKey)) {
                    log.trace("{} Current cache storage key '{}' differs from expected key '{}', updating",
                            getLogPrefix(), currentKey, expectedKey);
                    getPersistentCacheManager().updateKey(currentKey, expectedKey);
                    log.trace("{} Successfully updated cache storage key '{}' to '{}'", 
                            getLogPrefix(), currentKey, expectedKey);
                }
            } catch (final IOException e) {
                log.warn("{} Error updating cache storage key '{}' to '{}'", 
                        getLogPrefix(), currentKey, expectedKey, e);
            }
                
        } else {
            log.trace("{} EntityDescriptor with entityID '{}' and storaage key '{}' in cache was " 
                    + "not valid, skipping and removing", getLogPrefix(), descriptor.getEntityID(), currentKey);
            persistentCacheInitMetrics.entriesSkippedInvalid++;
            try {
                getPersistentCacheManager().remove(currentKey);
            } catch (final IOException e) {
                log.warn("{} Error removing invalid EntityDescriptor '{}' from persistent cache with key '{}'",
                        getLogPrefix(), descriptor.getEntityID(), currentKey);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void removeByEntityID(final String entityID, final EntityBackingStore backingStore) {
        if (isPersistentCachingEnabled()) {
            final List<EntityDescriptor> descriptors = backingStore.getIndexedDescriptors().get(entityID);
            if (descriptors != null) {
                for (final EntityDescriptor descriptor : descriptors) {
                    final String key = getPersistentCacheKeyGenerator().apply(descriptor);
                    try {
                        getPersistentCacheManager().remove(key);
                    } catch (final IOException e) {
                        log.warn("{} Error removing EntityDescriptor '{}' from cache store with key '{}'", 
                                getLogPrefix(), descriptor.getEntityID(), key);
                    }
                }
            }
        }
        
        super.removeByEntityID(entityID, backingStore);
    }

    /** {@inheritDoc} */
    @Override
    protected void doDestroy() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        if (createdOwnTaskTimer) {
            taskTimer.cancel();
        }
        cleanupTask = null;
        taskTimer = null;
        
        if (ratioGaugeFetchToResolve != null) {
            MetricsSupport.remove(MetricRegistry.name(getMetricsBaseName(), METRIC_RATIOGAUGE_FETCH_TO_RESOLVE), 
                    ratioGaugeFetchToResolve);
        }
        if (gaugeNumLiveEntityIDs != null) {
            MetricsSupport.remove(MetricRegistry.name(getMetricsBaseName(), METRIC_GAUGE_NUM_LIVE_ENTITYIDS), 
                    gaugeNumLiveEntityIDs);
        }
        if (gaugePersistentCacheInit != null) {
            MetricsSupport.remove(MetricRegistry.name(getMetricsBaseName(), METRIC_GAUGE_PERSISTENT_CACHE_INIT), 
                    gaugePersistentCacheInit);
        }
        ratioGaugeFetchToResolve = null;
        gaugeNumLiveEntityIDs = null;
        gaugePersistentCacheInit = null;
        timerFetchFromOriginSource = null;
        timerResolve = null;
        
        super.doDestroy();
    }
    
    /**
     * Specialized entity backing store implementation for dynamic metadata resolvers.
     */
    protected class DynamicEntityBackingStore extends EntityBackingStore {
        
        /** Map holding management data for each entityID. */
        private Map<String, EntityManagementData> mgmtDataMap;
        
        /** Constructor. */
        protected DynamicEntityBackingStore() {
            super();
            mgmtDataMap = new ConcurrentHashMap<>();
        }
        
        /**
         * Get the management data for the specified entityID.
         * 
         * @param entityID the input entityID
         * @return the corresponding management data
         */
        @Nonnull public EntityManagementData getManagementData(@Nonnull final String entityID) {
            Constraint.isNotNull(entityID, "EntityID may not be null");
            EntityManagementData entityData = mgmtDataMap.get(entityID);
            if (entityData != null) {
                return entityData;
            }
            
            // TODO use intern-ed String here for monitor target?
            synchronized (this) {
                // Check again in case another thread beat us into the monitor
                entityData = mgmtDataMap.get(entityID);
                if (entityData != null) {
                    return entityData;
                } else {
                    entityData = new EntityManagementData(entityID);
                    mgmtDataMap.put(entityID, entityData);
                    return entityData;
                }
            }
        }
        
        /**
         * Remove the management data for the specified entityID.
         * 
         * @param entityID the input entityID
         */
        public void removeManagementData(@Nonnull final String entityID) {
            Constraint.isNotNull(entityID, "EntityID may not be null");
            // TODO use intern-ed String here for monitor target?
            synchronized (this) {
                mgmtDataMap.remove(entityID);
            }
        }
        
    }
    
    /**
     * Class holding per-entity management data.
     */
    protected class EntityManagementData {
        
        /** The entity ID managed by this instance. */
        private String entityID;
        
        /** Last update time of the associated metadata. */
        private DateTime lastUpdateTime;
        
        /** Expiration time of the associated metadata. */
        private DateTime expirationTime;
        
        /** Time at which should start attempting to refresh the metadata. */
        private DateTime refreshTriggerTime;
        
        /** The last time in milliseconds at which the entity's backing store data was accessed. */
        private DateTime lastAccessedTime;
        
        /** Read-write lock instance which governs access to the entity's backing store data. */
        private ReadWriteLock readWriteLock;
        
        /** Constructor. 
         * 
         * @param id the entity ID managed by this instance
         */
        protected EntityManagementData(@Nonnull final String id) {
            entityID = Constraint.isNotNull(id, "Entity ID was null");
            final DateTime now = new DateTime(ISOChronology.getInstanceUTC());
            expirationTime = now.plus(getMaxCacheDuration());
            refreshTriggerTime = now.plus(getMaxCacheDuration());
            lastAccessedTime = now;
            readWriteLock = new ReentrantReadWriteLock(true);
        }
        
        /**
         * Get the entity ID managed by this instance.
         * 
         * @return the entity ID
         */
        @Nonnull public String getEntityID() {
            return entityID;
        }
        
        /**
         * Get the last update time of the metadata. 
         * 
         * @return the last update time, or null if no metadata is yet loaded for the entity
         */
        @Nullable public DateTime getLastUpdateTime() {
            return lastUpdateTime;
        }

        /**
         * Set the last update time of the metadata.
         * 
         * @param dateTime the last update time
         */
        public void setLastUpdateTime(@Nonnull final DateTime dateTime) {
            lastUpdateTime = dateTime;
        }
        
        /**
         * Get the expiration time of the metadata. 
         * 
         * @return the expiration time
         */
        @Nonnull public DateTime getExpirationTime() {
            return expirationTime;
        }

        /**
         * Set the expiration time of the metadata.
         * 
         * @param dateTime the new expiration time
         */
        public void setExpirationTime(@Nonnull final DateTime dateTime) {
            expirationTime = Constraint.isNotNull(dateTime, "Expiration time may not be null");
        }
        
        /**
         * Get the refresh trigger time of the metadata. 
         * 
         * @return the refresh trigger time
         */
        @Nonnull public DateTime getRefreshTriggerTime() {
            return refreshTriggerTime;
        }

        /**
         * Set the refresh trigger time of the metadata.
         * 
         * @param dateTime the new refresh trigger time
         */
        public void setRefreshTriggerTime(@Nonnull final DateTime dateTime) {
            refreshTriggerTime = Constraint.isNotNull(dateTime, "Refresh trigger time may not be null");
        }

        /**
         * Get the last time at which the entity's backing store data was accessed.
         * 
         * @return the time in milliseconds since the epoch
         */
        @Nonnull public DateTime getLastAccessedTime() {
            return lastAccessedTime;
        }
        
        /**
         * Record access of the entity's backing store data.
         */
        public void recordEntityAccess() {
            lastAccessedTime = new DateTime(ISOChronology.getInstanceUTC());
        }

        /**
         * Get the read-write lock instance which governs access to the entity's backing store data. 
         * 
         * @return the lock instance
         */
        @Nonnull public ReadWriteLock getReadWriteLock() {
            return readWriteLock;
        }
        
    }
    
    /**
     * Background maintenance task which cleans expired and idle metadata from the backing store, and removes
     * orphaned entity management data.
     */
    protected class BackingStoreCleanupSweeper extends TimerTask {
        
        /** Logger. */
        private final Logger log = LoggerFactory.getLogger(BackingStoreCleanupSweeper.class);

        /** {@inheritDoc} */
        @Override
        public void run() {
            if (isDestroyed() || !isInitialized()) {
                // just in case the metadata resolver was destroyed before this task runs, 
                // or if it somehow is being called on a non-successfully-inited resolver instance.
                log.debug("{} BackingStoreCleanupSweeper will not run because: inited: {}, destroyed: {}",
                        getLogPrefix(), isInitialized(), isDestroyed());
                return;
            }
            
            removeExpiredAndIdleMetadata();
        }

        /**
         *  Purge metadata which is either 1) expired or 2) (if {@link #isRemoveIdleEntityData()} is true) 
         *  which hasn't been accessed within the last {@link #getMaxIdleEntityData()} milliseconds.
         */
        private void removeExpiredAndIdleMetadata() {
            final DateTime now = new DateTime(ISOChronology.getInstanceUTC());
            final DateTime earliestValidLastAccessed = now.minus(getMaxIdleEntityData());
            
            final DynamicEntityBackingStore backingStore = getBackingStore();
            final Map<String, List<EntityDescriptor>> indexedDescriptors = backingStore.getIndexedDescriptors();
            
            for (final String entityID : indexedDescriptors.keySet()) {
                final EntityManagementData mgmtData = backingStore.getManagementData(entityID);
                final Lock writeLock = mgmtData.getReadWriteLock().writeLock();
                try {
                    writeLock.lock();
                    
                    if (isRemoveData(mgmtData, now, earliestValidLastAccessed)) {
                        removeByEntityID(entityID, backingStore);
                        backingStore.removeManagementData(entityID);
                    }
                    
                } finally {
                    writeLock.unlock();
                }
            }
            
        }
        
        /**
         * Determine whether metadata should be removed based on expiration and idle time data.
         * 
         * @param mgmtData the management data instance for the entity
         * @param now the current time
         * @param earliestValidLastAccessed the earliest last accessed time which would be valid
         * 
         * @return true if the entity is expired or exceeds the max idle time, false otherwise
         */
        private boolean isRemoveData(@Nonnull final EntityManagementData mgmtData, 
                @Nonnull final DateTime now, @Nonnull final DateTime earliestValidLastAccessed) {
            if (isRemoveIdleEntityData() && mgmtData.getLastAccessedTime().isBefore(earliestValidLastAccessed)) {
                log.debug("{} Entity metadata exceeds maximum idle time, removing: {}", 
                        getLogPrefix(), mgmtData.getEntityID());
                return true;
            } else if (now.isAfter(mgmtData.getExpirationTime())) {
                log.debug("{} Entity metadata is expired, removing: {}", getLogPrefix(), mgmtData.getEntityID());
                return true;
            } else {
                return false;
            }
        }
        
    }
    
    /**
     * Default function for generating a cache key for loading and saving an {@link EntityDescriptor}
     * using a {@link XMLObjectLoadSaveManager}.
     */
    public static class DefaultCacheKeyGenerator implements Function<EntityDescriptor, String> {
        
        /** String digester for the EntityDescriptor's entityID. */
        private StringDigester digester;
        
        /** Constructor. */
        public DefaultCacheKeyGenerator() {
            try {
                digester = new StringDigester(JCAConstants.DIGEST_SHA1, OutputFormat.HEX_LOWER);
            } catch (final NoSuchAlgorithmException e) {
                // this can't really happen b/c SHA-1 is required to be supported on all JREs.
            }
        }

        /** {@inheritDoc} */
        @Override
        public String apply(final EntityDescriptor input) {
            if (input == null) {
                return null;
            }
            
            final String entityID = StringSupport.trimOrNull(input.getEntityID());
            if (entityID == null) {
                return null;
            }
            
            return digester.apply(entityID);
        }
        
    }
    
    /**
     * Class used to track metrics related to the initialization from the persistent cache.
     */
    public static class PersistentCacheInitializationMetrics {
        
        /** Whether or not persistent caching was enabled. */
        private boolean enabled;
        
        /** Total processing time for the persistent cache, in nanoseconds. */
        private long processingTime;
        
        /** Total entries seen in the persistent cache. */
        private int entriesTotal;
        
        /** Entries which were successfully loaded and made live. */
        private int entriesLoaded;
        
        /** Entries which were skipped because they were already live by the time they were processed, 
         * generally only seen when initializing from the persistent cache in a background thread. */
        private int entriesSkippedAlreadyLive;
        
        /** Entries which were skipped because they were determined to be invalid. */
        private int entriesSkippedInvalid;
        
        /** Entries which were skipped because they failed the persistent cache predicate evaluation. */
        private int entriesSkippedFailedPredicate;
        
        /** Entries which were skipped due to a processing exception. */
        private int entriesSkippedProcessingException;
        
        /**
         * Get whether or not persistent caching was enabled. 
         * @return Returns the enabled.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Get total processing time for the persistent cache, in nanoseconds.
         * @return Returns the processingTime.
         */
        public long getProcessingTime() {
            return processingTime;
        }

        /**
         * Get total entries seen in the persistent cache.
         * @return Returns the entriesTotal.
         */
        public int getEntriesTotal() {
            return entriesTotal;
        }

        /**
         * Get entries which were successfully loaded and made live. 
         * @return Returns the entriesLoaded.
         */
        public int getEntriesLoaded() {
            return entriesLoaded;
        }

        /**
         * Get entries which were skipped because they were already live by the time they were processed, 
         * generally only seen when initializing from the persistent cache in a background thread. 
         * @return Returns the entriesSkippedAlreadyLive.
         */
        public int getEntriesSkippedAlreadyLive() {
            return entriesSkippedAlreadyLive;
        }

        /**
         * Get entries which were skipped because they were determined to be invalid.
         * @return Returns the entriesSkippedInvalid.
         */
        public int getEntriesSkippedInvalid() {
            return entriesSkippedInvalid;
        }

        /**
         * Get entries which were skipped because they failed the persistent cache predicate evaluation.
         * @return Returns the entriesSkippedFailedPredicate.
         */
        public int getEntriesSkippedFailedPredicate() {
            return entriesSkippedFailedPredicate;
        }

        /**
         * Get entries which were skipped due to a processing exception. 
         * @return Returns the entriesSkippedProcessingException.
         */
        public int getEntriesSkippedProcessingException() {
            return entriesSkippedProcessingException;
        }

        /** {@inheritDoc} */
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("enabled", enabled)
                    .add("processingTime", processingTime)
                    .add("entriesTotal", entriesTotal)
                    .add("entriesLoaded", entriesLoaded)
                    .add("entriesSkippedAlreadyLive", entriesSkippedAlreadyLive)
                    .add("entriesSkippedInvalid", entriesSkippedInvalid)
                    .add("entriesSkippedFailedPredicate", entriesSkippedFailedPredicate)
                    .add("entriesSkippedProcessingException", entriesSkippedProcessingException)
                    .toString();
        }
        
    }

}
