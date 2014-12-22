package org.mule.tooling.devkit.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class ConnectorImportWizzard extends AbstractDevkitProjectWizzard implements IImportWizard {

    private ConnectorImportWizzardPage importPage;

    public ConnectorImportWizzard() {
        super();
        setNeedsProgressMonitor(true);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Anypoint Connector Import from Folder");
    }

    public void addPages() {
        importPage = new ConnectorImportWizzardPage();
        addPage(importPage);
    }

    public boolean performCancel() {
        return true;
    }

    @Override
    public boolean performFinish() {
        return importPage.performFinish();
    }
}
