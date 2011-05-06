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

package org.apache.felix.sigil.common.core.repository;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.sigil.common.core.BldCore;
import org.apache.felix.sigil.common.model.ModelElementFactory;
import org.apache.felix.sigil.common.model.ModelElementFactoryException;
import org.apache.felix.sigil.common.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.common.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.common.model.osgi.IPackageExport;
import org.apache.felix.sigil.common.osgi.VersionTable;
import org.apache.felix.sigil.common.repository.AbstractBundleRepository;
import org.apache.felix.sigil.common.repository.IRepositoryVisitor;
import org.apache.felix.sigil.common.util.ManifestUtil;
import org.osgi.framework.Version;

public class SystemRepository extends AbstractBundleRepository
{

    private final String packages;
    private final File frameworkPath;

    public SystemRepository(String id, File frameworkPath, String packages)
    {
        super(id);
        this.frameworkPath = frameworkPath;
        this.packages = packages;
    }

    private static ISigilBundle systemBundle;

    @Override
    public void accept(IRepositoryVisitor visitor, int options)
    {
        ISigilBundle bundle = loadSystemBundle();

        if (bundle != null)
        {
            visitor.visit(bundle);
        }
    }

    private synchronized ISigilBundle loadSystemBundle()
    {
        if (systemBundle == null)
        {
            systemBundle = ModelElementFactory.getInstance().newModelElement(
                ISigilBundle.class);

            JarFile jar = null;

            try
            {
                final IBundleModelElement info;
                if (frameworkPath != null)
                {
                    systemBundle.setLocation(frameworkPath);
                    jar = new JarFile(frameworkPath,false);
                    info = ManifestUtil.buildBundleModelElement(jar);
                }
                else
                {
                    info = ModelElementFactory.getInstance().newModelElement(
                        IBundleModelElement.class);
                }

                info.setSymbolicName("system bundle");
                info.setName("Sigil system bundle");

                applyProfile(info);
                systemBundle.addChild(info);
            }
            catch (IOException e)
            {
                BldCore.error("Failed to read jar file " + frameworkPath, e);
            }
            catch (ModelElementFactoryException e)
            {
                BldCore.error("Failed to build bundle " + frameworkPath, e);
            }
            catch (RuntimeException e)
            {
                BldCore.error("Failed to build bundle " + frameworkPath, e);
            }
            finally
            {
                if (jar != null)
                {
                    try
                    {
                        jar.close();
                    }
                    catch (IOException e)
                    {
                        BldCore.error("Failed to close jar file", e);
                    }
                }
            }
        }

        return systemBundle;
    }

    private static final Pattern attrPattern = Pattern.compile("(.*?)=\"?(.*?)\"?$");
    
    private void applyProfile(IBundleModelElement info)
    {
        if (packages != null)
        {
            String version = null;
            for (String name : packages.split(";\\s*"))
            {
                Matcher m = attrPattern.matcher(name);
                if ( m.matches() ) {                    
                    if ("version".equals(m.group(1))) {
                        version = m.group(2);
                    }
                }
                else {
                    IPackageExport pe = ModelElementFactory.getInstance().newModelElement(
                        IPackageExport.class);
                    pe.setPackageName(name);
                    info.addExport(pe);
                }
            }
            
            if ( version != null ) {
                Version v = VersionTable.getVersion(version);
                for(IPackageExport pe : info.getExports()) {
                    pe.setVersion(v);
                }
            }
        }
    }

    public synchronized void refresh()
    {
        systemBundle = null;
        notifyChange();
    }
}
