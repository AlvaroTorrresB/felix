/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.felix.dm.test.annotation;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.test.Base;
import org.apache.felix.dm.test.BundleGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Use case: Verify Aspect Annotations usage.
 */
@RunWith(JUnit4TestRunner.class)
public class AdapterAnnotationTest extends AnnotationBase
{
    @Configuration
    public static Option[] configuration()
    {
        return options(
            systemProperty(DMLOG_PROPERTY).value( "true" ),
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version(Base.OSGI_SPEC_VERSION),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager.runtime").versionAsInProject()),
            provision(
                new BundleGenerator()
                    .set(Constants.BUNDLE_SYMBOLICNAME, "AdapterTest")
                    .set("Export-Package", "org.apache.felix.dm.test.bundle.annotation.sequencer")
                    .set("Private-Package", "org.apache.felix.dm.test.bundle.annotation.adapter")
                    .set("Import-Package", "*")
                    .set("-plugin", "org.apache.felix.dm.annotation.plugin.bnd.AnnotationPlugin")
                    .build()));           
    }

    /**
     * Check if an adapter gets injected with its adaptee using default auto config mode.
     */
    @Test
    public void testAnnotatedAdapterAutoConfig(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        // Provide the Sequencer to the org.apache.felix.dm.test.bundle.annotation.adapter.AdapterTest bundle 
        m.add(makeSequencer(m, "AdapterAutoConfig"));
        m_ensure.waitForStep(3, 10000);
    }
    
    /**
     * Check if an adapter gets injected with its adaptee in a named class field.
     */
    @Test
    public void testAnnotatedAdapterAutoConfigField(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        // Provide the Sequencer to the org.apache.felix.dm.test.bundle.annotation.adapter.AdapterTest bundle 
        m.add(makeSequencer(m, "AdapterAutoConfigField"));
        m_ensure.waitForStep(3, 10000);
    }
    
    /**
     * Check if an adapter gets injected with its adaptee in a callback method.
     */
    @Test
    public void testAnnotatedAdapterCallback(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        // Provide the Sequencer to the org.apache.felix.dm.test.bundle.annotation.adapter.AdapterTest bundle 
        Component sequencer = makeSequencer(m, "AdapterCallback");
        m.add(sequencer);
        m_ensure.waitForStep(2, 10000);
        m.remove(sequencer);
        m_ensure.waitForStep(4, 10000);        
    }
}
