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

package org.apache.felix.sigil.eclipse.ui.internal.quickfix;

import org.apache.felix.sigil.common.model.ModelElementFactory;
import org.apache.felix.sigil.common.model.ModelElementFactoryException;
import org.apache.felix.sigil.common.model.osgi.IPackageExport;
import org.apache.felix.sigil.common.model.osgi.IPackageImport;
import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.util.ModelHelper;
import org.apache.felix.sigil.eclipse.ui.SigilUI;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class ImportPackageProposal implements IJavaCompletionProposal
{

    private IPackageExport e;
    private ISigilProjectModel n;

    public ImportPackageProposal(IPackageExport e, ISigilProjectModel n)
    {
        this.e = e;
        this.n = n;
    }

    public int getRelevance()
    {
        return 100;
    }

    public void apply(IDocument document)
    {
        try
        {

            final IPackageImport i = ModelElementFactory.getInstance().newModelElement(
                IPackageImport.class);
            i.setPackageName(e.getPackageName());
            VersionRange selectedVersions = ModelHelper.getDefaultRange(e.getVersion());
            i.setVersions(selectedVersions);

            WorkspaceModifyOperation op = new WorkspaceModifyOperation()
            {
                @Override
                protected void execute(IProgressMonitor monitor) throws CoreException
                {
                    n.getBundle().getBundleInfo().addImport(i);
                    n.save(monitor);
                }
            };

            SigilUI.runWorkspaceOperation(op, null);
        }
        catch (ModelElementFactoryException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getAdditionalProposalInfo()
    {
        return null;
    }

    public IContextInformation getContextInformation()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getDisplayString()
    {
        return "Import package " + e.getPackageName() + " version " + e.getVersion()
            + " to bundle";
    }

    public Image getImage()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Point getSelection(IDocument document)
    {
        return null;
    }

}
