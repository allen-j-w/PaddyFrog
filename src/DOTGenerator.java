import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;


public class DOTGenerator {

	private String fileName;
	private CmptMap map;
	
	public DOTGenerator(String name, CmptMap map)
	{
		fileName = name;
		this.map = map;
	}
	
	private void smoothMap()
	{
		ComponentNode node;
		int i = 0;
		for(String key: map.getNodes().keySet())
		{
			node = map.getNodes().get(key);
			node.setName(node.getName().replace(';', ' ').replace('$', '\\').replace('/', '_').trim());
			node.setNumber(i);
			i++;
		}
	}
	
	public void generateDOTFile()
	{
		try
		{
			File file = new File(fileName);
			if(file.exists())
				file.delete();
			file.createNewFile();
			FileOutputStream out = new FileOutputStream(file);
			PrintWriter output = new PrintWriter(out);
			smoothMap();
			output.println("digraph apkDigram {");
			for(String key: map.getNodes().keySet())
			{
				ComponentNode nd = map.getNodes().get(key);
//				output.print(nd.getNumber());
				output.print(nd.getName());
				//Activity用椭圆，Service用矩形，Receiver用梯形形，Provider用三角形
				if(nd.getType().equals("Activity"))
					output.println(";");
				else if(nd.getType().equals("Service"))
					output.println("[shape=box];");
				else if(nd.getType().equals("Receiver"))
					output.println("[shape=polygon,sides=4,distortion=.7];");
				else if(nd.getType().equals("Privider"))
					output.println("[shape=triangle];");
				
			}
		
		for(String key: map.getNodes().keySet())
		{
			ComponentNode tmp = map.getNodes().get(key);
			if(!tmp.getLinkedTo().isEmpty())
				for(ComponentNode nd: tmp.getLinkedTo())
//					output.println(tmp.getNumber() + " -> " + nd.getNumber() + ";");
					output.println(tmp.getName() + "->" + nd.getName() + ";");
		}
		
		output.println("}");
		output.close();
		out.close();
		System.out.println("DOT file has been generated.");
		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
