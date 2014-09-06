package org.mule.tooling.incubator.gradle;

public class GradlePluginConstants {
	
	/**
	 * The default plugin version to use with the templates. 
	 */
	public static final String DEFAULT_PLUGIN_VERSION = "1.0.0";
	
	
	/**
	 * This is the version of gradle that will get downloaded in case of no installation is provided.
	 */
	public static final String RECOMMENDED_GRADLE_VERSION = "1.12";
	
	
	/**
	 * If selected gradle version is equals to this constant, then gradle home should be used.
	 */
	public static final String USE_GRADLE_HOME_VERSION_VALUE = "@usehome";
}
