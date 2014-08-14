package org.mule.tooling.devkit;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.progress.WorkbenchJob;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

public class DevkitUIPlugin extends AbstractUIPlugin implements IStartup {

    public static final String PLUGIN_ID = "org.mule.tooling.devkit";
    private static final String PREFERENCES_CURRENT_UI_PLUGIN_VERSION = "devkit_ui_preference_current_ui_plugin_version";
    private static DevkitUIPlugin plugin;

    private static final String TEMPLATES_KEY = PLUGIN_ID + ".custom_templates";

    private TemplateStore templateStore;
    private IPreferenceStore preferenceStore;
    private ContextTypeRegistry contextTypeRegistry;

    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        scheduleResetPerspectiveOnUpdateJob();
    }

    private void scheduleResetPerspectiveOnUpdateJob() {
        IPreferenceStore store = getPreferenceStore();

        String storedUiPluginVersion = store.getString(PREFERENCES_CURRENT_UI_PLUGIN_VERSION);
        Version storedVersion = Version.parseVersion(storedUiPluginVersion);
        Version currentVersion = this.getBundle().getVersion();

        if (currentVersion.compareTo(storedVersion) != 0) {
            WorkbenchJob resetPerspectiveJob = new ResetMulePerspectiveJob("Reset perspective on update");
            resetPerspectiveJob.schedule();
            store.setValue(PREFERENCES_CURRENT_UI_PLUGIN_VERSION, currentVersion.toString());
        }
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static DevkitUIPlugin getDefault() {
        return plugin;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public void logError(String message, Exception e) {
        this.getLog().log(new Status(Status.ERROR, PLUGIN_ID, message, e));

    }

    public void logError(String message) {
        this.getLog().log(new Status(Status.ERROR, PLUGIN_ID, message));

    }

    /**
     * Open an error dialog on the given shell.
     * 
     * @param shell
     * @param e
     */
    public static void openError(Shell shell, Exception e) {
        openError(shell, createStatus(IStatus.ERROR, e.getMessage()));
    }

    /**
     * Open an error dialog with the given status.
     * 
     * @param shell
     * @param e
     */
    public static void openError(Shell shell, IStatus e) {
        ErrorDialog.openError(shell, "Error", e.getMessage(), e);
    }

    public static IStatus createStatus(int type, String message) {
        return new Status(type, PLUGIN_ID, message);
    }

    @Override
    public void earlyStartup() {

    }

    public TemplateStore getCodeTemplateStore() {
        if (templateStore == null) {
            final IPreferenceStore store = getPreferenceStore();
            templateStore = new ContributionTemplateStore(getTemplateContextRegistry(), store, TEMPLATES_KEY);
            try {
                templateStore.load();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return templateStore;
    }

    public IPreferenceStore getPreferenceStore() {
        // Create the preference store lazily.
        if (preferenceStore == null) {
            preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, getBundle().getSymbolicName());

        }
        return preferenceStore;
    }
    
    public synchronized ContextTypeRegistry getTemplateContextRegistry() {
        if (contextTypeRegistry == null) {
            ContributionContextTypeRegistry registry= new ContributionContextTypeRegistry("org.mule.tooling.devkit.smartassist");

            TemplateContextType all_contextType= registry.getContextType("org.mule.tooling.devkit.smartassist.contextType");
            all_contextType.addResolver(new GlobalTemplateVariables.Cursor());
            contextTypeRegistry= registry;
        }

        return contextTypeRegistry;
    }
}
