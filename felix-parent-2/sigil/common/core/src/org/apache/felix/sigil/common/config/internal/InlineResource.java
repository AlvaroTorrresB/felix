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

package org.apache.felix.sigil.common.config.internal;

import java.io.File;

import org.apache.felix.sigil.common.config.IBldProject;

/**
 * @author dave
 *
 */
public class InlineResource extends AbstractResource
{

    /**
     * @param bPath
     */
    public InlineResource(IBldProject project, String bPath)
    {
        super(project, bPath);
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.core.Resource#getLocalFile()
     */
    public String getLocalFile()
    {
        return bPath;
    }

    public String toString()
    {
        return '@' + bPath;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.core.Resource#toBNDInstruction(java.io.File[])
     */
    public String toBNDInstruction(File[] classpath)
    {
        StringBuilder sb = new StringBuilder();
        sb.append('@');

        File f = project.resolve(bPath);

        if (f.exists())
        {
            sb.append(f);
        }
        else
            sb.append(bPath);

        return sb.toString();
    }

}
