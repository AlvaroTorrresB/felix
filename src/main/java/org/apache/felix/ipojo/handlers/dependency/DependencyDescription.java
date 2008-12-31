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
package org.apache.felix.ipojo.handlers.dependency;

import java.util.List;

import org.osgi.framework.ServiceReference;

/**
 * Service Dependency Description.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyDescription {
    /**
     * The described dependency.
     */
    private Dependency m_dependency;

    /**
     * Creates a dependency description.
     * @param dep the described dependency
     */
    public DependencyDescription(Dependency dep) {
        m_dependency = dep;
    }

    public boolean isMultiple() { return m_dependency.isAggregate(); }

    public boolean isOptional() { return m_dependency.isOptional(); }

    public String getFilter() { return m_dependency.getFilter(); }

    public String getInterface() { return m_dependency.getSpecification().getName(); }

    public int getState() { return m_dependency.getState(); }
    
    public String getId() { return m_dependency.getId(); }
    
    /**
     * Gets <code>true</code> if the dependency uses Nullable objects.
     * @return true if the dependency is optional and supports nullable object.
     */
    public boolean supportsNullable() { return m_dependency.supportsNullable(); }
    
    public String getDefaultImplementation() { return m_dependency.getDefaultImplementation(); }
    
    public int getPolicy() { return m_dependency.getBindingPolicy(); }
    
    public String getComparator() { return m_dependency.getComparator(); }
    
    public boolean isFrozen() { return m_dependency.isFrozen(); }

    /**
     * Gets the service reference list.
     * @return the list of matching service reference,
     * null if no service reference.
     */
    public List getServiceReferences() { return m_dependency.getServiceReferencesAsList(); }

    /**
     * Gets the service reference if only one service reference is used.
     * @return the ServiceReference (only if the cardinality could be 1),
     * or null if no service reference.
     */
    public ServiceReference getServiceReference() { 
        List list = getServiceReferences();
        if (list == null) {
            return null;
        } else {
            return (ServiceReference) list.get(0);
        }
    }

    /**
     * Gets the used service set.
     * @return the list [service reference] containing the used services,
     * null if no providers are used
     */
    public List getUsedServices() { return m_dependency.getUsedServiceReferences(); }

}
