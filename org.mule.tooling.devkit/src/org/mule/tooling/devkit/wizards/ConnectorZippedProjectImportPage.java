package org.mule.tooling.devkit.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.mule.tooling.core.utils.CoreUtils;
import org.mule.tooling.devkit.builder.DevkitBuilder;
import org.mule.tooling.devkit.builder.DevkitNature;
import org.mule.tooling.devkit.builder.ProjectBuilder;
import org.mule.tooling.devkit.builder.ProjectBuilderFactory;
import org.mule.tooling.devkit.common.DevkitUtils;
import org.mule.tooling.devkit.maven.MavenUtils;
import org.mule.tooling.devkit.maven.UpdateProjectClasspathWorkspaceJob;
import org.mule.tooling.maven.runner.SyncGetResultCallback;
import org.mule.tooling.maven.ui.MavenUIPlugin;
import org.mule.tooling.maven.ui.actions.MavenInstallationTester;
import org.mule.tooling.maven.ui.preferences.MavenPreferences;
import org.mule.tooling.ui.common.FileChooserComposite;

/**
 * Page for importing a Mule project from a zip file.
 * 
 */
public class ConnectorZippedProjectImportPage extends WizardPage {

    /** Unique page id */
    public static final String PAGE_ID = "connectorZippedProjectImportPage";

    /** Widget for choosing zip file to import */
    private FileChooserComposite zipChooser;

    /** Widget for project name */
    private Text txtProjectName;

    private boolean mavenFailure;

    public ConnectorZippedProjectImportPage() {
        super(PAGE_ID);
        setTitle("Import Anypoint Connector Project");
        setDescription("Import an Anypoint Connector project from a zip archive.");
    }

    public void createControl(final Composite parent) {
        Composite control = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(1, false);
        control.setLayout(layout);

        Group externalGroup = new Group(control, SWT.NULL);
        externalGroup.setLayout(new GridLayout(2, false));
        externalGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        externalGroup.setText("Zipped Project");

        Label pkgLabel = new Label(externalGroup, SWT.NULL);
        pkgLabel.setText("Zip File:");
        pkgLabel.setToolTipText("Choose the zip file of the project to import.");
        GridData data = new GridData(GridData.BEGINNING);
        data.widthHint = 120;
        pkgLabel.setLayoutData(data);
        zipChooser = new FileChooserComposite(externalGroup, SWT.NULL);
        zipChooser.setFilterExtensions(new String[] { "*.zip" });
        zipChooser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        zipChooser.addSelectionListener(new ISelectionListener() {

            public void selectionChanged(IWorkbenchPart part, ISelection selection) {
                updateSelectedRoot();
            }
        });

        Group rootGroup = new Group(control, SWT.NULL);
        rootGroup.setLayout(new GridLayout(2, false));
        rootGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        rootGroup.setText("Anypoint Connector Project");

        Label pnameLabel = new Label(rootGroup, SWT.NULL);
        pnameLabel.setText("Project Name:");
        pnameLabel.setToolTipText("Choose the name of the Anypoint Connector project to be created.");
        data = new GridData(GridData.BEGINNING);
        data.widthHint = 120;
        pkgLabel.setLayoutData(data);
        txtProjectName = new Text(rootGroup, SWT.BORDER);
        txtProjectName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        txtProjectName.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                updatePageComplete();
            }
        });

        setControl(control);
        testMaven();
        setPageComplete(false);
    }

    /**
     * Update the indicator for whether the page is complete.
     */
    protected void updatePageComplete() {
        if (mavenFailure) {
            setErrorMessage("Maven home is not properly configured. Check your maven preferences.");
            setPageComplete(false);
            return;
        }
        boolean complete = true;
        String projectName = txtProjectName.getText();
        if (StringUtils.isEmpty(zipChooser.getFilePath())) {
            setErrorMessage("A zip file must be chosen.");
            complete = false;
        } else if (StringUtils.isEmpty(projectName)) {
            setErrorMessage("Project name must not be empty.");
            complete = false;
        } else if (StringUtils.contains(projectName, ' ')) {
            setErrorMessage("Project name must not contain spaces.");
            complete = false;
        } else {
            File workspaceDir = CoreUtils.getWorkspaceLocation();
            File newProject = new File(workspaceDir, projectName);
            if (newProject.exists()) {
                setErrorMessage("Selected project name already exists in workspace.");
                complete = false;
            } else {
                setErrorMessage(null);
            }
        }
        setPageComplete(complete);
    }

    /**
     * Updates the project name based on the selected root folder.
     */
    protected void updateSelectedRoot() {
        String root = zipChooser.getFilePath();
        File file = new File(root);
        if (file.exists()) {
            String safeName = getSafeName(file.getName());
            if (!StringUtils.isEmpty(safeName)) {
                if (safeName.endsWith(".zip")) {
                    safeName = safeName.substring(0, safeName.lastIndexOf(".zip"));
                }
                txtProjectName.setText(safeName);
            }
        }
        updatePageComplete();
    }

    /**
     * Gets a safe project name based on a given name.
     * 
     * @param input
     * @return
     */
    protected String getSafeName(String input) {
        return input.toLowerCase().replace(' ', '_');
    }

    /**
     * Perform the import operation.
     * 
     * @param window
     * 
     * @return
     */
    public boolean performFinish() {
        final String zipFileName = zipChooser.getFilePath();
        final File zipFile = new File(zipFileName);
        if (!zipFile.exists()) {
            MessageDialog.openError(getShell(), "Error", "Zip file does not exist.");
            return false;
        }
        if (!zipFile.getName().endsWith(".zip") || !zipFile.isFile()) {
            MessageDialog.openError(getShell(), "Error", "The selection is not a Zip file.");
            return false;
        }

        final String projectName = txtProjectName.getText();
        final File workspaceDir = CoreUtils.getWorkspaceLocation();
        if ((new File(workspaceDir, projectName)).exists()) {
            MessageDialog.openError(getShell(), "Error", "A project with the given name already exists.");
            return false;
        }

        File tempExpandedZipFile = null;
        try {
            tempExpandedZipFile = CoreUtils.unzipFileToTemp(zipFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!hasExpectedProjectStructure(tempExpandedZipFile)) {
            // If the user wants to continue we will just create a project with the devkit nature in worst case scenario.
            MessageDialog.openError(getShell(), "The selected project is not supported",
                    "Check it has a pom.xml and the packaging is [mule-module].\n\nMulti Module projects are not supported.");
            return false;
        }
        File[] files = tempExpandedZipFile.listFiles();
        // Check if the zip has just 1 folder, and the poin is inside of it
        if (files.length == 1 && files[0].isDirectory()) {
            tempExpandedZipFile = files[0];
        }
        // weird mac scenario
        if (files.length == 2) {
            if (files[0].isDirectory() && "__MACOSX".equals(files[1].getName())) {
                tempExpandedZipFile = files[0];
            } else if (files[1].isDirectory() && "__MACOSX".equals(files[0].getName())) {
                tempExpandedZipFile = files[1];
            }
        }
        final File tempExpanded = tempExpandedZipFile;

        final IRunnableWithProgress op = new IRunnableWithProgress() {

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                monitor.beginTask("Importing modules", 100);
                try {
                    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

                    try {
                        IProject project = createProject(projectName, monitor, root, new File(root.getLocation().toFile(), projectName));

                        IJavaProject javaProject = JavaCore.create(root.getProject(projectName));
                        ProjectBuilder generator = ProjectBuilderFactory.newInstance();

                        List<IClasspathEntry> classpathEntries = generator.generateProjectEntries(project, monitor);
                        javaProject.setRawClasspath(classpathEntries.toArray(new IClasspathEntry[] {}), monitor);

                        try {
                            FileUtils.copyDirectory(tempExpanded, new File(root.getLocation().toFile(), projectName));
                            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                        } catch (IOException e1) {
                            throw new RuntimeException(e1.getMessage());
                        }
                        DevkitUtils.configureDevkitAPT(javaProject);

                        UpdateProjectClasspathWorkspaceJob job = new UpdateProjectClasspathWorkspaceJob(javaProject, new String[] { "clean", "compile", "eclipse:eclipse" });
                        job.runInWorkspace(monitor);
                        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                    } catch (CoreException e) {
                        e.printStackTrace();
                    }
                } finally {
                    monitor.worked(100);
                }
                monitor.done();
            }
        };

        return runInContainer(op);
    }

    private boolean hasExpectedProjectStructure(File tempExpandedZipFile) {

        File[] files = tempExpandedZipFile.listFiles();
        // Check if the zip has just 1 folder, and the pom is inside of it
        if (files.length == 1 && files[0].isDirectory()) {
            tempExpandedZipFile = files[0];
        }
        // weird mac scenario
        if (files.length == 2) {
            if (files[0].isDirectory() && "__MACOSX".equals(files[1].getName())) {
                tempExpandedZipFile = files[0];
            } else if (files[1].isDirectory() && "__MACOSX".equals(files[0].getName())) {
                tempExpandedZipFile = files[1];
            }
        }
        File pomFile = new File(tempExpandedZipFile, "pom.xml");
        return pomFile.exists() && MavenUtils.hasValidPom(pomFile);
    }

    private IProject createProject(String artifactId, IProgressMonitor monitor, IWorkspaceRoot root, File folder) throws CoreException {

        IProjectDescription projectDescription = getProjectDescription(root, artifactId, folder);

        return getProjectWithDescription(artifactId, monitor, root, projectDescription);
    }

    protected IProject getProjectWithDescription(String artifactId, IProgressMonitor monitor, IWorkspaceRoot root, IProjectDescription projectDescription) throws CoreException {
        IProject project = root.getProject(artifactId);
        if (!project.exists()) {
            project.create(projectDescription, monitor);
            project.open(monitor);
            project.setDescription(projectDescription, monitor);
        }
        return project;
    }

    private IProjectDescription getProjectDescription(IWorkspaceRoot root, String artifactId, File folder) throws CoreException {

        File projectDescriptionFile = new File(folder, ".project");
        IProjectDescription currentDescription = null;
        ICommand[] commands = null;
        int commandsLength = 0;
        if (projectDescriptionFile.exists()) {
            currentDescription = root.getWorkspace().loadProjectDescription(Path.fromOSString(new File(folder, ".project").getAbsolutePath()));
            commands = currentDescription.getBuildSpec();
            commandsLength = commands.length;
            for (int i = 0; i < commands.length; ++i) {
                if (commands[i].getBuilderName().equals(DevkitBuilder.BUILDER_ID)) {
                    return currentDescription;
                }
            }
        }
        ICommand[] newCommands = new ICommand[commandsLength + 1];
        if (commands != null) {
            System.arraycopy(commands, 0, newCommands, 0, commandsLength);
        }
        IProjectDescription projectDescription = root.getWorkspace().newProjectDescription(artifactId);
        projectDescription.setNatureIds(new String[] { JavaCore.NATURE_ID, DevkitNature.NATURE_ID });

        ICommand command = projectDescription.newCommand();
        command.setBuilderName(DevkitBuilder.BUILDER_ID);
        newCommands[newCommands.length - 1] = command;

        projectDescription.setBuildSpec(newCommands);

        // projectDescription.setLocation(Path.fromOSString(folder.getAbsolutePath()));

        return projectDescription;
    }

    protected void testMaven() {
        mavenFailure = false;
        MavenPreferences preferencesAccessor = MavenUIPlugin.getDefault().getPreferences();
        final MavenInstallationTester mavenInstallationTester = new MavenInstallationTester(preferencesAccessor.getMavenInstallationHome());
        mavenInstallationTester.test(new SyncGetResultCallback() {

            @Override
            public void finished(final int result) {
                super.finished(result);
                Display.getDefault().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        onTestFinished(result);
                    }
                });
            }
        });

    }

    void onTestFinished(final int result) {
        mavenFailure = result != 0;
        updatePageComplete();
    }

    private boolean runInContainer(final IRunnableWithProgress work) {
        try {
            getContainer().run(true, true, work);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException.getMessage());
            return false;
        }

        return true;
    }
}