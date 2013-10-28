import java.util.ArrayList;
import java.util.HashMap;


public class AndrClass {
	
	private ArrayList<String> staticVariables = new ArrayList<String>();
	private ArrayList<String> instanceVariables = new ArrayList<String>();
	private HashMap<String, AndrMethod> methods = new HashMap<String, AndrMethod>();
	private String className;
	private String superClassName;
	private String[] classCode;
	private AndrProject belongsTo;
	private ArrayList<AndrClass> subclasses = new ArrayList<>();
	private AndrClass superClass;
	private boolean vulnerable = false;
	private ArrayList<AndrClass> invokedBy = new ArrayList<>();
	private ArrayList<AndrClass> invokes = new ArrayList<>();
	
	public AndrClass(String[] code, AndrProject project)
	{
		classCode = code;
		belongsTo = project;
		classAnalyze();
	}
	//系统库中的类专用构造函数=_=!，系统库中的类全部用一个代替。
	public AndrClass(String name, AndrProject project)
	{
		belongsTo = project;
		className = name;
		superClassName = null;
		classCode = null;
		superClass = null;
	}
	
	public void addInvokedComponent(AndrClass invoked)
	{
		invokedBy.add(invoked);
	}
	
	public void addInvokes(AndrClass invoker)
	{
		invokes.add(invoker);
	}
	
	public ArrayList<AndrClass> getInvokedBy()
	{
		return invokedBy;
	}
	
	public ArrayList<AndrClass> getInvokes()
	{
		return invokes;
	}
	
	public boolean isVulnerable()
	{
		return vulnerable;
	}
	
	public void setIsVulnerable(boolean state)
	{
		vulnerable = state;
	}
	
	public AndrClass setHierarchy()
	{
		for(String line: classCode)
		{//将实现的接口作为父类，因为都为抽象方法，都需要到子类中寻找方法
			if(line.startsWith(".implements"))
			{
				String classname = line.substring(line.lastIndexOf(' ') + 1);
				if(belongsTo.classNameSetContains(classname))
					belongsTo.setInterfaceHierarchy(classname, this);
			}
		}
		if(!belongsTo.setHierarchy(superClassName, this))
			System.out.println(superClassName + "set hierarchy failed");
		return this;
	}
	
	public AndrClass setSuperClass(AndrClass superClass)
	{
		this.superClass = superClass;
		return this;
	}
	
	public AndrClass getSuperClass()
	{
		return superClass;
	}
	
	public AndrClass addSubClass(AndrClass aClass)
	{
		subclasses.add(aClass);
		return this;
	}
	
	public ArrayList<AndrClass> getSubClass()
	{
		return subclasses;
	}
	
	public AndrProject getProject()
	{
		return belongsTo;
	}
	private void classAnalyze()
	{
		//className后保留";"
		className = classCode[0].substring(classCode[0].lastIndexOf(" ") + 1);
		superClassName = classCode[1].substring(classCode[1].lastIndexOf(" ") + 1);
		int i = 2;
		while(i < classCode.length)
		{
			if(i < classCode.length && classCode[i].startsWith("# static fields"))
			{
				i++;
				while(i < classCode.length && classCode[i].startsWith(".field"))
				{
					staticVariables.add(classCode[i]);
					i++;
				}
			}
			else if(i < classCode.length && classCode[i].startsWith("# instance fields"))
			{
				i++;
				while(i < classCode.length && classCode[i].startsWith(".field"))
				{
					instanceVariables.add(classCode[i]);
					i++;
				}
			}
			else if(i < classCode.length && classCode[i].startsWith("# direct methods"))
			{
				i++;
				while(i < classCode.length && classCode[i].startsWith(".method"))
				{
					if(containNative(classCode[i]))//native方法不处理
						i = makeMethods(i, MethodType.NATIVE);
					else
						i = makeMethods(i, MethodType.DIRECT);
				}
			}
			else if(i < classCode.length && classCode[i].startsWith("# virtual methods"))
			{
				i++;
				while(i < classCode.length && classCode[i].startsWith(".method"))
				{
					if(containNative(classCode[i]))//native方法不处理,虚方法不处理
						i = makeMethods(i, MethodType.NATIVE);
					else
					{
						if(containAbstract(classCode[i]))
							i = makeMethods(i, MethodType.ABSTRACT);
						else
							i = makeMethods(i, MethodType.VIRTUAL);
					}
				}
			}
			else i++;
				
		}
	}
	
	private boolean containNative(String line)
	{
		String[] st = line.split(" ");
		for(int i = 0; i < st.length; i++)
			if(st[i].equals("native"))
				return true;
		return false;
	}
	
	private boolean containAbstract(String line)
	{
		String[] st = line.split(" ");
		for(int i = 0; i < st.length; i++)
			if(st[i].equals("abstract"))
				return true;
		return false;
	}
	
	private int makeMethods(int startLine, MethodType methodType)
	{
		int i = startLine;
		ArrayList<String> methCod = new ArrayList<String>();
		do
		{
			methCod.add(classCode[i]);
			i++;
		}
		while(!classCode[i - 1].startsWith(".end method"));
		String[] meCo = methCod.toArray(new String[0]);
		AndrMethod method = new AndrMethod(this, meCo, className + "->" + meCo[0].substring(meCo[0].lastIndexOf(" ") + 1),
				methodType);
		belongsTo.putInMethodHash(method.getMethodName(), method);
		methods.put(method.getMethodName(), method);
		return i;
	}
	
	public String getClassName()
	{
		return className;
	}
	
	public String getSuperClassName()
	{
		return superClassName;
	}
	
	public ArrayList<String> getStaticVariables()
	{
		return staticVariables;
	}
	
	public ArrayList<String> getInstanceVariables()
	{
		return instanceVariables;
	}
	
	public HashMap<String, AndrMethod> getMethods()
	{
		return methods;
	}
	
	public String[] getClassCode()
	{
		return classCode;
	}
	

}
