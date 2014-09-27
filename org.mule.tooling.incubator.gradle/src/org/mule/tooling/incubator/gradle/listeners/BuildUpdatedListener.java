package org.mule.tooling.incubator.gradle.listeners;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.mule.tooling.core.utils.CoreUtils;
import org.mule.tooling.incubator.gradle.GradlePluginUtils;
import org.mule.tooling.incubator.gradle.jobs.SynchronizeProjectGradleBuildJob;

public class BuildUpdatedListener implements IResourceChangeListener {

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
			visitResource(event.getDelta());
		}
	}
	
	private void visitResource(IResourceDelta delta) {
		try {
			delta.accept(new IResourceDeltaVisitor() {
				
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					String fileName = delta.getResource().getName();
					if ("build.gradle".equals(fileName) || GradlePluginUtils.STUDIO_DEPS_FILE.equals(fileName)) {
						IProject proj = delta.getResource().getProject();
						//trigger the refresh of the project.
						doRefreshProject(proj);
					}
					return true;
				}
			});
		} catch(CoreException ex) {
			ex.printStackTrace();
		}
	}
	
	
	private void doRefreshProject(IProject proj) throws CoreException {
		
		//the project should be a mule project.
		if (!CoreUtils.hasMuleNature(proj)) {
			//this is simply not a mule project. don't bother
			return;
		}
		
		SynchronizeProjectGradleBuildJob refreshProjectJob = new SynchronizeProjectGradleBuildJob(proj) {
			
			@Override
			protected void handleException(Exception ex) {
				ex.printStackTrace();
			}
		};
		refreshProjectJob.doSchedule();
	}
	
}
