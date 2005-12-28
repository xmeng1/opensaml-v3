/*
 * Copyright [2005] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package org.opensaml.saml1.core.impl;

import java.util.GregorianCalendar;

import org.opensaml.common.IllegalAddException;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.impl.AbstractSignableSAMLObject;
import org.opensaml.common.util.OrderedSet;
import org.opensaml.common.util.StringHelper;
import org.opensaml.common.util.UnmodifiableOrderedSet;
import org.opensaml.common.util.xml.XMLHelper;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml1.core.Response;
import org.opensaml.saml1.core.Status;

/**
 * Implementation of the {@link org.opensaml.saml1.core.Response} Object
 */
public class ResponseImpl extends AbstractSignableSAMLObject implements Response {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 6224869049066163659L;

    /** Contents of the InResponseTo attribute */

    private String inResponseTo = null;

    /** Minor Version of this element */

    private int minorVersion = 0;

    /** Contents of the Date attribute */

    private GregorianCalendar issueInstant = null;

    /** Contents of the recipient attribute */

    private String recipient = null;

    /** Status associated with this element */

    private Status status = null;

    /** Assertion associated with this element */

    private Assertion assertion = null;

    /**
     * Constructor
     * 
     */
    protected ResponseImpl() {
        super();
        setQName(Response.QNAME);
    }

    /*
     * @see org.opensaml.saml1.core.Response#getInResponseTo()
     */
    public String getInResponseTo() {

        return inResponseTo;
    }

    /*
     * @see org.opensaml.saml1.core.Response#setInResponseTo(java.lang.String)
     */
    public void setInResponseTo(String inResponseTo) {

        this.inResponseTo = prepareForAssignment(this.inResponseTo, inResponseTo);
    }

    /*
     * @see org.opensaml.saml1.core.Response#getMinorVersion()
     */
    public int getMinorVersion() {

        return minorVersion;
    }

    /*
     * @see org.opensaml.saml1.core.Response#setMinorVersion(int)
     */
    public void setMinorVersion(int version) {

        if (version != minorVersion) {
            releaseThisandParentDOM();
            minorVersion = version;
        }
    }

    /*
     * @see org.opensaml.saml1.core.Response#getIssueInstant()
     */
    public GregorianCalendar getIssueInstant() {

        return issueInstant;
    }

    /*
     * @see org.opensaml.saml1.core.Response#setIssueInstant(java.util.Date)
     */
    public void setIssueInstant(GregorianCalendar date) {

        if (issueInstant == null && date == null) {
            // no change - return
            return;
        }

        if (issueInstant == null || !issueInstant.equals(date)) {
            releaseThisandParentDOM();
            issueInstant = date;
        }
    }

    /*
     * @see org.opensaml.saml1.core.Response#getRecipient()
     */
    public String getRecipient() {

        return recipient;
    }

    /*
     * @see org.opensaml.saml1.core.Response#setRecipient(java.lang.String)
     */
    public void setRecipient(String recipient) {

        this.recipient = prepareForAssignment(this.recipient, recipient);
    }

    /*
     * @see org.opensaml.saml1.core.Response#getStatus()
     */
    public Status getStatus() {

        return status;
    }

    /*
     * @see org.opensaml.saml1.core.Response#getStatus(org.opensaml.saml1.core.Status)
     */
    public void setStatus(Status status) throws IllegalAddException {

        this.status = prepareForAssignment(this.status, status);
    }

    /*
     * @see org.opensaml.saml1.core.Response#getAssertions()
     */
    public Assertion getAssertion() {

        return assertion;
    }

    /*
     * @see org.opensaml.saml1.core.Response#addAssertion(org.opensaml.saml1.core.Assertion)
     */
    public void setAssertion(Assertion assertion) throws IllegalAddException {
        this.assertion = prepareForAssignment(this.assertion, assertion);
    }

    /*
     * @see org.opensaml.saml1.core.Response#removeAssertion(org.opensaml.saml1.core.Assertion)
     */

    public boolean equals(SAMLObject element) {

        if (!(element instanceof ResponseImpl)) {
            
            return false;
        }            
        Response other = (ResponseImpl) element;

        if (minorVersion != other.getMinorVersion()) {
            return false;
        }
        
        if (!StringHelper.safeEquals(inResponseTo, other.getInResponseTo())) {
            return false;
        }
        
        if (!StringHelper.safeEquals(recipient, other.getRecipient())) {
            return false;
        }
        
        String myIssueInstant = null;
        if (issueInstant != null) {
            myIssueInstant = XMLHelper.calendarToString(issueInstant);
        }
        
        String otherIssueInsant = null;
        if (other.getIssueInstant() != null) {
            otherIssueInsant = XMLHelper.calendarToString(other.getIssueInstant());
        }
        
        if (!StringHelper.safeEquals(myIssueInstant, otherIssueInsant)) {
            return false;
        }

        if (status == null) {
            if (other.getStatus() != null) {
                return false;
            }
        } else if (!status.equals(other.getStatus())) {
            return false;
        }

        //
        // Everything is the same - it remains to test the Assertion
        //
        
        if (assertion == null) {
            return other.getAssertion() == null;
        }
        
        return assertion.equals(other.getAssertion());
    }

    public UnmodifiableOrderedSet<SAMLObject> getOrderedChildren() {

        OrderedSet<SAMLObject> set = new OrderedSet<SAMLObject>(2);

        if (assertion != null) {
            set.add(assertion);
        }

        if (status != null) {
            set.add(status);
        }

        return new UnmodifiableOrderedSet<SAMLObject>(set);
    }
}
