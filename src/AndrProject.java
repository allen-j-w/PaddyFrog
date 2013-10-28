import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class AndrProject {
	
	private String projectPath;
	private HashMap<String, AndrClass> classHash;
	private HashMap<String, AndrMethod> methodHash;
	private HashMap<Integer, AndrCodeBlock> codeBlockHash;
	private HashSet<String> classNameSet;
	private XMLPreprocesser xmlprocesser;
	private static int blockNum = 0;
	private String recordPath = "E:\\apks\\record";
	private PrintWriter vulLoger;

	public AndrProject(String path, PrintWriter writer)
	{
		vulLoger = writer;
		projectPath = path;
		File f = new File(projectPath);
		xmlprocesser = new XMLPreprocesser(projectPath);
		instantita();
		makeClassSet(f);
		
		System.out.println("make class name set......OK. class num " + classNameSet.size());
		
		makeClassHash(f);
		
//		System.out.println("make class hash table......OK. class num " + classHash.size());
//		System.out.println("make method hash table......OK. methods num " + methodHash.size());
//		System.out.println("divide code block......OK. code block num " + codeBlockHash.size());
		
		addSystemClass();
		
		buildClassHierarchy();
//		System.out.println("class hierarchy building ...... done.");
		
		checkAndAddMethods2Hash();
		
		System.out.println("done");
	}
	
	public int getBlockNum()
	{
		return blockNum++;
	}
	
	public String getProjPath()
	{
		return projectPath;
	}
	
	//在找到调用
	public boolean existUnsafeComponentWithPermissonCheck()
	{
		AndrMethod method;
		for(String key: methodHash.keySet())
		{
			method = methodHash.get(key);
			if(method.getMethodType() == MethodType.DIRECT || method.getMethodType() == MethodType.VIRTUAL)
				if(existOpenComInvokeWithPermissionCheck(method))
					return true;
		}
		return false;
	}
	
	private boolean existOpenComInvokeWithPermissionCheck(AndrMethod method) {
		// TODO Auto-generated method stub
		String[] code = method.getMethodCode();
		String caller;
		HashSet<LinkedList<AndrMethod>> paths;
		for(String line: code)
		{
			if(line.startsWith("invoke"))
			{
				caller = line.substring(line.lastIndexOf(',') + 2);
				if(SensitiveFunction.isSensitiveFunction(caller))
				{
//					System.out.println(caller);
					paths = findPathsBetwnSnsMthAndCom(method);
					if(!paths.isEmpty())
					{
						return checkPermission(paths);
					}
				}
			}
		}
//		System.out.println("no path");
		return false;
	}
	
	private boolean checkPermission(HashSet<LinkedList<AndrMethod>> paths)
	{//思路，open的com到敏感函数之间建立一个路径，路径个每个函数检查下有没有调用权限检查函数
		HashSet<AndrMethod> allfunc = new HashSet<>();
		HashSet<AndrMethod> set1;
		for(LinkedList<AndrMethod> list: paths){
			for(AndrMethod mth: list){
				set1 = getInvoke(mth);
				allfunc = mergeset(allfunc, set1);
			}
			if(isPathVul(allfunc))
				return true;
		}
		return false;
	}
	
	private HashSet<AndrMethod> mergeset(HashSet<AndrMethod> set1, HashSet<AndrMethod> set2){
		for(AndrMethod mth: set2)
			if(!set1.contains(mth))
				set1.add(mth);
		return set1;
	}
	
	private boolean isPathVul(HashSet<AndrMethod> list)
	{
		String[] code;
		String invoked;
		for(AndrMethod method: list)
		{
			if(method.getMethodType() == MethodType.DIRECT || method.getMethodType() == MethodType.VIRTUAL){
				code = method.getMethodCode();
				for(String line: code)
				{
					if(line.startsWith("invoke"))
					{
						invoked = line.substring(line.lastIndexOf(',') + 2);
	//					System.out.println(invoked);
						if(PermissionCheckFunction.isPermissionCheckFunction(invoked))
						{
	//						for(AndrCodeBlock bk: list)
	//							System.out.println(bk.getMethod().getMethodName());
							vulLoger.println(method.getBl2Class().getProject().getProjPath());
							vulLoger.println("permission check ");
							vulLoger.flush();
//							System.out.println(method.getMethodName() + "+++++++++++");
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	private HashSet<LinkedList<AndrMethod>> findPathsBetwnSnsMthAndCom(AndrMethod mth)
	{
		HashSet<LinkedList<AndrMethod>> paths = new HashSet<>();
		LinkedList<AndrMethod> list = new LinkedList<>();
		list.add(mth);
		clearMethodVisitFlag();
		getAPath(mth, list, paths);
		return paths;
	}
	
	private void clearMethodVisitFlag(){
		for(String key: methodHash.keySet())
			methodHash.get(key).setVisitedFlag(false);
	}
	
	@SuppressWarnings("unchecked")
	private void getAPath(AndrMethod method, LinkedList<AndrMethod> list, HashSet<LinkedList<AndrMethod>> paths)
	{
		method.setVisitedFlag(true);
		for(AndrMethod mth: method.getInvokedby())
		{
			if(mth.getBl2Class().isVulnerable())
			{
				list.add(mth);
				paths.add((LinkedList<AndrMethod>)list.clone());
//				System.out.println("path found");
			}
			else if(!mth.getVisitedFlag())
			{
				list.add(mth);
				getAPath(mth, list, paths);
			}
			else return;
			list.removeLast();
		}
		
	}

	//是不是如果确定了不安全就可以结束了？还有啥要求没，比如输出个什么信息
	//如果存在开放组件调用敏感函数的情况就被认为不安全
	public boolean existUnsafeComponentWithoutPermissionCheck()
	{
		AndrMethod method;
		for(String key: methodHash.keySet())
		{
			method = methodHash.get(key);
			if(method.getMethodType() == MethodType.VIRTUAL || method.getMethodType() == MethodType.DIRECT)
				if(existOpenComInvokeWithoutPermissionCheck(method))
					return true;
		}
		return false;
	}
	//判断这个方法有没有调用敏感函数
	//如果有，再判断调用这个敏感函数的那些函数是不是被某个开放组件调用的
	private boolean existOpenComInvokeWithoutPermissionCheck(AndrMethod method)
	{
		String[] code = method.getMethodCode();
		String caller;
		HashSet<AndrMethod> callerSet;
		for(String line: code)
		{
			if(line.startsWith("invoke"))
			{
				caller = line.substring(line.lastIndexOf(',') + 2);
				if(SensitiveFunction.isSensitiveFunction(caller))
				{
					System.out.println(caller);
					System.out.println(method.getMethodName());
					callerSet = getInvokes(method);
					if(existCallerMethodInVulCom(callerSet))
					{
						vulLoger.println(caller + "sensitive function ");
						vulLoger.flush();
						return true;
					}
				}
			}
		}
		return false;
	}
	//判断集合中这些函数是否是某个开放组件的
	private boolean existCallerMethodInVulCom(HashSet<AndrMethod> set)
	{
		for(AndrMethod method: set)
			if(method.getBl2Class().isVulnerable())
			{
				vulLoger.println(method.getMethodName() + "     invokes");
				vulLoger.flush();
				return true;
			}
		return false;
	}
	
	private HashSet<AndrMethod> getInvokes(AndrMethod method)
	{
		HashSet<AndrMethod> set = new HashSet<>();
		LinkedList<AndrMethod> list = new LinkedList<>();
		AndrMethod mth;
		set.add(method);
		list.add(method);
		while(!list.isEmpty())
		{
			mth = list.pollFirst();
			for(AndrMethod m: mth.getInvokedby())
			{
				if(!set.contains(m))
				{
					set.add(m);
					list.add(m);
				}
			}
		}
		return set;
	}
	
	private HashSet<AndrMethod> getInvoke(AndrMethod method)
	{
		HashSet<AndrMethod> set = new HashSet<>();
		LinkedList<AndrMethod> list = new LinkedList<>();
		AndrMethod mth;
		set.add(method);
		list.add(method);
		while(!list.isEmpty())
		{
			mth = list.pollFirst();
			for(AndrMethod m: mth.getInvokes())
			{
				if(!set.contains(m))
				{
					set.add(m);
					list.add(m);
				}
			}
		}
		return set;
	}
	
	public void setVulnerableComponent()
	{
		String[] vulCom = getVulnerableComponent();
		String claName;
		HashSet<AndrClass> vulSet = new HashSet<>();
		for(int i = 0; i < vulCom.length; i++)
		{
			vulCom[i] = vulCom[i].replace('.', '/');
			claName = "L" + vulCom[i] + ";";
			if(classHash.containsKey(claName))
				vulSet.add(classHash.get(claName));
		}
		vulSet = getAllVulComponent(vulSet);
		for(AndrClass cla: vulSet)
			cla.setIsVulnerable(true);
	}
	
	private HashSet<AndrClass> getAllVulComponent(HashSet<AndrClass> set)
	{
		LinkedList<AndrClass> list = new LinkedList<>();
		AndrClass aCla;
		for(AndrClass cla: set)
			list.add(cla);
		while(!list.isEmpty())
		{
			aCla = list.pollFirst();
			for(AndrClass claa: aCla.getInvokes())
			{
				if(!set.contains(claa))
				{
					set.add(claa);
					list.add(claa);
				}
			}
		}
		return set;
	}
	
	private String[] getVulnerableComponent()
	{
//		String fileName = recordPath + projectPath.substring(projectPath.lastIndexOf('\\'), projectPath.lastIndexOf('.')) + ".txt";
//		File fl = new File(fileName);
		File fl = new File("E:\\apks\\record\\uc.txt");
		FileContentProvider pvd = new FileContentProvider(fl);
		return pvd.getPlainContent();
	}
	
	public void test() throws FileNotFoundException
	{
		File dot = new File("abcd.txt");
		PrintWriter p = new PrintWriter(dot);
		p.println("digraph apkDigram {");
		LinkedList<AndrMethod> mlist = new LinkedList<AndrMethod>();
		AndrClass cla = classHash.get("Lcom/uc/browser/ActivityInitial;");
		AndrMethod m;
		clearMethodVisitFlag();
		for(String key: cla.getMethods().keySet()){
			mlist.add(cla.getMethods().get(key));
			cla.getMethods().get(key).setVisitedFlag(true);
		}
		while(!mlist.isEmpty()){
			m = mlist.pollFirst();
//			p.println(m.getMethodName() + "[shape=box];");
			for(AndrMethod mth: m.getInvokes()){
				p.println("\"" + m.getMethodName() + "\"" + "->" + "\"" + mth.getMethodName() + "\"" + ";");
				if(!mth.getVisitedFlag()){
					mlist.add(mth);
				}
			}
		}
		p.println("}");
		p.close();
		
	}
	
	public void linkMethod()
	{
		for(String key: methodHash.keySet())
		{
			AndrMethod method = methodHash.get(key);
			if(method.getMethodType() != MethodType.SYSTEMLIB)
				analyzeMethodInvokeAndLink(method);
		}
	}
	
	public void generateDotFile()
	{
		xmlprocesser.generateDot();
	}
	
	public CmptMap getComponentMap()
	{
		return xmlprocesser.getComponentMap();
	}
	
	public boolean classNameSetContains(String content)
	{
		return classNameSet.contains(content);
	}
	
	public void putInCodeBlockHash(int key, AndrCodeBlock block)
	{
		codeBlockHash.put(key, block);
	}
	
	public void putInMethodHash(String key, AndrMethod method)
	{
		methodHash.put(key, method);
	}
	
	public void linkMethodBlock()//每个方法作为入口，若没连起来则进行连接
	{
		AndrMethod mthd;
		for(String itrt: methodHash.keySet())
		{
			mthd = methodHash.get(itrt);
			//如果不是抽象方法和系统方法则进行连接
			if(mthd.getMethodType() != MethodType.ABSTRACT && mthd.getMethodType() != MethodType.SYSTEMLIB
					&& mthd.getMethodType() != MethodType.NATIVE)
				linkBlock(mthd.getCodeBlockList().get(0));
		}
//		System.out.println("link block......OK.");

	}
	
	public static String getCaller(String line)
	{
		String former = line.substring(0, line.indexOf("->"));
		return former.substring(former.lastIndexOf(" "));
	}
	
	public void jmpAna()
	{
		for(int blk : codeBlockHash.keySet())
		{
			AndrCodeBlock block = codeBlockHash.get(blk);
			analyzeJmps(block);
		}
	}
	
	public boolean setHierarchy(String superClassName, AndrClass subclass)
	{
		AndrClass superClass;
		if(!classNameSetContains(superClassName))
			superClass = classHash.get("SystemClass");
		else
			superClass = classHash.get(superClassName);
		if(superClass == null)
			return false;
		else
		{
			superClass.addSubClass(subclass);
			subclass.setSuperClass(superClass);
			return true;
		}
	}
	
	public void setInterfaceHierarchy(String interfaceName, AndrClass subclass)
	{
		AndrClass superClass = classHash.get(interfaceName);
		superClass.addSubClass(subclass);
	}
	
	private void buildClassHierarchy()
	{
		AndrClass andrClass;
		for(String key: classHash.keySet())
		{
			if(!key.equals("SystemClass"))
			{
				andrClass = classHash.get(key);
				andrClass.setHierarchy();
			}
		}
	}
	
	private void addSystemClass()
	{
		AndrClass sysClass = new AndrClass("SystemClass", this);
		classHash.put("SystemClass", sysClass);
	}
	
	private void addSystemMethod(String methodName)
	{
		if(!methodHash.containsKey(methodName))
		{
			AndrMethod mth = new AndrMethod(methodName, classHash.get("SystemClass"));
			classHash.get("SystemClass").getMethods().put(methodName, mth);
			methodHash.put(methodName, mth);
		}
	}
	//分析所有类中invoke后面调用的是否是系统库函数
	private void checkAndAddMethods2Hash()
	{
		AndrClass cla;
		for(String key: classHash.keySet())
		{
			if(!key.equals("SystemClass"))
			{
				cla = classHash.get(key);
				addSystemMethodInThisClass2Hash(cla);
			}
		}
	}
	//将此类中调用系统库的函数添加到hashtable
	private void addSystemMethodInThisClass2Hash(AndrClass aClass)
	{
		String[] code = aClass.getClassCode();
		for(String line: code)
		{
			if(line.startsWith("invoke"))
			{
				String toMthdName = line.substring(line.lastIndexOf(" ") + 1);
				String claName = toMthdName.substring(0, toMthdName.indexOf(";") + 1);
				
				if(!classNameSetContains(claName))
					//system call 如果原来hashtable里面没有这个函数则将系统调用函数添加到hashtable中。
					addSystemMethod(toMthdName);
				else
				{
					if(!methodHash.containsKey(toMthdName))
					{
						if(findMethodInSuperClass(toMthdName) == null)
						{
							addSystemMethod(toMthdName);
						}
					}
				}
			}
		}
	}
	
	private void analyzeMethodInvokeAndLink(AndrMethod method)
	{
		String[] methodCode = method.getMethodCode();
		AndrMethod jmpTo;
		for(String line: methodCode)
		{
			if(line.startsWith("invoke"))
			{
				String toMthdName = line.substring(line.lastIndexOf(" ") + 1);

				if(line.startsWith("invoke-virtual") || line.startsWith("invoke-static") || line.startsWith("invoke-direct")
						|| line.startsWith("invoke-interface") || line.startsWith("invoke-super"))
				{//正常调用，先判断这个方法名是否存在，如果存在，再看是不是抽象方法，如果是抽象，则找子类中的方法与之连接起来
					//如果不抽象，直接连接，如果不存在，则到父类中去找此方法，直到找到。
					//这个方法存在的情况
					if(methodHash.containsKey(toMthdName))
					{
						jmpTo = methodHash.get(toMthdName);
						//如果是抽象方法，寻找子类中的方法，进行连接
						if(jmpTo.getMethodType() == MethodType.ABSTRACT)
						{
							String methodName = toMthdName.substring(toMthdName.indexOf("->"));
							for(AndrClass subClass: jmpTo.getBl2Class().getSubClass())
							{
								toMthdName = subClass.getClassName() + methodName;
								if(methodHash.containsKey(toMthdName))
								{
									jmpTo = methodHash.get(toMthdName);
									linkTwoMethod(method, jmpTo);
								}
							}
						}
						else//直接连接
							linkTwoMethod(method, jmpTo);
					}
					else
					{
						linkTwoMethod(method, findMethodInSuperClass(toMthdName));
					}
				}
			}
		}
	}
	
	private boolean isSystemClass(AndrClass aClass)
	{
		if(aClass.getClassName().equals("SystemClass"))
			return true;
		else
			return false;
	}
	
	private AndrMethod findMethodInSuperClass(String methodName)
	{
		String className = methodName.substring(0, methodName.indexOf(";") + 1);
		String mthName = methodName.substring(methodName.indexOf("->"));
		String jmpToName = methodName;
		AndrClass jmpToClass = classHash.get(className);
		
		try{
		while(!methodHash.containsKey(jmpToName) && !isSystemClass(jmpToClass))
		{
			jmpToClass = jmpToClass.getSuperClass();
			jmpToName = jmpToClass.getClassName() + mthName;
		}}
		catch(NullPointerException e)
		{
			System.out.println(jmpToName);
		}
		
		if(isSystemClass(jmpToClass))
			return null;
		else
			return methodHash.get(jmpToName);
	}
	
	private void linkTwoMethod(AndrMethod invoker, AndrMethod invoked)
	{
		if(!invoker.getInvokes().contains(invoked))
		{
			invoker.add2Invoke(invoked);
			invoked.add2Invokedby(invoker);
		}
	}
	
	private HashSet<AndrMethod> findAllSubMethod(AndrMethod method)
	{
		HashSet<AndrMethod> set = new HashSet<>();
		String methodName = method.getMethodName().substring(method.getMethodName().indexOf("->"));
		LinkedList<AndrMethod> abstractMethod = new LinkedList<>();
		String toMthdName;
		AndrMethod jmp2Method;
		abstractMethod.add(method);
		while(!abstractMethod.isEmpty())
		{
			for(AndrClass subClass: abstractMethod.pollFirst().getBl2Class().getSubClass())
			{
				toMthdName = subClass.getClassName() + methodName;
				if(methodHash.containsKey(toMthdName))
				{
					jmp2Method = methodHash.get(toMthdName);
					if(jmp2Method.getMethodType() == MethodType.ABSTRACT)
						abstractMethod.add(jmp2Method);
					else if(jmp2Method.getMethodType() != MethodType.NATIVE)
						set.add(jmp2Method);
				}
			}
		}
		return set;
	}
	
	private void linkBlock(AndrCodeBlock block)
	{
		String[] code = block.getBlockCode();
		AndrCodeBlock jmpTo;
		AndrMethod jmp2Method;
		String jmp2Flag;
		if(code[code.length - 1].startsWith("goto") || code[code.length - 1].startsWith("packed-switch") ||
				code[code.length - 1].startsWith("sparse-switch") || code[code.length - 1].startsWith(".catch"))
		{//以这些标志为结尾的块，后面跟着要跳转到的地方
			jmp2Flag = code[code.length - 1].substring(code[code.length - 1].lastIndexOf(" ") + 1);
			 jmpTo = block.getMethod().getCodeBlocks().get(jmp2Flag);
			if(!jmpTo.getCaller().contains(block))
			{
				link2Block(block, jmpTo);
				linkBlock(jmpTo);
			}
		}
		else if(code[code.length - 1].startsWith("return") || code[code.length - 1].startsWith("throw"))
			//把最后的throw忽略掉。。不知道怎么处理。
			return;
		else if(code[code.length - 1].startsWith("invoke-virtual") 
				|| code[code.length - 1].startsWith("invoke-static")
				|| code[code.length - 1].startsWith("invoke-direct")
				|| code[code.length - 1].startsWith("invoke-super")
				|| code[code.length - 1].startsWith("invoke-interface"))
		{
			String toMthdName = code[code.length - 1].substring(code[code.length - 1].lastIndexOf(" ") + 1);
			//////////////////////////////////////////////////////////////////////////////////////////
			if(methodHash.containsKey(toMthdName))
			{
				jmp2Method = methodHash.get(toMthdName);
				//如果最后一行是调用系统库，则不进行处理。
				if(jmp2Method.getMethodType() != MethodType.SYSTEMLIB)
				{	
					//如果是抽象方法，寻找子类中的方法，进行连接
					if(jmp2Method.getMethodType() == MethodType.ABSTRACT)
					{
						for(AndrMethod mth: findAllSubMethod(jmp2Method))
						{
//							System.out.println(mth.getMethodName());
							if(!mth.getCodeBlockList().isEmpty())
							{
								jmpTo = mth.getCodeBlockList().get(0);
								link2Block(block, jmpTo);
								linkBlock(jmpTo);
							}
							linkNextBlockInMethod(block);
						}
					}
					else//直接连接
					{
						if(jmp2Method.getMethodType() != MethodType.NATIVE)
						{
							jmpTo = jmp2Method.getCodeBlockList().get(0);
							link2Block(block, jmpTo);
							linkBlock(jmpTo);
						}
						linkNextBlockInMethod(block);
					}
				}
			}
			else
			{
				jmp2Method = findMethodInSuperClass(toMthdName);
				if(!isSystemClass(jmp2Method.getBl2Class()))
				{
					jmpTo = jmp2Method.getCodeBlockList().get(0);
					link2Block(block, jmpTo);
					linkBlock(jmpTo);
				}
				linkNextBlockInMethod(block);
			}
		}
		else if(code.length > 1 && (code[1].startsWith(".sparse-switch") || code[1].startsWith(".packed-switch")))
		{
			for(int i = 2; i < code.length - 1; i++)
			{
				jmp2Flag = code[i].substring(code[i].indexOf(":"));
				jmpTo = block.getMethod().getCodeBlocks().get(jmp2Flag);
				if(!jmpTo.getCaller().contains(block))
				{
					link2Block(block, jmpTo);
					linkBlock(jmpTo);
				}
			}
		}
		else if(code[code.length - 1].startsWith("if"))
		{
			jmp2Flag = code[code.length - 1].substring(code[code.length - 1].lastIndexOf(" ") + 1);
			jmpTo = block.getMethod().getCodeBlocks().get(jmp2Flag);
			
			if(!jmpTo.getCaller().contains(block))
			{
				link2Block(block, jmpTo);
				linkBlock(jmpTo);
			}
			linkNextBlockInMethod(block);

		}
		else
			linkNextBlockInMethod(block);
	}
	
	private void link2Block(AndrCodeBlock blk1, AndrCodeBlock blk2)
	{
		blk1.addCalls(blk2);
		blk2.addCaller(blk1);
	}
	
	private void linkNextBlockInMethod(AndrCodeBlock block)
	{
		ArrayList<AndrCodeBlock> blocklist = block.getMethod().getCodeBlockList();
		int numOfBlock = blocklist.indexOf(block);
		if(numOfBlock + 1 < blocklist.size())
		{
			AndrCodeBlock next = blocklist.get(numOfBlock + 1);
			if(!next.getCaller().contains(block))
			{
				link2Block(block, next);
				linkBlock(next);
			}
		}
	}
	
	private void instantita()
	{
		classHash = new HashMap<String, AndrClass>();
		methodHash = new HashMap<String, AndrMethod>();
		codeBlockHash = new HashMap<Integer, AndrCodeBlock>();
		classNameSet = new HashSet<String>();
	}
	
	private void makeClassSet(File path)
	{
		File[] files = path.listFiles();
		for(int i = 0; i < files.length; i++)
		{
			if(files[i].isDirectory())
				makeClassSet(files[i]);
			else if(files[i].getAbsolutePath().endsWith(".smali"))
				getClass(files[i]);
		}
	}
	
	private void getClass(File smali)
	{
		FileContentProvider pvd = new FileContentProvider(smali);
		String[] content = pvd.getSmaliContent();
		//类名最后面带";"
		classNameSet.add(content[0].substring(content[0].lastIndexOf(" ") + 1));
//		System.out.println(content[0].substring(content[0].lastIndexOf(" ") + 1));
		
	}
	
	private void makeClassHash(File path)
	{
		File[] files = path.listFiles();
		for(int i = 0; i < files.length; i++)
		{
			if(files[i].isDirectory())
				makeClassHash(files[i]);
			else if(files[i].getAbsolutePath().endsWith(".smali"))
				makeClass(files[i]);
		}
	}
	
	private void makeClass(File smali)
	{
		FileContentProvider pvd = new FileContentProvider(smali);
		String[] content = pvd.getSmaliContent();
		AndrClass andrClass = new AndrClass(content, this);
		classHash.put(andrClass.getClassName(), andrClass);
	}
	
	private boolean checkActivityJmp(String line)
	{
		if((line.contains("startActivity") || line.contains("startActivities") || line.contains("startNextMatchingActivity")
				|| line.contains("getActivity") || line.contains("getActivities") || line.contains("startIntentSender")) &&
				line.contains("invoke-"))
			return true;
		else
			return false;
	}
	
	private boolean checkServiceJmp(String line)
	{
		if((line.contains("startService") || line.contains("bindService") || line.contains("stopService") ||
				line.contains("unbindService") || line.contains("getService")) && line.contains("invoke-"))
			return true;
		else
			return false;
	}
	
	private boolean checkReceiverJmp(String line)
	{
		if(line.contains("invoke-") && (line.contains("sendBroadcast") || line.contains("sendOrderedBroadcast") ||
				line.contains("sendStickyOrderedBroadcast") || line.contains("registerReceiver") || line.contains("getBroadcast")))
			return true;
		else
			return false;
	}
	
	private HashSet<String> findMatchComponent(HashSet<String> action)
	{
		HashSet<String> result = new HashSet<>();
		for(String key: action)
		{
			HashSet<String> tmpSet = xmlprocesser.findMatchComponent(key);
			for(String key1: tmpSet)
				result.add(key1);
		}
		return result;
	}
	
	private ParameterState getParameterNumber(String line)//仅适用于确定参数全部为非基本类型，即以";"作为分隔符
	{
		ParameterState state = new ParameterState();
		String parameters = line.substring(line.indexOf('(') + 1, line.indexOf(')'));
		String[] parameter = parameters.split(";");
		state.setParameterNum(parameter.length);
		state.addParameterType(parameter);
		return state;
	}
	
	private void traceDirectRegister(String reg, int lineNum, AndrCodeBlock block, HashSet<String> result)
	{
		if(block.getVisitedFlag())
			return;
		block.setVisitedFlag(true);
		String[] code = block.getBlockCode();       //可能从寄存器判断值是不是在本方法中确定，如果是带P的寄存器就是从其它地方传过来的
		if(reg.startsWith("p"))//暂不处理在本方法以外的
		{
			System.out.println("decide value of register in different methods.");
			return;				//感觉方法之间的调用处理起来相对麻烦，先放一放，而且看到的很多都是在一个方法内就可以确定，跨方法
		}
								//确定的相对不多，因此。。。。
							//从调用此方法的地方寻找值
		else
		{
			for(int i = lineNum - 1; i > 0; i--)
			{
				if(code[i].startsWith("move"))//赋值
				{
					String regInLine = code[i].split(" ")[1];
					if(regInLine.endsWith(","))
						regInLine = regInLine.substring(0, regInLine.length() - 1);
					if(regInLine.equals("reg"))
					{
						traceDirectRegister(code[i].split(" ")[2], i, block, result);
					}
				}
				else if(code[i].startsWith("const"))//参数为常量
				{
					String former = code[i].split(",")[0];//逗号之前部分
					String regInLine = former.split(" ")[1];//寄存器名称
					if(regInLine.equals(reg))
					{
						String tmp = code[i].split(",")[1];
						tmp = tmp.replace('\"', ' ');
						result.add(tmp.trim());
						return;
					}
				}
			}
			//在一个块中确定不了。因此找到所有调用此块的块。
			for(AndrCodeBlock blk: block.getCaller())
			{
				traceDirectRegister(reg, blk.getBlockCode().length, blk, result);
			}
		}
	}
	
	private String getFuncName(String invokeLine)
	{
		return invokeLine.substring(invokeLine.indexOf("->") + 2, invokeLine.indexOf("("));
	}
	
	private void getJmpFromCmpName(AndrCodeBlock block, HashSet<String> result)
	{
		ComponentState state = getComponentState(block);
		if(state.isComponent && block.getVisitedFlag() == false)
		{
			result.add(state.componentName);
			block.setVisitedFlag(true);
			for(AndrCodeBlock blk: block.getCaller())
				getJmpFromCmpName(blk, result);
		}
	}
	
	private ComponentState getComponentState(AndrCodeBlock code)
	{
		String className = code.getMethod().getBl2Class().getClassName();
		//如果包含$符，则取符号前面的所有字符作为组件名。
		//如，一个类为Activity.class，可能反编译完后会有Activity$01, Activity$02等几个，此时若启动Activity的调用在Activity$01里面，
		//反向查找的时候找到的类为Activity$01，此时，若去XML里面去查找，则找不到组件。而真实情况是在组件Activity里面。
		//类后面统一加";"
		if(className.contains("$"))
			className = className.substring(0, className.indexOf("$")) + ";";
		
		ComponentState state = new ComponentState();
		state.setComponentName(className);

		if(getComponents().containsKey(className))
			state.setIsComponent(true);
		else
			state.setIsComponent(false);
		
		return state;
	}
	
	private HashMap<String, ComponentNode> getComponents() {
		return xmlprocesser.getComponentMap().getNodes();
	}

	private void clearVisitFlag()
	{
		for(int key: codeBlockHash.keySet())
			if(codeBlockHash.get(key).getVisitedFlag())
				codeBlockHash.get(key).setVisitedFlag(false);
	}
	
	private String[] getRegNames(String line)
	{
		String regs = line.substring(line.indexOf("{") + 1, line.indexOf("}"));
		return regs.split(", ");
	}
	
	private void addEdges2Map(HashSet<String> calls, HashSet<String> called)
	{
		xmlprocesser.addEdge2Map(calls, called);
		linkComponent(calls, called);
	}
	
	private void linkComponent(HashSet<String> calls, HashSet<String> called)
	{
		AndrClass cla1, cla2;
		for(String key1: calls)
			for(String key2: called)
			{
//				System.out.println(key2);
				cla1 = classHash.get(key1);
				cla2 = classHash.get(key2);
				cla1.addInvokedComponent(cla2);
				cla2.addInvokes(cla1);
			}
	}
	
	private void analyzeJmps(AndrCodeBlock block)
	{
		String[] code = block.getBlockCode();

		for(int i = 1; i < code.length; i++)
		{
			if(checkActivityJmp(code[i]))
				activityAna(block, i);
			else if(checkServiceJmp(code[i]))
				serviceAna(block, i);
			else if(checkReceiverJmp(code[i]))
				reciverAna(block, i);
		}
	}
	
	private void activityAna(AndrCodeBlock block, int lineNum)
	{
		String line = block.getBlockCode()[lineNum];
		HashSet<String> calls = new HashSet<String>();
		Jmp2Struct called = new Jmp2Struct(InvokeType.explicit);
		String funcName = getFuncName(line);
		if(funcName.equals("startActivity") || funcName.equals("startActivityForResult") || funcName.equals("startNextMatchingActivity")
				|| funcName.equals("startActivityIfNeeded"))
		{
			getJmp2CmpName(block, lineNum, getRegNames(line)[1], called);
			clearVisitFlag();
		}
		else if(funcName.equals("startActivityFromChild") || funcName.equals("startActivityFromFragment"))
		{
			getJmp2CmpName(block, lineNum, getRegNames(line)[2], called);
			clearVisitFlag();
		}
		smoothCalled(called);
		getJmpFromCmpName(block, calls);
		clearVisitFlag();
		if(called.getJmpto().isEmpty())
		{
			if(called.getInvokeType() == InvokeType.explicit)
			{//显式调用没有回溯到结果
//				System.out.println("-----------------bugs!!!!!!activity-----------------");
//				System.out.println(block.getBlockCode()[lineNum]);
//				System.out.println(block.getMethod().getMethodName());
			}
			else//隐式调用系统中符合的activity
			{
//				System.out.println("implicit activity.");
//				addSystemActivityNode(calls);
			}
		}
		else
		{
			addEdges2Map(calls, called.getJmpto());
		}
	}
	
//	private void addSystemActivityNode(HashSet<String> calls)
//	{
//		HashSet<String> sys = new HashSet<>();
//		sys.add("otherActivityInSystem");
//		addEdges2Map(calls, sys);
//	}
//	
//	private void addSystemServiceNode(HashSet<String> calls)
//	{
//		HashSet<String> sys = new HashSet<>();
//		sys.add("otherServiceInSystem");
//		addEdges2Map(calls, sys);
//	}
//	
//	private void addSystemReceiverNode(HashSet<String> calls)
//	{
//		HashSet<String> sys = new HashSet<>();
//		sys.add("otherReceiverInSystem");
//		addEdges2Map(calls, sys);
//	}
	
	private void smoothCalled(Jmp2Struct str)
	{//对回溯出来的结果进行修改，使之跟之前保存的形式相同
		//即，名字以L开始，并且没有引号，最后要加分号，"."以"/"代替
		HashSet<String> set = str.getJmpto();
		HashSet<String> modifiedSet = new HashSet<>();
		for(String name: set)
		{
			if(name.contains("\""));
				name = name.replace('\"', ' ');
			name = name.trim();
			if(!name.startsWith("L"))
				name = "L" + name;
			name = name.replace('.', '/');
			if(!name.endsWith(";"))
				name = name + ";";
			modifiedSet.add(name);
		}
		str.setJmpto(modifiedSet);
	}
	
	private void serviceAna(AndrCodeBlock block, int lineNum)//getService不知如何处理。
	{
		String line = block.getBlockCode()[lineNum];
		HashSet<String> calls = new HashSet<String>();
		Jmp2Struct called = new Jmp2Struct(InvokeType.explicit);
		String funcName = getFuncName(line);
		if(funcName.equals("bindService") || funcName.equals("startService") || funcName.equals("stopService"))
		{
			getJmp2CmpName(block, lineNum, getRegNames(line)[1], called);
			clearVisitFlag();
		}
		
		smoothCalled(called);
		getJmpFromCmpName(block, calls);
		clearVisitFlag();
		if(called.getJmpto().isEmpty())
		{
			if(called.getInvokeType() == InvokeType.explicit)
			{//显式调用没有回溯到结果
//				System.out.println("------------------bugs!!service---------");
//				System.out.println(block.getBlockCode()[lineNum]);
//				System.out.println(block.getMethod().getMethodName());
			}
			else
			{//隐式调用系统中符合的service
//				System.out.println("implicit service");
//				addSystemServiceNode(calls);
			}
		}
		else
			addEdges2Map(calls, called.getJmpto());
	}
	
	private void reciverAna(AndrCodeBlock block, int lineNum)//getBroadcast不知如何处理。
	{
		String line = block.getBlockCode()[lineNum];
		HashSet<String> calls = new HashSet<String>();
		Jmp2Struct called = new Jmp2Struct(InvokeType.explicit);
		String funcName = getFuncName(line);
		if(funcName.equals("sendBroadcast") || funcName.equals("sendOrderedBroadcast") || funcName.equals("sendStickyBroadcast")
				|| funcName.equals("sendStickyOrderedBroadcast"))
		{
			getJmp2CmpName(block, lineNum, getRegNames(line)[1], called);
			clearVisitFlag();
		}
		
		smoothCalled(called);
		getJmpFromCmpName(block, calls);
		clearVisitFlag();
		if(called.getJmpto().isEmpty())
		{
			if(called.getInvokeType() == InvokeType.explicit)
			{//显式调用没有回溯到结果
//				System.out.println("------------------bugs!!receiver---------");
//				System.out.println(block.getBlockCode()[lineNum]);
//				System.out.println(block.getMethod().getMethodName());
			}
			else
			{//调用系统中符合条件的receiver
//				System.out.println("implicit receiver");
//				addSystemReceiverNode(calls);
			}
		}
		else
			addEdges2Map(calls, called.getJmpto());
	}
	
	private HashSet<String> mergeSet(HashSet<String> set1, HashSet<String> set2)
	{
		for(String key: set2)
			set1.add(key);
		return set1;
	}
	
	private void handleComponent(AndrCodeBlock block, int lineNum, String register, HashSet<String> result)
	{
		String[] code = block.getBlockCode();
		if(block.getVisitedFlag())
			return;
		block.setVisitedFlag(true);
		for(int i = lineNum - 1; i > 0; i--)
		{
			if(code[i].startsWith("invoke") && getRegNames(code[i])[0].equals(register))
				if(code[i].contains("Landroid/content/ComponentName;-><init>"))
					if(getParameterNumber(code[i]).getParameterNum() == 2)
					{//componentName类只能通过构造函数来确定包名和类名，第二个参数为类名
						clearVisitFlag();
						traceDirectRegister(getRegNames(code[i])[2], i, block, result);
						return;
					}
		}
		//如果本块中没有，回溯调用此块的块
		for(AndrCodeBlock blk: block.getCaller())
			handleComponent(blk, blk.getBlockCode().length, register, result);
	}
	
	private void getJmp2CmpName(AndrCodeBlock block, int lineNum, String register, Jmp2Struct struct)
	{
		String[] code = block.getBlockCode();
		HashSet<String> result = struct.getJmpto();
		HashSet<String> action = new HashSet<String>();
		if(block.getVisitedFlag())
			return;
		block.setVisitedFlag(true);
		for(int i = lineNum - 1; i > 0; i--)
		{
			if(code[i].startsWith("invoke") && getRegNames(code[i])[0].equals(register))//第一个参数为调用者
			{
				if(getFuncName(code[i]).equals("setClass") || getFuncName(code[i]).equals("setClassName"))
				{
//					clearVisitFlag();
					traceDirectRegister(getRegNames(code[i])[2], i, block, result);//第三个参数为class或classname
					return;
				}
				else if(getFuncName(code[i]).equals("setComponent"))
				{
					System.out.println("setComponent");
					handleComponent(block, i, register, result);
				}
				//处理Intent内容，如class，作为构造函数的参数传进去的情况
				else if(code[i].contains("Landroid/content/Intent;-><init>"))
				{
					ParameterState state = getParameterNumber(code[i]);
					//以某个intent作为参数. 这里的类后面没有分号。
					//显式intent
					if(state.getParameterNum() == 1 && state.getParameterType()[0].equals("Landroid/content/Intent"))
					{
						getJmp2CmpName(block, i, getRegNames(code[i])[1], struct);
					}
					else if(state.getParameterNum() == 2 && state.getParameterType()[1].equals("Ljava/lang/Class"))
					{
//						clearVisitFlag();
						traceDirectRegister(getRegNames(code[i])[2], i, block, result);
						return;
					}
					else if(state.getParameterNum() == 4)
					{
//						clearVisitFlag();
						traceDirectRegister(getRegNames(code[i])[4], i, block, result);
						return;
					}
					//隐式intent
					//action作为参数传进去。
					else if(state.getParameterNum() == 1 && state.getParameterType()[0].equals("Ljava/lang/String"))
					{
						struct.setInvokeType(InvokeType.implicit);
//						clearVisitFlag();
						traceDirectRegister(getRegNames(code[i])[1], i, block, action);
						struct.setJmpto(mergeSet(result, findMatchComponent(action)));
						return;
					}
					//参数为action和uri的情况
					else if(state.getParameterNum() == 2 && state.getParameterType()[0].equals("Ljava/lang/String"))
					{
						struct.setInvokeType(InvokeType.implicit);
//						clearVisitFlag();
						traceDirectRegister(getRegNames(code[i])[1], i, block, action);
						struct.setJmpto(mergeSet(result, findMatchComponent(action)));
						return;
					}
				}
				//隐式调用，回溯action参数，进行匹配
				else if(getFuncName(code[i]).equals("setAction"))
				{
					struct.setInvokeType(InvokeType.implicit);
//					clearVisitFlag();
					traceDirectRegister(getRegNames(code[i])[1], i, block, action);
					struct.setJmpto(mergeSet(result, findMatchComponent(action)));
					return;
				}
			}
		}
		//本块中未发现以上设置，即只有startactivity而没有对intent的设置
		//回溯，从调用此块的其它块中寻找。
		for(AndrCodeBlock blk: block.getCaller())
			getJmp2CmpName(blk, blk.getBlockCode().length, register, struct);
	}
}
