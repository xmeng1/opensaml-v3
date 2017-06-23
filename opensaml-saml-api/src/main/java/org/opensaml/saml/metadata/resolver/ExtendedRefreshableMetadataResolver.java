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

package org.opensaml.saml.metadata.resolver;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

/**
 * Extended {@link RefreshableMetadataResolver}.
 */
public interface ExtendedRefreshableMetadataResolver extends RefreshableMetadataResolver {
    
    //TODO promote methods up and remove in 4.0.0
    
    /**
     * Gets the time the last successful refresh cycle occurred.
     * 
     * @return time the last successful refresh cycle occurred
     */
    @Nullable public DateTime getLastSuccessfulRefresh();

    /**
     * Gets whether the last refresh cycle was successful.
     * 
     * @return true if last refresh cycle was successful, false if not
     */
    @Nullable public Boolean wasLastRefreshSuccess();

}
