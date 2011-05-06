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

package org.apache.felix.sigil.ivy;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.sigil.common.config.IRepositoryConfig;
import org.apache.felix.sigil.common.core.BldCore;
import org.apache.felix.sigil.common.model.IModelElement;
import org.apache.felix.sigil.common.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.common.repository.IResolution;
import org.apache.felix.sigil.common.repository.IResolutionMonitor;
import org.apache.felix.sigil.common.repository.ResolutionConfig;
import org.apache.felix.sigil.common.repository.ResolutionException;

public class BldResolver implements IBldResolver
{
    private BldRepositoryManager manager;
    private final IRepositoryConfig config;
    
    static
    {
        try
        {
            BldCore.init();
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    };

    /**
     * @param project
     */
    public BldResolver(IRepositoryConfig config)
    {
        this.config = config;
    }

    public IResolution resolve(IModelElement element, boolean transitive)
    {
        int options = ResolutionConfig.IGNORE_ERRORS | ResolutionConfig.INCLUDE_OPTIONAL;
        if (transitive)
            options |= ResolutionConfig.INCLUDE_DEPENDENTS;

        ResolutionConfig config = new ResolutionConfig(options);
        try
        {
            return resolve(element, config);
        }
        catch (ResolutionException e)
        {
            throw new IllegalStateException(
                "eek! this shouldn't happen when ignoreErrors=true", e);
        }
    }

    public IResolution resolveOrFail(IModelElement element, boolean transitive)
        throws ResolutionException
    {
        int options = 0;
        if (transitive)
            options |= ResolutionConfig.INCLUDE_DEPENDENTS;
        ResolutionConfig config = new ResolutionConfig(options);
        return resolve(element, config);
    }

    private IResolution resolve(IModelElement element, ResolutionConfig config)
        throws ResolutionException
    {
        if (manager == null)
        {
            manager = new BldRepositoryManager(this.config);
        }

        IResolutionMonitor ivyMonitor = new IResolutionMonitor()
        {
            public void endResolution(IModelElement requirement, ISigilBundle sigilBundle)
            {
                Log.debug("Resolved " + requirement + " -> " + sigilBundle);
            }

            public boolean isCanceled()
            {
                return false;
            }

            public void startResolution(IModelElement requirement)
            {
                Log.verbose("Resolving " + requirement);
            }
        };

        return manager.getBundleResolver().resolve(element, config, ivyMonitor);
    }
}
