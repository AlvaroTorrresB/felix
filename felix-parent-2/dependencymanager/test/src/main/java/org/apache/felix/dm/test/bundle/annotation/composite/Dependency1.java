/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.test.bundle.annotation.composite;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

@Component(properties = @Property(name = "foo", value = "bar1"))
public class Dependency1 implements Runnable
{
    @ServiceDependency(filter="(name=Dependency1)")
    Sequencer m_sequencer;

    @Start
    void start()
    {
        System.out.println("Dependency1.start");
    }
    
    public void run()
    {
        m_sequencer.step(Integer.MAX_VALUE); // Makes the test fail.
    }
}
