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

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.common.AbstractSAMLObjectUnmarshaller;
import org.opensaml.saml.saml1.core.Condition;
import org.opensaml.saml.saml1.core.Conditions;
import org.w3c.dom.Attr;

import com.google.common.base.Strings;

/**
 * A thread-safe Unmarshaller for {@link org.opensaml.saml.saml1.core.Conditions} objects.
 */
public class ConditionsUnmarshaller extends AbstractSAMLObjectUnmarshaller {

    /** {@inheritDoc} */
    protected void processChildElement(XMLObject parentSAMLObject, XMLObject childSAMLObject)
            throws UnmarshallingException {
        final Conditions conditions = (Conditions) parentSAMLObject;

        if (childSAMLObject instanceof Condition) {
            conditions.getConditions().add((Condition) childSAMLObject);
        } else {
            super.processChildElement(parentSAMLObject, childSAMLObject);
        }
    }

    /** {@inheritDoc} */
    protected void processAttribute(XMLObject samlObject, Attr attribute) throws UnmarshallingException {

        final Conditions conditions = (Conditions) samlObject;

        if (attribute.getNamespaceURI() == null) {
            if (Conditions.NOTBEFORE_ATTRIB_NAME.equals(attribute.getLocalName())
                    && !Strings.isNullOrEmpty(attribute.getValue())) {
                conditions.setNotBefore(new DateTime(attribute.getValue(), ISOChronology.getInstanceUTC()));
            } else if (Conditions.NOTONORAFTER_ATTRIB_NAME.equals(attribute.getLocalName())
                    && !Strings.isNullOrEmpty(attribute.getValue())) {
                conditions.setNotOnOrAfter(new DateTime(attribute.getValue(), ISOChronology.getInstanceUTC()));
            } else {
                super.processAttribute(samlObject, attribute);
            }
        } else {
            super.processAttribute(samlObject, attribute);
        }
    }
}