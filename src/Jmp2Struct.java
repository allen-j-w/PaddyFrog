import java.util.HashSet;


public class Jmp2Struct {
	
	private HashSet<String> jmpto;
	private InvokeType invokeType;

	public Jmp2Struct(InvokeType type)
	{
		invokeType = type;
		jmpto = new HashSet<>();
	}

	public HashSet<String> getJmpto() {
		return jmpto;
	}

	public void setJmpto(HashSet<String> jmpto) {
		this.jmpto = jmpto;
	}

	public InvokeType getInvokeType() {
		return invokeType;
	}

	public void setInvokeType(InvokeType invokeType) {
		this.invokeType = invokeType;
	}
	
	
}
