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
package org.apache.felix.ipojo.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * iPOJO Logger. This logger send log message to a log service if presents.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Logger {
    
    /**
     * Ipojo default log level property.
     */
    public static final String IPOJO_LOG_LEVEL = "ipojo.log.level";

    /**
     * Log Level ERROR.
     */
    public static final int ERROR = 1;

    /**
     * Log Level WARNING.
     */
    public static final int WARNING = 2;

    /**
     * Log Level INFO.
     */
    public static final int INFO = 3;

    /**
     * Log Level DEBUG.
     */
    public static final int DEBUG = 4;

    /**
     * Bundle Context.
     */
    private BundleContext m_context;

    /**
     * Name of the logger.
     */
    private String m_name;

    /**
     * trace level of this logger.
     */
    private int m_level;

    /**
     * Constructor.
     * 
     * @param context : bundle context
     * @param name : name of the logger
     * @param level : trace level
     */
    public Logger(BundleContext context, String name, int level) {
        m_name = name;
        m_level = level;
        m_context = context;
    }
    
    /**
     * Constructor.
     * 
     * @param context : bundle context
     * @param name : name of the logger
     */
    public Logger(BundleContext context, String name) {
        this(context, name, getDefaultLevel(context));
    }

    /**
     * Log a message.
     * 
     * @param level : level of the message
     * @param msg : the message to log
     */
    public void log(int level, String msg) {
        if (m_level >= level) {
            dispatch(level, msg);
        }
    }

    /**
     * Log a message with an exception.
     * 
     * @param level : level of the message
     * @param msg : message to log
     * @param exception : exception attached to the message
     */
    public void log(int level, String msg, Throwable exception) {
        if (m_level >= level) {
            dispatch(level, msg, exception);
        }
    }
    
    /**
     * Internal log method.
     * 
     * @param level : level of the message.
     * @param msg : message to log
     */
    private void dispatch(int level, String msg) {
        
        ServiceReference ref = m_context.getServiceReference(LogService.class.getName());
        LogService log = null;
        if (ref != null) {
            log = (LogService) m_context.getService(ref);
        }
        
        String message = null;
        switch (level) {
            case DEBUG:
                message = "[" + m_name + "] DEBUG: " + msg;
                if (log != null) {
                    log.log(LogService.LOG_DEBUG, message);
                }
                System.err.println(message); // NOPMD
                break;
            case ERROR:
                message = "[" + m_name + "] ERROR: " + msg;
                if (log != null) {
                    log.log(LogService.LOG_ERROR, message);
                }
                System.err.println(message); //NOPMD
                break;
            case INFO:
                message = "[" + m_name + "] INFO: " + msg;
                if (log != null) {
                    log.log(LogService.LOG_INFO, message);
                }
                System.err.println(message); // NOPMD
                break;
            case WARNING:
                message = "[" + m_name + "] WARNING: " + msg;
                if (log != null) {
                    log.log(LogService.LOG_WARNING, message);
                }
                System.err.println(message); // NOPMD
                break;
            default:
                System.err.println("[" + m_name + "] UNKNOWN[" + level + "]: " + msg); // NOPMD
                break;
        }
        
        if (log != null) {
            m_context.ungetService(ref);
        }
    }

    /**
     * Internal log method.
     * 
     * @param level : level of the message.
     * @param msg : message to log
     * @param exception : exception attached to the message
     */
    private void dispatch(int level, String msg, Throwable exception) {
        
        ServiceReference ref = m_context.getServiceReference(LogService.class.getName());
        LogService log = null;
        if (ref != null) {
            log = (LogService) m_context.getService(ref);
        }
        
        String message = null;
        switch (level) {
            case DEBUG:
                message = "[" + m_name + "] DEBUG: " + msg;
                if (log != null) {
                    log.log(LogService.LOG_DEBUG, message, exception);
                }
                System.err.println(message); // NOPMD
                exception.printStackTrace(); // NOPMD
                break;
            case ERROR:
                message = "[" + m_name + "] ERROR: " + msg;
                if (log != null) {
                    log.log(LogService.LOG_ERROR, message, exception);
                }
                System.err.println(message); // NOPMD
                exception.printStackTrace(System.err); // NOPMD
                break;
            case INFO:
                message = "[" + m_name + "] INFO: " + msg;
                if (log != null) {
                    log.log(LogService.LOG_INFO, message, exception);
                }
                System.err.println(message); // NOPMD
                exception.printStackTrace(System.err); // NOPMD
                break;
            case WARNING:
                message = "[" + m_name + "] WARNING: " + msg;
                if (log != null) {
                    log.log(LogService.LOG_WARNING, message, exception);
                }
                System.err.println(message); // NOPMD
                exception.printStackTrace(System.err); // NOPMD
                break;
            default:
                System.err.println("[" + m_name + "] UNKNOWN[" + level + "]: " + msg); // NOPMD
                exception.printStackTrace(); // NOPMD
                break;
        }
        
        if (log != null) {
            m_context.ungetService(ref);
        }
    }
    
    /**
     * Get the default logger level.
     * The property is searched inside the framework properties, the system properties,
     * and in the manifest from the given bundle context. By default, set the level to WARNING. 
     * @param context : bundle context.
     * @return the default log level.
     */
    private static int getDefaultLevel(BundleContext context) {
        // First check in the framework and in the system properties
        String level = context.getProperty(IPOJO_LOG_LEVEL);
        
        // If null, look in bundle manifest
        if (level == null) {
            level = (String) context.getBundle().getHeaders().get(IPOJO_LOG_LEVEL);
        }
        
        if (level != null) {
            if (level.equalsIgnoreCase("info")) {
                return INFO;
            } else if (level.equalsIgnoreCase("debug")) {
                return DEBUG;
            } else if (level.equalsIgnoreCase("warning")) {
                return WARNING;
            } else if (level.equalsIgnoreCase("error")) {
                return ERROR;
            }
        }
        
        // Either l is null, either the specified log level was unknown
        // Set the default to WARNING
        return WARNING;
        
    }
}
