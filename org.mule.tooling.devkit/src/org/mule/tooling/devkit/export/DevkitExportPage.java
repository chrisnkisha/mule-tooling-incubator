package org.mule.tooling.devkit.export;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.mule.tooling.devkit.maven.BaseDevkitGoalRunner;
import org.mule.tooling.devkit.maven.MavenDevkitProjectDecorator;
import org.mule.tooling.ui.utils.SaveModifiedResourcesDialog;
import org.mule.tooling.ui.utils.UiUtils;

public class DevkitExportPage extends WizardPage {

    /** Unique page id */
    public static final String PAGE_ID = "muleProjectExportPage";

    /** Selected Mule project */
    protected IJavaProject project;

    /** Projects dropdown */
    protected ComboViewer projects;

    private Composite control;

    protected Text name;

    private String outputAbsolutePath;

    private Button browseButton;

    protected DevkitExportPage(IJavaProject selected) {
        super(PAGE_ID);
        setTitle("Export Mule Extension");
        setDescription("Export a Mule Extension as an Update  Site");
        this.project = selected;
    }

    public void createControl(Composite parent) {
        control = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(1, false);
        control.setLayout(layout);

        Group exportGroup = new Group(control, SWT.NULL);
        layout = new GridLayout(3, false);

        layout.numColumns = 3;
        layout.verticalSpacing = 9;
        exportGroup.setLayout(layout);
        exportGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        exportGroup.setText("Export Settings");

        Label outputFileName = new Label(exportGroup, SWT.NULL);
        outputFileName.setText("Location:");
        outputFileName.setToolTipText("The name for the archive that will be generated.");

        name = new Text(exportGroup, SWT.BORDER | SWT.SINGLE);
        name.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        browseButton = new Button(exportGroup, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
                pickFile();
            }

            public void mouseUp(MouseEvent e) {
            }
        });

        setControl(control);
    }

    /**
     * Choose a file with the dialog and save the result in the text field.
     */
    protected void pickFile() {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        String result = dialog.open();
        if (result != null) {
            name.setText(result);
        }
    }

    public Composite getControl() {
        return this.control;

    }

    /**
     * Execute logic for exporting Mule project as an archive.
     */
    public boolean performFinish() {
        outputAbsolutePath = name.getText();
        File tempFile = new File(outputAbsolutePath);
        File folder = new File(tempFile.getParent());
        if (!saveModifiedResources()) {
            MessageDialog.openError(this.getContainer().getShell(), "Update site export", "Update site creation failed. There are unsaved resources in the project.");
            return false;
        }
        if (!folder.exists()) {
            boolean success = createFolder(folder);
            if (!success)
                return false;
        }

        File outputFile = createOutputFile(folder.getAbsolutePath(), tempFile.getName());
        if (outputFile.exists()) {
            boolean confirmation = MessageDialog.openQuestion(this.getContainer().getShell(), "Confirmation", outputFile.getAbsolutePath()
                    + " already exists.\nDo you want to replace it?");
            if (confirmation)
                return createUpdateSite(outputFile);
        } else
            return createUpdateSite(outputFile);
        return false;
    }

    private boolean createUpdateSite(final File outputFile) {
        IRunnableWithProgress op = new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    MavenDevkitProjectDecorator mavenProject = MavenDevkitProjectDecorator.decorate(project);
                    new BaseDevkitGoalRunner(new String[] { "clean", "package", "-DskipTests", "-Ddevkit.studio.package.skip=false" },project).run(mavenProject.getPomFile(), monitor);
                    IFile updateSiteFile = project.getProject().getFile("/target/UpdateSite.zip");

                    FileUtils.moveFile(new File(updateSiteFile.getLocationURI()), outputFile);

                } catch (IOException e) {
                    throw new InvocationTargetException(e);

                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", "The update site was not created, check the log console for more details : \n" + realException.getMessage());
            return false;
        }

        return true;
    }

    private boolean saveModifiedResources() {
        List<IEditorPart> dirtyEditors = UiUtils.getDirtyEditors(project.getProject());
        if (dirtyEditors.isEmpty())
            return true;
        SaveModifiedResourcesDialog dialog = new SaveModifiedResourcesDialog(this.getContainer().getShell());
        if (dialog.open(this.getContainer().getShell(), dirtyEditors))
            return true;
        return false;
    }

    private boolean createFolder(File folder) {
        boolean confirmation = MessageDialog.openQuestion(this.getContainer().getShell(), "Confirmation", "Directory " + folder.getAbsolutePath()
                + " does not exist.\nDo you want to create it?");
        if (confirmation) {
            boolean success = folder.mkdirs();
            if (!success) {
                MessageDialog.openError(this.getContainer().getShell(), "Problem Ocurred", "Could not create directory " + folder.getAbsolutePath());
                return false;
            } else
                return true;
        } else
            return false;
    }

    private File createOutputFile(String outputFolder, String outputFileName) {
        String fileName;
        if (outputFileName.endsWith(".zip")) {
            fileName = outputFileName;
        } else {
            fileName = outputFileName + ".zip";
        }
        return new File(outputFolder, fileName);
    }

}
