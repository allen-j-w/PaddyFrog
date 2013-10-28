import java.util.LinkedList;


public class ComponentNode {
	private String name;
	private LinkedList<ComponentNode> to;
	private String type;
	private LinkedList<AndrIntentFilter> filter = new LinkedList<AndrIntentFilter>();
	private int number;
	
	public ComponentNode(String name, String type)
	{
		this.name = name;
		to = new LinkedList<ComponentNode>();
		this.type = type;
	}
	
	public void setNumber(int number)
	{
		this.number = number;
	}
	
	public int getNumber()
	{
		return this.number;
	}
	
	public void addFilter(AndrIntentFilter filter)
	{
		this.filter.add(filter);
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public boolean contains(AndrIntentFilter filter)
	{
		return this.filter.contains(filter);
	}
	
	public LinkedList<ComponentNode> getLinkedTo()
	{
		return to;
	}
	
	public void addLinkedTo(ComponentNode node)
	{
		to.add(node);
	}

	public String getName()
	{
		return name;
	}
	
	public String getType()
	{
		return type;
	}
	
	public boolean containsDefaultCategory()
	{
		for(AndrIntentFilter flt: filter)
		{
			if(flt.getType() == IntentFilterType.category && flt.getAttribute().equals("android.intent.category.DEFAULT"))
				return true;
		}
		return false;
	}
	
	
	@Override
	public boolean equals(Object arg0) {
		// TODO Auto-generated method stub
		if(arg0.getClass() == this.getClass())
		{
			ComponentNode node = (ComponentNode)arg0;
			if(this.getName().equals(node.getName()))
				return true;
		}
		return false;
	}

	public LinkedList<String> getActions()
	{
		LinkedList<String> actions = new LinkedList<>();
		for(AndrIntentFilter flt:filter)
		{
			if(flt.getType() == IntentFilterType.action)
				actions.add(flt.getAttribute());
		}
		return actions;
	}
}
