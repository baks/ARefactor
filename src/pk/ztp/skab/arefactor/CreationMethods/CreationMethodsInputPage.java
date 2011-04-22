package pk.ztp.skab.arefactor.CreationMethods;

import java.security.Signature;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.eclipse.core.commands.ITypedParameter;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class CreationMethodsInputPage extends UserInputWizardPage {

	private LinkedHashMap<IType, ArrayList<IMethod>> constructors=new LinkedHashMap<IType, ArrayList<IMethod>>();
	
	public CreationMethodsInputPage(String name,LinkedHashMap<IType,ArrayList<IMethod>> constructors) 
	{
		super(name);
		this.constructors=constructors;
	}

	@Override
	public void createControl(Composite parent) 
	{
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		
		GridLayout layout=new GridLayout();
		layout.numColumns=1;
		result.setLayout(layout);
		
		TabFolder folder=new TabFolder(result,SWT.BORDER);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		for(IType type : this.constructors.keySet())
		{
			CreateTabForClassAndItsConstructors(folder,type.getElementName(),this.constructors.get(type));
		}
		
		folder.pack();
		result.pack();
	}
	
	private void CreateTabForClassAndItsConstructors(TabFolder folder,String className,ArrayList<IMethod> constructors)
	{
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(className);
		
		Composite par=new Composite(folder,SWT.FILL);
		GridLayout tabItemLayout=new GridLayout();
		tabItemLayout.numColumns=2;
		tabItemLayout.makeColumnsEqualWidth=true;
		par.setLayout(tabItemLayout);
		
		Label label=new Label(par,SWT.FILL);
		label.setText("Constructor signature");
		label.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
		
		Label label2=new Label(par,SWT.FILL);
		label2.setText("Proposed creation method name");
		label2.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
		
		for(IMethod method : constructors)
		{
			Text text=new Text(par,SWT.MULTI | SWT.BORDER);
			StringBuilder sb=new StringBuilder();
			for(String paramType : method.getParameterTypes())
			{
				sb.append(org.eclipse.jdt.core.Signature.toString(paramType));
				sb.append(",");
			}
			if(sb.length()>0)
				sb.deleteCharAt(sb.length()-1);
			text.setText(method.getElementName()+ "(" + sb.toString() + ")");
			text.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
			text.setEditable(false);
			
			Text text2=new Text(par,SWT.MULTI | SWT.BORDER);
			text2.setText("create" + method.getElementName());
			text2.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
		}
		
		item.setControl(par);
	}
	

}
