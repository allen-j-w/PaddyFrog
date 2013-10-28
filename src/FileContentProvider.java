import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;


public class FileContentProvider {
	private ArrayList<String> contents;
	private File file;
	
	public FileContentProvider(File file)
	{
		this.file = file;
	}
	
	public String[] getPlainContent()
	{
		try
		{
			FileInputStream in = new FileInputStream(file);
			Scanner input = new Scanner(in);
			 contents = new ArrayList<String>();
			String line;
			while(input.hasNext())
			{
				line = input.nextLine();
				contents.add(line.trim());
			}
			input.close();
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return contents.toArray(new String[0]);
	}

	public String[] getSmaliContent()
	{
		try
		{
			FileInputStream in = new FileInputStream(file);
			Scanner input = new Scanner(in);
			 contents = new ArrayList<String>();
			String line;
			while(input.hasNext())
			{
				line = input.nextLine();
				if(!line.trim().equals("") && !line.trim().startsWith(".line"))
					contents.add(line.trim());
			}
			input.close();
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return contents.toArray(new String[0]);
	}

}
