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

package org.opensaml.saml.saml1.core.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.XMLObjectChildrenList;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml1.core.Action;
import org.opensaml.saml.saml1.core.AuthorizationDecisionQuery;
import org.opensaml.saml.saml1.core.Evidence;

/**
 * Concrete implementation of the {@link org.opensaml.saml.saml1.core.AuthorizationDecisionQuery} interface.
 */
public class AuthorizationDecisionQueryImpl extends SubjectQueryImpl implements AuthorizationDecisionQuery {

    /** Contains the resource attribute. */
    private String resource;

    /** Contains all the Action child elements. */
    private final XMLObjectChildrenList<Action> actions;

    /** Contains the Evidence child element. */
    private Evidence evidence;
    
    /**
     * Constructor.
     * 
     * @param namespaceURI the namespace the element is in
     * @param elementLocalName the local name of the XML element this Object represents
     * @param namespacePrefix the prefix for the given namespace
     */
    protected AuthorizationDecisionQueryImpl(String namespaceURI, String elementLocalName, String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
        setElementNamespacePrefix(SAMLConstants.SAML1P_PREFIX);
        actions = new XMLObjectChildrenList<>(this);
    }

    /** {@inheritDoc} */
    public String getResource() {
        return resource;
    }

    /** {@inheritDoc} */
    public void setResource(String res) {
        resource = prepareForAssignment(resource, res);
    }

    /** {@inheritDoc} */
    public List<Action> getActions() {
        return actions;
    }

    /** {@inheritDoc} */
    public Evidence getEvidence() {
        return evidence;
    }

    /** {@inheritDoc} */
    public void setEvidence(Evidence ev) {
        evidence = prepareForAssignment(evidence, ev);
    }

    /** {@inheritDoc} */
    public List<XMLObject> getOrderedChildren() {
        List<XMLObject> list = new ArrayList<>(actions.size() + 2);
        
        if (super.getOrderedChildren() != null) {
            list.addAll(super.getOrderedChildren());
        }
        
        list.addAll(actions);
        if (evidence != null) {
            list.add(evidence);
        }
        
        if (list.size() == 0) {
            return null;
        }
        
        return Collections.unmodifiableList(list);
    }
}