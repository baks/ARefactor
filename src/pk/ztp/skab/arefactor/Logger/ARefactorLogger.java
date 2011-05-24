package pk.ztp.skab.arefactor.Logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import pk.ztp.skab.arefactor.AutomaticRefactorPlugin;
import pk.ztp.skab.arefactor.popup.actions.CreationMethodsAction;

public class ARefactorLogger 
{
    private ARefactorLogger() {}
 
    public static void log(Exception e) {
    	AutomaticRefactorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR,"ARefactor",e.getMessage(),e));
    }
    
    public static void log(String s) {
    	AutomaticRefactorPlugin.getDefault().getLog().log(new Status(IStatus.INFO,"ARefactor",s));
    }
}
