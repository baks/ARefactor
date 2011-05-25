package pk.ztp.skab.arefactor.popup.actions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
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
	private ICompilationUnit selectedUnit=null;
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
	public void init(IWorkbenchWindow arg0) 
	{
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
		ARefactorLogger.log( "Started searching java project to classes to apply creation methods refactor in project : " + this.project.getElementName());
		Search search=new Search(this.project,this.selectedUnit);
		try 
		{
			if(search.getClassessWithConstructors().isEmpty() || !this.selectedUnit.isStructureKnown())
			{
				MessageDialog.openInformation(shell, "Information", "Can't apply creation methods refactoring to this class");
				return;
			}
		} 
		catch (JavaModelException e) 
		{
			ARefactorLogger.log(e);
		}
		ARefactorLogger.log("End searching classess to apply creation methods refactor in project : " + this.project.getElementName());
		if(fWindow==null)
		{
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					GetWorkbenchWindow();
					
				}
			});
		}
		CreationMethodsRefactoring cmr=new CreationMethodsRefactoring();
		cmr.setUnitToRefactor(this.selectedUnit);
		cmr.setJavaProject(project);
		cmr.setClassConstructors(search.getClassessWithConstructors());
		
		run(new CreationMethodsWizard(cmr, "Creation Methods Refactoring",search.getClassessWithConstructors()),"Creation Methods Refactoring");
	}
	
	public void run(RefactoringWizard wizard, String dialogTitle) 
	{
		try 
		{
			RefactoringWizardOpenOperation operation= new RefactoringWizardOpenOperation(wizard);
			ARefactorLogger.log("Refactoring Wizard Operation created");
			operation.run(fWindow.getShell(), dialogTitle);
		} 
		catch (InterruptedException exception) 
		{
			ARefactorLogger.log("Exception when runing Wizard Open Operation : " + exception.toString());
		}
		catch(Exception ex)
		{
			ARefactorLogger.log("Exception when runing Wizard Open Operation : " + ex.toString());
			final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    ex.printStackTrace(printWriter);
		    ARefactorLogger.log("Exception when runing Wizard Open Operation : " + result.toString());
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) 
	{
		if (selection instanceof IStructuredSelection) {
			   Object[] selectedObjects = ((IStructuredSelection)selection).toArray();
			   for(Object obj : selectedObjects)
			   {
				   if(obj instanceof ICompilationUnit)
				   {
					   ARefactorLogger.log(((ICompilationUnit)obj).getElementName());
					   this.selectedUnit=(ICompilationUnit)obj;
					   this.project=this.selectedUnit.getJavaProject();
				   }
			   }
			}
	}

	@Override
	public void dispose() {}

	private void GetWorkbenchWindow()
	{
		if(fWindow==null)
			fWindow=PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}
}
