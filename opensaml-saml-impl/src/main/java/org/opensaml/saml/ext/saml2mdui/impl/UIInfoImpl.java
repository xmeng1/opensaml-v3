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

package org.opensaml.saml.ext.saml2mdui.impl;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.IndexedXMLObjectChildrenList;
import org.opensaml.saml.common.AbstractSAMLObject;
import org.opensaml.saml.ext.saml2mdui.Description;
import org.opensaml.saml.ext.saml2mdui.DisplayName;
import org.opensaml.saml.ext.saml2mdui.InformationURL;
import org.opensaml.saml.ext.saml2mdui.Keywords;
import org.opensaml.saml.ext.saml2mdui.Logo;
import org.opensaml.saml.ext.saml2mdui.PrivacyStatementURL;
import org.opensaml.saml.ext.saml2mdui.UIInfo;

/**
 * Concrete implementation of {@link org.opensaml.saml.ext.saml2mdui.UIInfo}.
 * @author Rod Widdowson
 */
public class UIInfoImpl extends AbstractSAMLObject implements UIInfo {
    
    /** Children of the UIInfo. */
    private final IndexedXMLObjectChildrenList<XMLObject> uiInfoChildren;
    
    /**
     * Constructor.
     * @param namespaceURI namespaceURI
     * @param elementLocalName elementLocalName
     * @param namespacePrefix namespacePrefix
     */
    protected UIInfoImpl(String namespaceURI, String elementLocalName, String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
        
        uiInfoChildren = new IndexedXMLObjectChildrenList<>(this);
    }
    
    /** {@inheritDoc} */
    @Override
    public List<XMLObject> getXMLObjects() {
        return uiInfoChildren;
    }

    /** {@inheritDoc} */
    @Override
    public List<XMLObject> getXMLObjects(QName typeOrName) {
        return (List<XMLObject>) uiInfoChildren.subList(typeOrName);
    }

    /** {@inheritDoc} */
    @Override
    public List<Description> getDescriptions() {
        return (List<Description>) uiInfoChildren.subList(Description.DEFAULT_ELEMENT_NAME);
    }

    /** {@inheritDoc} */
    @Override
    public List<DisplayName> getDisplayNames() {
        return (List<DisplayName>) uiInfoChildren.subList(DisplayName.DEFAULT_ELEMENT_NAME);
    }

    /** {@inheritDoc} */
    @Override
    public List<Keywords> getKeywords() {
        return (List<Keywords>) uiInfoChildren.subList(Keywords.DEFAULT_ELEMENT_NAME);
    }

    /** {@inheritDoc} */
    @Override
    public List<InformationURL> getInformationURLs() {
        return (List<InformationURL>) uiInfoChildren.subList(InformationURL.DEFAULT_ELEMENT_NAME);
    }

    /** {@inheritDoc} */
    @Override
    public List<Logo> getLogos() {
        return (List<Logo>) uiInfoChildren.subList(Logo.DEFAULT_ELEMENT_NAME);
    }

    /** {@inheritDoc} */
    @Override
    public List<PrivacyStatementURL> getPrivacyStatementURLs() {
        return (List<PrivacyStatementURL>) uiInfoChildren.subList(PrivacyStatementURL.DEFAULT_ELEMENT_NAME);
    }

    /** {@inheritDoc} */
    @Override
    public List<XMLObject> getOrderedChildren() {
        ArrayList<XMLObject> children = new ArrayList<>();
        
        children.addAll(uiInfoChildren);
        return children;
    }

}
