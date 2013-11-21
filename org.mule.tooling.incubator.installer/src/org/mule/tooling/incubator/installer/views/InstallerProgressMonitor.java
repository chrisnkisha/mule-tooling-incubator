package org.mule.tooling.incubator.installer.views;

import org.eclipse.core.runtime.NullProgressMonitor;

public class InstallerProgressMonitor extends NullProgressMonitor {

    private static final int COMPLETE = 100;
    private IEventDispatcher dispatcher;
    private String featureId;
    private int totalWork;

    public InstallerProgressMonitor(IEventDispatcher dispatcher, String featureId) {
        super();
        this.dispatcher = dispatcher;
        this.featureId = featureId;
    }

    @Override
    public void beginTask(String name, int totalWork) {
        this.totalWork = COMPLETE / totalWork;
    }

    @Override
    public void done() {
        dispatcher.dispatchEvent("progress", COMPLETE);
    }

    @Override
    public void setCanceled(boolean value) {
        dispatcher.dispatchEvent("canceled", featureId);

    }

    @Override
    public void worked(int work) {
        dispatcher.dispatchEvent("progress", work * totalWork);
    }

}
