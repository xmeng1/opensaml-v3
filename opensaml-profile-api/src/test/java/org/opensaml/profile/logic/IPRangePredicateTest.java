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

package org.opensaml.profile.logic;

import java.util.Map;

import net.shibboleth.ext.spring.config.IdentifiableBeanPostProcessor;
import net.shibboleth.ext.spring.util.ApplicationContextBuilder;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
public class IPRangePredicateTest {

    @Test public void testRanges() {

       final GenericApplicationContext ctx = new ApplicationContextBuilder()
               .setName("IpRange")
               .setServiceConfiguration(new ClassPathResource("iprange.xml"))
               .setBeanPostProcessor(new IdentifiableBeanPostProcessor())
               .build();

       final Map<String, IPRangePredicate> bar = ctx.getBeansOfType(IPRangePredicate.class);
       
       Assert.assertEquals(bar.size(), 2);
    }
}
