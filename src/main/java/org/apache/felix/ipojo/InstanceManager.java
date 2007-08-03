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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;

/**
 * The instance manager class manages one instance of a component type. It
 * manages component lifecycle, component instance creation and handlers.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceManager implements ComponentInstance {

    /**
     * Parent factory (ComponentFactory).
     */
    private ComponentFactory m_factory;

    /**
     * Name of the component instance.
     */
    private String m_name;

    /**
     * Name of the component type implementation class.
     */
    private String m_className;

    /**
     * The context of the component.
     */
    private BundleContext m_context;

    /**
     * Handler list.
     */
    private Handler[] m_handlers = new Handler[0];

    /**
     * Map [field, handler list] storing handlers interested by the field.
     */
    private Map m_fieldRegistration = new HashMap();
    
    /**
     * Map [method identifier, handler list] storing handlers interested by the method.
     */
    private Map m_methodRegistration = new HashMap();

    /**
     * Component state (STOPPED at the beginning).
     */
    private int m_state = STOPPED;

    /**
     * Manipulated class.
     */
    private Class m_clazz;

    /**
     * Instances of the components.
     */
    private Object[] m_pojoObjects = new Object[0];
    
    /**
     * Instance State Listener List.
     */
    private InstanceStateListener[] m_instanceListeners = new InstanceStateListener[0];

    /**
     * Component type information.
     */
    private ComponentDescription m_componentDesc;
    

    // Constructor
    /**
     * Construct a new Component Manager.
     * 
     * @param factory : the factory managing the instance manager
     * @param bc : the bundle context to give to the instance
     */
    public InstanceManager(ComponentFactory factory, BundleContext bc) {
        m_factory = factory;
        m_context = bc;
        m_factory.getLogger().log(Logger.INFO, "[Bundle " + m_context.getBundle().getBundleId() + "] Create an instance manager from the factory " + m_factory);
    }

    /**
     * Configure the instance manager. Stop the existing handler, clear the
     * handler list, change the metadata, recreate the handlers
     * 
     * @param cm : the component type metadata
     * @param configuration : the configuration of the instance
     */
    public void configure(Element cm, Dictionary configuration) {
        // Stop all previous registered handler
        if (m_handlers.length != 0) {
            stop();
        }

        // Clear the handler list
        m_handlers = new Handler[0];

        // Set the component-type metadata
        m_className = cm.getAttribute("className");
        if (m_className == null) {
            m_factory.getLogger().log(Logger.ERROR, "The class name of the component cannot be set, it does not exist in the metadata");
        }

        // ComponentInfo initialization
        m_componentDesc = new ComponentDescription(m_factory.getName(), m_className);

        // Add the name
        m_name = (String) configuration.get("name");
        
        // Create the standard handlers and add these handlers to the list
        for (int i = 0; i < IPojoConfiguration.INTERNAL_HANDLERS.length; i++) {
            // Create a new instance
            try {
                Handler h = (Handler) IPojoConfiguration.INTERNAL_HANDLERS[i].newInstance();
                h.configure(this, cm, configuration);
            } catch (InstantiationException e) {
                m_factory.getLogger().log(Logger.ERROR,
                        "[" + m_name + "] Cannot instantiate the handler " + IPojoConfiguration.INTERNAL_HANDLERS[i] + " : " + e.getMessage());
            } catch (IllegalAccessException e) {
                m_factory.getLogger().log(Logger.ERROR,
                        "[" + m_name + "] Cannot instantiate the handler " + IPojoConfiguration.INTERNAL_HANDLERS[i] + " : " + e.getMessage());
            }
        }

        // Look for namespaces
        for (int i = 0; i < cm.getNamespaces().length; i++) {
            if (!cm.getNamespaces()[i].equals("")) {
                // It is not an internal handler, try to load it
                try {
                    Class c = m_context.getBundle().loadClass(cm.getNamespaces()[i]);
                    Handler h = (Handler) c.newInstance();
                    h.configure(this, cm, configuration);
                } catch (ClassNotFoundException e) {
                    m_factory.getLogger()
                            .log(Logger.ERROR, "[" + m_name + "] Cannot instantiate the handler " + cm.getNamespaces()[i] + " : " + e.getMessage());
                } catch (InstantiationException e) {
                    m_factory.getLogger()
                            .log(Logger.ERROR, "[" + m_name + "] Cannot instantiate the handler " + cm.getNamespaces()[i] + " : " + e.getMessage());
                } catch (IllegalAccessException e) {
                    m_factory.getLogger()
                            .log(Logger.ERROR, "[" + m_name + "] Cannot instantiate the handler " + cm.getNamespaces()[i] + " : " + e.getMessage());
                }
            }
        }
    }

    /**
     * Get the component type description of the current instance.
     * @return the component type information.
     * @see org.apache.felix.ipojo.ComponentInstance#getComponentDescription()
     */
    public ComponentDescription getComponentDescription() {
        return m_componentDesc;
    }

    /**
     * Get the description of the current instance. 
     * @return the instance description.
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceDescription()
     */
    public InstanceDescription getInstanceDescription() {
        int componentState = getState();
        InstanceDescription instanceDescription = new InstanceDescription(m_name, componentState, getContext().getBundle().getBundleId(), m_componentDesc);

        String[] objects = new String[getPojoObjects().length];
        for (int i = 0; i < getPojoObjects().length; i++) {
            objects[i] = getPojoObjects()[i].toString();
        }
        instanceDescription.setCreatedObjects(objects);

        Handler[] handlers = getRegistredHandlers();
        for (int i = 0; i < handlers.length; i++) {
            instanceDescription.addHandler(handlers[i].getDescription());
        }
        return instanceDescription;
    }

    /**
     * Get the list of handlers plugged on the instance.
     * @return the handler array of plugged handlers.
     */
    public Handler[] getRegistredHandlers() {
        return m_handlers;
    }

    /**
     * Return a specified handler.
     * 
     * @param name : class name of the handler to find
     * @return : the handler, or null if not found
     */
    public Handler getHandler(String name) {
        for (int i = 0; i < m_handlers.length; i++) {
            if (m_handlers[i].getClass().getName().equalsIgnoreCase(name)) {
                return m_handlers[i];
            }
        }
        return null;
    }

    // ===================== Lifecycle management =====================

    /**
     * Start the instance manager.
     */
    public void start() {
        if (m_state != STOPPED) {
            return;
        } // Instance already started

        // Start all the handlers
        m_factory.getLogger().log(Logger.INFO, "[" + m_name + "] Start the instance manager with " + m_handlers.length + " handlers");

        // The new state of the component is UNRESOLVED
        m_state = INVALID;

        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].start();
        }

        // Defines the state of the component :
        checkInstanceState();
    }

    /**
     * Stop the instance manager.
     */
    public void stop() {
        if (m_state == STOPPED) {
            return;
        } // Instance already stopped

        setState(INVALID);
        // Stop all the handlers
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].stop();
        }
        
        m_pojoObjects = new Object[0];

        m_state = STOPPED;
        for (int i = 0; i < m_instanceListeners.length; i++) {
            m_instanceListeners[i].stateChanged(this, STOPPED);
        }
    }
    
    /** 
     * Dispose the instance.
     * @see org.apache.felix.ipojo.ComponentInstance#dispose()
     */
    public void dispose() {
        if (m_state > STOPPED) { // Valid or invalid
            stop();
        }
        
        for (int i = 0; i < m_instanceListeners.length; i++) {
            m_instanceListeners[i].stateChanged(this, DISPOSED);
        }
        
        m_factory.disposed(this);

        // Cleaning
        m_handlers = new Handler[0];
        m_fieldRegistration = new HashMap();
        m_clazz = null;
        m_pojoObjects = new Object[0];
        m_instanceListeners = new InstanceStateListener[0];
    }
    
    /**
     * Kill the current instance.
     * Only the factory of this instance can call this method.
     */
    protected void kill() {
        if (m_state > STOPPED) {
            stop();
        }
        
        for (int i = 0; i < m_instanceListeners.length; i++) {
            m_instanceListeners[i].stateChanged(this, DISPOSED);
        }

        // Cleaning
        m_state = DISPOSED;
        m_handlers = new Handler[0];
        m_fieldRegistration = new HashMap();
        m_clazz = null;
        m_pojoObjects = new Object[0];
        m_instanceListeners = new InstanceStateListener[0];
    }

    /**
     * Set the state of the component instance.
     * Ff the state changed call the stateChanged(int) method on the handlers.
     * @param state : the new state
     */
    public void setState(int state) {
        if (m_state != state) {

            // Log the state change
            if (state == INVALID) {
                m_factory.getLogger().log(Logger.INFO, "[" + m_name + "]  State -> INVALID");
            }
            if (state == VALID) {
                m_factory.getLogger().log(Logger.INFO, "[" + m_name + "] State -> VALID");
            }

            // The state changed call the handler stateChange method
            m_state = state;
            for (int i = m_handlers.length - 1; i > -1; i--) {
                m_handlers[i].stateChanged(state);
            }
            
            for (int i = 0; i < m_instanceListeners.length; i++) {
                m_instanceListeners[i].stateChanged(this, state);
            }
        }
    }

    /**
     * Get the actual state of the instance.
     * @return the actual state of the component instance.
     * @see org.apache.felix.ipojo.ComponentInstance#getState()
     */
    public int getState() {
        return m_state;
    }

    /**
     * Check if the instance if started.
     * @return true if the instance is started.
     * @see org.apache.felix.ipojo.ComponentInstance#isStarted()
     */
    public boolean isStarted() {
        return m_state > STOPPED;
    }
    
    /**
     * Add an instance to the created instance list.
     * @param listener : the instance state listener to add.
     * @see org.apache.felix.ipojo.ComponentInstance#addInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public void addInstanceStateListener(InstanceStateListener listener) {
        for (int i = 0; (m_instanceListeners != null) && (i < m_instanceListeners.length); i++) {
            if (m_instanceListeners[i] == listener) {
                return;
            }
        }

        if (m_instanceListeners.length > 0) {
            InstanceStateListener[] newInstances = new InstanceStateListener[m_instanceListeners.length + 1];
            System.arraycopy(m_instanceListeners, 0, newInstances, 0, m_instanceListeners.length);
            newInstances[m_instanceListeners.length] = listener;
            m_instanceListeners = newInstances;
        } else {
            m_instanceListeners = new InstanceStateListener[] { listener };
        }
    }
    
    /**
     * Remove an instance state listener.
     * @param listener : the listener to remove
     * @see org.apache.felix.ipojo.ComponentInstance#removeInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public void removeInstanceStateListener(InstanceStateListener listener) {
        int idx = -1;
        for (int i = 0; i < m_instanceListeners.length; i++) {
            if (m_instanceListeners[i] == listener) {
                idx = i;
                break;
            }
        }
        
        if (idx >= 0) {
            if ((m_instanceListeners.length - 1) == 0) {
                m_instanceListeners = new InstanceStateListener[0];
            } else {
                InstanceStateListener[] newInstances = new InstanceStateListener[m_instanceListeners.length - 1];
                System.arraycopy(m_instanceListeners, 0, newInstances, 0, idx);
                if (idx < newInstances.length) {
                    System.arraycopy(m_instanceListeners, idx + 1, newInstances, idx, newInstances.length - idx);
                }
                m_instanceListeners = newInstances;
            }
        }
    }

    // ===================== end Lifecycle management =====================

    // ================== Class & Instance management ===================

    /**
     * Get the factory which create the current instance.
     * @return the factory of the component
     * @see org.apache.felix.ipojo.ComponentInstance#getFactory()
     */
    public ComponentFactory getFactory() {
        return m_factory;
    }

    /**
     * Load the manipulated class.
     */
    private void load() {
        try {
            m_clazz = m_factory.loadClass(m_className);
        } catch (ClassNotFoundException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] Class not found during the loading phase : " + e.getMessage());
            return;
        }
    }

    /**
     * Is the implementation class loaded?
     * @return true if the class is loaded
     */
    private boolean isLoaded() {
        return m_clazz != null;
    }

    /**
     * Add an instance to the created instance list.
     * @param o : the instance to add
     */
    private void addInstance(Object o) {
        for (int i = 0; (m_pojoObjects != null) && (i < m_pojoObjects.length); i++) {
            if (m_pojoObjects[i] == o) {
                return;
            }
        }

        if (m_pojoObjects.length > 0) {
            Object[] newInstances = new Object[m_pojoObjects.length + 1];
            System.arraycopy(m_pojoObjects, 0, newInstances, 0, m_pojoObjects.length);
            newInstances[m_pojoObjects.length] = o;
            m_pojoObjects = newInstances;
        } else {
            m_pojoObjects = new Object[] { o };
        }
    }

    /**
     * Remove an instance from the created instance list.
     * The instance will be destroyed by the garbage collector.
     * 
     * @param o : the instance to remove
     */
    private void removeInstance(Object o) {
        int idx = -1;
        for (int i = 0; i < m_pojoObjects.length; i++) {
            if (m_pojoObjects[i] == o) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            if ((m_pojoObjects.length - 1) == 0) {
                m_pojoObjects = new Object[0];
            } else {
                Object[] newInstances = new Object[m_pojoObjects.length - 1];
                System.arraycopy(m_pojoObjects, 0, newInstances, 0, idx);
                if (idx < newInstances.length) {
                    System.arraycopy(m_pojoObjects, idx + 1, newInstances, idx, newInstances.length - idx);
                }
                m_pojoObjects = newInstances;
            }
        }
    }

    /**
     * Get the array of object created by the instance.
     * @return the created instance of the component instance.
     */
    public Object[] getPojoObjects() {
        return m_pojoObjects;
    }

    /**
     * Delete the created instance (remove it from the list, to allow the
     * garbage collector to eat the instance).
     * 
     * @param o : the instance to delete
     */
    public void deletePojoObject(Object o) {
        removeInstance(o);
    }

    /**
     * Create an instance of the component. This method need to be called one
     * time only for singleton provided service
     * 
     * @return a new instance
     */
    public Object createPojoObject() {

        if (!isLoaded()) {
            load();
        }
        Object instance = null;
        try {

            // Try to find if there is a constructor with a bundle context as
            // parameter :
            try {
                Constructor constructor = m_clazz.getConstructor(new Class[] { InstanceManager.class, BundleContext.class });
                constructor.setAccessible(true);
                instance = constructor.newInstance(new Object[] { this, m_context });
            } catch (NoSuchMethodException e) {
                instance = null;
            }

            // Create an instance if no instance are already created with
            // <init>()BundleContext
            if (instance == null) {
                Constructor constructor = m_clazz.getConstructor(new Class[] { InstanceManager.class });
                constructor.setAccessible(true);
                instance = constructor.newInstance(new Object[] { this });
            }

        } catch (InstantiationException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> The Component Instance cannot be instancied : " + e.getMessage());
        } catch (IllegalAccessException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> The Component Instance is not accessible : " + e.getMessage());
        } catch (SecurityException e) {
            m_factory.getLogger().log(Logger.ERROR,
                    "[" + m_name + "] createInstance -> The Component Instance is not accessible (security reason) : " + e.getMessage());
        } catch (InvocationTargetException e) {
            m_factory.getLogger().log(Logger.ERROR,
                    "[" + m_name + "] createInstance -> Cannot invoke the constructor method (illegal target) : " + e.getMessage());
        } catch (NoSuchMethodException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> Cannot invoke the constructor (method not found) : " + e.getMessage());
        }

        // Register the new instance
        addInstance(instance);
        // Call createInstance on Handlers :
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].createInstance(instance);
        }
        return instance;
    }

    /**
     * Get the first object created by the instance.
     * If no object created, create and return one object.
     * @return the instance of the component instance to use for singleton component
     */
    public Object getPojoObject() {
        if (m_pojoObjects.length == 0) {
            createPojoObject();
        }
        return m_pojoObjects[0];
    }

    /**
     * Get the manipulated class.
     * @return the manipulated class
     */
    public Class getClazz() {
        if (!isLoaded()) {
            load();
        }
        return m_clazz;
    }

    // ================== end Class & Instance management ================

    // ======================== Handlers Management ======================

    /**
     * Register the given handler to the current instance manager.
     * 
     * @param h : the handler to register
     */
    public void register(Handler h) {
        for (int i = 0; (m_handlers != null) && (i < m_handlers.length); i++) {
            if (m_handlers[i] == h) {
                return;
            }
        }

        if (m_handlers != null) {
            Handler[] newList = new Handler[m_handlers.length + 1];
            System.arraycopy(m_handlers, 0, newList, 0, m_handlers.length);
            newList[m_handlers.length] = h;
            m_handlers = newList;
        }
    }

    /**
     * Register an handler. The handler will be notified of event on each field
     * given in the list.
     * 
     * @param h : the handler to register
     * @param fields : the field metadata list
     * @param methods : the method metadata list
     */
    public void register(Handler h, FieldMetadata[] fields, MethodMetadata[] methods) {
        register(h);
        for (int i = 0; fields != null && i < fields.length; i++) {
            if (m_fieldRegistration.get(fields[i].getFieldName()) == null) {
                m_fieldRegistration.put(fields[i].getFieldName(), new Handler[] { h });
            } else {
                Handler[] list = (Handler[]) m_fieldRegistration.get(fields[i].getFieldName());
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == h) {
                        return;
                    }
                }
                Handler[] newList = new Handler[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = h;
                m_fieldRegistration.put(fields[i].getFieldName(), newList);
            }
        }
        for (int i = 0; methods != null && i < methods.length; i++) {
            if (m_methodRegistration.get(methods[i].getMethodIdentifier()) == null) {
                m_methodRegistration.put(methods[i].getMethodIdentifier(), new Handler[] { h });
            } else {
                Handler[] list = (Handler[]) m_methodRegistration.get(methods[i].getMethodIdentifier());
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == h) {
                        return;
                    }
                }
                Handler[] newList = new Handler[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = h;
                m_methodRegistration.put(methods[i].getMethodIdentifier(), newList);
            }
        }
        
    }

    /**
     * Unregister an handler for the field list. The handler will not be
     * notified of field access but is always register on the instance manager.
     * 
     * @param h : the handler to unregister.
     * @param fields : the field metadata list
     * @param methods : the method metadata list
     */
    public void unregister(Handler h, FieldMetadata[] fields, MethodMetadata[] methods) {
        for (int i = 0; i < fields.length; i++) {
            if (m_fieldRegistration.get(fields[i].getFieldName()) == null) {
                break;
            } else {
                Handler[] list = (Handler[]) m_fieldRegistration.get(fields[i].getFieldName());
                int idx = -1;
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == h) {
                        idx = j;
                        break;
                    }
                }

                if (idx >= 0) {
                    if ((list.length - 1) == 0) {
                        list = new Handler[0];
                    } else {
                        Handler[] newList = new Handler[list.length - 1];
                        System.arraycopy(list, 0, newList, 0, idx);
                        if (idx < newList.length) {
                            System.arraycopy(list, idx + 1, newList, idx, newList.length - idx);
                        }
                        list = newList;
                    }
                    m_fieldRegistration.put(fields[i].getFieldName(), list);
                }
            }
        }
        for (int i = 0; i < methods.length; i++) {
            if (m_methodRegistration.get(methods[i].getMethodIdentifier()) == null) {
                break;
            } else {
                Handler[] list = (Handler[]) m_methodRegistration.get(methods[i].getMethodIdentifier());
                int idx = -1;
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == h) {
                        idx = j;
                        break;
                    }
                }

                if (idx >= 0) {
                    if ((list.length - 1) == 0) {
                        list = new Handler[0];
                    } else {
                        Handler[] newList = new Handler[list.length - 1];
                        System.arraycopy(list, 0, newList, 0, idx);
                        if (idx < newList.length) {
                            System.arraycopy(list, idx + 1, newList, idx, newList.length - idx);
                        }
                        list = newList;
                    }
                    m_methodRegistration.put(methods[i].getMethodIdentifier(), list);
                }
            }
        }
    }

    /**
     * Unregister the given handler.
     * 
     * @param h : the handler to unregister
     */
    public void unregister(Handler h) {
        int idx = -1;
        for (int i = 0; i < m_handlers.length; i++) {
            if (m_handlers[i] == h) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            if ((m_handlers.length - 1) == 0) {
                m_handlers = new Handler[0];
            } else {
                Handler[] newList = new Handler[m_handlers.length - 1];
                System.arraycopy(m_handlers, 0, newList, 0, idx);
                if (idx < newList.length) {
                    System.arraycopy(m_handlers, idx + 1, newList, idx, newList.length - idx);
                }
                m_handlers = newList;
            }
        }
    }
    
    public Set getRegistredFields() {
        return m_fieldRegistration.keySet();
    }
    
    public Set getRegistredMethods() {
        return m_methodRegistration.keySet();
    }

    /**
     * This method is called by the manipulated class each time that a GETFIELD
     * instruction is found. The method ask to each handler which value need to
     * be returned.
     * 
     * @param fieldName : the field name on which the GETFIELD instruction is
     * called
     * @param initialValue : the value of the field in the code
     * @return the value decided by the last asked handler (throw a warining if
     * two fields decide two different values)
     */
    public Object getterCallback(String fieldName, Object initialValue) {
        Object result = null;
        // Get the list of registered handlers
        Handler[] list = (Handler[]) m_fieldRegistration.get(fieldName);
        for (int i = 0; list != null && i < list.length; i++) {
            Object handlerResult = list[i].getterCallback(fieldName, initialValue);
            if (handlerResult != initialValue) {
                result = handlerResult;
            }
        }

        if (result != null) {
            return result;
        } else {
            return initialValue;
        }
    }
    
    /**
     * Dispatch entry method event on registered handler.
     * @param methodId : method id
     */
    public void entryCallback(String methodId) {
        Handler[] list = (Handler[]) m_methodRegistration.get(methodId);
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].entryCallback(methodId);
        }
    }

    /**
     * Dispatch exit method event on registered handler.
     * The given returned object is an instance of Exception if the method has launched an exception.
     * If the given object is null, either the method returns void, either the method has returned null.
     * @param methodId : method id
     * @param e : returned object.
     */
    public void exitCallback(String methodId, Object e) {
        Handler[] list = (Handler[]) m_methodRegistration.get(methodId);
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].exitCallback(methodId, e);
        }
    }

    /**
     * This method is called by the manipulated class each time that a PUTFILED
     * instruction is found. the method send to each handler the new value.
     * 
     * @param fieldName : the field name on which the PUTFIELD instruction is
     * called
     * @param objectValue : the value of the field
     */
    public void setterCallback(String fieldName, Object objectValue) {
        // Get the list of registered handlers
        Handler[] list = (Handler[]) m_fieldRegistration.get(fieldName);

        for (int i = 0; list != null && i < list.length; i++) {
            list[i].setterCallback(fieldName, objectValue);
        }
    }

    /**
     * Get the bundle context used by this component instance.
     * @return the context of the component.
     * @see org.apache.felix.ipojo.ComponentInstance#getContext()
     */
    public BundleContext getContext() {
        return m_context;
    }
    
    public BundleContext getGlobalContext() {
        return ((IPojoContext) m_context).getGlobalContext();
    }
    
    public ServiceContext getLocalServiceContext() {
        return ((IPojoContext) m_context).getServiceContext();
    }

    /**
     * Check the state of all handlers.
     */
    public void checkInstanceState() {
        if (!isStarted()) { return; }
        
        boolean isValid = true;
        for (int i = 0; i < m_handlers.length; i++) {
            boolean b = m_handlers[i].isValid();
            isValid = isValid && b;
        }
     
        // Update the component state if necessary
        if (!isValid && m_state == VALID) {
            // Need to update the state to UNRESOLVED
            setState(INVALID);
            return;
        }
        if (isValid && m_state == INVALID) {
            setState(VALID);
        }
    }

    /**
     * Get the instance name.
     * @return the instance name.
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceName()
     */
    public String getInstanceName() {
        return m_name;
    }

    /**
     * Reconfigure the current instance.
     * @param configuration : the new configuration to push
     * @see org.apache.felix.ipojo.ComponentInstance#reconfigure(java.util.Dictionary)
     */
    public void reconfigure(Dictionary configuration) {
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].reconfigure(configuration);
        }
    }

    /**
     * Get the implementation class of the component type.
     * @return the class name of the component type.
     */
    public String getClassName() {
        return m_className;
    }

    // ======================= end Handlers Management =====================

}
