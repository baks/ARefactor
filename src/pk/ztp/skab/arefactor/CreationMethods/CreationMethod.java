package pk.ztp.skab.arefactor.CreationMethods;

import org.eclipse.jdt.core.IMethod;

public class CreationMethod 
{

	private IMethod replacedMethod;
	private String name;
	
	public IMethod getReplacedMethod() 
	{
		return replacedMethod;
	}
	
	public void setReplacedMethod(IMethod replacedMethod) 
	{
		this.replacedMethod = replacedMethod;
	}
	
	public String getName() 
	{
		return name;
	}
	
	public void setName(String name) 
	{
		this.name = name;
	}
}
