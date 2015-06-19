package org.mule.tooling.devkit.popup.actions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.OperationStatus;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.mule.tooling.core.MuleCorePlugin;
import org.mule.tooling.core.event.MuleModuleManagerRestartedEvent;
import org.mule.tooling.core.module.ModuleContributionManager;
import org.mule.tooling.core.runtime.server.MuleServerManager;
import org.mule.tooling.core.utils.BundleJarFileInspector;
import org.mule.tooling.core.utils.BundleManifestReader;
import org.mule.tooling.devkit.DevkitUIPlugin;
import org.mule.tooling.devkit.common.DevkitUtils;
import org.mule.tooling.devkit.common.URLUtil;
import org.mule.tooling.devkit.maven.BaseDevkitGoalRunner;
import org.mule.tooling.devkit.maven.MavenRunBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class InstallOrUpdateConnector extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IJavaProject selectedProject = getSelectedJavaProject(event);
        if (selectedProject != null && !DevkitUtils.existsUnsavedChanges(selectedProject.getProject())) {
            final String installingPalette = "Installing connector...";
            final WorkspaceJob installOrUpdate = new WorkspaceJob(installingPalette) {

                @Override
                public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
                    try {
                        IStatus status = Status.OK_STATUS;
                        monitor.beginTask(installingPalette, 100);
                        final Integer result = generateUpdateSite(selectedProject, monitor);

                        DevkitUtils.refreshFolder(selectedProject.getProject().getFolder(DevkitUtils.GENERATED_SOURCES_FOLDER), monitor).execute(Status.OK);

                        if (result == BaseDevkitGoalRunner.CANCELED) {
                            status = Status.CANCEL_STATUS;
                        } else if (selectedProject.getProject().getFolder(DevkitUtils.UPDATE_SITE_FOLDER) != null && (result == Status.OK)) {

                            final URI uri = selectedProject.getProject().getFolder(DevkitUtils.UPDATE_SITE_FOLDER).getLocationURI();

                            DevkitUtils.refreshFolder(selectedProject.getProject().getFolder(DevkitUtils.GENERATED_SOURCES_FOLDER), monitor).execute(Status.OK);

                            status = installAtDropinsFolder(selectedProject, monitor, uri);

                        } else {
                            status = new OperationStatus(Status.ERROR, DevkitUIPlugin.PLUGIN_ID, OperationStatus.ERROR,
                                    "Failed to generate Update Site. Check the logs for more details.", null);
                        }
                        return status;
                    } finally {
                        monitor.done();
                    }
                }

            };
            installOrUpdate.setUser(true);
            installOrUpdate.setRule(selectedProject.getProject());
            installOrUpdate.setPriority(Job.LONG);
            installOrUpdate.schedule();

        }
        return Status.OK_STATUS;
    }

    private IJavaProject getSelectedJavaProject(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();
        if (selection != null & selection instanceof IStructuredSelection) {
            Object selected = ((IStructuredSelection) selection).getFirstElement();

            if (selected instanceof IJavaElement) {
                return ((IJavaElement) selected).getJavaProject();
            }
        }
        return null;
    }

    private Integer generateUpdateSite(final IJavaProject selectedProject, final IProgressMonitor monitor) {
        MavenRunBuilder builder = MavenRunBuilder.newMavenRunBuilder().withProject(selectedProject)
                .withArgs(new String[] { "clean", "install", "-DskipTests", "-Ddevkit.studio.package.skip=false" })
                .withTaskName("Generating update site for " + DevkitUtils.getProjectLabel(selectedProject));

        return builder.build().run(monitor);
    }

    private void installOrUpdateBundle(File pluginDir, String synmbolicName) {
        String location = pluginDir.getAbsolutePath();
        try {

            BundleContext bundleContext = DevkitUIPlugin.getDefault().getBundle().getBundleContext();

            Bundle bundle = Platform.getBundle(synmbolicName);
            boolean wasAnUpdated = false;
            if (bundle != null) {
                bundle.update();
                wasAnUpdated = true;
            } else {
                bundle = bundleContext.installBundle(location);
            }

            bundle.start();
            final String title = wasAnUpdated ? "Updated" : "Installed";
            final String symbalicName = bundle.getSymbolicName();
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    MessageDialog.openInformation(null, MessageFormat.format("{0} [{1}] succesfully.", title, symbalicName),
                            MessageFormat.format("The connector was succesfully {0}.", title.toLowerCase()));
                }
            });
        } catch (BundleException e) {
            DevkitUIPlugin.getDefault().logError(MessageFormat.format("Could not install connector at [{0}].\nError: {1}", location, e.getMessage()), e);
        }
    }

    private BundleManifestReader geManifestFromJar(File pluginJar) throws IOException {
        return new BundleJarFileInspector(new JarFile(pluginJar)).getManifest();

    }

    private IStatus installAtDropinsFolder(final IJavaProject selectedProject, final IProgressMonitor monitor, final URI uri) {
        IStatus status = Status.OK_STATUS;
        try {

            File eclipseHome = getEclipseHome();
            File dropins = new File(eclipseHome, "dropins");

            if (!dropins.exists()) {
                if (!dropins.mkdir()) {
                    return new OperationStatus(Status.ERROR, DevkitUIPlugin.PLUGIN_ID, OperationStatus.ERROR, "Could not create dropins folder at [" + dropins.getAbsolutePath()
                            + "]", null);
                }
            }

            unzipPluginOnDropinsFolder(selectedProject, monitor, dropins);

        } catch (IllegalStateException e) {
            status = new OperationStatus(Status.ERROR, DevkitUIPlugin.PLUGIN_ID, OperationStatus.ERROR, "No installable was found at repository: " + uri, e);
        } catch (IOException e) {
            status = new OperationStatus(Status.ERROR, DevkitUIPlugin.PLUGIN_ID, OperationStatus.ERROR, "No installable was found at repository: " + uri, e);
        }
        return status;
    }

    private void unzipPluginOnDropinsFolder(final IJavaProject selectedProject, final IProgressMonitor monitor, File dropins) throws IOException {
        Collection<File> files = FileUtils.listFiles(selectedProject.getProject().getFolder(DevkitUtils.UPDATE_SITE_FOLDER).getFolder("plugins").getLocation().toFile(),
                new String[] { "jar" }, false);
        for (File pluginJarFile : files) {

            BundleManifestReader manifestReader = geManifestFromJar(pluginJarFile);

            File dropinPluginFolder = new File(dropins, manifestReader.getSymbolicName());

            if (dropinPluginFolder.exists()) {
                FileUtils.deleteDirectory(dropinPluginFolder);
            }

            if (DevkitUtils.unzipToFolder(pluginJarFile, dropinPluginFolder)) {
                installOrUpdateBundle(dropinPluginFolder, manifestReader.getSymbolicName());
                reloadPalette();
            }

        }
    }

    private void reloadPalette() {
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                MuleServerManager serverManager = MuleCorePlugin.getServerManager();
                try {
                    ModuleContributionManager.clear();
                    serverManager.initialize();
                } catch (CoreException e) {
                    DevkitUIPlugin.getDefault().logError(e.getMessage(), e);
                }
                MuleCorePlugin.getEventBus().fireEvent(new MuleModuleManagerRestartedEvent(null));
                // Refresh workspace project so that any mule app using this plugin refresh the classpath container
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                try {
                    root.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
                } catch (CoreException e) {
                    DevkitUIPlugin.getDefault().logError(e.getMessage(), e);
                }
            }
        });
    }

    private File getEclipseHome() {
        Location eclipseHome = getService(DevkitUIPlugin.getDefault().getBundle().getBundleContext(), Location.ECLIPSE_HOME_FILTER);
        if (eclipseHome == null || !eclipseHome.isSet())
            return null;
        URL url = eclipseHome.getURL();
        if (url == null)
            return null;
        return URLUtil.toFile(url);
    }

    private Location getService(BundleContext context, String filter) {
        Collection<ServiceReference<Location>> references;
        try {
            references = context.getServiceReferences(Location.class, filter);
        } catch (InvalidSyntaxException e) {
            // TODO Auto-generated catch block
            return null;
        }
        if (references.isEmpty())
            return null;
        final ServiceReference<Location> ref = references.iterator().next();
        Location result = context.getService(ref);
        context.ungetService(ref);
        return result;
    }
}
