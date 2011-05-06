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
package org.apache.felix.dm.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
public class AdapterWithExtraDependenciesTest {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version(Base.OSGI_SPEC_VERSION),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }    

    @Test
    public void testAdapterWithExtraDependenciesAndCallbacks(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create a service adapter that adapts to services S1 and has an optional dependency on services S2
        Component sa = m.createAdapterService(S1.class, null)
            .setImplementation(SA.class)
            .add(m.createServiceDependency().setService(S2.class).setCallbacks("add", "remove"));
        m.add(sa);
        
        // create a service S1, which triggers the creation of the first adapter instance (A1)
        Component s1 = m.createComponent().setInterface(S1.class.getName(), null).setImplementation(new S1Impl());
        m.add(s1);
        
        // create a service S2, which will be added to A1
        Component s2 = m.createComponent().setInterface(S2.class.getName(), null).setImplementation(new S2Impl(e));
        m.add(s2);
        
        // create a second service S1, which triggers the creation of the second adapter instance (A2)
        Component s1b = m.createComponent().setInterface(S1.class.getName(), null).setImplementation(new S1Impl());
        m.add(s1b);
        
        // observe that S2 is also added to A2
        e.waitForStep(2, 5000);
        
        // remove S2 again
        m.remove(s2);
        
        // make sure both adapters have their "remove" callbacks invoked
        e.waitForStep(4, 5000);
    }
    
    static interface S1 {
    }
    static interface S2 {
        public void invoke();
    }
    static class S1Impl implements S1 {
    }
    static class S2Impl implements S2 {

        private final Ensure m_e;

        public S2Impl(Ensure e) {
            m_e = e;
        }

        public void invoke() {
            m_e.step();
        }
    }
    
    public static class SA {
        volatile S2 s2;
        
        public SA() {
            System.out.println("Adapter created");
        }
        public void init() {
            System.out.println("Adapter init " + s2);
        }
        public void add(S2 s) {
            System.out.println("adding " + s);
            s.invoke();
        }
        public void remove(S2 s) {
            System.out.println("removing " + s);
            s.invoke();
        }
    }
}
