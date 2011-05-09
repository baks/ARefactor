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

public class CreationMethodsInputPage extends UserInputWizardPage {

	private LinkedHashMap<IType, LinkedHashMap<IMethod,String>> constructors=new LinkedHashMap<IType, LinkedHashMap<IMethod,String>>();
	
	public CreationMethodsInputPage(String name,LinkedHashMap<IType, LinkedHashMap<IMethod, String>> constructors) 
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
			CreateTabForClassAndItsConstructors(folder,type.getElementName(),
					this.constructors.get(type));
		}
		
		folder.pack();
		result.pack();
	}
	
	private void CreateTabForClassAndItsConstructors(TabFolder folder,String className,final LinkedHashMap<IMethod,String> constr)
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
		
		for(final IMethod method : constr.keySet())
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
			
			final Text text2=new Text(par,SWT.MULTI | SWT.BORDER);
			text2.setText("create" + method.getElementName());
			constr.put(method, text2.getText());
			text2.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
			text2.addModifyListener(new ModifyListener() {
				
				@Override
				public void modifyText(ModifyEvent arg0) {
					constr.put(method, text2.getText());
				}
			});
		}
		
		item.setControl(par);
	}
	

}
