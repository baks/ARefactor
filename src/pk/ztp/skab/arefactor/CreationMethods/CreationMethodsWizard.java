package pk.ztp.skab.arefactor.CreationMethods;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class CreationMethodsWizard extends RefactoringWizard {

	private LinkedHashMap<IType,LinkedHashMap<IMethod,String>> constructors;
	
	public CreationMethodsWizard(Refactoring refactoring, String pageTitle, LinkedHashMap<IType, LinkedHashMap<IMethod,String>> constructors) 
	{
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(pageTitle);
		this.constructors=constructors;
	}

	@Override
	protected void addUserInputPages() 
	{
		addPage(new CreationMethodsInputPage("CreationMethodsInputPage",this.constructors));
	}

}
