import java.util.HashSet;


public class PermissionCheckFunction {
	
	private static final HashSet<String> function = new HashSet<>();
	
	static{
		function.add("Landroid/content/Context;->checkCallingPermission(");
		function.add("Landroid/content/Context;->checkCallingUriPermission(");
		function.add("Landroid/content/Context;->checkPermission(");
		function.add("Landroid/content/Context;->checkUriPermission(");
		function.add("Landroid/content/Context;->checkCallingOrSelfPermission(");
		function.add("Landroid/content/Context;->checkCallingOrSelfUriPermission(");
		function.add("Landroid/content/Context;->enforceCallingOrSelfPermission(");
		function.add("Landroid/content/Context;->enforceCallingOrSelfUriPermission(");
		function.add("Landroid/content/Context;->enforceCallingPermission(");
		function.add("Landroid/content/Context;->enforceCallingUriPermission(");
		function.add("Landroid/content/Context;->enforcePermission(");
		function.add("Landroid/content/Context;->enforceUriPermission(");
		function.add("Landroid/content/Context;->enforceUriPermission(");
		function.add("Landroid/content/Context;->grantUriPermission(");
		function.add("Landroid/content/Context;->revokeUriPermission(");
		function.add("Landroid/content/Context;->checkUriPermission(");
//		function.add("Landroid/content/Context;->");
//		function.add("Landroid/content/Context;->");
//		function.add("Landroid/content/Context;->");
	}
	
	public static boolean isPermissionCheckFunction(String functionName)
	{
		//functionName不包括前边invoke那一通，从类名开始
		String func = functionName.substring(0, functionName.indexOf('(') + 1);
//		System.out.println(func);
		return function.contains(func);
	}

}
