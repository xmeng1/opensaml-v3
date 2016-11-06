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

package org.opensaml.core.metrics;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

/**
 *
 */
public class MetricsSupportTest {
    
    private MetricRegistry registry;
    
    private Gauge<Integer> gauge1, gauge2;
    
    @BeforeClass
    public void setUpGauges() {
        gauge1 = new Gauge<Integer>() {
            public Integer getValue() {
                return 42;
            }};
        gauge2 = new Gauge<Integer>() {
            public Integer getValue() {
                return 100;
            }};
    }
    
    @BeforeMethod
    public void setUp() {
        registry = new MetricRegistry();
    }
    
    @Test
    public void testRegister() {
        String name = "test1";
        Assert.assertNull(registry.getMetrics().get(name));
        
        MetricsSupport.register(name, gauge1, false, registry);
        Assert.assertNotNull(registry.getMetrics().get(name));
        Assert.assertSame(registry.getMetrics().get(name), gauge1);
        
        try {
            MetricsSupport.register(name, gauge2, false, registry);
            Assert.fail("Should have failed due to duplicate registration under existing name");
        } catch (IllegalArgumentException e) {
            //expected
        }
        Assert.assertSame(registry.getMetrics().get(name), gauge1);
        
        MetricsSupport.register(name, gauge2, true, registry);
        Assert.assertNotNull(registry.getMetrics().get(name));
        Assert.assertSame(registry.getMetrics().get(name), gauge2);
    }
    
    @Test
    public void testRemoveSameInstance() {
        String name = "test1";
        Assert.assertNull(registry.getMetrics().get(name));
        registry.register(name, gauge1);
        Assert.assertNotNull(registry.getMetrics().get(name));
        
        Assert.assertTrue(MetricsSupport.remove(name, gauge1, registry));
        Assert.assertNull(registry.getMetrics().get(name));
    }
    
    @Test
    public void testRemoveDifferingInstance() {
        String name = "test1";
        Assert.assertNull(registry.getMetrics().get(name));
        registry.register(name, gauge1);
        Assert.assertNotNull(registry.getMetrics().get(name));
        
        Assert.assertFalse(MetricsSupport.remove(name, gauge2, registry));
        Assert.assertNotNull(registry.getMetrics().get(name));
        Assert.assertSame(registry.getMetrics().get(name), gauge1);
    }
    
    @Test
    public void testIsInstanceRegisteredUnderName() {
        String name = "test1";
        Assert.assertNull(registry.getMetrics().get(name));
        registry.register(name, gauge1);
        Assert.assertNotNull(registry.getMetrics().get(name));
        
        Assert.assertTrue(MetricsSupport.isMetricInstanceRegisteredUnderName(name, gauge1, registry));
        Assert.assertFalse(MetricsSupport.isMetricInstanceRegisteredUnderName("testXXX", gauge1, registry));
        Assert.assertFalse(MetricsSupport.isMetricInstanceRegisteredUnderName(name, gauge2, registry));
    }

}
