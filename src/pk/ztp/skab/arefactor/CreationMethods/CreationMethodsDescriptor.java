package pk.ztp.skab.arefactor.CreationMethods;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class CreationMethodsDescriptor extends RefactoringDescriptor {

	public static final String REFACTORING_ID= "pk.ztp.skab.arefactor.CreationMethods";

	private final Map fArguments=null;
	
	protected CreationMethodsDescriptor(String project,String description, String comment, 
			Map arguments) {
		super(REFACTORING_ID, project, description, comment,
				RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus arg0)
			throws CoreException {
		return new CreationMethodsRefactoring();
	}

}
