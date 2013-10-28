import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;


public class Entry {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */

	
	public static void main(String[] args) throws FileNotFoundException  {
		// TODO Auto-generated method stub

//		Calendar ca1, ca2;
		
		String openAPKListPath = "E:\\apks\\exported\\case.txt";
		Scanner input = new Scanner(new File(openAPKListPath));
		ArrayList<String> vulAPKs = new ArrayList<>();
		File vulLog = new File("vulLog.txt");
		File invokeSens = new File("invoke_sensitive_function.txt");
		File unsafe = new File("unsafe_apk.txt");
		File errLog = new File("errLog.txt");
		PrintWriter writer = new PrintWriter(vulLog);
		PrintWriter sensFunc = new PrintWriter(invokeSens);
		PrintWriter unsafe_writer = new PrintWriter(unsafe);
		PrintWriter errlogWriter = new PrintWriter(errLog);
		AndrProject proj;
		input.nextLine();
//		long sum = 0;
		while(input.hasNext())
			vulAPKs.add(input.nextLine());
		for(String line: vulAPKs)
		{
			try{
				System.out.println(line);
//				ca1 = Calendar.getInstance();
				proj = new AndrProject(line, writer);
				proj.linkMethod();
				proj.linkMethodBlock();
				proj.jmpAna();
				proj.setVulnerableComponent();
//				proj.test();
//				if(proj.existUnsafeComponentWithPermissonCheck())
//				{
////					ca2 = Calendar.getInstance();
////					sum += (ca2.getTimeInMillis() - ca1.getTimeInMillis())/100;
//					unsafe_writer.println(line);
//					unsafe_writer.flush();
//				}
				if(proj.existUnsafeComponentWithoutPermissionCheck())
				{
					sensFunc.println(line);
					sensFunc.flush();
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				errlogWriter.println(line);
			}

		}
		
		writer.close();
		input.close();
		sensFunc.close();
		unsafe_writer.close();
		errlogWriter.close();
//		String path = "D:\\apks\\temp\\1";
		
//		project.generateDotFile();
//		project.linkMethod();
//		project.test();
		

		
	}

}
