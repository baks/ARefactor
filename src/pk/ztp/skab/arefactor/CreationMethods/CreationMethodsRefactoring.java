package pk.ztp.skab.arefactor.CreationMethods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
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

import pk.ztp.skab.arefactor.CreationMethods.ReferencesUpdater.RewriteData;
import pk.ztp.skab.arefactor.Logger.ARefactorLogger;

public class CreationMethodsRefactoring extends Refactoring {

	private LinkedHashMap<IType,ArrayList<CreationMethod>> classConstructors;
	private LinkedHashMap<IMethod, MethodDeclaration> creationMethods = new LinkedHashMap<IMethod, MethodDeclaration>();
	private LinkedHashMap<MethodDeclaration, TypeDeclaration> creationMethodsWithTypeDeclaration = new LinkedHashMap<MethodDeclaration, TypeDeclaration>();
	private Map<ICompilationUnit, TextFileChange> codeChanges = new HashMap<ICompilationUnit, TextFileChange>();
	private LinkedHashMap<CompilationUnit, ICompilationUnit> compilationUnits = new LinkedHashMap<CompilationUnit, ICompilationUnit>();
	private IJavaProject javaProject;
	private IProgressMonitor monitor;
	private LinkedHashMap<IType, ArrayList<IMethod>> constructorsUsedInInheritedClass = new LinkedHashMap<IType, ArrayList<IMethod>>();
	private Search referencesSearch;
	
	//NIE ZMIENIAÆ TYPU!!!!
	private LinkedHashMap<IMethod, IMethod> replacedConstructors = new LinkedHashMap<IMethod, IMethod>();
	
	private ICompilationUnit unitToRefactor;
	private HashMap<IMethod,MethodDeclaration> constructorDeclarations=new HashMap<IMethod, MethodDeclaration>();
	private ArrayList<IMethod> constructorsToLeave=new ArrayList<IMethod>();
	
	//CHANGE LINKEDHASHMAP TO HASHMAP!!!!!!!
	
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		try {
			
			arg0.beginTask("Checking preconditions", 1);
			if (classConstructors == null) {
				status.merge(RefactoringStatus.createWarningStatus("No class to apply creation methods"));
			}
		} finally {
			arg0.done();
		}
		return status;
	}
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		
		this.monitor = arg0;
		final RefactoringStatus status = new RefactoringStatus();
		
		try {	
			arg0.beginTask("Creating creation methods", 1);

			IProgressMonitor subMonitor = new SubProgressMonitor(arg0, 1);
			try {			
				subMonitor.beginTask("Compiling source...", 2);
				parseProject(subMonitor,status);		
				UpdateAllReferencesInCompilationUnit();
				CheckConstructors();
			} catch (Exception e) {
				ARefactorLogger.log(e);
			} finally {
				subMonitor.done();
			}
		} finally {

		}
		return status;
	}
	
	private void parseProject(IProgressMonitor subMonitor,final RefactoringStatus status)
	{
		ASTRequestor requestors = new ASTRequestor() {
			@Override
			public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
				try {
					rewriteCompilationUnit(this, source, ast, status);
				} catch (CoreException exception) {
					status.merge(RefactoringStatus.createErrorStatus("Error when rewriting compilation unit"));
					ARefactorLogger.log(exception);
				}
			}
		};
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setProject(this.javaProject);
		parser.setResolveBindings(true);
		
		for(ICompilationUnit unit : this.collectCompilationUnits()) {
			parser.createASTs(new ICompilationUnit[] {unit}, new String[0], 
					requestors,new SubProgressMonitor(subMonitor, 1));
		}
		
	}
	
	private ICompilationUnit[] collectCompilationUnits()
	{
		IType[] types = classConstructors.keySet().toArray(new IType[classConstructors.keySet().size()]);
		ICompilationUnit[] units = new ICompilationUnit[types.length];
		
		int i = 0;
		for (IType type : types) {
			units[i++] = type.getCompilationUnit();
		}
		return units;
	}
	
	private void rewriteCompilationUnit(ASTRequestor requestor,ICompilationUnit unit, CompilationUnit node,
			RefactoringStatus status) throws CoreException {
		
		ASTRewrite astRewrite = ASTRewrite.create(node.getAST());
		ImportRewrite importRewrite = ImportRewrite.create(node, true);

		for (IType type : classConstructors.keySet()) {
			
			IType typeToChange = unit.getType(type.getElementName());
			ASTNode result = NodeFinder.perform(node, typeToChange.getSourceRange());
			
			if (result instanceof TypeDeclaration) {
				TypeDeclaration td = (TypeDeclaration) result;

				ArrayList<CreationMethod> newMethods = classConstructors.get(type);

				for (CreationMethod method : newMethods) {
					
					ITypeBinding classType=getTypeBinding(requestor,method.getReplacedMethod());
					IMethodBinding methodBinding=getMethodBinding(classType,method.getReplacedMethod());
					
					constructorDeclarations.put(method.getReplacedMethod(), 
							(MethodDeclaration)NodeFinder.perform(node,method.getReplacedMethod().getSourceRange()));
					
					MethodDeclaration md = createNewCreationMethodDeclaration(method.getName(),classType,
							methodBinding,method.getReplacedMethod(),astRewrite,importRewrite);

					
					creationMethods.put(method.getReplacedMethod(), md);
					creationMethodsWithTypeDeclaration.put(md, td);
				}
			}
		}
	}
	
	private ITypeBinding getTypeBinding(ASTRequestor requestor,IMethod method)
	{
		ITypeBinding classType = null;
		IBinding[] bindings = requestor.createBindings(new String[] { method.getKey() });
		if (bindings[0] instanceof ITypeBinding) {
			classType = (ITypeBinding) bindings[0];
		}	
		return classType;
	}
	
	private IMethodBinding getMethodBinding(ITypeBinding classType,IMethod method)
	{
		IMethodBinding methodBinding = null;
		IMethodBinding[] methodBindings = classType.getDeclaredMethods();
		for (IMethodBinding mb : methodBindings) {
			if (mb.getJavaElement() instanceof IMethod) {
				IMethod x = (IMethod) mb.getJavaElement();
				if (method.isSimilar(x)) {
					methodBinding = mb;
					break;
				}
			}
		}
		return methodBinding;
	}
	
	private MethodDeclaration createNewCreationMethodDeclaration(String newMethodName,ITypeBinding classType,
			IMethodBinding methodBinding,IMethod method,ASTRewrite astRewrite,ImportRewrite importRewrite)
	{
		MethodDeclaration md = astRewrite.getAST().newMethodDeclaration();
		md.setName(astRewrite.getAST().newSimpleName(newMethodName));

		List<Modifier> modifiers = md.modifiers();
		modifiers.add(astRewrite.getAST().newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		modifiers.add(astRewrite.getAST().newModifier(ModifierKeyword.STATIC_KEYWORD));
		md.setReturnType2(importRewrite.addImport(classType, astRewrite.getAST()));
		copyMethodArguments(importRewrite, method, methodBinding,md, astRewrite.getAST());

		ClassInstanceCreation cic = astRewrite.getAST().newClassInstanceCreation();
		cic.setType(importRewrite.addImport(classType, astRewrite.getAST()));

		copyInvocationParameters(method, cic, astRewrite.getAST());
		copyExceptions(importRewrite, methodBinding, md, astRewrite.getAST());

		ReturnStatement statement = cic.getAST().newReturnStatement();
		statement.setExpression(cic);

		Block methodBlock = astRewrite.getAST().newBlock();
		methodBlock.statements().add(statement);

		md.setBody(methodBlock);
		
		return md;
	}

	private void copyMethodArguments(ImportRewrite rewrite, IMethod method,IMethodBinding methodBinding, 
			MethodDeclaration md, AST ast) {
		String[] names = null;
		try {
			names = method.getParameterNames();
			ITypeBinding[] typesBindings = methodBinding.getParameterTypes();

			for (int i = 0; i < typesBindings.length; i++) {
				SingleVariableDeclaration svd = ast.newSingleVariableDeclaration();
				svd.setName(ast.newSimpleName(names[i]));

				ITypeBinding typeBinding = typesBindings[i];

				if (i == (names.length - 1) && methodBinding.isVarargs()) {
					svd.setVarargs(true);
					if (typesBindings[i].isArray())
						typeBinding = typesBindings[i].getComponentType();
				}
				svd.setType(rewrite.addImport(typeBinding, ast));
				md.parameters().add(svd);
			}
		} catch (JavaModelException e) {
			ARefactorLogger.log(e);
		}
	}
	
	private void copyInvocationParameters(IMethod method,ClassInstanceCreation creation, AST ast) {
		String[] names = null;
		try {
			names = method.getParameterNames();
		} catch (JavaModelException e) {
			ARefactorLogger.log(e);
		}
		for (String element : names)
			creation.arguments().add(ast.newSimpleName(element));
	}
	
	private void copyExceptions(ImportRewrite rewrite, IMethodBinding binding,MethodDeclaration declaration, AST ast) {
		ITypeBinding[] types = binding.getExceptionTypes();
		for (ITypeBinding element : types) {
			declaration.thrownExceptions().add(ast.newName(rewrite.addImport(element)));
		}
	}
	
	private void UpdateAllReferencesInCompilationUnit() {
		if(this.referencesSearch==null)
			this.referencesSearch = new Search(this.javaProject,this.unitToRefactor);
		
		ReferencesUpdater updater = new ReferencesUpdater(this.javaProject,this.compilationUnits,this.monitor);
		updater.setCreationMethods(creationMethods);
		
		for (IType type : classConstructors.keySet()) {		
			ArrayList<CreationMethod> newMethods = classConstructors.get(type);

			try {
				for (CreationMethod method : newMethods) {
					SearchPattern pattern = SearchPattern.createPattern(method.getReplacedMethod(),IJavaSearchConstants.REFERENCES);
					updater.setSearchedMethod(method.getReplacedMethod());
					referencesSearch.searchProjectForReferences(pattern,updater);
				}
				for(RewriteData rd : updater.getRewriteData())
				{
					rewriteAST(rd.getCompilationUnit(),rd.getAstRewrite(),rd.getImportRewrite());
				}
			} catch (Exception e) {
				ARefactorLogger.log(e);
			}

			ArrayList<IMethod> c = updater.getSuperConstr();
			if (c.size() > 0) {
				constructorsUsedInInheritedClass.put(type, c);
			}

		}
	}
	
	private void CheckConstructors() {
		for (IType type : classConstructors.keySet()) {
			
			ArrayList<CreationMethod> constrs = classConstructors.get(type);
			ArrayList<IMethod> otherConstrs=new ArrayList<IMethod>();
			for(CreationMethod c : constrs)
			{
				otherConstrs.add(c.getReplacedMethod());
			}
			for (CreationMethod constr : constrs) {	
				CheckIfConstructorIfSubsetOfOther(constr.getReplacedMethod(), otherConstrs);
			}
		}
		
		ArrayList<IMethod> toDelete=new ArrayList<IMethod>();
		
		for(int i=0; i<replacedConstructors.size(); i++)
		{
			IMethod rep = (IMethod) replacedConstructors.values().toArray()[i];
			while (IsConstructorReplacedByAnother(rep) != false) {
				rep = replacedConstructors.get(rep);
			}
			
			replacedConstructors.put((IMethod) replacedConstructors.keySet().toArray()[i], rep);
		}
		
		for (IMethod deletedConstr : replacedConstructors.keySet()) {
			IMethod rep = replacedConstructors.get(deletedConstr);
			while (IsConstructorReplacedByAnother(rep) != false) {
				rep = replacedConstructors.get(rep);
			}
			
			MethodDeclaration firstDeclaration=this.constructorDeclarations.get(deletedConstr);
			MethodDeclaration secondDeclaration=this.constructorDeclarations.get(rep);
			
			if(!compareMethodBodies(firstDeclaration.getBody(), secondDeclaration.getBody()))
				toDelete.add(deletedConstr);
			
			IMethod m1=CheckMethodBodyForOtherConstructorInvocations(firstDeclaration.getBody());
			
			if(m1!=null)
			{
				if(toDelete.contains(deletedConstr))
					toDelete.remove(deletedConstr);
			}
		}
		
		CheckConstructorsThatDontMatchNamesAndTypes();
		
		for(IMethod m :toDelete)
			replacedConstructors.remove(m);
		
		for(IMethod creationMethod : this.creationMethods.keySet())
		{
			if(!replacedConstructors.containsKey(creationMethod))
			{
				MethodDeclaration constrDeclaration=this.constructorDeclarations.get(creationMethod);
				Block block=constrDeclaration.getBody();
				List<Statement> statements = block.statements();
				for(Statement stmt : statements)
				{
					if(stmt instanceof ConstructorInvocation)
					{
						ConstructorInvocation ci=(ConstructorInvocation)stmt;
						IMethodBinding constructorBinding=ci.resolveConstructorBinding();
						
						for(IMethod m : this.constructorDeclarations.keySet())
						{
							if(constructorBinding==this.constructorDeclarations.get(m).resolveBinding())
							{
								this.constructorsToLeave.add(m);
							}
						}
					}
				}
			}
		}
				
		ArrayList<IMethod> modifiedConstr = new ArrayList<IMethod>();
		for (IType type : classConstructors.keySet()) {
			ArrayList<CreationMethod> constrs = classConstructors.get(type);
			for (CreationMethod constr : constrs) {
				if(replacedConstructors.containsKey(constr.getReplacedMethod()))
					FindCompilationUnitAndChangeConstructorVisibility(constr.getReplacedMethod(), type,modifiedConstr);
			}
		}

		for (IMethod deletedConstr : replacedConstructors.keySet()) {
			IMethod rep = replacedConstructors.get(deletedConstr);
			while (IsConstructorReplacedByAnother(rep) != false) {
				rep = replacedConstructors.get(rep);
			}

			if (!modifiedConstr.contains(rep)) {
				ChangeConstructorVisibilityToPrivate(rep,modifiedConstr);
			}
		}
		
		final ArrayList<MethodDeclaration> rewritedReplaceConstr = new ArrayList<MethodDeclaration>();
		ArrayList<IMethod> notReplaced=new ArrayList<IMethod>();
		for(final IMethod creationMethod : this.creationMethods.keySet())
		{
			if(!replacedConstructors.containsKey(creationMethod))
			{
				boolean change=true;
				if(!this.constructorsUsedInInheritedClass.isEmpty())
				{
				ArrayList<IMethod> list=(ArrayList<IMethod>) this.constructorsUsedInInheritedClass.values().toArray()[0];
				
				for(int i=0; i<list.size(); i++)
				{
					IMethod meth=list.get(i);
					if(meth==creationMethod)
					{
						change=false;
						IType t=null;;
						for(IType type : this.constructorsUsedInInheritedClass.keySet())
						{
							if(this.constructorsUsedInInheritedClass.get(type)==list)
								t=type;
								
						}
						FindCompilationUnitAndChangeConstructorVisibility(creationMethod, t,modifiedConstr);
						
						MethodDeclaration md=this.creationMethods.get(creationMethod);
						if(!rewritedReplaceConstr.contains(md))
						{
							rewritedReplaceConstr.add(md);
						final ICompilationUnit cu = creationMethod.getCompilationUnit();
						if (md != null) {
							md.accept(new ASTVisitor() {
								@Override
								public boolean visit(ClassInstanceCreation node) {
									ASTRewrite rewrite = ASTRewrite.create(node.getAST());
									ImportRewrite importRewrite = null;
									try {
										importRewrite = ImportRewrite.create(cu, true);
									} catch (JavaModelException e) {
										ARefactorLogger.log(e);
									}
									if (importRewrite != null) {

										MethodDeclaration md = (MethodDeclaration) getParent(node, MethodDeclaration.class);
										MethodDeclaration repMethod = creationMethods.get(creationMethod);
										TypeDeclaration td = creationMethodsWithTypeDeclaration.get(md);

										if (td != null) {
											createClassInstanceCreation(td,repMethod,md,node,rewritedReplaceConstr,cu,rewrite,
													importRewrite);
										}

									}
									return super.visit(node);
								}
							});
						}
					}
				}
				}
				}
				if(change)
				{
					ChangeConstructorVisibilityToPrivate(creationMethod, modifiedConstr);
					notReplaced.add(creationMethod);
				}
			}
		}

		
		UpdateReferencesForReplacedConstructors(rewritedReplaceConstr);
		
		
		for(final IMethod notReplacedMethod : notReplaced)
		{
			MethodDeclaration md=this.creationMethods.get(notReplacedMethod);
			if(!rewritedReplaceConstr.contains(md))
			{
				rewritedReplaceConstr.add(md);
			final ICompilationUnit cu = notReplacedMethod.getCompilationUnit();
			if (md != null) {
				md.accept(new ASTVisitor() {
					@Override
					public boolean visit(ClassInstanceCreation node) {
						ASTRewrite rewrite = ASTRewrite.create(node.getAST());
						ImportRewrite importRewrite = null;
						try {
							importRewrite = ImportRewrite.create(cu, true);
						} catch (JavaModelException e) {
							ARefactorLogger.log(e);
						}
						if (importRewrite != null) {

							MethodDeclaration md = (MethodDeclaration) getParent(node, MethodDeclaration.class);
							MethodDeclaration repMethod = creationMethods.get(notReplacedMethod);
							TypeDeclaration td = creationMethodsWithTypeDeclaration.get(md);

							if (td != null) {
								createClassInstanceCreation(td,repMethod,md,node,rewritedReplaceConstr,cu,rewrite,
										importRewrite);
							}

						}
						return super.visit(node);
					}
				});
			}
		}
		}
	}
	
	private boolean CheckIfConstructorIfSubsetOfOther(IMethod constr,ArrayList<IMethod> otherConstructors) {
		String[] paramNames = null;
		try {
			paramNames = constr.getParameterNames();
		} catch (JavaModelException e) {
			ARefactorLogger.log(e);
		}
		String[] paramTypes = constr.getParameterTypes();

		for (IMethod constructor : otherConstructors) {
			if (constr != constructor) {
				String[] otherParamNames = null;
				try {
					otherParamNames = constructor.getParameterNames();
				} catch (JavaModelException e) {
					ARefactorLogger.log(e);
				}
				String[] otherParamTypes = constructor.getParameterTypes();

				int findedNames = 0;
				int index = 0;
				for (String checkParamName : paramNames) {
					int nestedIndex = 0;
					for (String pn : otherParamNames) {
						if (checkParamName.equals(pn)) {
							if (paramTypes[index].equals(otherParamTypes[nestedIndex])) {
								findedNames++;
							}
						}
						nestedIndex++;
					}
					index++;
				}
				if (findedNames != paramNames.length) {
					continue;
				} else {
						//MethodDeclaration firstDeclaration=this.constructorDeclarations.get(constr);
						//MethodDeclaration secondDeclaration=this.constructorDeclarations.get(constructor);
						
						//compareMethodBodies(firstDeclaration.getBody(), secondDeclaration.getBody());
					}
				replacedConstructors.put(constr, constructor);
				return true;
			}
		}
		return false;
	}
	
	private IMethod CheckMethodBodyForOtherConstructorInvocations(Block block)
	{
		List<Statement> statements=block.statements();
		if(statements.size()==1)
		{
			if(statements.get(0) instanceof ConstructorInvocation)
			{
				ConstructorInvocation ci=(ConstructorInvocation) statements.get(0);
				IMethodBinding constructorBinding=ci.resolveConstructorBinding();
				
				for(IMethod m : this.constructorDeclarations.keySet())
				{
					if(constructorBinding==this.constructorDeclarations.get(m).resolveBinding())
					{
						return m;
					}
				}
			}
		}
		else
		{
			for(Statement stmt : statements)
			{
				if(stmt instanceof ConstructorInvocation)
				{
					ConstructorInvocation ci=(ConstructorInvocation)stmt;
					IMethodBinding constructorBinding=ci.resolveConstructorBinding();
					
					for(int i=0; i<this.constructorDeclarations.keySet().size(); i++)
					{
						IMethod m=(IMethod) this.constructorDeclarations.keySet().toArray()[i];
						if(constructorBinding==this.constructorDeclarations.get(m).resolveBinding())
						{
							
							for(IMethod constr : this.constructorDeclarations.keySet())
							{
								MethodDeclaration first=this.constructorDeclarations.get(m);
								if(compareMethodBodies(first.getBody(),this.constructorDeclarations.get(constr).getBody()))
									return m;
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	private void CheckConstructorsThatDontMatchNamesAndTypes()
	{
		for(IMethod m : this.constructorDeclarations.keySet())
		{
			if(!replacedConstructors.containsKey(m))
			{
				IMethod method=CheckMethodBodyForOtherConstructorInvocations(this.constructorDeclarations.get(m).getBody());
				if(method!=null)
				{
					replacedConstructors.put(m, method);
				}
			}
		}
	}
	
	private boolean compareMethodBodies(Block first,Block second)
	{
		List<Statement> firstStatements=first.statements();
		int firstLength=firstStatements.size();
		List<Statement> secondStatements=second.statements();
		ASTMatcher matcher=new ASTMatcher();
		int equalStmt=0;
		for(Statement stmtFromFirst : firstStatements)
		{
			for(Statement stmtFromSecond : secondStatements)
			{
				if(stmtFromFirst.subtreeMatch(matcher, stmtFromSecond))
				{
					equalStmt++;
					break;
				}
			}
		}
		if(equalStmt==firstLength)
			return true;
		else
			return false;
	}
	
	private void FindCompilationUnitAndChangeConstructorVisibility(IMethod constr,IType type,
			ArrayList<IMethod> modifiedConstr)
	{
		ICompilationUnit cu = constr.getCompilationUnit();
		for (CompilationUnit unit : compilationUnits.keySet()) {
			if (compilationUnits.get(unit).equals(cu)) {
				try {
					ASTNode result = NodeFinder.perform(unit,constr.getSourceRange());
					if (result instanceof MethodDeclaration) {
						ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
						ImportRewrite importRewrite = ImportRewrite.create(cu, true);
						MethodDeclaration md = (MethodDeclaration) result;
						TypeDeclaration td = (TypeDeclaration) getParent(md, TypeDeclaration.class);
						ChildListPropertyDescriptor descriptor = null;
						if (td instanceof AbstractTypeDeclaration)
							descriptor = ((AbstractTypeDeclaration) td).getBodyDeclarationsProperty();

						if (constructorsUsedInInheritedClass.get(type) != null 
								&& constructorsUsedInInheritedClass.get(type).contains(constr)) {
							List<Modifier> l = md.modifiers();

							ArrayList<Modifier> publicModifiers = new ArrayList<Modifier>();
							for (Modifier m : l) {
								if (m.isPublic()) {
									publicModifiers.add(m);
								}
							}
							for (Modifier toDelete : publicModifiers) {
								rewrite.getListRewrite(md,MethodDeclaration.MODIFIERS2_PROPERTY).remove(toDelete, null);
							}
							rewrite.getListRewrite(md,MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(
									unit.getAST().newModifier(ModifierKeyword.PROTECTED_KEYWORD),null);
						} else {
							if(!this.constructorsToLeave.contains(constr))
								rewrite.remove(md, null);
							else
							{
								ChangeConstructorVisibilityToPrivate(constr,modifiedConstr);
								return;
							}
						}
						rewriteAST(cu, rewrite, importRewrite);

					}
				} catch (JavaModelException e) {
					ARefactorLogger.log(e);
				}
			}
		}
	}
	
	private void ChangeConstructorVisibilityToPrivate(IMethod rep,ArrayList<IMethod> modifiedConstr)
	{
		ICompilationUnit cu = rep.getCompilationUnit();
		for (CompilationUnit unit : compilationUnits.keySet()) {
			if (compilationUnits.get(unit).equals(cu)) {
				try {
					ASTNode result = NodeFinder.perform(unit, rep.getSourceRange());
					if (result instanceof MethodDeclaration) {
						ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
						ImportRewrite importRewrite = ImportRewrite.create(cu, true);
						MethodDeclaration md = (MethodDeclaration) result;
						TypeDeclaration td = (TypeDeclaration) getParent(md, TypeDeclaration.class);
						ChildListPropertyDescriptor descriptor = null;
						if (td instanceof AbstractTypeDeclaration)
							descriptor = ((AbstractTypeDeclaration) td).getBodyDeclarationsProperty();

						List<Modifier> l = md.modifiers();
						ArrayList<Modifier> publicAndProtected = new ArrayList<Modifier>();
						for (Modifier m : l) {
							if (m.isPublic() || m.isProtected()) {
								publicAndProtected.add(m);
							}
						}
						for (Modifier toDelete : publicAndProtected) {
							rewrite.getListRewrite(md,MethodDeclaration.MODIFIERS2_PROPERTY).remove(toDelete, null);
						}
						rewrite.getListRewrite(md,MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(
								unit.getAST().newModifier(ModifierKeyword.PRIVATE_KEYWORD),null);
						rewriteAST(cu, rewrite, importRewrite);
						modifiedConstr.add(rep);

					}
				} catch (JavaModelException e) {
					ARefactorLogger.log(e);
				}
			}
		}
	}
	
	private boolean IsConstructorReplacedByAnother(IMethod rep) {
		for (IMethod deletedConstr : replacedConstructors.keySet()) {
			if (rep == deletedConstr) {
				return true;
			}
		}
		return false;
	}

	private void UpdateReferencesForReplacedConstructors(final ArrayList<MethodDeclaration> rewritedReplaceConstr) {
		for (IMethod deletedConstr : replacedConstructors.keySet()) {
			IMethod rep = replacedConstructors.get(deletedConstr);
			while (IsConstructorReplacedByAnother(rep) != false) {
				rep = replacedConstructors.get(rep);
			}
			final ICompilationUnit cu = deletedConstr.getCompilationUnit();
			final IMethod repFinal = rep;
			try {
				MethodDeclaration md = creationMethods.get(deletedConstr);
				if (md != null) {
					md.accept(new ASTVisitor() {
						@Override
						public boolean visit(ClassInstanceCreation node) {
							ASTRewrite rewrite = ASTRewrite.create(node.getAST());
							ImportRewrite importRewrite = null;
							try {
								importRewrite = ImportRewrite.create(cu, true);
							} catch (JavaModelException e) {
								ARefactorLogger.log(e);
							}
							if (importRewrite != null) {

								MethodDeclaration md = (MethodDeclaration) getParent(node, MethodDeclaration.class);
								MethodDeclaration repMethod = creationMethods.get(repFinal);
								TypeDeclaration td = creationMethodsWithTypeDeclaration.get(md);

								if (td != null) {
									createClassInstanceCreation(td,repMethod,md,node,rewritedReplaceConstr,cu,rewrite,
											importRewrite);
								}

							}
							return super.visit(node);
						}
					});
				}
			} catch (Exception e) {
				ARefactorLogger.log(e);
			}
		}
	}
	
	private void createClassInstanceCreation(TypeDeclaration td,MethodDeclaration repMethod,MethodDeclaration md,
			ClassInstanceCreation node,ArrayList<MethodDeclaration> rewritedReplaceConstr,
			ICompilationUnit cu,ASTRewrite rewrite,ImportRewrite importRewrite)
	{
		ITypeBinding typeBinding = td.resolveBinding();
		AST ast=node.getAST();
		ClassInstanceCreation newInstance = ast.newClassInstanceCreation();

		newInstance.setType(importRewrite.addImport(typeBinding, ast));
		List<SingleVariableDeclaration> replaceParams = repMethod.parameters();
		List<SingleVariableDeclaration> params = md.parameters();
		node.arguments().clear();
		for (int index = 0; index < replaceParams.size(); index++) {
			SingleVariableDeclaration repSvd = replaceParams.get(index);

			if (CheckIfThisArgExistsInMethod(repSvd, params)) {
				node.arguments().add(ast.newSimpleName(
						replaceParams.get(index).getName().getIdentifier()));
			} else {
				Type test = (Type) Type.copySubtree(ast, repSvd.getType());
				if(test.isPrimitiveType())
				{
					Expression defaultValue=getDefaultValueForPrimitiveType(test,ast);
					node.arguments().add(defaultValue);
					continue;
				}
				
				VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
				vdf.setInitializer(ast.newNullLiteral());
				
				//LOSOWE LITERKI DO NAZW!!!!
				String code = repSvd.getName().getIdentifier().substring(0, 3);
				vdf.setName(ast.newSimpleName(code));
				VariableDeclarationStatement vds = ast.newVariableDeclarationStatement(vdf);
			
				vds.setType(test);
				rewrite.getListRewrite(md.getBody(),Block.STATEMENTS_PROPERTY).insertFirst(
						vds, null);
				node.arguments().add(ast.newSimpleName(code));
			}

		}

		rewrite.getListRewrite(td,td.getBodyDeclarationsProperty()).insertLast(md, null);

		if (!rewritedReplaceConstr.contains(repMethod)) {
			rewrite.getListRewrite(td,td.getBodyDeclarationsProperty()).insertLast(
					repMethod, null);
			rewritedReplaceConstr.add(repMethod);
		}
		rewriteAST(cu, rewrite, importRewrite);
	}
	
	private boolean CheckIfThisArgExistsInMethod(SingleVariableDeclaration svd,List<SingleVariableDeclaration> params) {
		for (SingleVariableDeclaration sv : params) {
			if (svd.getName().getIdentifier().equals(
					sv.getName().getIdentifier())) {
				return true;
			}
		}
		return false;
	}
	
	private Expression getDefaultValueForPrimitiveType(Type type,AST ast)
	{
		PrimitiveType primitiveType=(PrimitiveType)type;
		PrimitiveType.Code code=primitiveType.getPrimitiveTypeCode();
		if(code==PrimitiveType.INT || code==PrimitiveType.BYTE || code==PrimitiveType.SHORT)
		{
			return ast.newNumberLiteral("0");
		}
		else if(code==PrimitiveType.LONG)
		{
			return ast.newNumberLiteral("0L");
		}
		else if(code==PrimitiveType.FLOAT)
		{
			return ast.newNumberLiteral("0.0f");
		}
		else if(code==PrimitiveType.DOUBLE)
		{
			return ast.newNumberLiteral("0.0d");
		}
		else if(code==PrimitiveType.CHAR)
		{
			CharacterLiteral cl=ast.newCharacterLiteral();
			cl.setCharValue('\u0000');
			return cl;
		}
		else if(code==PrimitiveType.BOOLEAN)
		{
			return ast.newBooleanLiteral(false);
		}
		
		return null;
		
	}

	
	@Override
	public Change createChange(IProgressMonitor arg0) throws CoreException,
			OperationCanceledException {
		try {
			arg0.beginTask("Creating change...", 1);
			final Collection<TextFileChange> changes = codeChanges.values();
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					String project = javaProject.getElementName();
					String description="Creation Methods Refactoring";
					Map<String, String> arguments = new HashMap<String, String>();
					
					return new RefactoringChangeDescriptor(new CreationMethodsDescriptor(
							project, description,"comm", arguments));
				}
			};
			return change;
		} finally {
			arg0.done();
		}
	}

	@Override
	public String getName() {
		return "Creation Methods";
	}

	private ChildListPropertyDescriptor typeToBodyDeclarationProperty(IType type, CompilationUnit node) 
	throws JavaModelException {
		ASTNode result = typeToDeclaration(type, node);
		if (result instanceof AbstractTypeDeclaration)
			return ((AbstractTypeDeclaration) result).getBodyDeclarationsProperty();
		else if (result instanceof AnonymousClassDeclaration)
			return AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
		return null;
	}

	private ASTNode typeToDeclaration(IType type, CompilationUnit node)
			throws JavaModelException {
		Name result = (Name) NodeFinder.perform(node, type.getNameRange());
		if (type.isAnonymous())
			return getParent(result, AnonymousClassDeclaration.class);
		return getParent(result, AbstractTypeDeclaration.class);
	}

	private void rewriteAST(ICompilationUnit unit, ASTRewrite astRewrite,
			ImportRewrite importRewrite) {
		try {
			MultiTextEdit edit = new MultiTextEdit();
			TextEdit astEdit = astRewrite.rewriteAST();

			if (!isEmptyEdit(astEdit))
				edit.addChild(astEdit);
			TextEdit importEdit = importRewrite.rewriteImports(new NullProgressMonitor());
			if (!isEmptyEdit(importEdit))
				edit.addChild(importEdit);
			if (isEmptyEdit(edit))
				return;

			TextFileChange change = codeChanges.get(unit);
			if (change == null) {
				change = new TextFileChange(unit.getElementName(), (IFile) unit.getResource());
				change.setTextType("java");
				change.setEdit(edit);
			} else
				change.getEdit().addChild(edit);

			codeChanges.put(unit, change);
		} catch (MalformedTreeException exception) {
			ARefactorLogger.log(exception);
		} catch (IllegalArgumentException exception) {
			ARefactorLogger.log(exception);
		} catch (CoreException exception) {
			ARefactorLogger.log(exception);
		} catch(Exception e) {
			ARefactorLogger.log(e);
		}
	}

	private boolean isEmptyEdit(TextEdit edit) {
		return edit.getClass() == MultiTextEdit.class && !edit.hasChildren();
	}

	public void setClassConstructors(
			LinkedHashMap<IType, ArrayList<CreationMethod>> classConstructors) {
		this.classConstructors = classConstructors;
	}

	public void setJavaProject(IJavaProject javaProject) {
		this.javaProject = javaProject;
	}
	
	public void setUnitToRefactor(ICompilationUnit unitToRefactor) {
		this.unitToRefactor = unitToRefactor;
	}
	
	public static ASTNode getParent(ASTNode node, Class parentClass) {
		do {
			node = node.getParent();
		} while (node != null && !parentClass.isInstance(node));
		return node;
	}
}