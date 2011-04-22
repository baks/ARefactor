package pk.ztp.skab.arefactor.CreationMethods;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;

import pk.ztp.skab.arefactor.Logger.ARefactorLogger;

public class CountingSearchRequestor extends SearchRequestor 
{
	private boolean hasMatches;
	private String constructorClass;
	
	@Override
	public void acceptSearchMatch(SearchMatch arg0) throws CoreException 
	{
		hasMatches=true;
		IJavaElement element=(IJavaElement) arg0.getElement();
		if(element.getElementName().equals(constructorClass))
			hasMatches=false;
		
		if(hasMatches==true)
			ARefactorLogger.getInstance().log(Level.ALL,"Found in element :" + arg0.getElement().toString());
	}

	public boolean getHasMatches() {
		return hasMatches;
	}

	public void setConstructorClass(String constructorClass) {
		this.constructorClass = constructorClass;
	}

}
