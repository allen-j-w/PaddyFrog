import java.util.ArrayList;
import java.util.HashMap;


public class AndrMethod {
	
	private HashMap<String, AndrCodeBlock> codeBlocks = new HashMap<String, AndrCodeBlock>();
	private ArrayList<AndrCodeBlock> blockList = new ArrayList<AndrCodeBlock>();
	private AndrClass belongsTo;
	private String[] methodCode;
	private String methodName;
	private ArrayList<AndrMethod> invokes = new ArrayList<>();
	private ArrayList<AndrMethod> invokedby = new ArrayList<>(); 
	private MethodType methodType;
	private boolean isVisited = false;
	
	public AndrMethod(AndrClass cla, String[] code, String name, MethodType methodType)
	{
		belongsTo = cla;
		methodCode = code;
		methodName = name;
		this.methodType = methodType;
		divedeToBlock();
	}
	//系统库中类的方法专用构造函数，库中方法全用一个方法代替^_^
	public AndrMethod(String name, AndrClass aClass)
	{
		belongsTo = aClass;
		methodCode = null;
		methodName = name;
		methodType = MethodType.SYSTEMLIB;
	}
	
	public void setVisitedFlag(boolean flag){
		isVisited = flag;
	}
	
	public boolean getVisitedFlag(){
		return isVisited;
	}
	
	public MethodType getMethodType()
	{
		return methodType;
	}
	
	public void add2Invoke(AndrMethod method)
	{
		invokes.add(method);
	}
	
	public void add2Invokedby(AndrMethod method)
	{
		invokedby.add(method);
	}
	
	public ArrayList<AndrMethod> getInvokes()
	{
		return invokes;
	}
	
	public ArrayList<AndrMethod> getInvokedby()
	{
		return invokedby;
	}
	
	private void divedeToBlock()
	{
		int i = 1;
		while(i < methodCode.length - 1)
		{
			if(i < methodCode.length - 1 && methodCode[i + 1].startsWith(".packed-switch"))
			{
				for(int j = i + 2; j < methodCode.length; j++)
				{
					if(methodCode[j].startsWith(".end packed-switch"))
					{
						makeBlock(i, j);
						i = j + 1;
						break;
					}
				}
			}
			
			if(i < methodCode.length - 1 && methodCode[i + 1].startsWith(".sparse-switch"))
			{
				for(int j = i + 2; j < methodCode.length; j++)
				{
					if(methodCode[j].startsWith(".end sparse-switch"))
					{
						makeBlock(i, j);
						i = j + 1;
						break;
					}
				}
			}
			
			if(i == methodCode.length - 2)
			{
				makeBlock(i, i);
				break;
			}
			
			for(int j = i + 1; j < methodCode.length; j++)
			{
				if(methodCode[j].startsWith("if") || methodCode[j].startsWith("goto") 
						|| methodCode[j].startsWith("sparse-switch") || methodCode[j].startsWith("packed-switch")
						|| methodCode[j].startsWith(".catch") || methodCode[j].startsWith("return"))
				{
					makeBlock(i, j);
					i = j + 1;
					break;
				}
				//方法调用不分块
				if(methodCode[j].startsWith("invoke"))
				{
					if(belongsTo.getProject().classNameSetContains(AndrProject.getCaller(methodCode[j])))
					{//只把调用本程序中所有的类的方法划分，java库android库等其它本程序中没有的调用不划分块
						makeBlock(i, j);
						i = j + 1;
						break;
					}
				}
				
				if(methodCode[j].startsWith(":"))
				{
					makeBlock(i, j - 1);
					i = j;
					break;
				}
				
				if(j == methodCode.length - 2)
				{
					makeBlock(i, j);
					i = j + 1;
					break;
				}
			}
		}
	}
	
	private void makeBlock(int startLine, int endLine)
	{
		String[] co = new String[endLine - startLine + 1];
		for(int i = 0; i < co.length; i++)
		{
			co[i] = methodCode[startLine + i];
		}
		AndrCodeBlock block = new AndrCodeBlock(co, this, startLine, endLine);
		belongsTo.getProject().putInCodeBlockHash(belongsTo.getProject().getBlockNum(), block);
		codeBlocks.put(block.getBlockCode()[0], block);
		blockList.add(block);
	}
	
	public String getMethodName()
	{
		return methodName;
	}
	
	
	public String[] getMethodCode()
	{
		return methodCode;
	}
	
	public AndrClass getBl2Class()
	{
		return belongsTo;
	}
	
	public HashMap<String, AndrCodeBlock> getCodeBlocks()
	{
		return codeBlocks;
	}
	
	public ArrayList<AndrCodeBlock> getCodeBlockList()
	{
		return blockList;
	}

	public String toString()
	{
		StringBuffer buff = new StringBuffer();
		for(String line: methodCode)
		{
			buff.append(line);
			buff.append('\n');
		}
		buff.append("\n\n");
		return buff.toString();
	}
}
