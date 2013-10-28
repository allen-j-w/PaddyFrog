import java.util.ArrayList;


public class ParameterState {

	private int parameterNum;
	private ArrayList<String> parameterType;
	
	public ParameterState()
	{
		parameterNum = 0;
		parameterType = new ArrayList<String>();
	}
	
	public void setParameterNum(int num)
	{
		parameterNum = num;
	}
	
	public void addParameterType(String[] type)
	{
		for(String typeElement: type)
			parameterType.add(typeElement);
	}
	
	public int getParameterNum()
	{
		return parameterNum;
	}
	
	public String[] getParameterType()
	{
		return parameterType.toArray(new String[0]);
	}
}
