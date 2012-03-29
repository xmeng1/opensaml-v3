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

/**
 * 
 */
package org.opensaml.saml.saml2.core.validator;

import javax.xml.namespace.QName;

import org.opensaml.saml.common.BaseSAMLObjectValidatorTestCase;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.validator.StatusCodeSchemaValidator;

/**
 *
 */
public class StatusCodeSchemaTest extends BaseSAMLObjectValidatorTestCase {

    /**
     * Constructor
     *
     */
    public StatusCodeSchemaTest() {
        super();
        targetQName = new QName(SAMLConstants.SAML20P_NS, StatusCode.DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML20P_PREFIX);
        validator = new StatusCodeSchemaValidator();
    }

    /** {@inheritDoc} */
    protected void populateRequiredData() {
        super.populateRequiredData();
        StatusCode sc = (StatusCode) target;
        sc.setValue("urn:string:status-code");
    }
    
    public void testValueFailure() {
        StatusCode sc = (StatusCode) target;
        
        sc.setValue(null);
        assertValidationFail("Value attribute was null");
        
        sc.setValue("");
        assertValidationFail("Value attribute was empty");
        
        sc.setValue("               ");
        assertValidationFail("Value attribute was all whitespace");
    }
    
}