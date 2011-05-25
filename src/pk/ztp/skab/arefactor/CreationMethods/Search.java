package pk.ztp.skab.arefactor.CreationMethods;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import pk.ztp.skab.arefactor.Logger.ARefactorLogger;

public class Search 
{
	private IJavaProject javaProject;
	private IPackageFragment[] packageFragments;
	private ArrayList<IType> proposedClassesToRefactorUsingCreationMethods;
	private IJavaSearchScope projectSearchScope;
	private CountingSearchRequestor searchRequestor;
	private SearchEngine searchEngine=new SearchEngine();
	private ArrayList<IMethod> constructorsForClass;
	private LinkedHashMap<IType, ArrayList<CreationMethod>> classessWithConstructors;
	
	public Search(IJavaProject javaProject,ICompilationUnit unitToRefactor)
	{
		this.javaProject=javaProject;
		projectSearchScope=SearchEngine.createJavaSearchScope(new IJavaElement[] {this.javaProject});
		searchRequestor=new CountingSearchRequestor();
		proposedClassesToRefactorUsingCreationMethods=new ArrayList<IType>();
		classessWithConstructors=new LinkedHashMap<IType, ArrayList<CreationMethod>>();
		findClassWithMoreThanOneConstructor(unitToRefactor);
	}
	
	private void findClassWithMoreThanOneConstructor(ICompilationUnit compilationUnit)
	{
		try 
		{
			for(IType type : compilationUnit.getAllTypes())
			{
				ARefactorLogger.log("Checking type : " + type.getElementName());
				if(type.isClass())
				{
					int count=0;
					ARefactorLogger.log("Searching constructors in class : " + type.getElementName());
					ArrayList<CreationMethod> constructorsForClass=new ArrayList<CreationMethod>();
					for(IMethod method : type.getMethods())
					{
						ARefactorLogger.log("Checking if method is constructor");
						if(method.isConstructor())
						{
							CreationMethod cm=new CreationMethod();
							cm.setName("");
							cm.setReplacedMethod(method);
							constructorsForClass.add(cm);
							count++;
						}
					}
					if(count>1)
					{
						ARefactorLogger.log("Added class to proposed class to apply creation methods refactor : " + type.getElementName());
						proposedClassesToRefactorUsingCreationMethods.add(type);
						classessWithConstructors.put(type,constructorsForClass);
					}
				}
			}
		} 
		catch (JavaModelException e) 
		{
			ARefactorLogger.log(e);
		}
	}
	
	private boolean CheckIfClassConstructorAreReferenced(IMethod method,IType type)
	{
		ARefactorLogger.log("Searching references in project : " + this.javaProject.getElementName() 
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
			ARefactorLogger.log(e);
		}
		if(searchRequestor.getHasMatches())
			return true;
		else
			return false;
	}
	
	public void searchProjectForReferences(SearchPattern pattern,SearchRequestor requestor)
	{
		try 
		{
			searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant()}, 
					this.projectSearchScope, requestor, null);
		} 
		catch (CoreException e) 
		{
			ARefactorLogger.log(e);
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
			ARefactorLogger.log(e);
		}
	}

	public LinkedHashMap<IType, ArrayList<CreationMethod>> getClassessWithConstructors() 
	{
		return classessWithConstructors;
	}

	public void setClassessWithConstructors(
			LinkedHashMap<IType, ArrayList<CreationMethod>> classessWithConstructors) 
	{
		this.classessWithConstructors = classessWithConstructors;
	}
}
