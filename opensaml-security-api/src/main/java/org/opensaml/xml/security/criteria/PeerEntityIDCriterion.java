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

package org.opensaml.xml.security.criteria;

import org.opensaml.util.StringSupport;
import org.opensaml.util.criteria.Criterion;

/**
 * An implementation of {@link Criterion} which specifies criteria pertaining 
 * to peer entity IDs.  This is typically used only in conjunction with a
 * {@link EntityIDCriterion}, where the peer is interpreted to be relative
 * to that primary entity ID. In this sense it serves to scope the primary entity ID.
 * 
 * Note that the peer entity ID may be either local or remote,
 * depending on whether the associated primary entity ID is remote or local.
 */
public final class PeerEntityIDCriterion implements Criterion {
    
    /** Peer entity ID criteria. */
    private String peerID;
    
    /**
    * Constructor.
     *
     * @param peer the entity ID which is the peer relative to a primary entity ID
     */
    public PeerEntityIDCriterion(String peer) {
        setPeerID(peer);
    }

    /**
     * Get the entity ID which is the peer relative to a primary entity ID.
     * 
     * @return the peer entity ID.
     */
    public String getPeerID() {
        return peerID;
    }

    /**
     * Set the entity ID which is the peer relative to a primary entity ID.
     * 
     * @param peer The peerID to set.
     */
    public void setPeerID(String peer) {
        String trimmed = StringSupport.trimOrNull(peer);
        if (trimmed == null) {
            throw new IllegalArgumentException("Peer entity ID criteria must be supplied");
        }
        peerID = trimmed;
    }

}