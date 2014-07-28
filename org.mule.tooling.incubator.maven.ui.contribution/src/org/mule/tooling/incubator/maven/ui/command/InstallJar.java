package org.mule.tooling.incubator.maven.ui.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedOutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.mule.tooling.core.utils.VMUtils;
import org.mule.tooling.incubator.maven.ui.view.InstallJarDialog;
import org.mule.tooling.maven.MavenPlugin;
import org.mule.tooling.maven.cmdline.MavenCommandLine;
import org.mule.tooling.maven.runner.MavenRunner;
import org.mule.tooling.maven.runner.MavenRunnerBuilder;
import org.mule.tooling.maven.runner.SyncGetResultCallback;
import org.mule.tooling.maven.ui.MavenUIPlugin;
import org.mule.tooling.maven.ui.preferences.MavenPreferences;

public class InstallJar extends AbstractHandler {

    private MavenRunner mavenRunner;

    org.apache.maven.artifact.Artifact mavenArtifact;
    File jarFile;
    File pomFile;

    public void cancelBuild() {
        mavenRunner.cancelBuild();
    }

    public void run(IProgressMonitor monitor) {
        final MavenRunnerBuilder mavenRunnerBuilder = new MavenRunnerBuilder();
        MavenPreferences preferencesAccessor = MavenUIPlugin.getDefault().getPreferences();
        mavenRunnerBuilder.setMavenInstallationHome(preferencesAccessor.getMavenInstallationHome());
        mavenRunnerBuilder.addMavenOpts(preferencesAccessor.getMavenOpts());
        mavenRunnerBuilder.setJavaHome(VMUtils.getDefaultJvmHome(null));
        final PipedOutputStream pipedOutputStream = new PipedOutputStream();

        mavenRunner = mavenRunnerBuilder.build();
        SyncGetResultCallback callback = new SyncGetResultCallback();

        try {

            runCommand(jarFile, pomFile, pipedOutputStream, callback);

            while (callback.getResult(100) == SyncGetResultCallback.STILL_NOT_FINISHED) {
                if (monitor.isCanceled()) {
                    this.cancelBuild();
                    break;
                }
            }

        } catch (InterruptedException e) {
            MavenPlugin.logWarning("Maven build interrupted", e);
        } finally {
            if (monitor != null)
                monitor.done();
        }
    }

    protected void runCommand(File jarFile, File pomFile, final PipedOutputStream pipedOutputStream, SyncGetResultCallback callback) {
        StringBuilder commandString = new StringBuilder();
        commandString.append(" install:install-file -Dfile=" + jarFile.getAbsolutePath());
        JarFile artifact;
        try {
            artifact = new JarFile(jarFile);
            ZipEntry entry = artifact.getEntry("META-INF/maven/");
            for (Enumeration<JarEntry> e = artifact.entries(); e.hasMoreElements();) {
                JarEntry item = e.nextElement();
                System.out.println(item.getName());
                if (item.getName().startsWith("META-INF/maven/") && item.getName().endsWith("pom.xml")) {
                    InputStream plugin = artifact.getInputStream(item);
                    BufferedReader in = new BufferedReader(new InputStreamReader(plugin));
                    Model model = new MavenXpp3Reader().read(in);
                    MavenXpp3Writer writter = new MavenXpp3Writer();
                    File tempFile = File.createTempFile("pom", ".xml");
                    FileWriter fWriter = new FileWriter(tempFile);
                    writter.write(fWriter, model);
                    pomFile = tempFile;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e1) {
            e1.printStackTrace();
        }
        if (pomFile != null) {
            commandString.append(" -DpomFile=" + pomFile.getAbsolutePath());
        } else {
            commandString.append("  -DgroupId=" + mavenArtifact.getGroupId());
            commandString.append("  -DartifactId=" + mavenArtifact.getArtifactId());
            commandString.append("  -Dversion=" + mavenArtifact.getVersion());
        }
        commandString.append("  -Dpackaging=jar");
        mavenRunner.runBare(MavenCommandLine.fromString(commandString.toString()), callback, pipedOutputStream);

    }

    protected void runCommand(String command, final PipedOutputStream pipedOutputStream, SyncGetResultCallback callback) {
        mavenRunner.runBare(MavenCommandLine.fromString(command), callback, pipedOutputStream);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        InstallJarDialog dialog = new InstallJarDialog(null);
        if (dialog.open() == 0) {
            pomFile = dialog.getPomFile();
            if (pomFile == null) {
                mavenArtifact = dialog.getArtifact();
                dialog.getPomFile();
            }
            jarFile = dialog.getJarFile();

            Job job = new Job("Installing jar") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    InstallJar.this.run(monitor);
                    return Status.OK_STATUS;
                }

            };
            job.schedule();
        }

        return null;
    }

}
