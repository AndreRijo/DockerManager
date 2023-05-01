import java.util.HashMap;
import java.util.Map;

public class Command {
	
	//public static final String NOT_DEFINED = "none";

	private Map<String, Integer> varToPos;
	private String[] base;
	private String[] replacable;
	private int minSize;
	
	public Command(String cmd) {
		varToPos = new HashMap<String, Integer>();
		minSize = cmd.length();
		splitCommand(cmd);
	}
	
	private void splitCommand(String cmd) {
		String[] parts = cmd.split("#");
		base = new String[parts.length / 2 + 1];
		replacable = new String[parts.length / 2];
		base[0] = parts[0];
		
		for (int i = 1; i < parts.length; i += 2) {
			base[i/2 + 1] = parts[i+1];
			replacable[i/2] = parts[i].trim();
			varToPos.put(parts[i], i/2);
		}
	}
	
	public String buildCommand(Map<String, String> testValues) {
		if (base.length == 1)
			return base[0];
		
		StringBuilder sb = new StringBuilder(minSize);
		sb.append(base[0]);
		for (int i = 0; i < replacable.length; i++) {
			//Some tests may not define all variables, as they can be optional
			String toReplace = testValues.get(replacable[i]);
			if (toReplace != null)
				sb.append(testValues.get(replacable[i]));
			/*else
				sb.append(NOT_DEFINED);		//Can't use this as some configs depend on not having anything (i.e., empty string)
				*/
			sb.append(base[i+1]);
		}
		
		return sb.toString();
	}
}
