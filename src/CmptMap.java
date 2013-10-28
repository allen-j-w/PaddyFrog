import java.util.HashMap;
import java.util.HashSet;


public class CmptMap {
	
	private HashMap<String, ComponentNode> nodes;
	
	public CmptMap()
	{
		nodes = new HashMap<String, ComponentNode>();
	}
	
	public void addNode(ComponentNode node)
	{
		nodes.put(node.getName(), node);
	}
	
	public HashMap<String, ComponentNode> getNodes()
	{
		return nodes;
	}
	
	public CmptMap addEdges(HashSet<String> from, HashSet<String> to)
	{
		ComponentNode fr, t;
		for(String frm: from)
		{
			fr = nodes.get(frm);
			for(String t2o: to)
			{
				t = nodes.get(t2o);
				if(!fr.getLinkedTo().contains(t))
					fr.addLinkedTo(t);
			}
		}
		return this;
	}

}
