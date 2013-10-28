import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XMLPreprocesser {
	private String outputPath;
	private NodeList actList;
	private NodeList serList;
	private NodeList pvdList;
	private NodeList rcvList;
	private String packageName;
	private Document document;
	private CmptMap map = new CmptMap();
	private HashMap<String, HashSet<String>> actionStoreTable = new HashMap<>(); 
	private DOTGenerator dotGenerator;
	
	public XMLPreprocesser(String path)
	{
		this.outputPath = path + "\\AndroidManifest.xml.bak";
		try
		{


				FileInputStream in = new FileInputStream(path +"\\AndroidManifest.xml");
				
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				document = builder.parse(in);
				Element elements = document.getDocumentElement();
				actList = elements.getElementsByTagName("activity");
				serList = elements.getElementsByTagName("service");
				pvdList = elements.getElementsByTagName("provider");
				rcvList = elements.getElementsByTagName("receiver");
				
				packageName = elements.getAttribute("package");
				modifyXML();
			
			
			getMap();
			initializeActionTable();
			dotGenerator = new DOTGenerator(path.substring(path.lastIndexOf('\\') + 1, path.length()) + ".dot", map);
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void generateDot()
	{
		dotGenerator.generateDOTFile();
	}
	
	private void initializeActionTable()
	{
		ComponentNode node;
		for(String key: map.getNodes().keySet())
		{
			node = map.getNodes().get(key);
			if(node.containsDefaultCategory())
			{
				for(String action: node.getActions())
					addAction2Table(action, node.getName());
			}
		}
	}
	
	public void addAction2Table(String action, String component)
	{
		HashSet<String> tmpset;
		if(actionStoreTable.containsKey(action))
			tmpset = actionStoreTable.get(action);
		else
			tmpset = new HashSet<String>();
		if(!tmpset.contains(component))
			tmpset.add(component);
		actionStoreTable.put(action, tmpset);
	}
	
	public HashSet<String> findMatchComponent(String action)
	{
		HashSet<String> result = new HashSet<>();
		if(actionStoreTable.containsKey(action))
		{
			HashSet<String> tmp = actionStoreTable.get(action);
			for(String key: tmp)
				result.add(key);
		}
		return result;
	}
	
	public String getOutPath()
	{
		return outputPath;
	}
	
	private void modifyAttribute(NodeList list)
	{
		Element a;
		for(int i = 0; i < list.getLength(); i++)
		{
			a = (Element) list.item(i);
			String originName = a.getAttribute("android:name");
			if(!originName.contains(packageName))
			{
//				System.out.println("original attribute is " + a.getAttribute("android:name"));
				if(a.getAttribute("android:name").startsWith("."))
					a.setAttribute("android:name", packageName + originName);
				else if(!originName.contains("."))
					a.setAttribute("android:name", packageName + "." + originName);
			}
		}
	}
	
	public void addEdge2Map(HashSet<String> calls, HashSet<String> called)
	{
		map.addEdges(calls, called);
	}
	
	private void modifyXML()
	{
		try
		{
			modifyAttribute(actList);
			modifyAttribute(pvdList);
			modifyAttribute(rcvList);
			modifyAttribute(serList);
			
//			System.out.println("manifest modification is done.");
			
			TransformerFactory transformerFactory=TransformerFactory.newInstance();
	        Transformer transformer=transformerFactory.newTransformer();
	        DOMSource domSource=new DOMSource(document);
		    //设置编码类型
		    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF8");
	        StreamResult result=new StreamResult(new FileOutputStream(outputPath));
		    //把DOM树转换为xml文件
		    transformer.transform(domSource, result);
	    
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void getMap()
	{
		createNode(actList, "Activity");
		createNode(serList, "Service");
		createNode(pvdList, "Provider");
		createNode(rcvList, "Receiver");
		addSystemNode();
	}
	
	private void addSystemNode()
	{
		ComponentNode node = new ComponentNode("otherActivityInSystem", "Activity");
		map.addNode(node);
		node = new ComponentNode("otherServiceInSystem", "Service");
		map.addNode(node);
		node = new ComponentNode("otherReceiverInSystem", "Receiver");
		map.addNode(node);
	}
	
	private void createNode(NodeList list, String type)
	{
		Element element;
		for(int i = 0; i < list.getLength(); i++)
		{
			Node node = list.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE)
			{
				element = (Element) node;
				//xml里面路径表示是用'.'，而程序里面则用的是'/'，统一用'/'表示。类统一后面加';'
				ComponentNode cmpnode = new ComponentNode("L" + element.getAttribute("android:name").replace('.', '/') + ";", type);
				addFilter2Node(cmpnode, element);
				map.addNode(cmpnode);
			}
		}
	}
	
	//暂时不处理data。有action和类别就能够确定调用哪个部件。
	private void addFilter2Node(ComponentNode node, Element element)
	{
		NodeList list = element.getElementsByTagName("intent-filter");
		NodeList action;
//		NodeList data;
		NodeList category;
		if(list.getLength() == 1)
		{
			Element filter = (Element) list.item(0);
			action = filter.getElementsByTagName("action");
//			data = filter.getElementsByTagName("data");
			category = filter.getElementsByTagName("category");
			
			if(action.getLength() > 0)
				makeActionFilters(node, action);
//			if(data.getLength() > 0)
//				makeDataFilters(node, data);
			if(category.getLength() > 0)
				makeCategoryFilters(node, category);
		}
	}
	
	private void makeActionFilters(ComponentNode node, NodeList list)
	{
		for(int i = 0; i < list.getLength(); i++)
		{
			Element item = (Element) list.item(i);
			AndrIntentFilter flt = getActionAndCategoryIntentFilter(item, IntentFilterType.action);
			node.addFilter(flt);
			//如果action为main，则加一个系统调用此activity的边
			if(flt.getAttribute().equals("android.intent.action.MAIN"))
				addStartNode(node);
		}
	}
	
	private void makeCategoryFilters(ComponentNode node, NodeList list)
	{
		for(int i = 0; i < list.getLength(); i++)
		{
			Element item = (Element) list.item(i);
			AndrIntentFilter flt = getActionAndCategoryIntentFilter(item, IntentFilterType.category);
			node.addFilter(flt);
		}
	}
	
//	private void makeDataFilters(ComponentNode node, NodeList list)
//	{
//		for(int i = 0; i < list.getLength(); i++)
//		{
//			Element item = (Element) list.item(i);
//			node.addFilter(getDataIntentFilter(item, IntentFilterType.data));
//		}
//	}
	
	private void addStartNode(ComponentNode node)
	{
		ComponentNode startNode = new ComponentNode("start", "Activity");
		startNode.addLinkedTo(node);
		map.addNode(startNode);
	}
	
	private AndrIntentFilter getActionAndCategoryIntentFilter(Element element, IntentFilterType type)
	{
		AndrIntentFilter filter = new AndrIntentFilter(type);
		filter.setAttribute(element.getAttribute("android:name"));
		return filter;
	}

	public CmptMap getComponentMap() {
		// TODO Auto-generated method stub
		return map;
	}

//	private AndrIntentFilter getDataIntentFilter(Element element, IntentFilterType type)
//	{
//		//not important
//		AndrIntentFilter filter = new AndrIntentFilter(type);
//		return filter;
//	}
}
