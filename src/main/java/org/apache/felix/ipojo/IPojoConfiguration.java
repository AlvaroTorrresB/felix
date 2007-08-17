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
package org.apache.felix.ipojo;

import org.apache.felix.ipojo.composite.instance.InstanceHandler;
import org.apache.felix.ipojo.composite.service.importer.ImportExportHandler;
import org.apache.felix.ipojo.composite.service.instantiator.ServiceInstantiatorHandler;
import org.apache.felix.ipojo.handlers.architecture.ArchitectureHandler;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandler;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandler;
import org.apache.felix.ipojo.handlers.lifecycle.callback.LifecycleCallbackHandler;
import org.apache.felix.ipojo.handlers.lifecycle.controller.ControllerHandler;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler;
import org.apache.felix.ipojo.util.Logger;

/**
 * iPOJO Configuration : Default Log Level - Available (core) handlers.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class IPojoConfiguration {

    /**
     * iPOJO logger log level.
     */
    public static final int LOG_LEVEL = Logger.WARNING;

    /**
     * Available handlers in the iPOJO bundle.
     */
    public static final Class[] INTERNAL_HANDLERS = new Class[] { 
        DependencyHandler.class, 
        ProvidedServiceHandler.class, 
        ConfigurationHandler.class,
        LifecycleCallbackHandler.class,
        ControllerHandler.class,
        ArchitectureHandler.class
    };

    /**
     * Available composite handlers in the iPOJO bundle.
     */
    public static final Class[] INTERNAL_COMPOSITE_HANDLERS = new Class[] { 
        ServiceInstantiatorHandler.class, 
        ImportExportHandler.class,
        InstanceHandler.class,
        org.apache.felix.ipojo.composite.service.provides.ProvidedServiceHandler.class,
        org.apache.felix.ipojo.composite.architecture.ArchitectureHandler.class
    };

}
