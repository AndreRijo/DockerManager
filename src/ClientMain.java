import java.io.IOException;
import java.util.Scanner;

public class ClientMain {
	
	public static final String SLEEP_BEFORE = "sleep";	//seconds
	public static final String TEST_TIME = "testtime";	//seconds
	public static final String REMOTE_CONFIGS = "coordconfigs";
	public static final String PORT = "port";
	public static final String SAVE_LOG = "savelog";
	//public static final String NOTIFICATION_PORT = "notificationport";	//NOTE: NOT IMPLEMENTED!
	public static final String MODE = "mode";		//If not defined, "TIME" is assumed.
	
	
	//The three types of modes.
	public static final String MODE_TIME = "time";	//Uses time defined in configs.
	public static final String MODE_ACTIVE = "active";	//Will watch and wait until start cmd finishes.
	public static final String MODE_PASSIVE = "passive";	//Will wait until coordinator says that it should move to stop command.
	public static final int DEFAULT_PORT = 6666;
	
	public static void main(String[] args) throws InterruptedException {
		while(true) {
			try {
			TestConfigs configs = new TestConfigs();
			processArgs(args, configs);
			Client client;
			
			if (configs.isRemoteConfigs())
				client = new Client(configs);
			else {
				Scanner in = new Scanner(System.in);
				System.out.println("Node number? (from 0 to nNodes-1)");
				int node = Integer.parseInt(in.nextLine());
				System.out.println("Start command?");
				//Note: Added '"' guarantees that the command is properly executed even if it has spaces.
				//Command cmd = new Command("sh -c " + "'" + in.nextLine().trim() + "'");
				Command cmd = new Command(in.nextLine().trim());
				System.out.println("Stop command?");
				//Command stopCmd = new Command("sh -c " + "'" + in.nextLine().trim() + "'");
				Command stopCmd = new Command(in.nextLine().trim());
				System.out.println("Configs location?");
				String configsLocation = in.nextLine();
				in.close();
				
				try {
					configs = TestFileReader.readInputFile(configsLocation, configs);
					configs.setNode(node);
				} catch (Exception e) {
					System.err.println("Error while reading configs file. Path: " + configsLocation);
					e.printStackTrace();
					return;
				}
				client = new Client(configs, cmd, stopCmd);
			}	
			client.startTests();
			System.out.println("Client finished. Will sleep for 3s and restart client.");
			} catch (Exception e) {
				System.out.println("An error has occoured. Stopping current tests.");
				e.printStackTrace();
			}
			Thread.sleep(3000);
			System.out.println("Client restarted and waiting for new configurations.");
		}
	}
	
	private static void processArgs(String[] args, TestConfigs configs) {
		if (args.length % 2 != 0) {
			System.out.println("Invalid program arguments. Exiting");
			System.exit(0);
		}
		boolean hasPortSet = false;
		for (int i = 0; i < args.length; i+=2) {
			String name = args[i];
			String value = args[i+1];
			if (name.toLowerCase().endsWith(SLEEP_BEFORE))
				configs.setWaitTime(Integer.parseInt(value));
			else if (name.toLowerCase().endsWith(TEST_TIME))
				configs.setTestTime(Integer.parseInt(value));
			else if (name.toLowerCase().endsWith(REMOTE_CONFIGS))
				configs.useRemoteConfigs();
			else if (name.toLowerCase().endsWith(PORT)) {
				configs.setOwnPort(Integer.parseInt(value));
				hasPortSet = true;
			}
			else if (name.toLowerCase().endsWith(SAVE_LOG))
				configs.setShouldLog(Boolean.parseBoolean(value));
			else if (name.toLowerCase().endsWith(MODE))
				configs.setMode(value.toLowerCase());
			/*
			else if (name.toLowerCase().endsWith(NOTIFICATION_PORT))
				configs.setNotificationPort(Integer.parseInt(value));
				*/
			else {
				System.out.println("Unknown param " + name);
				System.out.printf("Known params: --%s --%s --%s --%s --%s --%s (%s, %s, %s)\n", SLEEP_BEFORE, TEST_TIME, REMOTE_CONFIGS, PORT, SAVE_LOG,
						MODE, MODE_TIME, MODE_ACTIVE, MODE_PASSIVE);
			}
		}
		
		if (!hasPortSet)
			configs.setOwnPort(DEFAULT_PORT);
	}

}
