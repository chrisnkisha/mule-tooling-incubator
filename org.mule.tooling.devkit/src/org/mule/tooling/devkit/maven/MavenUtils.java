package org.mule.tooling.devkit.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.mule.tooling.devkit.popup.actions.DevkitCallback;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

public class MavenUtils {

    public static void runMavenGoalJob(final IProject selectedProject, final String[] mavenCommand, final String jobMsg, final DevkitCallback callback) {

        final WorkspaceJob changeClasspathJob = new WorkspaceJob(jobMsg) {

            @Override
            public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
                monitor.beginTask(jobMsg, 100);
                IJavaProject javaProject = JavaCore.create(selectedProject);

                int result = new BaseDevkitGoalRunner(mavenCommand, javaProject).run(monitor);

                if (callback != null) {
                    callback.execute(result);
                }
                return Status.OK_STATUS;
            }
        };
        changeClasspathJob.setUser(true);
        changeClasspathJob.setRule(selectedProject);
        changeClasspathJob.setPriority(Job.SHORT);
        changeClasspathJob.schedule();
    }

    public static String getProjectScmUrl(File pom) {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pom));
            if (model.getScm() != null)
                return StringUtils.remove(model.getScm().getDeveloperConnection(), "scm:git:");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return "";
    }
}
