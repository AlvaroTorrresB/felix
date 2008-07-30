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
package org.apache.felix.ipojo.handlers.event.subscriber;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.event.Event;

/**
 * Represent an subscriber.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class EventAdminSubscriberMetadata {

    // Names of metadata attributes

    /**
     * The name attribute in the component metadata.
     */
    public static final String NAME_ATTRIBUTE = "name";

    /**
     * The callback attribute in the component metadata.
     */
    public static final String CALLBACK_ATTRIBUTE = "callback";

    /**
     * The topics attribute in the component metadata.
     */
    public static final String TOPICS_ATTRIBUTE = "topics";

    /**
     * The data key attribute in the component metadata.
     */
    public static final String DATA_KEY_ATTRIBUTE = "data-key";

    /**
     * The data type attribute in the component metadata.
     */
    public static final String DATA_TYPE_ATTRIBUTE = "data-type";

    /**
     * The filter attribute in the component metadata.
     */
    public static final String FILTER_ATTRIBUTE = "filter";

    // Default values

    /**
     * The data type atttribute's default value.
     */
    public static final Class DEFAULT_DATA_TYPE_VALUE = java.lang.Object.class;

    /**
     * The name which acts as an identifier.
     */
    private final String m_name;

    /**
     * Name of the callback method.
     */
    private final String m_callback;

    /**
     * Listened topics.
     */
    private String[] m_topics;

    /**
     * The key where user data are stored in the event dictionary.
     */
    private final String m_dataKey;

    /**
     * The type of received data.
     */
    private final Class m_dataType;

    /**
     * Event filter.
     */
    private Filter m_filter;

    /**
     * The context of the bundle.
     */
    private final BundleContext m_bundleContext;

    /**
     * Constructor.
     * 
     * @param bundleContext : bundle context of the managed instance.
     * @param subscriber : subscriber metadata.
     * @throws ConfigurationException
     *             if the configuration of the component or the instance is
     *             invalid.
     */
    public EventAdminSubscriberMetadata(BundleContext bundleContext,
            Element subscriber)
        throws ConfigurationException {

        m_bundleContext = bundleContext;

        /**
         * Setup required attributes
         */

        // NAME_ATTRIBUTE
        if (subscriber.containsAttribute(NAME_ATTRIBUTE)) {
            m_name = subscriber.getAttribute(NAME_ATTRIBUTE);
        } else {
            throw new ConfigurationException(
                    "Missing required attribute in component configuration : "
                            + NAME_ATTRIBUTE);
        }

        // CALLBACK_ATTRIBUTE
        if (subscriber.containsAttribute(CALLBACK_ATTRIBUTE)) {
            m_callback = subscriber.getAttribute(CALLBACK_ATTRIBUTE);
        } else {
            throw new ConfigurationException(
                    "Missing required attribute in component configuration : "
                            + CALLBACK_ATTRIBUTE);
        }

        // TOPICS_ATTRIBUTE
        if (subscriber.containsAttribute(TOPICS_ATTRIBUTE)) {
            m_topics = ParseUtils.split(subscriber
                    .getAttribute(TOPICS_ATTRIBUTE), ",");
            // Check each topic is valid
            Dictionary empty = new Hashtable();
            for (int i = 0; i < m_topics.length; i++) {
                String topic = m_topics[i];
                try {
                    new Event(topic, empty);
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException("Malformed topic : "
                            + topic);
                }
            }
        } else {
            m_topics = null;
            // Nothing to do if TOPICS_ATTRIBUTE is not present as it can be
            // overridden in the instance configuration.
        }

        /**
         * Setup optional attributes
         */

        // DATA_KEY_ATTRIBUTE
        m_dataKey = subscriber.getAttribute(DATA_KEY_ATTRIBUTE);
        if (subscriber.containsAttribute(DATA_TYPE_ATTRIBUTE)) {
            Class type;
            String typeName = subscriber.getAttribute(DATA_TYPE_ATTRIBUTE);
            try {
                type = m_bundleContext.getBundle().loadClass(typeName);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Data type class not found : "
                        + typeName);
            }
            m_dataType = type;
        } else {
            m_dataType = DEFAULT_DATA_TYPE_VALUE;
        }

        // FILTER_ATTRIBUTE
        if (subscriber.containsAttribute(FILTER_ATTRIBUTE)) {
            try {
                m_filter = m_bundleContext.createFilter(subscriber
                        .getAttribute(FILTER_ATTRIBUTE));
            } catch (InvalidSyntaxException e) {
                throw new ConfigurationException("Invalid filter syntax");
            }
        }
    }

    /**
     * Set the topics attribute of the subscriber.
     * 
     * @param topicsString
     *            the comma separated list of the topics to listen
     * @throws ConfigurationException
     *             the specified topic list is malformed
     */
    public void setTopics(String topicsString)
        throws ConfigurationException {
        m_topics = ParseUtils.split(topicsString, ",");
        // Check each topic is valid
        Dictionary empty = new Hashtable();
        for (int i = 0; i < m_topics.length; i++) {
            String topic = m_topics[i];
            try {
                new Event(topic, empty);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Malformed topic : " + topic);
            }
        }
    }

    /**
     * Set the filter attribute of the subscriber.
     * 
     * @param filterString
     *            the string representation of the event filter
     * @throws ConfigurationException : the LDAP filter is malformed
     */
    public void setFilter(String filterString)
        throws ConfigurationException {
        try {
            m_filter = m_bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            throw new ConfigurationException("Invalid filter syntax");
        }
    }

    /**
     * Check that the required instance configurable attributes are all set.
     * 
     * @throws ConfigurationException
     *             if a required attribute is missing
     */
    public void check()
        throws ConfigurationException {
        if (m_topics == null || m_topics.length == 0) {
            throw new ConfigurationException(
                    "Missing required attribute in component or instance configuration : "
                            + TOPICS_ATTRIBUTE);
        }
    }

    /**
     * Get the name attribute of the subscriber.
     * 
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Get the topics attribute of the subscriber.
     * 
     * @return the topics
     */
    public String[] getTopics() {
        return m_topics;
    }

    /**
     * Get the callback attribute of the subscriber.
     * 
     * @return the callback
     */
    public String getCallback() {
        return m_callback;
    }

    /**
     * Get the data key attribute of the subscriber.
     * 
     * @return the dataKey
     */
    public String getDataKey() {
        return m_dataKey;
    }

    /**
     * Get the data type attribute of the subscriber.
     * 
     * @return the dataType
     */
    public Class getDataType() {
        return m_dataType;
    }

    /**
     * Get the filter attribute of the subscriber.
     * 
     * @return the filter
     */
    public Filter getFilter() {
        return m_filter;
    }

}
