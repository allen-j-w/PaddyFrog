import java.util.ArrayList;


public class AndrCodeBlock {
	
	private ArrayList<AndrCodeBlock> calledFrom = new ArrayList<AndrCodeBlock>();
	private ArrayList<AndrCodeBlock> call = new ArrayList<AndrCodeBlock>();
	private String[] blockCode;
	private AndrMethod belongTo;
	private int beginLine;
	private int endLine;
	private boolean visited = false;
	
	public AndrCodeBlock(String[] code, AndrMethod belongsTo, int beginLine, int endLine)
	{
		blockCode = code;
		belongTo = belongsTo;
		this.beginLine = beginLine;
		this.endLine = endLine;
	}
	
	public int getBeginLine()
	{
		return beginLine;
	}
	
	public int getEndLine()
	{
		return endLine;
	}
	
	public String[] getBlockCode()
	{
		return blockCode;
	}
	
	public AndrMethod getMethod()
	{
		return belongTo;
	}
	
	public ArrayList<AndrCodeBlock> getCaller()
	{
		return calledFrom;
	}
	
	public ArrayList<AndrCodeBlock> getCalls()
	{
		return call;
	}
	
	public void addCaller(AndrCodeBlock codeBlock)
	{
		calledFrom.add(codeBlock);
	}
	
	public void addCalls(AndrCodeBlock codeBlock)
	{
		call.add(codeBlock);
	}
	
	public String toString()
	{
		StringBuffer out = new StringBuffer();
		for(String line: blockCode)
		{
			out.append(line);
			out.append("\n");
		}
		return out.toString();
	}
	
	public void setVisitedFlag(boolean flag)
	{
		visited = flag;
	}
	
	public boolean getVisitedFlag()
	{
		return visited;
	}

}
