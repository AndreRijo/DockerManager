import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import exceptions.ConnectionFailedException;

public class CoordinatorMain {
	
	//TODO: Oarwalltime support. Check end of file for some information.
	//TODO: Proper time left support for automatic repair. For now it only displays a correct (non-predict) time when changing configurations.

	public static final String REPAIR = "repair";
	public static final String WAIT_FOR = "waitfor";		//Note: assumes all tests have the same nodes involved. Used when those nodes are in ACTIVE mode.
	//Detects when a test fails (via timeout) and schedules it for repeat later. Requires --waitfor option.
	//If passed an int (x) argument, it will also consider tests that end too early by x seconds as failed.
	public static final String AUTOMATIC_REPAIR = "autorepair";
	public static final String OAR_JOB_ID = "oarjobid";		//If this isn't present then OAR isn't used. OAR is only supported on frontnode so... this is useless unfortunately :(
	public static final String NODES = "nodes";	//Replaces the list of nodes in each configuration file by the one provided in this argument. Separate each node with a ",".
	public static final String STOP_ON_ERR = "stoponerror";	//If true, DockerManager will stop if any tests fails too early or too late. Useful for debugging. Default: false.
	
	private static String rangesString;
	
	//Ranges: "|" for different tests; "," for multiple ranges in the same test.
	public static void main(String[] args) throws NumberFormatException, IOException, InterruptedException {
		String[][][] allCmds = null;	
		TestConfigs configs = new TestConfigs();
		CoordinatorConfigs coordConfigs = new CoordinatorConfigs();
		coordConfigs.setSuspiciousOffset(Integer.MAX_VALUE);
		processArgs(args, configs, coordConfigs);
		Scanner in = new Scanner(System.in);
		String[] configsLocs;

		if (configs.isRemoteConfigs()) {
			System.out.println("Number of configs?");
			int nConfigs = Integer.parseInt(in.nextLine());
			configsLocs = new String[nConfigs];
			System.out.printf("Configs location (%d)\n", nConfigs);
			for (int i = 0; i < nConfigs; i++)
				configsLocs[i] = in.nextLine();
		} else {
			System.out.println("Configs location?");
			configsLocs = new String[1];
			configsLocs[0] = in.nextLine();
		}

		TestConfigs[] allConfigs = new TestConfigs[configsLocs.length];
		int i = 0;
		try {
			allConfigs[0] = TestFileReader.readInputFile(configsLocs[0], configs);
			for (i = 1; i < configsLocs.length; i++)
				allConfigs[i] = TestFileReader.readInputFile(configsLocs[i]);
			if (coordConfigs.hasNodeList()) {
				String[] nodeList = coordConfigs.getNodeList();
				for (i = 0; i < configsLocs.length; i++)
					allConfigs[i].replaceIPs(nodeList);
			}
		} catch (Exception e) {
			System.err.println("Error while reading configs file. Path: " + configsLocs[i]);
			e.printStackTrace();
			in.close();
			return;
		}

		if (rangesString != null) {
			System.out.println(rangesString);
			String[] rangesPerConfig = rangesString.split("\\|");
			System.out.println(rangesPerConfig.length);
			System.out.println(allConfigs.length);
			for (i = 0; i < rangesPerConfig.length && i < allConfigs.length; i++)
				if (rangesPerConfig[i].contains("all"))
					allConfigs[i].addRepairRange(IntRanges.getFullRange(allConfigs[i].getNTests()-1));
				else
					allConfigs[i].addRepairRange(IntRanges.stringToRanges(rangesPerConfig[i].split(",")));
			for (i = 0; i < rangesPerConfig.length && i < allConfigs.length; i++) {
				boolean isOk = checkRepairs(in, allConfigs[i], allConfigs[i].getRepairRange());
				if (!isOk) {
					System.out.println("Aborting tests as the configurations are incorrect.");
					System.exit(0);
				}
			}
			//in.nextLine();	//For some reason this is required...

		}

		for (int k = 0; k < allConfigs.length; k++)
			if(!checkNumberOfTests(in, configsLocs[k], allConfigs[k])) {
				System.out.println("Aborting as requested by the user. Please fix the tests configurations.");
				System.exit(0);
			}


		Coordinator cd = null;
		if (configs.isRemoteConfigs()) {
			allCmds = new String[allConfigs.length][][];
			for (i = 0; i < allConfigs.length; i++) {
				int nNodes = configs.getNNodes();
				String[][] remoteCmds = new String[nNodes][4];
				System.out.println("Configs for " + nNodes + " nodes? Format: nodeNumber, startCmd, stopCmd, configLocation. 4 lines per node.");
				for (int j = 0; j < nNodes; j++) {
					remoteCmds[j][0] = readNonEmptyLine(in);
					remoteCmds[j][1] = readNonEmptyLine(in);
					remoteCmds[j][2] = readNonEmptyLine(in);
					remoteCmds[j][3] = readNonEmptyLine(in);
				}
				allCmds[i] = remoteCmds;
			}
			cd = new Coordinator(coordConfigs, allConfigs, allCmds);
		} else
			cd = new Coordinator(coordConfigs, allConfigs);


		boolean testStartedSuccessfully = false;
		do {
			try {
				cd.prepareTests();
				in.close();
				testStartedSuccessfully = true;
				cd.startTests();
			} catch (ConnectionFailedException e) {
				configs.setWaitTime(0);//Reset any sleeping time, as we already slept.
				System.err.println("Will re-attempt to connect to clients and start the tests. Please press enter when ready.");
				System.err.println("Type yes to re-attempt connection. Type new or nodes to specify a new list of clients. Type no or exit to close the program.");
				String input = in.nextLine().toLowerCase();
				boolean redo = false;
				switch (input) {
				case "yes":
				case "y":
				case "":
					redo = true;
					break;
				case "new":
				case "nodes":
				case "node":
					System.out.println("Please type the new list of nodes:");
					String nodes = in.nextLine();
					coordConfigs.setNodeList(nodes.split(","));
					String[] nodeList = coordConfigs.getNodeList();
					for (i = 0; i < configsLocs.length; i++)
						allConfigs[i].replaceIPs(nodeList);
					System.out.println("New node list: " + nodes);
					redo = true;
					break;
				case "no":
				case "exit":
				case "quit":
				case "abort":
					testStartedSuccessfully = true;
					break;
				}
				if (redo) {
					if (configs.isRemoteConfigs())
						cd = new Coordinator(coordConfigs, allConfigs, allCmds);
					else
						cd = new Coordinator(coordConfigs, allConfigs);
				}
			}
		} while (!testStartedSuccessfully);
	}
	
	private static boolean checkNumberOfTests(Scanner in, String configName, TestConfigs configs) {
		int[] loopLevels = configs.getLoopLevels();
		int max = 0;
		for (int i = 0; i < loopLevels.length; i++)
			if (loopLevels[i] > 0)
				max = loopLevels[i];
			else
				break;
		int maxSplit = 1, maxNormalLoop = 1;
		//Still missing split; possibly LOOP if no LOOP_x is present.
		Map<String, Variable> vars = configs.getVars();
		for (Variable var : vars.values())
			if (var.getRepeatType() == RepeatType.SPLIT)
				maxSplit = Math.max(maxSplit, var.getNumberOfValues());
			else if (var.getRepeatType() == RepeatType.LOOP)
				maxNormalLoop = Math.max(maxNormalLoop, var.getNumberOfValues());
		
		//We now have all the required info
		//May happen if we have no LOOP_x
		if (max < maxNormalLoop)
			max = maxNormalLoop;
		max = max * maxSplit;
		int nTests = configs.getNTests();
		if (max != nTests) {
			System.out.printf("[WARNING]Number of tests (%d, user-provided) and expected number of tests (%d, calculated via the variables) don't match. "
					+ "This mitchmatch happened on config file %s. Do you wish to proceed? (true/false)\n", nTests, max, configName);
			return Boolean.parseBoolean(in.nextLine());
		}
		System.out.println("Number of tests appears to be correct given the variables.");
		return true;
	}
	
	private static boolean checkRepairs(Scanner in, TestConfigs configs, IntRanges repairRange) {
		for (int i = 0; i < configs.getNTests(); i++) {
			if (repairRange.isInRange(i)) {
				Iterator<Entry<String, String>> valuesIt = configs.getTestValues(i).entrySet().iterator();
				System.out.printf("Test %d: [", i);
				while (valuesIt.hasNext()) {
					Entry<String, String> next = valuesIt.next();
					System.out.printf("%s: %s; ", next.getKey(), next.getValue());
				}
				System.out.println("]");
			}
		}
		while (true) {	//Boolean.parseBoolean only checks if the string is exactly "true". Might as well we check that manually for finer control.
			System.out.println("Are these configs OK? (true/false)");
			String input = in.nextLine();
			if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("yes"))
				return true;
			if (input.equalsIgnoreCase("false") || input.equalsIgnoreCase("no"))
				return false;
			System.out.println("Invalid input, ignored. Are these configs OK? (true/false)");		
		}
	}

	private static void processArgs(String[] args, TestConfigs configs, CoordinatorConfigs coordConfigs) {
		if (args.length % 2 != 0) {
			System.out.println("Invalid program arguments. Exiting");
			System.exit(0);
		}
		for (int i = 0; i < args.length; i+=2) {
			String name = args[i].toLowerCase();
			String value = args[i+1];
			if (name.endsWith(ClientMain.SLEEP_BEFORE))
				configs.setWaitTime(Integer.parseInt(value));
			else if (name.endsWith(AUTOMATIC_REPAIR)) {
				coordConfigs.setAutomaticRepair(true);
				try {
					coordConfigs.setSuspiciousOffset(Integer.parseInt(value));
					System.out.println("Will do automatic repairs for any tests that end early by at least " + coordConfigs.getSuspiciousOffset() + " seconds in ALL nodes.");
				} catch (NumberFormatException e) {
					System.out.println("Will do automatic repairs only for timeouts.");
				}
			}
			else if (name.endsWith(REPAIR))
				rangesString = value;
			else if (name.endsWith(ClientMain.REMOTE_CONFIGS))
				configs.useRemoteConfigs();
			else if (name.endsWith(WAIT_FOR))
				coordConfigs.setWaitFor(parseIntArray(value));
			else if (name.endsWith(OAR_JOB_ID)) {
				coordConfigs.setUseOAR(true);
				OARHandler.setJobID(Integer.parseInt(value));
			} else if (name.endsWith(NODES))
				coordConfigs.setNodeList(value.split(","));
			else if (name.endsWith(STOP_ON_ERR)) {
				coordConfigs.setStopOnError(Boolean.parseBoolean(value));
				if (coordConfigs.stopOnError())
					System.out.println("Will stop executing tests if any error is found or if a test timeouts or ends too early.");
			}
			else {
				System.out.println("Unknown param " + name);
				System.out.printf("Known params: --%s --%s --%s --%s --%s --%s\n", ClientMain.SLEEP_BEFORE, REPAIR, AUTOMATIC_REPAIR, ClientMain.REMOTE_CONFIGS, WAIT_FOR, NODES);
			}
		}
	}
	
	private static int[] parseIntArray(String stringList) {
		String[] parts = stringList.split(",");
		int[] result = new int[parts.length];
		int i = 0;
		for (String s: parts)
			result[i++] = Integer.parseInt(s.trim());
		return result;
	}
	
	private static String readNonEmptyLine(Scanner in) {
		String line = "";
		do {
			line = in.nextLine();
		} while (line.isEmpty() || line.trim().equals("") || line.startsWith("//") || 
				line.startsWith("#") || line.equalsIgnoreCase("true") || line.equalsIgnoreCase("false"));
		return line;
	}
	
	/*
	- We can obtain the used walltime by using oarstat -j JOB_ID. Then, use oarwalltime JOB_ID to know how much was reserved.
	- Example outputs:
	oarstat:
	Job id    S User     Duration   System message
	--------- - -------- ---------- ------------------------------------------------
	2876      R a.rijo      0:08:04 R=12,W=0:30:0,J=B (Karma=0.652,quota_ok)
	oarwalltime:
	Walltime change status for job 2876 (job is running):
  		Current walltime:      0:30:0
  		Possible increase:  UNLIMITED
  		Already granted:        0:0:0
  		Pending/unsatisfied:    0:0:0
	- Successful walltime change gives us this:
		Accepted: walltime change request accepted for job 2876, it will be handled shortly.
	- Changing walltime is done by: oarwalltime jobID +hh:mm:ss
	 */
}
