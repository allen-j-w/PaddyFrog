
public class AndrIntentFilter {

	private IntentFilterType type;
	private String attribute;
	private Data data;
	
	public AndrIntentFilter(IntentFilterType type)
	{
		this.type = type;
		if(!type.equals(IntentFilterType.data))
			data = null;
		else
			attribute = "invalid";
	}
	
	public IntentFilterType getType()
	{
		return type;
	}
	
	public String getAttribute()
	{
		return attribute;
	}
	
	public Data getData()
	{
		return data;
	}
	
	public boolean setAttribute(String attribute)
	{
		if(type.equals(IntentFilterType.data))
			return false;
		this.attribute = attribute;
		return true;
	}

}
