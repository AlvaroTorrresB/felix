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

package org.apache.felix.sigil.common.core.internal.model.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.JarFile;

import org.apache.felix.sigil.common.config.Resource;
import org.apache.felix.sigil.common.core.BldCore;
import org.apache.felix.sigil.common.model.AbstractCompoundModelElement;
import org.apache.felix.sigil.common.model.eclipse.IBundleCapability;
import org.apache.felix.sigil.common.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.common.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.common.model.osgi.IPackageExport;
import org.apache.felix.sigil.common.model.osgi.IPackageImport;
import org.apache.felix.sigil.common.progress.IProgress;
import org.apache.felix.sigil.common.util.ManifestUtil;
import org.osgi.framework.Version;

/**
 * @author dave
 *
 */
public class SigilBundle extends AbstractCompoundModelElement implements ISigilBundle
{

    private static final long serialVersionUID = 1L;

    private IBundleModelElement bundle;
    private Resource[] sourcePaths;
    private String[] classpath;
    private String[] packages;
    private File location;

    private File sourcePathLocation;
    private File licencePathLocation;
    private String sourceRootPath;

    public SigilBundle()
    {
        super("Sigil Bundle");
        sourcePaths = new Resource[0];
        classpath = new String[0];
        packages = new String[0];
    }
    
    public void synchronize(IProgress progress) throws IOException
    {
        progress = progress.newTask(100);
        progress.report("Synchronizing " + bundle.getSymbolicName() + " binary");
        sync(location, bundle.getUpdateLocation(), progress.newChild(45));

        if (bundle.getSourceLocation() != null)
        {
            try
            {
                progress.report("Synchronizing " + bundle.getSymbolicName() + " source");
                sync(sourcePathLocation, bundle.getSourceLocation(),
                    progress.newChild(45));
            }
            catch (IOException e)
            {
                BldCore.error("Failed to download source for " + bundle.getSymbolicName()
                    + " " + bundle.getVersion(), e.getCause());
            }
        }

        if (bundle.getLicenseURI() != null)
        {
            try
            {
                progress.report("Synchronizing " + bundle.getSymbolicName() + " licence");
                sync(licencePathLocation, bundle.getLicenseURI(), progress.newChild(10));
            }
            catch (IOException e)
            {
                BldCore.error("Failed to download licence for "
                    + bundle.getSymbolicName() + " " + bundle.getVersion(), e.getCause());
            }
        }

        updateManifest(location);
    }

    private void updateManifest(File location) throws IOException
    {
        if (location != null && location.exists())
        {
            JarFile f = new JarFile(location, false);
            try
            {
                setBundleInfo(ManifestUtil.buildBundleModelElement(f));
            }
            finally
            {
                f.close();
            }
        }
    }

    public boolean isSynchronized()
    {
        return location == null || location.exists();
    }

    private static void sync(File local, URI remote, IProgress progress)
        throws IOException
    {
        try
        {
            if (remote != null)
            {
                if (local != null && !local.exists())
                {
                    URL url = remote.toURL();
                    URLConnection connection = url.openConnection();
                    int contentLength = connection.getContentLength();

                    progress = progress.newTask(contentLength);
                    progress.report("Downloading from " + url.getHost());

                    InputStream in = null;
                    OutputStream out = null;
                    try
                    {
                        URLConnection conn = url.openConnection();
                        if (conn instanceof HttpURLConnection)
                        {
                            HttpURLConnection http = (HttpURLConnection) conn;
                            http.setConnectTimeout(10000);
                            http.setReadTimeout(5000);
                        }
                        in = conn.getInputStream();
                        local.getParentFile().mkdirs();
                        out = new FileOutputStream(local);
                        stream(in, out, progress);
                    }
                    finally
                    {
                        if (in != null)
                        {
                            in.close();
                        }
                        if (out != null)
                        {
                            out.close();
                        }
                        progress.done();
                    }
                }
            }
        }
        catch (IOException e)
        {
            local.delete();
            throw e;
        }
    }

    private static void stream(InputStream in, OutputStream out, IProgress progress)
        throws IOException
    {
        byte[] b = new byte[1024];
        for (;;)
        {
            if (progress.isCanceled())
            {
                throw new InterruptedIOException("User canceled download");
            }
            int r = in.read(b);
            if (r == -1)
                break;
            out.write(b, 0, r);
            progress.worked(r);
        }

        out.flush();
    }

    public IBundleModelElement getBundleInfo()
    {
        return bundle;
    }

    public void setBundleInfo(IBundleModelElement bundle)
    {
        if (bundle == null)
        {
            if (this.bundle != null)
            {
                this.bundle.setParent(null);
            }
        }
        else
        {
            bundle.setParent(this);
        }
        this.bundle = bundle;
    }

    public void addSourcePath(Resource path)
    {
        ArrayList<Resource> tmp = new ArrayList<Resource>(getSourcePaths());
        tmp.add(path);
        sourcePaths = tmp.toArray(new Resource[tmp.size()]);
    }

    public void removeSourcePath(Resource path)
    {
        ArrayList<Resource> tmp = new ArrayList<Resource>(getSourcePaths());
        if (tmp.remove(path))
        {
            sourcePaths = tmp.toArray(new Resource[tmp.size()]);
        }
    }

    public Collection<Resource> getSourcePaths()
    {
        return Arrays.asList(sourcePaths);
    }

    public void clearSourcePaths()
    {
        sourcePaths = new Resource[0];
    }

    public void addClasspathEntry(String encodedClasspath)
    {
        encodedClasspath = encodedClasspath.trim();
        ArrayList<String> tmp = new ArrayList<String>(getClasspathEntrys());
        if (!tmp.contains(encodedClasspath)) {
            tmp.add(encodedClasspath);
            classpath = tmp.toArray(new String[tmp.size()]);
        }
    }

    public Collection<String> getClasspathEntrys()
    {
        return Arrays.asList(classpath);
    }

    public void removeClasspathEntry(String encodedClasspath)
    {
        ArrayList<String> tmp = new ArrayList<String>(getClasspathEntrys());
        if (tmp.remove(encodedClasspath.trim()))
        {
            classpath = tmp.toArray(new String[tmp.size()]);
        }
    }

    public File getLocation()
    {
        return location;
    }

    public void setLocation(File location)
    {
        this.location = location;
    }

    public File getSourcePathLocation()
    {
        return sourcePathLocation;
    }

    public void setSourcePathLocation(File location)
    {
        this.sourcePathLocation = location;
    }

    public String getSourceRootPath()
    {
        return sourceRootPath;
    }

    public void setSourceRootPath(String location)
    {
        this.sourceRootPath = location;
    }

    public File getLicencePathLocation()
    {
        return licencePathLocation;
    }

    public void setLicencePathLocation(File licencePathLocation)
    {
        this.licencePathLocation = licencePathLocation;
    }

    public String getElementName()
    {
        return bundle.getSymbolicName();
    }

    public Version getVersion()
    {
        return bundle.getVersion();
    }

    public void setVersion(Version version)
    {
        this.bundle.setVersion(version);
    }

    public String getSymbolicName()
    {
        return bundle.getSymbolicName();
    }

    public Collection<String> getPackages()
    {
        return Arrays.asList(packages);
    }

    public void addPackage(String pkg)
    {
        ArrayList<String> tmp = new ArrayList<String>(getPackages());
        tmp.add(pkg);
        packages = tmp.toArray(new String[tmp.size()]);
    }

    public boolean removePackage(String pkg)
    {
        ArrayList<String> tmp = new ArrayList<String>(getPackages());
        if (tmp.remove(pkg))
        {
            packages = tmp.toArray(new String[tmp.size()]);
            return true;
        }
        else
        {
            return false;
        }
    }

    public IPackageExport findExport(String packageName)
    {
        for (IPackageExport e : bundle.getExports())
        {
            if (packageName.equals(e.getPackageName()))
            {
                return e;
            }
        }
        return null;
    }

    public IPackageImport findImport(String packageName)
    {
        for (IPackageImport i : bundle.getImports())
        {
            if (packageName.equals(i.getPackageName()))
            {
                return i;
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return "SigilBundle["
            + (getBundleInfo() == null ? null
                : (getBundleInfo().getSymbolicName() + ":" + getBundleInfo().getVersion()))
            + "]";
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (obj == this)
            return true;

        if (obj instanceof SigilBundle)
        {
            return obj.toString().equals(toString());
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return 31 * toString().hashCode();
    }

    @Override
    public SigilBundle clone()
    {
        SigilBundle b = (SigilBundle) super.clone();
        b.bundle = (IBundleModelElement) b.bundle.clone();

        Resource[] newPaths = new Resource[b.sourcePaths.length];
        System.arraycopy(b.sourcePaths, 0, newPaths, 0, b.sourcePaths.length);
        b.sourcePaths = newPaths;

        String[] tmp = new String[classpath.length];
        System.arraycopy(b.classpath, 0, tmp, 0, b.classpath.length);
        b.classpath = tmp;

        tmp = new String[packages.length];
        System.arraycopy(b.packages, 0, tmp, 0, b.packages.length);
        b.packages = tmp;

        return b;
    }

    public IBundleCapability getBundleCapability()
    {
        return new BundleCapability(bundle);
    }
}
