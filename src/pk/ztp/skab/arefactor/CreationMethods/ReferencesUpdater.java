package pk.ztp.skab.arefactor.CreationMethods;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.core.ResolvedSourceMethod;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;

import pk.ztp.skab.arefactor.Logger.ARefactorLogger;

public class ReferencesUpdater extends SearchRequestor {
	
	private LinkedHashMap<IMethod, MethodDeclaration> creationMethods;
	private ArrayList<IMethod> superConstr = new ArrayList<IMethod>();
	private LinkedHashMap<CompilationUnit, ICompilationUnit> compilationUnits = new LinkedHashMap<CompilationUnit, ICompilationUnit>();
	private ArrayList<RewriteData> rewriteData=new ArrayList<RewriteData>();
	
	private ASTRequestor requestors = new ASTRequestor() {
		@Override
		public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
			compilationUnits.put(ast, source);
		}
	};

	private IMethod searchedMethod;

	public ReferencesUpdater(IJavaProject javaProject,LinkedHashMap<CompilationUnit,ICompilationUnit> compilationUnits,
			IProgressMonitor monitor) {
		this.compilationUnits=compilationUnits;
		
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setProject(javaProject);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		try {
			for (IPackageFragment packFrag : javaProject.getPackageFragments()) {
				parser.createASTs(packFrag.getCompilationUnits(),new String[0], requestors, 
						new SubProgressMonitor(monitor, 3));
			}
		} catch (JavaModelException e) {
			ARefactorLogger.log(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void acceptSearchMatch(SearchMatch arg0) throws CoreException {
		IJavaElement element = (IJavaElement) arg0.getElement();
		ResolvedSourceMethod rsm = (ResolvedSourceMethod) element;
		ICompilationUnit cu = rsm.getCompilationUnit();
		for (CompilationUnit unit : compilationUnits.keySet()) {
			if (compilationUnits.get(unit).equals(cu)) {

				ASTNode result = NodeFinder.perform(unit, arg0.getOffset(),arg0.getLength());
				if (result != null) {
					if (result instanceof SuperConstructorInvocation) {
						SuperConstructorInvocation sci = (SuperConstructorInvocation) result;
						superConstr.add(searchedMethod);
					}
					if (result instanceof ClassInstanceCreation) {
						ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
						ImportRewrite importRewrite = ImportRewrite.create(cu, true);

						ClassInstanceCreation cic = (ClassInstanceCreation) result;
						ASTNode node = cic.getParent();
						TypeDeclaration td = (TypeDeclaration) CreationMethodsRefactoring.getParent(cic, TypeDeclaration.class);
						MethodDeclaration md = (MethodDeclaration) CreationMethodsRefactoring.getParent(cic, MethodDeclaration.class);

						int start = cic.getStartPosition();
						int length = cic.getLength();

						AST ast = cic.getAST();

						MethodDeclaration newMethod = creationMethods.get(searchedMethod);
						MethodInvocation mi = ast.newMethodInvocation();
						mi.setSourceRange(start, length);

						String temp = importRewrite.addImport(
								searchedMethod.getDeclaringType().getFullyQualifiedName());

						mi.setExpression(ast.newSimpleName(temp));
						mi.setName(ast.newSimpleName(newMethod.getName().getIdentifier()));
						
						for (int index = 0; index < cic.arguments().size(); index++) {
							mi.arguments().add(
									rewrite.createMoveTarget((ASTNode) cic.arguments().get(index)));
						}

						ChildListPropertyDescriptor descriptor = null;
						if (td instanceof AbstractTypeDeclaration)
							descriptor = ((AbstractTypeDeclaration) td).getBodyDeclarationsProperty();

						rewrite.replace(cic, mi, null);
						
						RewriteData rd=new RewriteData();
						rd.setAstRewrite(rewrite);
						rd.setImportRewrite(importRewrite);
						rd.setCompilationUnit(cu);
						
						rewriteData.add(rd);
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

	public ArrayList<IMethod> getSuperConstr() {
		return superConstr;
	}
	
	public class RewriteData
	{
		private ASTRewrite astRewrite;
		private ImportRewrite importRewrite;
		private ICompilationUnit compilationUnit;
		
		public ASTRewrite getAstRewrite() {
			return astRewrite;
		}
		public void setAstRewrite(ASTRewrite astRewrite) {
			this.astRewrite = astRewrite;
		}
		public ImportRewrite getImportRewrite() {
			return importRewrite;
		}
		public void setImportRewrite(ImportRewrite importRewrite) {
			this.importRewrite = importRewrite;
		}
		public ICompilationUnit getCompilationUnit() {
			return compilationUnit;
		}
		public void setCompilationUnit(ICompilationUnit compilationUnit) {
			this.compilationUnit = compilationUnit;
		}
	}

	public ArrayList<RewriteData> getRewriteData() {
		return rewriteData;
	}

	public void setRewriteData(ArrayList<RewriteData> rewriteData) {
		this.rewriteData = rewriteData;
	}
}
