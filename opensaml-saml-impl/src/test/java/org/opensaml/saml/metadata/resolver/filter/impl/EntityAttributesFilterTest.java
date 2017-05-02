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

package org.opensaml.saml.metadata.resolver.filter.impl;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolverTest;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;

public class EntityAttributesFilterTest extends XMLObjectBaseTestCase implements Predicate<EntityDescriptor> {
    
    private SAMLObjectBuilder<Attribute> tagBuilder;
    
    private XMLObjectBuilder<XSString> valueBuilder;

    private FilesystemMetadataResolver metadataProvider;
    
    private File mdFile;
    
    @BeforeMethod
    protected void setUp() throws Exception {

        tagBuilder = (SAMLObjectBuilder<Attribute>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<Attribute>getBuilderOrThrow(
                        Attribute.DEFAULT_ELEMENT_NAME);
        valueBuilder = XMLObjectProviderRegistrySupport.getBuilderFactory().<XSString>getBuilderOrThrow(
                XSString.TYPE_NAME);

        URL mdURL = FilesystemMetadataResolverTest.class
                .getResource("/org/opensaml/saml/saml2/metadata/InCommon-metadata.xml");
        mdFile = new File(mdURL.toURI());

        metadataProvider = new FilesystemMetadataResolver(mdFile);
        metadataProvider.setParserPool(parserPool);
    }
    
    @Test
    public void test() throws ComponentInitializationException, ResolverException {
        
        final Attribute tag = tagBuilder.buildObject();
        tag.setName("foo");
        final XSString value = valueBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        value.setValue("bar");
        tag.getAttributeValues().add(value);
        final Collection<Attribute> tags = Collections.singletonList(tag);
        
        final EntityAttributesFilter filter = new EntityAttributesFilter();
        filter.setRules(Collections.<Predicate<EntityDescriptor>,Collection<Attribute>>singletonMap(this, tags));
        filter.initialize();
        
        metadataProvider.setMetadataFilter(filter);
        metadataProvider.setId("test");
        metadataProvider.initialize();

        EntityIdCriterion key = new EntityIdCriterion("https://carmenwiki.osu.edu/shibboleth");
        EntityDescriptor entity = metadataProvider.resolveSingle(new CriteriaSet(key));
        Assert.assertNotNull(entity);
        Extensions exts = entity.getExtensions();
        Assert.assertNotNull(exts);
        Collection<XMLObject> extElements = exts.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
        Assert.assertFalse(extElements.isEmpty());
        EntityAttributes extTags = (EntityAttributes) extElements.iterator().next();
        Assert.assertNotNull(extTags);
        Assert.assertEquals(extTags.getAttributes().size(), 2);
        Assert.assertEquals(extTags.getAttributes().get(0).getName(), "http://macedir.org/entity-category");
        Assert.assertEquals(extTags.getAttributes().get(1).getName(), "foo");
        
        key = new EntityIdCriterion("https://cms.psu.edu/Shibboleth");
        entity = metadataProvider.resolveSingle(new CriteriaSet(key));
        Assert.assertNotNull(entity);
        exts = entity.getExtensions();
        Assert.assertNull(exts);
    }

    @Test
    public void testWithWhitelist() throws ComponentInitializationException, ResolverException {
        
        final Attribute tag = tagBuilder.buildObject();
        tag.setName("foo");
        final XSString value = valueBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        value.setValue("bar");
        tag.getAttributeValues().add(value);
        final Collection<Attribute> tags = Collections.singletonList(tag);
        
        final EntityAttributesFilter filter = new EntityAttributesFilter();
        filter.setRules(Collections.<Predicate<EntityDescriptor>,Collection<Attribute>>singletonMap(this, tags));
        filter.setAttributeFilter(new Predicate<Attribute>() {
            public boolean apply(Attribute input) {
                return "foo".equals(input.getName());
            }
        });
        filter.initialize();
        
        metadataProvider.setMetadataFilter(filter);
        metadataProvider.setId("test");
        metadataProvider.initialize();

        EntityIdCriterion key = new EntityIdCriterion("https://carmenwiki.osu.edu/shibboleth");
        EntityDescriptor entity = metadataProvider.resolveSingle(new CriteriaSet(key));
        Assert.assertNotNull(entity);
        Extensions exts = entity.getExtensions();
        Assert.assertNotNull(exts);
        Collection<XMLObject> extElements = exts.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
        Assert.assertFalse(extElements.isEmpty());
        EntityAttributes extTags = (EntityAttributes) extElements.iterator().next();
        Assert.assertNotNull(extTags);
        Assert.assertEquals(extTags.getAttributes().size(), 1);
        Assert.assertEquals(extTags.getAttributes().get(0).getName(), "foo");
    }

    /** {@inheritDoc} */
    public boolean apply(final EntityDescriptor input) {
        return input.getEntityID().equals("https://carmenwiki.osu.edu/shibboleth");
    }

}
