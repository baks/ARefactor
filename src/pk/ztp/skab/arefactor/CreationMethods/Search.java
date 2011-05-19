package pk.ztp.skab.arefactor.CreationMethods;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import pk.ztp.skab.arefactor.Logger.ARefactorLogger;
import pk.ztp.skab.arefactor.popup.actions.CreationMethodsAction;

public class Search 
{
	private IJavaProject javaProject;
	private IPackageFragment[] packageFragments;
	private ArrayList<ICompilationUnit> allUnitsInProject;
	private ArrayList<IType> proposedClassesToRefactorUsingCreationMethods;
	private IJavaSearchScope projectSearchScope;
	private CountingSearchRequestor searchRequestor;
	private SearchEngine searchEngine=new SearchEngine();
	private ArrayList<IMethod> constructorsForClass;
	private LinkedHashMap<IType, LinkedHashMap<IMethod,String>> classessWithConstructors;
	
	public Search(IJavaProject javaProject)
	{
		this.javaProject=javaProject;
		projectSearchScope=SearchEngine.createJavaSearchScope(new IJavaElement[] {this.javaProject});
		allUnitsInProject=new ArrayList<ICompilationUnit>();
		searchRequestor=new CountingSearchRequestor();
		proposedClassesToRefactorUsingCreationMethods=new ArrayList<IType>();
		classessWithConstructors=new LinkedHashMap<IType, LinkedHashMap<IMethod,String>>();
		try 
		{
			this.packageFragments=javaProject.getPackageFragments();
			PopulateCompilationUnits();
			FindClassWithMoreThanOneConstructor();
		} 
		catch (JavaModelException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void PopulateCompilationUnits()
	{
		for(IPackageFragment pkcg : packageFragments)
		{
			try 
			{
				if(pkcg.getKind()==IPackageFragmentRoot.K_SOURCE)
				{
					ARefactorLogger.getInstance().log(Level.ALL,"Checking compilation units in package : " + pkcg.getElementName());
					for(ICompilationUnit compilationUnit : pkcg.getCompilationUnits())
					{
						ARefactorLogger.getInstance().log(Level.ALL,"Added CUnit : " + compilationUnit.getElementName());
						allUnitsInProject.add(compilationUnit);
					}
				}
			} 
			catch (JavaModelException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	private void FindClassWithMoreThanOneConstructor()
	{
		for(ICompilationUnit compilationUnit : allUnitsInProject)
		{
			try 
			{
				for(IType type : compilationUnit.getAllTypes())
				{
					ARefactorLogger.getInstance().log(Level.ALL,"Checking type : " + type.getElementName());
					if(type.isClass())
					{
						int count=0;
						ARefactorLogger.getInstance().log(Level.ALL,"Searching constructors in class : " + type.getElementName());
						LinkedHashMap<IMethod,String> constructorsForClass=new LinkedHashMap<IMethod, String>();
						for(IMethod method : type.getMethods())
						{
							ARefactorLogger.getInstance().log(Level.ALL,"Checking if method is constructor");
							if(method.isConstructor())
							{
								if(CheckIfClassConstructorAreReferenced(method,type))
								{
									constructorsForClass.put(method,"");
									count++;
								}
							}
						}
						if(count>1)
						{
							ARefactorLogger.getInstance().log(Level.ALL,"Added class to proposed class to apply creation methods refactor : " + type.getElementName());
							proposedClassesToRefactorUsingCreationMethods.add(type);
							classessWithConstructors.put(type,constructorsForClass);
						}
					}
				}
			} 
			catch (JavaModelException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	private boolean CheckIfClassConstructorAreReferenced(IMethod method,IType type)
	{
		ARefactorLogger.getInstance().log(Level.ALL,"Searching references in project : " + this.javaProject.getElementName() 
				+ " for constructor from class : " + type.getElementName());
		searchRequestor.setConstructorClass(type.getElementName());
		SearchPattern pattern=SearchPattern.createPattern(method, IJavaSearchConstants.REFERENCES);
		try 
		{
			searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant()}, 
			this.projectSearchScope, searchRequestor, null);
		}
		catch (CoreException e) 
		{
			e.printStackTrace();
		}
		if(searchRequestor.getHasMatches())
			return true;
		else
			return false;
	}
	
	public void searchProjectForReferences(SearchPattern pattern,SearchRequestor requestor)
	{
		//SearchPattern pattern=SearchPattern.createPattern(method, IJavaSearchConstants.REFERENCES);
		try 
		{
			searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant()}, 
					this.projectSearchScope, requestor, null);
		} 
		catch (CoreException e) 
		{
			e.printStackTrace();
		}
	}
	
	public void searchProjectForReferences(SearchPattern pattern,SearchRequestor requestor, IJavaSearchScope scope)
	{
		try 
		{
			searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant()}, 
					scope, requestor, null);
		} 
		catch (CoreException e) 
		{
			e.printStackTrace();
		}
	}

	public LinkedHashMap<IType, LinkedHashMap<IMethod, String>> getClassessWithConstructors() {
		return classessWithConstructors;
	}

	public void setClassessWithConstructors(
			LinkedHashMap<IType, LinkedHashMap<IMethod, String>> classessWithConstructors) {
		this.classessWithConstructors = classessWithConstructors;
	}
}
