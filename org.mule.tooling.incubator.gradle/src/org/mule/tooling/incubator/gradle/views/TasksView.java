package org.mule.tooling.incubator.gradle.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.mule.tooling.core.builder.MuleNature;
import org.mule.tooling.incubator.gradle.GradleBuildJob;
import org.mule.tooling.incubator.gradle.GradlePluginUtils;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view shows data obtained from the model. The sample creates a dummy model on the fly, but a real
 * implementation would connect to the model available either in this or another plug-in (e.g. the workspace). The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be presented in the view. Each view can present the same model objects using different labels and icons, if
 * needed. Alternatively, a single label provider can be shared between views in order to ensure that objects of the same type are presented in the same way everywhere.
 * <p>
 */

public class TasksView extends ViewPart implements ISelectionListener {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "org.mule.tooling.gradle.views.TasksView";

    private TreeViewer viewer;
    private DrillDownAdapter drillDownAdapter;
    private Action doubleClickAction;
    IProject project;

    class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

        private GradleProject invisibleRoot;

        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        public void dispose() {
        }

        public Object[] getElements(Object parent) {
            if (parent.equals(getViewSite())) {
                return getChildren(invisibleRoot);
            }
            return getChildren(parent);
        }

        public Object getParent(Object child) {
            return null;
        }

        public Object[] getChildren(Object parent) {
            if (parent instanceof GradleProject) {
                return GradlePluginUtils.buildFilteredTaskList((GradleProject) parent).toArray();
            }
            return new Object[0];
        }

        public boolean hasChildren(Object parent) {
            if (parent instanceof GradleProject) {
                return getChildren(parent) != null && getChildren(parent).length > 0;
            }
            return false;
        }

    }

    class ViewLabelProvider extends LabelProvider {

        public String getText(Object obj) {
            if (obj instanceof GradleTask) {
                GradleTask task = (GradleTask) obj;

                String text = task.getName();

                if (task.getDescription() != null) {
                    text = text + " - " + task.getDescription();
                }

                return text;
            }
            return obj.toString();
        }

        public Image getImage(Object obj) {
            String imageKey = org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJS_TASK_TSK;
            if (obj instanceof GradleProject)
                imageKey = ISharedImages.IMG_OBJ_FOLDER;
            return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
        }
    }

    class NameSorter extends ViewerSorter {
    }

    /**
     * The constructor.
     */
    public TasksView() {
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     */
    public void createPartControl(Composite parent) {
        PatternFilter filter = new PatternFilter();
        FilteredTree tree = new FilteredTree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL, filter, true);
        viewer = tree.getViewer();
        drillDownAdapter = new DrillDownAdapter(viewer);
        viewer.setContentProvider(new ViewContentProvider());
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setSorter(new NameSorter());
        viewer.setInput(getViewSite());
        makeActions();
        hookContextMenu();
        hookDoubleClickAction();
        contributeToActionBars();
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                TasksView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(new Separator());
    }

    private void fillContextMenu(IMenuManager manager) {
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        Action runAction = new Action("Run with arguments...") {

            @Override
            public void runWithEvent(Event event) {
                InputDialog dialog = new InputDialog(getSite().getShell(), "Task Arguments", "Enter task run arguments", "", null);
                dialog.open();

                if (dialog.getReturnCode() == InputDialog.CANCEL) {
                    return;
                }

                String args = dialog.getValue();

                String[] argsArr = args.split(" ");

                runSelectedTask(argsArr);
            }
        };

        runAction.setEnabled(((IStructuredSelection) viewer.getSelection()).getFirstElement() != null);

        // add the actions.
        manager.add(runAction);
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
    }

    private void makeActions() {

        doubleClickAction = new Action() {

            public void run() {
                runSelectedTask(null);
            }
        };
    }

    private void hookDoubleClickAction() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {

            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        
        if (this.viewer.isBusy()) {
            return;
        }
        
        if (!(selection instanceof TreeSelection)) {
            return;
        }
        
        if (selection.isEmpty())
            return;
        if (!isMuleProject(selection)) {
            return;
        }
        final ISelection currentSelection = selection;
        final String convertingMsg = "Listing gradle tasks...";
        final WorkspaceJob refreshDevkitViewJob = new WorkspaceJob(convertingMsg) {

            @Override
            public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
                if (currentSelection instanceof IStructuredSelection) {
                    Object selected = ((IStructuredSelection) currentSelection).getFirstElement();

                    // sometimes project might be a simple project.
                    if (selected instanceof IJavaProject) {
                        // convert into a java project
                        selected = ((IJavaProject) selected).getProject();
                    }

                    if (selected instanceof IProject) {
                        project = (IProject) selected;

                        final GradleProject gradleProject = GradlePluginUtils.getProjectModelForProject(project);
                        Display.getDefault().asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                viewer.setInput(gradleProject);

                            }

                        });

                    }
                }
                return Status.OK_STATUS;
            }
        };
        refreshDevkitViewJob.setUser(false);
        refreshDevkitViewJob.setPriority(Job.SHORT);
        refreshDevkitViewJob.schedule();
    }

    private void runSelectedTask(String[] arguments) {
        ISelection selection = viewer.getSelection();
        Object obj = ((IStructuredSelection) selection).getFirstElement();

        if (obj == null) {
            return;
        }

        final GradleTask task = (GradleTask) obj;
        GradleBuildJob buildJob = new GradleBuildJob("Running task " + task.getName(), project, task.getName()) {

            @Override
            protected void handleException(Exception ex) {
                MessageDialog.openError(getSite().getShell(), "Task run Error", "Could not run task " + task.getName() + " : " + ex.getMessage());
            }
        };

        buildJob.setArguments(arguments);
        buildJob.doSchedule();
    }

    public void dispose() {
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
    }

    private boolean isMuleProject(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object selected = ((IStructuredSelection) selection).getFirstElement();

            // sometimes project might be a simple project.
            if (selected instanceof IJavaProject) {
                // convert into a java project
                selected = ((IJavaProject) selected).getProject();
            }

            if (selected instanceof IProject) {
                try {
                    if (((IProject) selected).isAccessible()) {
                        if (((IProject) selected).hasNature(MuleNature.NATURE_ID)) {
                            return true;
                        }
                    }
                } catch (CoreException e) {
                    // Ignore all errors
                }
            }
        }
        return false;
    }
}