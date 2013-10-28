
public class ComponentState {
	
	boolean isComponent;
	String componentName;
	
	public ComponentState()
	{
		isComponent = false;
		componentName = null;
	}
	
	public void setIsComponent(boolean isComt)
	{
		isComponent = isComt;
	}
	
	public void setComponentName(String name)
	{
		componentName = name;
	}
	
	public boolean isComt()
	{
		return isComponent;
	}
	
	public String getComponentName()
	{
		return componentName;
	}

}
