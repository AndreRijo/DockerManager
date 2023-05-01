import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class Client {
	
	public static final String LOG_LOCATION = "logs/";
	public static final String LOG_SUFFIX = ".log";
	public static final double TIMEOUT_FACTOR = 1;
	
	private TestConfigs originalConfigs;		//Useful for configurations defined via command line
	private TestConfigs configs;
	private TestConfigs[] allConfigs;
	private Command[] cmds, stopCmds;
	private Command cmd, stopCmd;
	private int node;
	private int nTestsDone;
	
	private PrintWriter out;
	private BufferedReader in;
	
	public Client(TestConfigs configs, Command cmd, Command stopCmd) {
		this.configs = configs;
		originalConfigs = configs;
		this.cmd = cmd;
		this.stopCmd = stopCmd;
	}
	
	public Client(TestConfigs configs) {
		this.configs = configs;
		originalConfigs = configs;
	}
	
	public void startTests() throws IOException {
		waitUntilStart();
		
		System.out.println("Using port: " + configs.getOwnPort());
		ServerSocket serverS = new ServerSocket(configs.getOwnPort());
		
		Socket socket = serverS.accept();
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
		int indexOffset = Messages.START_TEST.name().length();
		
		if (configs.isRemoteConfigs()) {
			receiveRemoteConfigs(in);
		}
		
		for (int i = 0; i < allConfigs.length; i++) {
			configs = allConfigs[i];
			cmd = cmds[i];
			stopCmd = stopCmds[i];
			executeTests(indexOffset);
		}
		
		waitForSignal();
		
		socket.close();
		serverS.close();
	}
	
	private void executeTests(int indexOffset) throws IOException {
		int receivedTest;
		boolean doTest;
		String cmdS = null, stopCmdS = null;
		Map<String, String> currValues;
		for (int i = 0; i < configs.getNTests(); i++) {
			System.out.printf("Waiting for test %d out of %d\n", i, configs.getNTests());
			//Wait for input
			String input = in.readLine();
			if (input.equalsIgnoreCase(Messages.REPAIR_TEST_OVER.name())) {
				nTestsDone += (configs.getNTests() - i - 1);
				break;
			};
			System.out.println("Received input, canStart. Input: " + input);
			Messages.valueOf(input.substring(0, indexOffset));
			receivedTest = Integer.parseInt(input.substring(indexOffset));
			if (receivedTest != i) {
				nTestsDone += (receivedTest - i - 1);
				i = receivedTest;
				System.out.printf("Skipping to test %d!\n", i);
			}
			//Do test
			doTest = configs.shouldRunTest(node, i);
			if (doTest) {
				currValues = configs.getTestValues(i);
				cmdS = cmd.buildCommand(currValues);
				stopCmdS = stopCmd.buildCommand(currValues);
			}
			//Wait for coordinator
			if (doTest)
				runTest(cmdS, stopCmdS);
			else {
				System.out.println("Skipping test " + i);
				skipTest();
			}
			
			out.println(Messages.TEST_DONE);
			nTestsDone++;
		}
		System.out.println("All tests done");
	}
	
	private void runTest(String cmdS, String stopCmdS) {
		System.out.println(cmdS);
		ProcessExecutioner ps = runStartCmd(cmdS);
		List<String> firstLog = null, secondLog = null;
		if (originalConfigs.shouldLog())
			firstLog = ps.getLog();
		System.out.println(stopCmdS);
		ps = new ProcessExecutioner(stopCmdS, true, originalConfigs.shouldLog(), false);
		ps.start();
		if (originalConfigs.shouldLog()) {
			secondLog = ps.getLog();
			saveLog(firstLog, secondLog, cmdS, stopCmdS);
		}
	}
	
	private ProcessExecutioner runStartCmd(String cmdS) {
		switch (originalConfigs.getMode()) {
		case TestConfigs.TIME:
			return runTimeStartCmd(cmdS);
		case TestConfigs.ACTIVE:
			return runActiveStartCmd(cmdS);
		case TestConfigs.PASSIVE:
			return runPassiveStartCmd(cmdS);
		}
		
		return null;
	}
	
	private ProcessExecutioner runTimeStartCmd(String cmdS) {
		System.out.println("Starting time test");
		long startTime = System.currentTimeMillis();
		ProcessExecutioner ps = new ProcessExecutioner(cmdS, false, originalConfigs.shouldLog(), true);
		ps.start();
		long diff = System.currentTimeMillis() - startTime;
		if (diff < configs.getTestTime() * 1000) {
			try {
				Thread.sleep(configs.getTestTime() * 1000 - diff);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return ps;
	}
	
	private ProcessExecutioner runActiveStartCmd(String cmdS) {
		System.out.println("Starting active test");
		ProcessExecutioner ps = new ProcessExecutioner(cmdS, true, originalConfigs.shouldLog(), false);
		boolean endedNormally = ps.startWithTimeout((int) (configs.getTestTime() * TIMEOUT_FACTOR));
		if (endedNormally)
			out.println(Messages.ACTIVE_OVER);
		else {
			System.out.println("[WARNING]Test timeout!");
			out.println(Messages.ACTIVE_OVER_TIMEOUT);
		}
		try {
			in.readLine();	//Wait for input.
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ps;
	}
	
	private ProcessExecutioner runPassiveStartCmd(String cmdS) {
		System.out.println("Starting passive test");
		ProcessExecutioner ps = new ProcessExecutioner(cmdS, false, originalConfigs.shouldLog(), true);
		ps.start(); 
		try {
			in.readLine();	//Wait for input.
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ps;
	}
	
	private void skipTest() {
		//If active, need to let the coordinator know still
		if (originalConfigs.getMode() == TestConfigs.ACTIVE) {
			out.println(Messages.ACTIVE_OVER);
		}
		//Wait for input regardless.
		try {
			in.readLine();	//Wait for input.
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void waitUntilStart() {
		long waitMillis = configs.getWaitTime() * 1000;
		if (waitMillis == 0) {
			return;
		}
		long waitedFor = 0;
		long startedSleeping = System.currentTimeMillis();
		System.out.println("Sleeping...");
		while (waitedFor < waitMillis - 1000) {
			try {
				Thread.sleep(waitMillis);
			} catch (InterruptedException e) {
				
			}
			waitedFor = System.currentTimeMillis() - startedSleeping;
		}
		System.out.println("Woke up, time to start testing.");
	}

	private void receiveRemoteConfigs(BufferedReader in) throws IOException {
		System.out.println("Waiting for remote configs...");
		int nConfigs = Integer.parseInt(in.readLine());
		allConfigs = new TestConfigs[nConfigs];
		allConfigs[0] = configs;
		for (int i = 1; i < allConfigs.length; i++)
			allConfigs[i] = new TestConfigs();
		cmds = new Command[nConfigs];
		stopCmds = new Command[nConfigs];
		
		for (int i = 0; i < nConfigs; i++) {
			node = Integer.parseInt(in.readLine());
			allConfigs[i].setNode(node);
			cmds[i] = new Command(in.readLine().trim());;
			stopCmds[i] = new Command(in.readLine().trim());
			allConfigs[i] = TestFileReader.readInputFile(in.readLine().trim(), allConfigs[i]);
		}
		System.out.println("Remote configs received.");
	}
	
	private void waitForSignal() throws IOException {
		System.out.println("Waiting for coordinator instructions...");
		String msg = in.readLine();
		if (msg.equalsIgnoreCase(Messages.START_AUTOMATIC_REPAIR.toString()))
			doAutomaticRepairs();
		System.out.println("Shutting down...");
	}
	
	private void doAutomaticRepairs() throws IOException {
		//Wait for repair cfg number
		System.out.println("Ready to start automatic repairs.");
		String msg = in.readLine();
		int cfgMsgOffset = Messages.AUTO_REPAIR_CFG_.name().length();
		int startTestOffset = Messages.START_TEST.name().length();
		
		while (msg.startsWith(Messages.AUTO_REPAIR_CFG_.toString())) {
			configs = allConfigs[Integer.parseInt(msg.substring(cfgMsgOffset))];
			System.out.println("Starting automatic repairs for " + configs.getName());
			executeTests(startTestOffset);
			msg = in.readLine();
		}
	}
	
	private void saveLog(List<String> firstLog, List<String> secondLog, String firstCmd, String secondCmd) {
		try {
			PrintWriter pw = new PrintWriter(new FileOutputStream(LOG_LOCATION + configs.getNode() + "_" + (nTestsDone + 1) + LOG_SUFFIX));
			pw.println(firstCmd);
			pw.println();
			for (String line: firstLog)
				pw.println(line);
			pw.println();
			pw.println("********** ********** ********** **********");
			pw.println(secondCmd);
			pw.println();
			for (String line: secondLog)
				pw.println(line);
			pw.close();
		} catch (IOException e) {
			System.err.println("Error while saving log file.");
			e.printStackTrace();
		}
	}
}
