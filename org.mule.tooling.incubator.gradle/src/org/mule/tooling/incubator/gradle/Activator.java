package org.mule.tooling.incubator.gradle;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.mule.tooling.incubator.gradle.preferences.WorkbenchPreferencePage;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.mule.tooling.incubator.gradle"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		applyDefaultValues();
	}

	private void applyDefaultValues() {
		
		//apply default values to various settings.
		IPreferenceStore prefsStore = getPreferenceStore();
		
		//set the default gradle plugin version.
		prefsStore.setDefault(WorkbenchPreferencePage.GRADLE_PLUGIN_VERSION_ID, GradlePluginConstants.DEFAULT_PLUGIN_VERSION);
		prefsStore.setDefault(WorkbenchPreferencePage.GRADLE_VERSION_ID, GradlePluginConstants.RECOMMENDED_GRADLE_VERSION);
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
