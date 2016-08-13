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

package org.opensaml.profile.context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;


/**
 * A context component which holds state for making an access control decision using
 * the {@link net.shibboleth.utilities.java.support.security.AccessControl} interface.
 * 
 * @since 3.3.0
 */
public class AccessControlContext extends BaseContext {

    /** The operation. */
    @Nullable private String operation;

    /** The resource. */
    @Nullable private String resource;

    /**
     * Get the operation being performed.
     * 
     * @return the operation
     */
    @Nullable public String getOperation() {
        return operation;
    }

    /**
     * Set the operation being performed.
     * 
     * @param op the operation
     * 
     * @return this context
     */
    @Nonnull public AccessControlContext setOperation(@Nullable final String op) {
        operation = op;
        
        return this;
    }

    /**
     * Get the resource being operated on.
     * 
     * @return the resource
     */
    @Nullable public String getResource() {
        return resource;
    }

    /**
     * Set the resource being operated on.
     * 
     * @param res the resource
     * 
     * @return this context
     */
    @Nonnull public AccessControlContext setResource(@Nullable final String res) {
        resource = res;
        
        return this;
    }

}