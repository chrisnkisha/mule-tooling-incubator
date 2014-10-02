package org.mule.tooling.devkit.wizards;

import java.util.regex.Pattern;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.mule.tooling.devkit.common.ConnectorMavenModel;
import org.mule.tooling.devkit.common.DevkitUtils;
import org.mule.tooling.ui.MuleUiConstants;
import org.mule.tooling.ui.utils.UiUtils;

public class NewDevkitProjectWizardPageAdvance extends WizardPage {

    private static String DEFAULT_USER = "mulesoft";

    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    private static final String DEFAULT_ARTIFACT_ID = "hello-connector";
    private static final String DEFAULT_GROUP_ID = "org.mule.modules";
    private static final String GROUP_TITLE_MAVEN_SETTINGS = "Maven Settings";
    private static final String CREATE_POM_LABEL = "Manually set values";
    private final Pattern ownerName = Pattern.compile("^[\\S]+$");
    private ConnectorMavenModel connectorModel;

    protected NewDevkitProjectWizardPageAdvance(ConnectorMavenModel connectorModel) {
        super("Advanced Options");
        setTitle(NewDevkitProjectWizard.WIZZARD_PAGE_TITTLE);
        setDescription("Advanced configuration");
        this.connectorModel = connectorModel;
    }

    private Text owner;
    private Text connection;
    private Text devConnection;
    private Text url;
    private Button manuallyEditCheckBox;
    private Button addGitHubInfo;

    private Text groupId;
    private Text artifactId;
    private Text version;

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);

        addMavenGroup(container);

        addGitHubGroup(container);

        GridLayoutFactory.fillDefaults().numColumns(1).extendedMargins(2, 2, 10, 0).margins(0, 0).spacing(0, 0).applyTo(container);
        GridDataFactory.fillDefaults().indent(0, 0).applyTo(container);
        setControl(container);
    }

    private void addGitHubGroup(Composite container) {
        Group gitHubGroupBox = UiUtils.createGroupWithTitle(container, "GitHub", 2);
        addGitHubInfo = initializeCheckBox(gitHubGroupBox, "Add GitHub information", new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateAllComponentsEnablement();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                updateAllComponentsEnablement();
            }
        });

        owner = initializeTextField(gitHubGroupBox, "GitHub Owner", DEFAULT_USER, "Owner of the repository", new ModifyListener() {

            public void modifyText(ModifyEvent e) {

                if (!ownerName.matcher(owner.getText()).find()) {
                    updateStatus();
                }
                refresh();
            }
        });

        connection = initializeTextField(
                gitHubGroupBox,
                "Connection",
                "scm:git:git://github.com:" + DEFAULT_USER + "/" + connectorModel.getConnectorName().toLowerCase() + ".git",
                "The two connection elements convey to how one is to connect to the version control system through Maven. Where connection requires read access for Maven to be able to find the source code (for example, an update), developerConnection requires a connection that will give write access.",
                null);
        devConnection = initializeTextField(
                gitHubGroupBox,
                "Dev. Connection",
                "scm:git:git@github.com:" + DEFAULT_USER + "/" + connectorModel.getConnectorName().toLowerCase() + "-module" + ".git",
                "The two connection elements convey to how one is to connect to the version control system through Maven. Where connection requires read access for Maven to be able to find the source code (for example, an update), developerConnection requires a connection that will give write access.",
                null);
        url = initializeTextField(gitHubGroupBox, "Url", "http://github.com/" + DEFAULT_USER + "/" + connectorModel.getConnectorName().toLowerCase(),
                "A publicly browsable repository. For example, via ViewCVS.", null);

    }

    private Button initializeCheckBox(Composite parent, String label, SelectionListener listener) {
        final Button checkboxButton = new Button(parent, SWT.CHECK);
        checkboxButton.setSelection(false);
        checkboxButton.setText(" " + label);
        checkboxButton.setLayoutData(GridDataFactory.swtDefaults().span(2, 1).create());
        if (listener != null) {
            checkboxButton.addSelectionListener(listener);
        }

        return checkboxButton;
    }

    private Text initializeTextField(Group groupBox, String labelText, String defaultValue, String tooltip, ModifyListener modifyListener) {
        Label label = new Label(groupBox, SWT.NULL);
        label.setText(labelText);
        label.setLayoutData(GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(MuleUiConstants.LABEL_WIDTH, SWT.DEFAULT).create());
        Text textField = new Text(groupBox, SWT.BORDER);
        textField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        textField.setText(defaultValue);
        textField.setEnabled(false);
        textField.setToolTipText(tooltip);
        if (modifyListener != null) {
            textField.addModifyListener(modifyListener);
        }
        return textField;
    }

    private void updateAllComponentsEnablement() {
        boolean shouldAddInfo = addGitHubInfo.getSelection();
        owner.setEnabled(shouldAddInfo);
        connection.setEnabled(shouldAddInfo);
        devConnection.setEnabled(shouldAddInfo);
        url.setEnabled(shouldAddInfo);
        updateStatus();
    }

    private void updateComponentsEnablement() {
        boolean editManually = manuallyEditCheckBox.getSelection();
        groupId.setEnabled(editManually);
        artifactId.setEnabled(editManually);
        version.setEnabled(editManually);
    }

    public void refresh() {

        artifactId.setText(DevkitUtils.toConnectorName(connectorModel.getConnectorName()) + "-connector");
        connection.setText("scm:git:git://github.com:" + owner.getText() + "/" + DevkitUtils.toConnectorName(connectorModel.getConnectorName()) + ".git");
        devConnection.setText("scm:git:git@github.com:" + owner.getText() + "/" + DevkitUtils.toConnectorName(connectorModel.getConnectorName()) + "-connector" + ".git");
        url.setText("http://github.com/" + owner.getText() + "/" + DevkitUtils.toConnectorName(connectorModel.getConnectorName()));
    }

    public boolean getAddGitHubInfo() {
        return this.addGitHubInfo.getSelection();
    }

    public String getConnection() {
        return this.connection.getText();
    }

    public String getDevConnection() {
        return this.devConnection.getText();
    }

    public String getUrl() {
        return this.url.getText();
    }

    private void addMavenGroup(Composite container) {
        Group mavenGroupBox = UiUtils.createGroupWithTitle(container, GROUP_TITLE_MAVEN_SETTINGS, 2);

        manuallyEditCheckBox = new Button(mavenGroupBox, SWT.CHECK);
        manuallyEditCheckBox.setSelection(false);
        manuallyEditCheckBox.setText(" " + CREATE_POM_LABEL);
        manuallyEditCheckBox.setLayoutData(GridDataFactory.swtDefaults().span(2, 1).create());
        manuallyEditCheckBox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateComponentsEnablement();
                updateStatus();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                updateComponentsEnablement();
                updateStatus();
            }
        });

        ModifyListener groupIdListener = new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                updateComponentsEnablement();
                updateStatus();
            }
        };
        ModifyListener artifactIdListener = new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                updateComponentsEnablement();
                updateStatus();
            }
        };
        ModifyListener versionListener = new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                updateComponentsEnablement();
                updateStatus();
            }
        };
        groupId = initializeTextField(mavenGroupBox, "Group Id: ", DEFAULT_GROUP_ID,
                "This element indicates the unique identifier of the organization or group that created the project.", groupIdListener);
        artifactId = initializeTextField(mavenGroupBox, "Artifact Id: ", DEFAULT_ARTIFACT_ID,
                " This element indicates the unique base name of the primary artifact being generated by this project. ", artifactIdListener);
        version = initializeTextField(mavenGroupBox, "Version: ", DEFAULT_VERSION, "This element indicates the version of the artifact generated by the project.", versionListener);

        mavenGroupBox.layout();
        manuallyEditCheckBox.setSelection(false);

    }

    public String getPackage() {
        return groupId.getText() + "." + connectorModel.getConnectorName().toLowerCase();
    }

    public String getGroupId() {
        return groupId.getText();
    }

    public String getArtifactId() {
        return artifactId.getText();
    }

    public String getVersion() {
        return version.getText();
    }

    private void updateStatus() {
        if (getGroupId().isEmpty()) {
            setErrorMessage("Group Id cannot be empty.");
            setPageComplete(false);
            return;
        }
        if (getArtifactId().isEmpty()) {
            setErrorMessage("Artifact Id cannot be empty.");
            setPageComplete(false);
            return;
        }
        if (getVersion().isEmpty()) {
            setErrorMessage("Version cannot be empty.");
            setPageComplete(false);
            return;
        }
        if (this.getAddGitHubInfo()) {
            if (owner.getText().isEmpty()) {
                setErrorMessage("GitHub Owner cannot be empty.");
                setPageComplete(false);
                return;
            }

            if (!ownerName.matcher(owner.getText()).find()) {
                setErrorMessage("You are using characters that are not allowed for the GitHub Owner.");
                setPageComplete(false);
                return;
            }
        }
        setErrorMessage(null);
        setPageComplete(true);
    }
}
