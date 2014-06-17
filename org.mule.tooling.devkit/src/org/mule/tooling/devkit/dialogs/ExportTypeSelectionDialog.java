package org.mule.tooling.devkit.dialogs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.mule.tooling.devkit.common.TestDataModelDto.ExportPolicy;


public class ExportTypeSelectionDialog extends MessageDialog {

    private static final String[] buttons = new String[] {"Update", "Skip", "Replace"};
    
    private static final String TITLE = "File Already Exists";
    private static final String MESSAGE = "The file %s already exist, do you want to replace it?";
    
    private int result = -1;
    
    public ExportTypeSelectionDialog(Shell parentShell, String file) {
        
        super(parentShell, TITLE, null, String.format(MESSAGE, file), MessageDialog.CONFIRM, buttons, 0);
    }
    
    @Override
    public int open() {
        this.result =  super.open();
        return result;
    }
   
    public ExportPolicy getSelectedPolicy(){
        switch (result) {
        case 0:
            return ExportPolicy.UPDATE;
        case 2:
            return ExportPolicy.REPLACE;
        default:
            return ExportPolicy.SKIP;
        }
    }
    
}
