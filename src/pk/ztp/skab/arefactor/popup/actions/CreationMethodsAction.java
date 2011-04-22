package pk.ztp.skab.arefactor.popup.actions;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import pk.ztp.skab.arefactor.CreationMethods.CreationMethodsRefactoring;
import pk.ztp.skab.arefactor.CreationMethods.CreationMethodsWizard;
import pk.ztp.skab.arefactor.CreationMethods.Search;
import pk.ztp.skab.arefactor.Logger.ARefactorLogger;

public class CreationMethodsAction implements IWorkbenchWindowActionDelegate 
{
	private IJavaProject project=null;
	private Shell shell;
	private IWorkbenchWindow fWindow= null;
	
	/**
	 * Constructor for Action1.
	 */
	public CreationMethodsAction() 
	{
		super();
	}
	
	@Override
	public void init(IWorkbenchWindow arg0) {
		fWindow=arg0;
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) 
	{
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) 
	{
		ARefactorLogger.getInstance().log(Level.ALL, "Started searching java project to classes to apply creation methods refactor in project : " + this.project.getElementName());
		Search search=new Search(this.project);
		ARefactorLogger.getInstance().log(Level.ALL,"End searching classess to apply creation methods refactor in project : " + this.project.getElementName());
		/*MessageDialog.openInformation(
				shell,
				"ARefactor",
				"Creation Methods was executed.");*/
		if(fWindow==null)
		{
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					GetWorkbenchWindow();
					
				}
			});
		}
		run(new CreationMethodsWizard(new CreationMethodsRefactoring(), "Baks - test",search.getClassessWithConstructors()),"Baks - test");
	}
	
	public void run(RefactoringWizard wizard, String dialogTitle) {
		try 
		{
			RefactoringWizardOpenOperation operation= new RefactoringWizardOpenOperation(wizard);
			ARefactorLogger.getInstance().log(Level.ALL,"Refactoring Wizard Operation created");
			operation.run(fWindow.getShell(), dialogTitle);
		} 
		catch (InterruptedException exception) 
		{
			ARefactorLogger.getInstance().log(Level.ALL,"Exception when runing Wizard Open Operation : " + exception.toString());
		}
		catch(Exception ex)
		{
			ARefactorLogger.getInstance().log(Level.ALL,"Exception when runing Wizard Open Operation : " + ex.toString());
			final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    ex.printStackTrace(printWriter);
		    ARefactorLogger.getInstance().log(Level.ALL,"Exception when runing Wizard Open Operation : " + result.toString());

		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			   Object[] selectedObjects = ((IStructuredSelection)selection).toArray();
			   for(Object obj : selectedObjects)
			   {
				   if(obj instanceof IProject)
				   {
					   ARefactorLogger.getInstance().log(Level.ALL,((IProject)obj).getName());
					   if(checkIfProjectIsJavaProject((IProject)obj))
					   {
						   this.project=JavaCore.create((IProject)obj);
						   ARefactorLogger.getInstance().log(Level.ALL,"Selection Changed to Java project : " + this.project.getElementName());
					   }
				   }
			   }
			}
	}
	
	private Boolean checkIfProjectIsJavaProject(IProject project)
	{
		try 
		{
			if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) 
			{
				return true;
			}
		} 
		catch (CoreException e) 
		{
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	private void GetWorkbenchWindow()
	{
		if(fWindow==null)
			fWindow=PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}
}
