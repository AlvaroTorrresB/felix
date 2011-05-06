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
package org.apache.felix.dm.test.bundle.annotation.aspect;

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.osgi.framework.ServiceRegistration;

public class AspectChainTest
{
    public interface ServiceInterface
    {
        public void invoke(Runnable run);
    }

    @Component
    public static class ServiceProvider implements ServiceInterface
    {
        @ServiceDependency(filter="(name=AspectChainTest.ServiceProvider)")
        protected Sequencer m_sequencer;
        // Injected by reflection.
        protected ServiceRegistration m_sr;
               
        @Init
        void init() {
            System.out.println("ServiceProvider.init");
        }
        
        @Destroy
        void destroy() {
            System.out.println("ServiceProvider.destroy");
        }

        public void invoke(Runnable run)
        {
            run.run();
            m_sequencer.step(6);
        }
    }
    
    @AspectService(ranking = 20)
    public static class ServiceAspect2 implements ServiceInterface
    {
        @ServiceDependency(filter="(name=AspectChainTest.ServiceAspect2)")
        protected Sequencer m_sequencer;
        // Injected by reflection.
        private volatile ServiceInterface m_parentService;

        @Init
        void init() {
            System.out.println("ServiceAspect2.init");
        }
        
        @Destroy
        void destroy() {
            System.out.println("ServiceAspect2.destroy");
        }
        
        public void invoke(Runnable run)
        {
            m_sequencer.step(3);
            m_parentService.invoke(run);
        }
    }

    @AspectService(ranking = 30, added="add")
    public static class ServiceAspect3 implements ServiceInterface
    {
        @ServiceDependency(filter="(name=AspectChainTest.ServiceAspect3)")
        protected Sequencer m_sequencer;
        // Injected using add callback.
        private volatile ServiceInterface m_parentService;

        @Init
        void init() {
            System.out.println("ServiceAspect3.init");
        }
        
        @Destroy
        void destroy() {
            System.out.println("ServiceAspect3.destroy");
        }

        void add(ServiceInterface si)
        {
            m_parentService = si;
        }
        
        public void invoke(Runnable run)
        {
            m_sequencer.step(2);
            m_parentService.invoke(run);
        }
    }

    @AspectService(ranking = 10, added="added", removed="removed")
    public static class ServiceAspect1 implements ServiceInterface
    {
        @ServiceDependency(filter="(name=AspectChainTest.ServiceAspect1)")
        protected Sequencer m_sequencer;
        // Injected by reflection.
        private volatile ServiceInterface m_parentService;

        @Init
        void init() {
            System.out.println("ServiceAspect1.init");
        }
        
        @Destroy
        void destroy() {
            System.out.println("ServiceAspect1.destroy");
        }

        void added(ServiceInterface si)
        {
            m_parentService = si;
        }
                
        @Stop
        void stop()
        {
            m_sequencer.step(7);
        }
        
        void removed(ServiceInterface si)
        {
            m_sequencer.step(8);
        }
        
        public void invoke(Runnable run)
        {
            m_sequencer.step(4);
            m_parentService.invoke(run);
        }
    }

    @Component
    public static class ServiceConsumer implements Runnable
    {
        @ServiceDependency(filter = "(name=AspectChainTest.ServiceConsumer)")
        protected Sequencer m_sequencer;

        @ServiceDependency
        private volatile ServiceInterface m_service;

        private Thread m_thread;

        @Init
        public void init()
        {
            m_thread = new Thread(this, "ServiceConsumer");
            m_thread.start();
        }

        public void run()
        {
            m_sequencer.waitForStep(1, 2000);
            m_service.invoke(new Runnable()
            {
                public void run()
                {
                    m_sequencer.step(5);
                }
            });
        }
        
        @Destroy
        void destroy()
        {
            m_thread.interrupt();
            try
            {
                m_thread.join();
            }
            catch (InterruptedException e)
            {
            }
        }
    }
}
