import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;


public class AndroidLog {

	private File output;
	private PrintWriter printWriter;
	
	public AndroidLog(String name)
	{
		try
		{
			output = new File(name);
			printWriter = new PrintWriter(output);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void log(String message)
	{
		printWriter.println(message);
		printWriter.println();
	}
}
