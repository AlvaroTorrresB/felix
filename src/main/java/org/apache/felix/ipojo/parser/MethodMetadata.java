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
package org.apache.felix.ipojo.parser;

import org.apache.felix.ipojo.metadata.Element;

/**
 * A Method Metadata represent a method from the implementation class.
 * This class allow to get information about a method : name, arguments, return type...
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodMetadata {
    
    /**
     * Name of the method.
     */
    private String m_name;
    
    /**
     * Argument type array. 
     */
    private String[] m_arguments = new String[0];
    
    /**
     * Returned type. 
     */
    private String m_return = "void";
    
    /**
     * Constructor.
     * @param metadata : method manipulation element.
     */
    MethodMetadata(Element metadata) {
        m_name = metadata.getAttribute("name");
        String arg = metadata.getAttribute("arguments");
        String rt = metadata.getAttribute("return");
        if (arg != null) {
            m_arguments = ParseUtils.parseArrays(arg);
        }
        if (rt != null) {
            m_return = rt;
        }
    }
    
    public String getMethodName() { return m_name; }
    
    public String[] getMethodArguments() { return m_arguments; }
    
    public String getMethodReturn() { return m_return; }
    
    /**
     * Get the method unique identifier. For internal usage only.
     * @return the method identifier.
     */
    public String getMethodIdentifier() {
        String id = m_name;
        for (int i = 0; i < m_arguments.length; i++) {
            String cn = m_arguments[i];
            if (cn.endsWith("[]")) {
                cn = cn.replace('[', '$');
                cn = cn.substring(0, cn.length() - 1);
            }
            cn = cn.replace('.', '_');
            id += cn;
        }
        return id;
    }

}
