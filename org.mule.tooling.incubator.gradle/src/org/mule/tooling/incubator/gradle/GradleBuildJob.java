package org.mule.tooling.incubator.gradle;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.ProjectConnection;

/**
 * Generic implementation of a gradle build as a background workspace task.
 * 
 * This implements the correct sequence for invoking a gradle build.
 * @author juancavallotti
 *
 */
public abstract class GradleBuildJob extends WorkspaceJob {
	
	IProject project;
	String[] tasks;
	String[] arguments;
	
	public GradleBuildJob(String name, IProject project, String... tasks) {
		super(name);
		this.project = project;
		this.tasks = tasks;
	}
	

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {
		ProjectConnection projConnection = null;
		
		try {
			File projectPath = project.getLocation().toFile().getAbsoluteFile();
			projConnection = GradlePluginUtils.buildConnectionForProject(projectPath).connect();
			BuildLauncher build = projConnection.newBuild().forTasks(tasks);
			
			if (arguments != null) {
				GradleRunner.run(build, monitor, arguments);
			} else {
				GradleRunner.run(build, monitor);
			}
			
			project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			return Status.OK_STATUS;
		} catch (Exception ex) {
			handleException(ex);
			return Status.CANCEL_STATUS;
		} finally {
			projConnection.close();
		}
	}
	
	public final void doSchedule() {
        super.setUser(false);
        super.setPriority(Job.DECORATE);
        super.setRule(project);
        super.schedule();
	}
	
	
	public String[] getArguments() {
		return arguments;
	}


	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}
	
	public String[] getTasks() {
		return tasks;
	}


	public void setTasks(String[] tasks) {
		this.tasks = tasks;
	}


	protected abstract void handleException(Exception ex);
	
}
