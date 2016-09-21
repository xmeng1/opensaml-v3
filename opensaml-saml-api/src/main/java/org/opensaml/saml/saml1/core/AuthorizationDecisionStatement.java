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

package org.opensaml.saml.saml1.core;

import java.util.List;

import javax.xml.namespace.QName;

import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;

/**
 * This interface defines how the object representing a SAML1 <code> AuthorizationDecisionStatement </code> element
 * behaves.
 */
public interface AuthorizationDecisionStatement extends SAMLObject, SubjectStatement {

    /** Element name, no namespace. */
    public static final String DEFAULT_ELEMENT_LOCAL_NAME = "AuthorizationDecisionStatement";
    
    /** Default element name. */
    public static final QName DEFAULT_ELEMENT_NAME =
            new QName(SAMLConstants.SAML1_NS, DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML1_PREFIX);
    
    /** Local name of the XSI type. */
    public static final String TYPE_LOCAL_NAME = "AuthorizationDecisionStatementType"; 
        
    /** QName of the XSI type. */
    public static final QName TYPE_NAME =
            new QName(SAMLConstants.SAML1_NS, TYPE_LOCAL_NAME, SAMLConstants.SAML1_PREFIX);

    /** Name for Resource attribute. */
    public static final String RESOURCE_ATTRIB_NAME = "Resource";
    
    /** Name for Decision attribute. */
    public static final String DECISION_ATTRIB_NAME = "Decision";
    
    /**
     * Get the resource.
     * 
     * @return the resource
     */
    public String getResource();
    
    /**
     * Set the resource.
     * 
     * @param resource the resource
     */
    public void setResource(String resource);

    /**
     * Get the decision.
     * 
     * @return the decision.
     */
    public DecisionTypeEnumeration getDecision();

    /**
     * Set the decision.
     * 
     * @param decision the decision.
     */
    public void setDecision(DecisionTypeEnumeration decision);

    /**
     * Get the actions.
     * 
     * @return the actions.
     */
    public List<Action> getActions();

    /**
     * Get the evidence.
     * 
     * @return the evidence
     */
    public Evidence getEvidence();

    /**
     * Set the evidence.
     * 
     * @param evidence the evidence
     * 
     * @throws IllegalArgumentException if an error occurs
     */
    public void setEvidence(Evidence evidence) throws IllegalArgumentException;
   
}