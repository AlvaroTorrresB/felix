package org.apache.felix.sigil.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.felix.sigil.common.core.BldCore;
import org.apache.felix.sigil.common.model.ModelElementFactory;
import org.apache.felix.sigil.common.model.ModelElementFactoryException;
import org.apache.felix.sigil.common.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.common.model.osgi.IPackageExport;
import org.apache.felix.sigil.common.model.osgi.IPackageImport;
import org.apache.felix.sigil.common.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.common.osgi.VersionTable;
import org.osgi.framework.Version;

public class ManifestUtil
{
    /**
     * Use this utility to read Manifest from a JarFile or ZipFile
     * due to http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6735255
     * 
     * @param zip
     * @return
     * @throws IOException
     */
    public static Manifest getManifest(ZipFile zip) throws IOException {
        ZipEntry entry = zip.getEntry("META-INF/MANIFEST.MF");
        if ( entry == null ) return null;
        
        InputStream in = zip.getInputStream(entry);
        try {
            return new Manifest(in);
        }
        finally {
            // explicitly close due to 
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6735255
            in.close();
        }
    }
    
    public static IBundleModelElement buildBundleModelElement(ZipFile zip) throws IOException {
        IBundleModelElement info = null;

        Manifest mf = getManifest(zip);
        
        if (mf != null)
        {
            Attributes attrs = mf.getMainAttributes();
            String name = attrs.getValue("Bundle-SymbolicName");
            if (name == null)
            {
                // framework.jar doesn't have Bundle-SymbolicName!
                name = attrs.getValue("Bundle-Name");
            }

            if (name != null)
            {
                try
                {
                    info = ModelElementFactory.getInstance().newModelElement(
                        IBundleModelElement.class);
                    info.setSymbolicName(name.split(";")[0]);
                    info.setVersion(VersionTable.getVersion(attrs.getValue("Bundle-Version")));
                    info.setName(attrs.getValue("Bundle-Name"));
                    info.setDescription(attrs.getValue("Bundle-Description"));
                    info.setVendor(attrs.getValue("Bundle-Vendor"));

                    String str = attrs.getValue("Import-Package");
                    if (str != null)
                    {
                        addImports(info, str);
                    }

                    str = attrs.getValue("Export-Package");
                    if (str != null)
                    {
                        addExports(info, str);
                    }

                    str = attrs.getValue("Require-Bundle");
                    if (str != null)
                    {
                        addRequires(info, str);
                    }

                    str = attrs.getValue("Bundle-Classpath");

                    if (str != null)
                    {
                        addClasspath(info, str);
                    }

                    str = attrs.getValue("Fragment-Host");
                    if (str != null)
                    {
                        addHost(info, str);
                    }
                }
                catch (RuntimeException e)
                {
                    BldCore.error("Failed to read info from bundle " + name, e);
                    // clear elements as clearly got garbage
                    info = null;
                }
            }
        }

        return info;
    }

    private static void addClasspath(IBundleModelElement info, String cpStr)
    {
        for (String cp : cpStr.split("\\s*,\\s*"))
        {
            info.addClasspath(cp);
        }
    }

    private static void addExports(IBundleModelElement info, String exportStr)
        throws ModelElementFactoryException
    {
        for (String exp : QuoteUtil.split(exportStr))
        {
            try
            {
                String[] parts = exp.split(";");
                IPackageExport pe = ModelElementFactory.getInstance().newModelElement(
                    IPackageExport.class);
                pe.setPackageName(parts[0].trim());

                if (parts.length > 1)
                {
                    for (int i = 1; i < parts.length; i++)
                    {
                        String check = parts[i];
                        if (check.toLowerCase().startsWith("version="))
                        {
                            pe.setVersion(parseVersion(check.substring("version=".length())));
                        }
                        else if (check.toLowerCase().startsWith("specification-version="))
                        {
                            pe.setVersion(parseVersion(check.substring("specification-version=".length())));
                        }
                        else if (check.toLowerCase().startsWith("uses:="))
                        {
                            for (String use : parseUses(check.substring("uses:=".length())))
                            {
                                pe.addUse(use);
                            }
                        }
                    }
                }
                info.addExport(pe);
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
            }
        }
    }

    private static Collection<String> parseUses(String uses)
    {
        if (uses.startsWith("\""))
        {
            uses = uses.substring(1, uses.length() - 2);
        }

        return Arrays.asList(uses.split(","));
    }

    private static Version parseVersion(String val)
    {
        val = val.replaceAll("\"", "");
        return VersionTable.getVersion(val);
    }

    private static void addImports(IBundleModelElement info, String importStr)
        throws ModelElementFactoryException
    {
        for (String imp : QuoteUtil.split(importStr))
        {
            String[] parts = imp.split(";");
            IPackageImport pi = ModelElementFactory.getInstance().newModelElement(
                IPackageImport.class);
            pi.setPackageName(parts[0].trim());

            if (parts.length > 1)
            {
                for (int i = 1; i < parts.length; i++)
                {
                    String p = parts[i];
                    if (p.toLowerCase().startsWith("version="))
                    {
                        pi.setVersions(VersionRange.parseVersionRange(p.substring("version=".length())));
                    }
                    else if (p.toLowerCase().startsWith("specification-version="))
                    {
                        pi.setVersions(VersionRange.parseVersionRange(p.substring("specification-version=".length())));
                    }
                    else if (p.toLowerCase().startsWith("resolution:="))
                    {
                        pi.setOptional(p.toLowerCase().substring("resolution:=".length()).equals(
                            "optional"));
                    }
                }
            }
            info.addImport(pi);
        }
    }

    private static void addRequires(IBundleModelElement info, String reqStr)
        throws ModelElementFactoryException
    {
        for (String imp : QuoteUtil.split(reqStr))
        {
            String[] parts = imp.split(";");
            IRequiredBundle req = ModelElementFactory.getInstance().newModelElement(
                IRequiredBundle.class);
            req.setSymbolicName(parts[0]);

            if (parts.length > 1)
            {
                if (parts[1].toLowerCase().startsWith("version="))
                {
                    req.setVersions(VersionRange.parseVersionRange(parts[1].substring("version=".length())));
                }
                else if (parts[1].toLowerCase().startsWith("specification-version="))
                {
                    req.setVersions(VersionRange.parseVersionRange(parts[1].substring("specification-version=".length())));
                }
            }
            info.addRequiredBundle(req);
        }
    }

    /**
     * @param info
     * @param str
     */
    private static void addHost(IBundleModelElement info, String str)
    {
        String[] parts = str.split(";");
        IRequiredBundle req = ModelElementFactory.getInstance().newModelElement(
            IRequiredBundle.class);
        req.setSymbolicName(parts[0].trim());

        if (parts.length > 1)
        {
            String part = parts[1].toLowerCase().trim();
            if (part.startsWith("bundle-version="))
            {
                req.setVersions(VersionRange.parseVersionRange(part.substring("bundle-version=".length())));
            }
        }
        info.setFragmentHost(req);
    }
}
