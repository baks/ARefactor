package pk.ztp.skab.arefactor.CreationMethods;

import java.beans.Expression;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferFactory;
import org.eclipse.jdt.core.ICodeCompletionRequestor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ICompletionRequestor;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.core.ResolvedSourceMethod;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.actions.ExtractMethodAction;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

import pk.ztp.skab.arefactor.Logger.ARefactorLogger;

public class CreationMethodsRefactoring extends Refactoring {

	private LinkedHashMap<IType,LinkedHashMap<IMethod,String>> classConstructors;
	private LinkedHashMap<IMethod,MethodDeclaration> creationMethods=new LinkedHashMap<IMethod, MethodDeclaration>();
	private Map<ICompilationUnit, TextFileChange> codeChanges= new HashMap<ICompilationUnit, TextFileChange>();
	private IJavaProject javaProject;
	private IProgressMonitor monitor;
	private LinkedHashMap<IType,ArrayList<IMethod>> constructorsToChangeToProtected;
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		this.monitor=arg0;
		final RefactoringStatus status=new RefactoringStatus();
		try
		{
			arg0.beginTask("Creating creation methods", 1);
			
			ASTRequestor requestors= new ASTRequestor() {
				@Override
				public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
					try 
					{
						rewriteCompilationUnit(this, source, ast, status);
					} 
					catch (CoreException exception) 
					{
						exception.toString();
						//RefactoringPlugin.log(exception);
					}
				}
			};
			
			IProgressMonitor subMonitor= new SubProgressMonitor(arg0, 1);
			try 
			{
				subMonitor.beginTask("Compiling source...", 1);

				ASTParser parser= ASTParser.newParser(AST.JLS3);
				parser.setProject(this.javaProject);
				parser.setResolveBindings(true);
				
				IType[] types=classConstructors.keySet().toArray(
						new IType[classConstructors.keySet().size()]);
				ICompilationUnit[] units=new ICompilationUnit[types.length];
				int i=0;
				for(IType type : types)
				{
					units[i++]=type.getCompilationUnit();
				}
				
				parser.createASTs(units,new String[0], requestors, new SubProgressMonitor(subMonitor, 1));
				UpdateAllReferencesInCompilationUnit();
				CheckConstructors();
				parser.toString();
			} 
			catch(Exception e)
			{
				ARefactorLogger.getInstance().log(Level.ALL,e.getMessage());
			}
			finally 
			{
				subMonitor.done();
			}
			
		}
		finally
		{
			
		}
		return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status=new RefactoringStatus();
		try
		{
			arg0.beginTask("Checking preconditions",1);
			
			if(classConstructors==null)
			{
				status.merge(RefactoringStatus.createFatalErrorStatus("No class to apply creation methods"));
			}
		}
		finally
		{
			arg0.done();
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor arg0) throws CoreException,OperationCanceledException 
	{
		try 
		{
			arg0.beginTask("Creating change...", 1);
			final Collection<TextFileChange> changes= codeChanges.values();
			CompositeChange change= new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {

				@Override
				public ChangeDescriptor getDescriptor() {
					String project= javaProject.getElementName();
					//String description= MessageFormat.format("Introduce indirection for ''{0}''", new Object[] { fMethod.getElementName()});
					//String methodLabel= JavaElementLabels.getTextLabel(fMethod, JavaElementLabels.ALL_FULLY_QUALIFIED);
					//String typeLabel= JavaElementLabels.getTextLabel(fType, JavaElementLabels.ALL_FULLY_QUALIFIED);
					//String comment= MessageFormat.format("Introduce indirection for ''{0}'' in ''{1}''", new Object[] { methodLabel, typeLabel});
					Map<String, String> arguments= new HashMap<String, String>();
					//arguments.put(METHOD, fMethod.getHandleIdentifier());
					//arguments.put(TYPE, fType.getHandleIdentifier());
					arguments.put("NAME", "createLoan");
					return new RefactoringChangeDescriptor(new CreationMethodsDescriptor(project,"desc", "comm", arguments));
				}
			};
			return change;
		} 
		finally 
		{
			arg0.done();
		}
	}

	@Override
	public String getName() {
		return "Creation Methods";
	}
	
	private void ExtractMethodFromConstructorReference(IMethod method)
	{
	}
	
	protected void rewriteCompilationUnit(ASTRequestor requestor, ICompilationUnit unit, 
			CompilationUnit node, RefactoringStatus status) throws CoreException 
	{
		ASTRewrite astRewrite= ASTRewrite.create(node.getAST());
		ImportRewrite importRewrite= ImportRewrite.create(node, true);
		
		for(IType type : classConstructors.keySet())
		{
			IType typeToChange=unit.getType(type.getElementName());
			ASTNode result=NodeFinder.perform(node,typeToChange.getSourceRange());
			if(result instanceof TypeDeclaration)
			{
				TypeDeclaration td=(TypeDeclaration)result;
				
				LinkedHashMap<IMethod,String> newMethods=classConstructors.get(type);
				
				for(IMethod method : newMethods.keySet())
				{
					
					IMethodBinding methodBinding= null;
					ITypeBinding classType= null;
					IBinding[] bindings= requestor.createBindings(new String[] { method.getKey()});
					if (bindings[0] instanceof ITypeBinding) {
						classType= (ITypeBinding) bindings[0];
						//if (classType != null)
						//	classType.get
					}
					
					IMethodBinding[] methodBindings=classType.getDeclaredMethods();
					for(IMethodBinding mb : methodBindings)
					{
						
						if(mb.getJavaElement() instanceof IMethod)
						{
							IMethod x=(IMethod) mb.getJavaElement();
							if(method.isSimilar(x))
							{
								methodBinding=mb;
								break;
							}
						}
					}

					//if (methodBinding == null || firstParameterType == null)
					//	return;
					
					copyExceptions();
					
					MethodDeclaration md=astRewrite.getAST().newMethodDeclaration();
					md.setName(astRewrite.getAST().newSimpleName(newMethods.get(method)));
					
					List<Modifier> modifiers=md.modifiers();
					modifiers.add(astRewrite.getAST().newModifier(ModifierKeyword.PUBLIC_KEYWORD));
					modifiers.add(astRewrite.getAST().newModifier(ModifierKeyword.STATIC_KEYWORD));
					md.setReturnType2(importRewrite.addImport(classType, node.getAST()));
					copyMethodArguments(importRewrite,method,methodBinding, md, node.getAST());
					
					ClassInstanceCreation cic=astRewrite.getAST().newClassInstanceCreation();
					cic.setType(importRewrite.addImport(classType,node.getAST()));
					
					copyInvocationParameters(method,cic,astRewrite.getAST());
					
					ReturnStatement statement= cic.getAST().newReturnStatement();
					statement.setExpression(cic);
					
					Block methodBlock=astRewrite.getAST().newBlock();
					methodBlock.statements().add(statement);
					
					md.setBody(methodBlock);
					
					AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) typeToDeclaration(typeToChange, node);
					ChildListPropertyDescriptor descriptor= typeToBodyDeclarationProperty(typeToChange, node);
					
					astRewrite.getListRewrite(declaration, descriptor).insertLast(md, null);
					
					creationMethods.put(method, md);
					
				}
				
				
			}
		}
		/*if (unit.equals(fType.getCompilationUnit()))
			rewriteDeclaringType(requestor, astRewrite, importRewrite, unit, node);
		if (!fUpdateReferences) {
			rewriteAST(unit, astRewrite, importRewrite);
			return;
		}
		for (final Iterator iterator= matches.iterator(); iterator.hasNext();) {
			SearchMatch match= (SearchMatch) iterator.next();
			if (match.getAccuracy() == SearchMatch.A_ACCURATE) {
				ASTNode result= NodeFinder.perform(node, match.getOffset(), match.getLength());
				if (result instanceof MethodInvocation)
					status.merge(rewriteMethodInvocation(requestor, astRewrite, importRewrite, (MethodInvocation) result));
			}
		}*/
		rewriteAST(unit, astRewrite, importRewrite);
	}
	
	private void copyMethodArguments(ImportRewrite rewrite,IMethod method,IMethodBinding methodBinding,
			MethodDeclaration md, AST ast)
	{
		String[] names=null;
		try
		{
			names=method.getParameterNames();
			ITypeBinding[] typesBindings=methodBinding.getParameterTypes();
			
			for(int i=0; i<typesBindings.length; i++)
			{			
				SingleVariableDeclaration svd=ast.newSingleVariableDeclaration();
				svd.setName(ast.newSimpleName(names[i]));
				
				ITypeBinding typeBinding=typesBindings[i];
				
				if (i == (names.length - 1) && methodBinding.isVarargs()) {
					svd.setVarargs(true);
					if (typesBindings[i].isArray())
						typeBinding= typesBindings[i].getComponentType();
				}
				
				svd.setType(rewrite.addImport(typeBinding, ast));
				
				md.parameters().add(svd);
			}	
		}
		catch(JavaModelException e)
		{
			e.printStackTrace();
		}
	}
	
	private void copyInvocationParameters(IMethod method,ClassInstanceCreation creation, AST ast) 
	{
		String[] names=null;
		try
		{
			names = method.getParameterNames();
		}
		catch (JavaModelException e) 
		{
			e.printStackTrace();
		}
		for (String element : names)
			creation.arguments().add(ast.newSimpleName(element));
	}

	private void copyExceptions() {
		// TODO Auto-generated method stub
		
	}
	
	private void CheckConstructors() 
	{
	}

	private ChildListPropertyDescriptor typeToBodyDeclarationProperty(IType type, CompilationUnit node) throws JavaModelException {
		ASTNode result= typeToDeclaration(type, node);
		if (result instanceof AbstractTypeDeclaration)
			return ((AbstractTypeDeclaration) result).getBodyDeclarationsProperty();
		else if (result instanceof AnonymousClassDeclaration)
			return AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;

		Assert.isTrue(false);
		return null;
	}

	private ASTNode typeToDeclaration(IType type, CompilationUnit node) throws JavaModelException {
		Name result= (Name) NodeFinder.perform(node, type.getNameRange());
		if (type.isAnonymous())
			return getParent(result, AnonymousClassDeclaration.class);
		return getParent(result, AbstractTypeDeclaration.class);
	}
	
	private ASTNode getParent(ASTNode node, Class parentClass) {
		do {
			node= node.getParent();
		} while (node != null && !parentClass.isInstance(node));
		return node;
	}
	
	private MethodDeclaration createNewMethod(CompilationUnit node)
	{
		MethodDeclaration md=node.getAST().newMethodDeclaration();
		md.setConstructor(false);
		SimpleName sn=md.getName();
		sn.setIdentifier("createLoan");
		md.setName(sn);
		
		return md;
	}
	
	private IMethodBinding findMethodInType(ITypeBinding type, IMethodBinding binding) {
		if (type.isPrimitive())
			return null;

		IMethodBinding[] methods= type.getDeclaredMethods();
		for (IMethodBinding element : methods) {
			if (element.isSubsignature(binding))
				return element;
		}
		return null;
	}
	
	private Search referencesSearch;
	
	private void UpdateAllReferencesInCompilationUnit()
	{
		ReferencesUpdater updater=new ReferencesUpdater();
		updater.setCreationMethods(creationMethods);
		for(IType type : classConstructors.keySet())
		{		
			LinkedHashMap<IMethod,String> newMethods=classConstructors.get(type);
			
			try
			{
				for(IMethod method : newMethods.keySet())
				{
					SearchPattern pattern=SearchPattern.createPattern(method, IJavaSearchConstants.REFERENCES);
					updater.setSearchedMethod(method);
					referencesSearch.searchProjectForReferences(pattern, updater);
				}
			}
			catch(Exception e)
			{
				e.toString();
			}
			
		}
	}
	
	public class ReferencesUpdater extends SearchRequestor
	{
		private LinkedHashMap<IMethod,MethodDeclaration> creationMethods;
		private LinkedHashMap<CompilationUnit,ICompilationUnit> compilationUnits = new LinkedHashMap<CompilationUnit,ICompilationUnit>();
		private ASTRequestor requestors= new ASTRequestor() {
			@Override
			public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
					compilationUnits.put(ast, source);
			}
		};
		
		private IMethod searchedMethod;
		
		public ReferencesUpdater()
		{
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setProject(javaProject);
			parser.setResolveBindings(true);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			
			try 
			{
				for(IPackageFragment packFrag : javaProject.getPackageFragments())
				{
					parser.createASTs(packFrag.getCompilationUnits(), new String[0], requestors, new SubProgressMonitor(monitor, 3));
				}
			} 
			catch (JavaModelException e) 
			{
				e.printStackTrace();
			}
		}
		
		@SuppressWarnings("unchecked")
		public void acceptSearchMatch(SearchMatch arg0) throws CoreException 
		{
			IJavaElement element=(IJavaElement)arg0.getElement();
			ResolvedSourceMethod rsm=(ResolvedSourceMethod)element;
			ICompilationUnit cu=rsm.getCompilationUnit();
			for(CompilationUnit unit : compilationUnits.keySet())
			{
				if(compilationUnits.get(unit).equals(cu))
				{
				
					ASTNode result = NodeFinder.perform(unit, arg0.getOffset(),arg0.getLength());
					if(result !=null)
					{
						if(result instanceof SuperConstructorInvocation)
						{
							SuperConstructorInvocation sci=(SuperConstructorInvocation)result;
						}
						if(result instanceof ClassInstanceCreation)
						{
							ASTRewrite rewrite=ASTRewrite.create(unit.getAST());
							ImportRewrite importRewrite= ImportRewrite.create(cu, true);
							
							ClassInstanceCreation cic=(ClassInstanceCreation)result;
							ASTNode node=cic.getParent();
							TypeDeclaration td=(TypeDeclaration) getParent(cic, TypeDeclaration.class);
							MethodDeclaration md=(MethodDeclaration) getParent(cic,MethodDeclaration.class);
							
							int start=cic.getStartPosition();
							int length=cic.getLength();
							
							AST ast=cic.getAST();
							
							//cic.delete();
						
							MethodDeclaration newMethod=creationMethods.get(searchedMethod);
							MethodInvocation mi=ast.newMethodInvocation();
							//mi.setName(ast.newSimpleName("createLoan"));
							mi.setSourceRange(start, length);
							mi.setName(ast.newSimpleName(newMethod.getName().getIdentifier()));
						
							
							for (int index= 0; index < cic.arguments().size(); index++)
							{
								mi.arguments().add(rewrite.createMoveTarget((ASTNode) cic.arguments().get(index)));
							}
							
							
							//AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) typeToDeclaration(typeToChange, unit);
							//ChildListPropertyDescriptor descriptor= typeToBodyDeclarationProperty(typeToChange, unit);
							
							ChildListPropertyDescriptor descriptor=null;
							if (td instanceof AbstractTypeDeclaration)
								descriptor= ((AbstractTypeDeclaration) td).getBodyDeclarationsProperty();
							
							//rewrite.getListRewrite(td,descriptor).replace(cic, mi, null);
							//rewrite.getListRewrite(td, descriptor).remove(cic, null);
							//rewrite.getListRewrite(td, descriptor).insertLast(mi, null);
						
							rewrite.replace(cic, mi, null);
							
							rewriteAST(cu, rewrite, importRewrite);
							//else if (td instanceof AnonymousClassDeclaration)
								//descriptor= AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
						}
					}
				}
			}
			
		}

		public void setCreationMethods(
				LinkedHashMap<IMethod, MethodDeclaration> creationMethods) {
			this.creationMethods = creationMethods;
		}

		public void setSearchedMethod(IMethod searchedMethod) {
			this.searchedMethod = searchedMethod;
		}
		
	}
	
	private void rewriteAST(ICompilationUnit unit, ASTRewrite astRewrite, ImportRewrite importRewrite) 
	{
		try 
		{
			MultiTextEdit edit= new MultiTextEdit();
			TextEdit astEdit= astRewrite.rewriteAST();

			if (!isEmptyEdit(astEdit))
				edit.addChild(astEdit);
			//TextEdit importEdit= importRewrite.rewriteImports(new NullProgressMonitor());
			//if (!isEmptyEdit(importEdit))
			//	edit.addChild(importEdit);
			if (isEmptyEdit(edit))
				return;

			TextFileChange change= codeChanges.get(unit);
			if (change == null) {
				change= new TextFileChange(unit.getElementName(), (IFile) unit.getResource());
				change.setTextType("java");
				change.setEdit(edit);
			} else
				change.getEdit().addChild(edit);

			codeChanges.put(unit, change);
		} 
		catch (MalformedTreeException exception) 
		{
			//RefactoringPlugin.log(exception);
		} 
		catch (IllegalArgumentException exception) 
		{
			//RefactoringPlugin.log(exception);
		} 
		catch (CoreException exception) 
		{
			//RefactoringPlugin.log(exception);
		}
	}
	
	private boolean isEmptyEdit(TextEdit edit) {
		return edit.getClass() == MultiTextEdit.class && !edit.hasChildren();
	}

	public void setClassConstructors(
			LinkedHashMap<IType, LinkedHashMap<IMethod, String>> classConstructors) {
		this.classConstructors = classConstructors;
	}

	public void setJavaProject(IJavaProject javaProject) {
		this.javaProject = javaProject;
		this.referencesSearch=new Search(this.javaProject);
	}
	
	

}
