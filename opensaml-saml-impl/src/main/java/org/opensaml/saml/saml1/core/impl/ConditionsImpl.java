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

import javax.xml.namespace.QName;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.IndexedXMLObjectChildrenList;
import org.opensaml.saml.common.AbstractSAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml1.core.AudienceRestrictionCondition;
import org.opensaml.saml.saml1.core.Condition;
import org.opensaml.saml.saml1.core.Conditions;
import org.opensaml.saml.saml1.core.DoNotCacheCondition;

/**
 * This is a concrete implementation of the {@link org.opensaml.saml.saml1.core.Conditions} interface.
 */
public class ConditionsImpl extends AbstractSAMLObject implements Conditions {

    /** Value saved in the NotBefore attribute. */
    private DateTime notBefore;

    /** Value saved in the NotOnOrAfter attribute. */
    private DateTime notOnOrAfter;

    /** Set containing all the Conditions. */
    private final IndexedXMLObjectChildrenList<Condition> conditions;

    /**
     * Constructor.
     * 
     * @param namespaceURI the namespace the element is in
     * @param elementLocalName the local name of the XML element this Object represents
     * @param namespacePrefix the prefix for the given namespace
     */
    protected ConditionsImpl(final String namespaceURI, final String elementLocalName, final String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
        conditions = new IndexedXMLObjectChildrenList<>(this);
    }

    /** {@inheritDoc} */
    public DateTime getNotBefore() {
        return notBefore;
    }

    /** {@inheritDoc} */
    public void setNotBefore(final DateTime dt) {
        notBefore = prepareForAssignment(notBefore, dt);
    }

    /** {@inheritDoc} */
    public DateTime getNotOnOrAfter() {
        return notOnOrAfter;
    }

    /** {@inheritDoc} */
    public void setNotOnOrAfter(final DateTime dt) {
        notOnOrAfter = prepareForAssignment(notOnOrAfter, dt);
    }

    /** {@inheritDoc} */
    public List<Condition> getConditions() {
        return conditions;
    }

    /** {@inheritDoc} */
    public List<Condition> getConditions(final QName typeOrName) {
        return conditions;
    }

    /** {@inheritDoc} */
    public List<AudienceRestrictionCondition> getAudienceRestrictionConditions() {
        final QName qname = new QName(SAMLConstants.SAML1_NS, AudienceRestrictionCondition.DEFAULT_ELEMENT_LOCAL_NAME);
        return (List<AudienceRestrictionCondition>) conditions.subList(qname);
    }

    /** {@inheritDoc} */
    public List<DoNotCacheCondition> getDoNotCacheConditions() {
        final QName qname = new QName(SAMLConstants.SAML1_NS, DoNotCacheCondition.DEFAULT_ELEMENT_LOCAL_NAME);
        return (List<DoNotCacheCondition>) conditions.subList(qname);
    }

    /** {@inheritDoc} */
    public List<XMLObject> getOrderedChildren() {
        if (conditions.size() == 0) {
            return null;
        }
        final ArrayList<XMLObject> children = new ArrayList<>();
        children.addAll(conditions);
        return Collections.unmodifiableList(children);
    }
}