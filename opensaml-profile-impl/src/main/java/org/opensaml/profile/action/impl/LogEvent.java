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

package org.opensaml.profile.action.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.context.EventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.CurrentOrPreviousEventLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;


/**
 * A profile action that logs an event if one is found in the profile request context.
 * 
 * @param <Input> input message type
 * @param <Output> output message type
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class LogEvent<Input,Output> extends AbstractProfileAction<Input,Output> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LogEvent.class);

    /** Strategy function for access to {@link EventContext} to check. */
    @Nonnull private Function<ProfileRequestContext,EventContext> eventContextLookupStrategy;

    /** Set of events to ignore for logging purposes. */
    @Nonnull @NotEmpty private Set<String> suppressedEvents;
    
    /** Constructor. */
    public LogEvent() {
        eventContextLookupStrategy = new CurrentOrPreviousEventLookup();
        suppressedEvents = Collections.emptySet();
    }

    /**
     * Set lookup strategy for {@link EventContext} to check.
     * 
     * @param strategy  lookup strategy
     */
    public void setEventContextLookupStrategy(@Nonnull final Function<ProfileRequestContext,EventContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        eventContextLookupStrategy = Constraint.isNotNull(strategy, "EventContext lookup strategy cannot be null");
    }
    
    /**
     * Set a collection of events to ignore for logging purposes. 
     * 
     * @param events events to ignore
     */
    public void setSuppressedEvents(@Nullable @NotEmpty final Collection<String> events) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        if (events != null) {
            suppressedEvents = new HashSet<>(StringSupport.normalizeStringCollection(events));
        } else {
            suppressedEvents = Collections.emptySet();
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext<Input,Output> profileRequestContext) {
        
        final EventContext eventCtx = eventContextLookupStrategy.apply(profileRequestContext);
        if (eventCtx == null || eventCtx.getEvent() == null) {
            return;
        }
        
        final String eventString = eventCtx.getEvent().toString();
        if (!suppressedEvents.contains(eventString)) {
            log.warn("A non-proceed event occurred while processing the request: {}", eventString);
        }
    }
    
}