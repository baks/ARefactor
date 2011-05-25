package pk.ztp.skab.arefactor.CreationMethods;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class CreationMethodsInputPage extends UserInputWizardPage 
{

	private LinkedHashMap<IType, ArrayList<CreationMethod>> constructors=new LinkedHashMap<IType, ArrayList<CreationMethod>>();
	
	public CreationMethodsInputPage(String name,LinkedHashMap<IType, ArrayList<CreationMethod>> constructors) 
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
			CreateTabForClassAndItsConstructors(folder,type.getElementName(),this.constructors.get(type));
		
		folder.pack();
		result.pack();
	}
	
	private void CreateTabForClassAndItsConstructors(TabFolder folder,String className,final ArrayList<CreationMethod> constr)
	{
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(className);
		
		Composite composite=new Composite(folder,SWT.FILL);
		GridLayout tabItemLayout=new GridLayout();
		tabItemLayout.numColumns=2;
		tabItemLayout.makeColumnsEqualWidth=true;
		composite.setLayout(tabItemLayout);
		
		Label label=new Label(composite,SWT.FILL);
		label.setText("Constructor signature");
		label.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
		
		Label label2=new Label(composite,SWT.FILL);
		label2.setText("Proposed creation method name");
		label2.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
		
		for(final CreationMethod method : constr)
		{
			createTextControlForConstructorSignature(composite, method.getReplacedMethod());
			createTextControlForCreationMethodName(composite, method, constr);
		}
		item.setControl(composite);
	}
	
	private void createTextControlForConstructorSignature(Composite composite,IMethod method) 
	{
		Text constructorSignature=new Text(composite,SWT.MULTI | SWT.BORDER);
		StringBuilder sb=new StringBuilder();
		for(String paramType : method.getParameterTypes())
		{
			sb.append(org.eclipse.jdt.core.Signature.toString(paramType));
			sb.append(",");
		}
		if(sb.length()>0)
			sb.deleteCharAt(sb.length()-1);
		constructorSignature.setText(method.getElementName()+ "(" + sb.toString() + ")");
		constructorSignature.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
		constructorSignature.setEditable(false);
	}
	
	private void createTextControlForCreationMethodName(Composite composite,final CreationMethod method,
			final ArrayList<CreationMethod> constr) 
	{
		final Text creationMethodName=new Text(composite,SWT.MULTI | SWT.BORDER);
		creationMethodName.setText("create" + method.getReplacedMethod().getElementName());
		setTextForMethod(constr,method,creationMethodName.getText());
		creationMethodName.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
		
		creationMethodName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) 
			{
				setTextForMethod(constr,method,creationMethodName.getText());
			}
		});
	}

	private void setTextForMethod(ArrayList<CreationMethod> constr,CreationMethod cm,String text)
	{
		for(CreationMethod method : constr)
		{
			if(method.getReplacedMethod()==cm.getReplacedMethod())
				method.setName(text);
		}
	}
	

}
