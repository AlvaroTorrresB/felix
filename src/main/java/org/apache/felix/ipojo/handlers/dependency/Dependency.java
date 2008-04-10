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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.MethodInterceptor;
import org.apache.felix.ipojo.Nullable;
import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.handlers.dependency.ServiceUsage.Usage;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
 * Represent a service dependency of the component instance.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Dependency extends DependencyModel implements FieldInterceptor, MethodInterceptor {

    /**
     * Reference on the Dependency Handler.
     */
    private DependencyHandler m_handler;

    /**
     * Field of the dependency.
     */
    private String m_field;

    /**
     * List of dependency callback.
     */
    private DependencyCallback[] m_callbacks;

    /**
     * Is the dependency a service level dependency.
     */
    private boolean m_isServiceLevelRequirement;

    /**
     * Is the provider set frozen ?
     */
    private boolean m_isFrozen;

    /**
     * Is the dependency started ?
     */
    private boolean m_isStarted;

    /**
     * Thread Local.
     */
    private ServiceUsage m_usage;

    /**
     * Nullable object.
     */
    private Object m_nullable;

    /**
     * Default-Implementation.
     */
    private String m_di;
    
    /**
     * Is the Nullable pattern enable?
     */
    private boolean m_supportNullable;

    /**
     * Id of the dependency.
     */
    private String m_id;

    /**
     * Dependency constructor. After the creation the dependency is not started.
     * 
     * @param handler : the dependency handler managing this dependency
     * @param field : field of the dependency
     * @param spec : required specification
     * @param filter : LDAP filter of the dependency
     * @param isOptional : is the dependency an optional dependency ?
     * @param isAggregate : is the dependency an aggregate dependency
     * @param nullable : describe if the nullable ability is enable or disable
     * @param identity : id of the dependency, may be null
     * @param context : bundle context (or service context) to use.
     * @param policy : resolution policy
     * @param cmp : comparator to sort references
     * @param defaultImplem : default-implementation class
     */
    public Dependency(DependencyHandler handler, String field, Class spec, Filter filter, boolean isOptional, boolean isAggregate, boolean nullable, String identity, BundleContext context, int policy, Comparator cmp, String defaultImplem) {
        super(spec, isAggregate, isOptional, filter, cmp, policy, context, handler);
        m_handler = handler;
        if (field != null) {
            m_field = field;
            m_usage = new ServiceUsage();
        }
        
        m_supportNullable = nullable;
        m_di = defaultImplem;

        if (identity == null) {
            if (spec != null) {
                m_id = spec.getName();
            }
        } else {
            m_id = identity;
        } 
        // Else wait the setSpecification call.
    }

    /**
     * Set the specification of the current dependency.
     * In order to store the id of the dependency, this
     * method is override.
     * @param spec : request service Class
     * @see org.apache.felix.ipojo.util.DependencyModel#setSpecification(java.lang.Class)
     */
    public void setSpecification(Class spec) {
        super.setSpecification(spec);
        if (m_id == null) {
            m_id = spec.getName();
        }
    }

    public String getField() {
        return m_field;
    }

    /**
     * Add a callback to the dependency.
     * @param callback : callback to add
     */
    protected void addDependencyCallback(DependencyCallback callback) {
        if (m_callbacks == null) {
            m_callbacks = new DependencyCallback[] { callback };
        } else {
            DependencyCallback[] newCallbacks = new DependencyCallback[m_callbacks.length + 1];
            System.arraycopy(m_callbacks, 0, newCallbacks, 0, m_callbacks.length);
            newCallbacks[m_callbacks.length] = callback;
            m_callbacks = newCallbacks;
        }
    }

    /**
     * Stop the current dependency.
     * @see org.apache.felix.ipojo.util.DependencyModel#stop()
     */
    public void stop() {
        m_isStarted = false;
        super.stop();
    }

    /**
     * Get the string form of the filter.
     * @return : the string form of the filter.
     */
    public String getStringFilter() {
        return getFilter().toString();
    }

    public DependencyHandler getHandler() {
        return m_handler;
    }

    public synchronized boolean isFrozen() {
        return m_isFrozen;
    }

    /**
     * Call the bind method.
     * @param pojo : pojo instance on which calling the bind method.
     */
    protected synchronized void onObjectCreation(Object pojo) {
        if (!m_isStarted) { return; }

        // We are notified of an instance creation, we have to freeze when the static policy is used
        if (getBindingPolicy() == STATIC_BINDING_POLICY) {
            m_isFrozen = true;
        }

        // Check optional case : nullable object case : do not call bind on nullable object
        if (isOptional() && getSize() == 0) { return; }

        // Call bind callback.
        for (int j = 0; m_callbacks != null && j < m_callbacks.length; j++) {
            if (m_callbacks[j].getMethodType() == DependencyCallback.BIND) {
                if (isAggregate()) {
                    ServiceReference[] refs = getServiceReferences();
                    for (int i = 0; i < refs.length; i++) {
                        invokeCallback(m_callbacks[j], refs[i], pojo);
                    }
                } else {
                    ServiceReference ref = getServiceReference();
                    if (ref != null) {
                        invokeCallback(m_callbacks[j], ref, pojo);
                    }
                }
            }
        }
    }

    /**
     * Call unbind callback method.
     * @param ref : reference to send (if accepted) to the method
     */
    private void callUnbindMethod(ServiceReference ref) {
        if (m_handler.getInstanceManager().getState() > InstanceManager.STOPPED && m_handler.getInstanceManager().getPojoObjects() != null) {
            for (int i = 0; m_callbacks != null && i < m_callbacks.length; i++) {
                if (m_callbacks[i].getMethodType() == DependencyCallback.UNBIND) {
                    invokeCallback(m_callbacks[i], ref, null); // Call on each created pojo objects.
                }
            }
        }
    }

    /**
     * Helper method calling the given callback.
     * @param callback : callback to call.
     * @param ref : service reference.
     * @param pojo : pojo on which calling the callback, if null call on each created pojo objects.
     */
    private void invokeCallback(DependencyCallback callback, ServiceReference ref, Object pojo) {
        try {
            if (pojo == null) {
                callback.call(ref, getService(ref));
            } else {
                callback.callOnInstance(pojo, ref, getService(ref));
            }
        } catch (NoSuchMethodException e) {
            m_handler.error("The method " + callback.getMethodName() + " does not exist in the class " + m_handler.getInstanceManager().getClassName());
            m_handler.getInstanceManager().stop();
        } catch (IllegalAccessException e) {
            m_handler.error("The method " + callback.getMethodName() + " is not accessible in the class " + m_handler.getInstanceManager().getClassName());
            m_handler.getInstanceManager().stop();
        } catch (InvocationTargetException e) {
            m_handler.error("The method " + callback.getMethodName() + " in the class " + m_handler.getInstanceManager().getClassName() + " throws an exception : " + e.getTargetException().getMessage(), e.getTargetException());
            m_handler.getInstanceManager().stop();
        }

    }

    /**
     * Call bind method with the service reference in parameter (if accepted).
     * @param ref : the service reference of the new service
     */
    private void callBindMethod(ServiceReference ref) {
        // call bind method :
        // if (m_handler.getInstanceManager().getState() == InstanceManager.VALID) {
        if (m_handler.getInstanceManager().getState() > InstanceManager.STOPPED && m_handler.getInstanceManager().getPojoObjects() != null) {
            for (int i = 0; m_callbacks != null && i < m_callbacks.length; i++) {
                if (m_callbacks[i].getMethodType() == DependencyCallback.BIND) {
                    invokeCallback(m_callbacks[i], ref, null);
                }
            }
        }
    }
    
    /**
     * Start the dependency.
     */
    public void start() {
        if (isOptional() && !isAggregate()) {
            if (m_di == null) {
                // If nullable are supported, create the nullable object.
                if (m_supportNullable) {
                    // To load the proxy we use the POJO class loader. Indeed, this classloader imports iPOJO (so can access to Nullable) and has
                    // access to the service specification.
                    m_nullable =
                            Proxy.newProxyInstance(getHandler().getInstanceManager().getClazz().getClassLoader(), new Class[] {
                                    getSpecification(), Nullable.class }, new NullableObject()); // NOPMD
                }
            } else {
                // Create the default-implementation object.
                try {
                    Class clazz = getHandler().getInstanceManager().getContext().getBundle().loadClass(m_di);
                    m_nullable = clazz.newInstance();
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot load the default-implementation " + m_di + " : " + e.getMessage());
                } catch (InstantiationException e) {
                    throw new IllegalStateException("Cannot load the default-implementation " + m_di + " : " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot load the default-implementation " + m_di + " : " + e.getMessage());
                }
            }
        }

        super.start();

        if (getBindingPolicy() == STATIC_BINDING_POLICY && m_handler.getInstanceManager().getPojoObjects() != null) {
            m_isFrozen = true;
        }

        m_isStarted = true;
    }

    protected DependencyCallback[] getCallbacks() {
        return m_callbacks;
    }

    /**
     * Set that this dependency is a service level dependency.
     * This forces the scoping policy to be STRICT. 
     */
    public void setServiceLevelDependency() {
        m_isServiceLevelRequirement = true;
        setBundleContext(new PolicyServiceContext(m_handler.getInstanceManager().getGlobalContext(), m_handler.getInstanceManager().getLocalServiceContext(), PolicyServiceContext.LOCAL));
    }

    public String getId() {
        return m_id;
    }

    public boolean isServiceLevelRequirement() {
        return m_isServiceLevelRequirement;
    }


    /**
     * A new service has to be injected.
     * @param reference : the new matching service reference.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceArrival(org.osgi.framework.ServiceReference)
     */
    public void onServiceArrival(ServiceReference reference) {
        callBindMethod(reference);
        //The method is only called when a new service arrives, or when the used one is replaced.
    }

    /**
     * A used (already injected) service disappears.
     * @param ref : leaving service reference.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceDeparture(org.osgi.framework.ServiceReference)
     */
    public void onServiceDeparture(ServiceReference ref) {
        callUnbindMethod(ref);
    }

    /**
     * The dependency has been reconfigured.
     * @param departs : service no more matching.
     * @param arrivals : new services
     * @see org.apache.felix.ipojo.util.DependencyModel#onDependencyReconfiguration(org.osgi.framework.ServiceReference[], org.osgi.framework.ServiceReference[])
     */
    public void onDependencyReconfiguration(ServiceReference[] departs, ServiceReference[] arrivals) {
        throw new UnsupportedOperationException("Dependency set change is not yet supported");
    }

    /**
     * Get the used service references list.
     * @return the used service reference or null if no service reference are available.
     */
    public List getServiceReferencesAsList() {
        ServiceReference[] refs = super.getServiceReferences();
        if (refs == null) {
            return null;
        } else {
            return Arrays.asList(refs);
        }
    }

    /**
     * This method is called by the replaced code in the component
     * implementation class. Construct the service object list is necessary.
     * @param pojo : POJO object.
     * @param fieldName : field
     * @param value : last value.
     * @return the service object or a nullable / default implementation if defined.
     * @see org.apache.felix.ipojo.FieldInterceptor#onGet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public Object onGet(Object pojo, String fieldName, Object value) {
     // Initialize the thread local object is not already touched.
        Usage usage = (Usage) m_usage.get();
        if (usage.m_stack == 0) { // uninitialized usage.
            ServiceReference[] refs = super.getServiceReferences();
            if (isAggregate()) { // If we are aggregate we get the objects.
                if (refs == null) {
                    usage.m_objects = (Object[]) Array.newInstance(getSpecification(), 0); // Create an empty array.
                } else {
                   // Use a reflective construction to avoid class cast exception. This method allow to set the component type.
                    usage.m_objects = (Object[]) Array.newInstance(getSpecification(), refs.length); 
                    for (int i = 0; refs != null && i < refs.length; i++) {
                        ServiceReference ref = refs[i];
                        usage.m_objects[i] = getService(ref);
                    }
                }
            } else { // We are not aggregate.
                // Use a reflective construction to avoid class cast exception. This method allow to set the component type.
                usage.m_objects = (Object[]) Array.newInstance(getSpecification(), 1);
                if (refs == null) {
                    if (m_nullable == null && m_supportNullable) {
                        m_handler.warn("[" + m_handler.getInstanceManager().getInstanceName() + "] The dependency is not optional, however no service object can be injected in " + m_field + " -> " + getSpecification().getName());
                    }
                    usage.m_objects[0] = m_nullable; // Add null if the Nullable pattern is disable.
                } else {
                    ServiceReference ref = getServiceReference();
                    usage.m_objects[0] = getService(ref);
                }
            }
            usage.inc(); // Start the tracking, so fix the stack level to 1
            m_usage.set(usage);
        }

        if (isAggregate()) { // Multiple dependency
            return usage.m_objects;
        } else {
            return usage.m_objects[0];
        }
    }

    /**
     * The field was set.
     * This method should not be call if the POJO is written correctly.
     * @param pojo : POJO object
     * @param fieldName : field name
     * @param value : set value.
     * @see org.apache.felix.ipojo.FieldInterceptor#onSet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public void onSet(Object pojo, String fieldName, Object value) {
        // Nothing to do.
    }

    /**
     * A POJO method will be invoked.
     * @param pojo : Pojo object
     * @param method : called method
     * @param args : arguments
     * @see org.apache.felix.ipojo.MethodInterceptor#onEntry(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public void onEntry(Object pojo, Method method, Object[] args) {
        if (m_usage != null) {
            Usage usage = (Usage) m_usage.get();
            if (usage.m_stack > 0) {
                usage.inc();
                m_usage.set(usage); // Set the Thread local as value has been modified
            }
        }
    }

    /**
     * A POJO method has thrown an error.
     * This method does nothing and wait for the finally.
     * @param pojo : POJO object.
     * @param method : Method object.
     * @param throwable : thrown error
     * @see org.apache.felix.ipojo.MethodInterceptor#onError(java.lang.Object, java.lang.reflect.Method, java.lang.Throwable)
     */
    public void onError(Object pojo, Method method, Throwable throwable) {
        // Nothing to do  : wait onFinally
    }

    /**
     * A POJO method has returned.
     * @param pojo : POJO object.
     * @param method : Method object.
     * @param returnedObj : returned object (null for void method)
     * @see org.apache.felix.ipojo.MethodInterceptor#onExit(java.lang.Object, java.lang.reflect.Method, java.lang.Object)
     */
    public void onExit(Object pojo, Method method, Object returnedObj) {
        // Nothing to do  : wait onFinally
        
    }

    /**
     * A POJO method is finished.
     * @param pojo : POJO object.
     * @param method : Method object.
     * @see org.apache.felix.ipojo.MethodInterceptor#onFinally(java.lang.Object, java.lang.reflect.Method)
     */
    public void onFinally(Object pojo, Method method) {
        if (m_usage != null) {
            Usage usage = (Usage) m_usage.get();
            if (usage.m_stack > 0) {
                if (usage.dec()) {
                    // Exit the method flow => Release all objects
                    usage.clear();
                    m_usage.set(usage); // Set the Thread local as value has been modified
                }
            }
        }
    }

}
