package org.mule.tooling.incubator.gradle.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.mule.tooling.incubator.gradle.Activator;
import org.mule.tooling.incubator.gradle.GradleRunner;
import org.mule.tooling.incubator.gradle.model.GradleProject;
import org.mule.tooling.incubator.gradle.preferences.WorkbenchPreferencePage;
import org.mule.tooling.incubator.gradle.template.TemplateFileWriter;
import org.mule.tooling.incubator.gradle.template.VelocityReplacer;

public class ProjectNewWizard extends Wizard implements INewWizard {

    ProjectNewWizardPage page;

    public ProjectNewWizard() {
        setNeedsProgressMonitor(true);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addPages() {
        page = new ProjectNewWizardPage();
        this.addPage(page);
    }

    @Override
    public IWizardPage getNextPage(IWizardPage currentPage) {

        return null;
    }

    @Override
    public boolean performFinish() {
        final GradleProject gradleProject = page.getProject();
        final String projectName = page.getProjectName();
        final IRunnableWithProgress op = new IRunnableWithProgress() {

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask("Creating project" + gradleProject.getGroupId(), 2);
                try {
                    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

                    IProject project = root.getProject(projectName);
                    if (!project.exists()) {
                        project.create(monitor);
                        project.open(monitor);
                    }
                    TemplateFileWriter fileWriter = new TemplateFileWriter(project, monitor);
                    fileWriter.apply("/templates/build.gradle.tmpl", "build.gradle", new VelocityReplacer(gradleProject));
                    File gradleHome = new File(Activator.getDefault().getPreferenceStore().getString(WorkbenchPreferencePage.GRADLE_HOME_ID));
                    if (!gradleHome.exists()) {
                        throw new RuntimeException("Gradle home has not been configured");
                    }
                    ProjectConnection connection = GradleConnector.newConnector().forProjectDirectory(project.getLocation().toFile().getAbsoluteFile())
                            .useGradleUserHomeDir(gradleHome).connect();

                    GradleRunner.run(connection.newBuild().forTasks("initMuleProject", "studio"), monitor);
                    
                    //needs to be refreshed
                    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                    
                    
                    //programmatically add the src/main/app folder to the build path
                    //this is required until https://github.com/mulesoft-labs/mule-gradle-plugin/issues/13 is fixed
                    addSourceFolder(project, monitor, "src/main/app");
                    
                    connection.close();
                    
                } catch (CoreException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        try {
            getContainer().run(true, true, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException.getMessage());
            return false;
        }
        return true;
    }
    
    private void addSourceFolder(IProject project, IProgressMonitor monitor, String folder) throws JavaModelException {
    	//now it should be a java project
        IJavaProject javaProj = JavaCore.create(project);
    	IPath entry = javaProj.getPath().append("src/main/app");
        IClasspathEntry[] entries = javaProj.getRawClasspath(); 
        IClasspathEntry[] newEntries = new IClasspathEntry[] {JavaCore.newSourceEntry(entry)};
        entries = (IClasspathEntry[]) ArrayUtils.addAll(newEntries, entries);
        javaProj.setRawClasspath(entries, monitor);    	
    }

}
