package pk.ztp.skab.arefactor.CreationMethods;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;

import pk.ztp.skab.arefactor.Logger.ARefactorLogger;

public class CountingSearchRequestor extends SearchRequestor 
{
	private boolean hasMatches=false;
	private String constructorClass;
	
	@Override
	public void acceptSearchMatch(SearchMatch arg0) throws CoreException 
	{
		IJavaElement element=(IJavaElement) arg0.getElement();
		if(element.getElementName().equals(constructorClass) && hasMatches!=true)
			hasMatches=false;
		else
			hasMatches=true;
		
		if(hasMatches==true)
			ARefactorLogger.log("Found in element :" + arg0.getElement().toString());
	}

	public boolean getHasMatches() {
		return hasMatches;
	}

	public void setConstructorClass(String constructorClass) {
		this.constructorClass = constructorClass;
	}

}
