import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

	//Idea: each client knows what to execute somehow, and what is variable.
	//The coordinator simply iterates through the list of values for each variable and sends all values to all clients.
	//Each client then ignores or applies each variable.
	//Strings to be replaced are at indexes where index%2 == 1.
	
	
	public static void main(String[] args) {
		//String cmd = args[0];
		Scanner in = new Scanner(System.in);
		System.out.println("Command?");
		String cmd = in.nextLine();
		in.close();
		
		
		String[] parts = cmd.split("#");
		for (int i = 0; i < parts.length; i++)
			System.out.println(parts[i]);
		System.out.println(cmd);
		
		
		testCommandArgs(cmd);
	}
	
	private static void simpleTest() {
		try {
			System.out.println("Stopping potionDB via java...");
			Process proc = Runtime.getRuntime().exec("docker stop potiondb");
			System.out.println("Waiting for exec to finish...");
			int value = proc.waitFor();
			System.out.println("Exit value: " + value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void testCommandArgs(String cmd) {
		String[] parts = cmd.split("#");
		Map<String, Integer> replaceMap = new HashMap<>();
		for (int i = 1; i < parts.length; i += 2) {
			//Positions with commands
			replaceMap.put(parts[i], i);
		}
		
		String[] nameValues = {"aaa", "bbb", "ccc", "ddd", "eee", "fff"};
		String[] queryValues = {"24", "48", "72", "96", "120", "144"};
		
		for (int i = 0; i < nameValues.length; i++) {
			parts[1] = nameValues[i];
			parts[3] = queryValues[i];
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < parts.length; j++) {
				sb.append(parts[j]);
			}
			System.out.println(sb.toString());
		}
	}
}
