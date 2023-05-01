import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import exceptions.OARException;

public class Coordinator {

	public static final boolean IS_AUTOMATIC = true;
	
	private CoordinatorConfigs coordConfigs;
	private TestConfigs mainConfig;		//Contains configs given through program arguments
	private TestConfigs[] multipleConfigs;
	private TimeManager time;
	private int currentConfigIndex;
	private PrintWriter[] outs;
	private BufferedReader[] ins;
	private String[][][] remoteCmds;	//config -> node -> array of cmds
	private int totalTime;
	private int totalTests, testsDone, testsFailed;
	private int[] waitFor;
	private AutoRepair repairs;
	
	public Coordinator(CoordinatorConfigs cCfg, TestConfigs[] multipleCfg) {
		coordConfigs = cCfg;
		mainConfig = multipleCfg[0];
		multipleConfigs = multipleCfg;
	}
	
	public Coordinator(CoordinatorConfigs cCfg, TestConfigs[] multipleCfg, String[][][] remoteCmds) {
		coordConfigs = cCfg;
		mainConfig = multipleCfg[0];
		multipleConfigs = multipleCfg;
		this.remoteCmds = remoteCmds;
	}

	public void manageTests() throws NumberFormatException, IOException, InterruptedException {
		waitUntilStart();
		if (coordConfigs.isAutomaticRepair())
			System.out.println("[INFO]Automatic repair is on.");
		time = new TimeManager(multipleConfigs);
		totalTime = time.getTotalTime();
		totalTests = time.getTotalTests();
		waitFor = coordConfigs.getWaitFor();
		repairs = new AutoRepair(multipleConfigs, coordConfigs.getSuspiciousOffset(), waitFor.length);
		String[] ips = mainConfig.getIPs();
		Socket[] clientSockets = new Socket[ips.length];
		outs = new PrintWriter[ips.length];
		ins = new BufferedReader[ips.length];
		
		for (int i = 0; i < ips.length; i++) {
			int separatorPos = ips[i].indexOf(':');
			clientSockets[i] = new Socket(ips[i].substring(0, separatorPos), 
					Integer.parseInt(ips[i].substring(separatorPos+1)));
			outs[i] = new PrintWriter(clientSockets[i].getOutputStream(), true);
			ins[i] = new BufferedReader(new InputStreamReader(clientSockets[i].getInputStream()));
		}
		
		if (mainConfig.isRemoteConfigs())
			sendRemoteConfigs();
		
		System.out.println("Duration of all tests: " + time.getTimeString(totalTime));
		Thread.sleep(4000);
		startTests();
		
	}
	
	private void sendRemoteConfigs() {
		System.out.println("Sending commands...");
		for (PrintWriter pw: outs)		//Let clients know how many configs to expect
			pw.println(multipleConfigs.length);
		for (int i = 0; i < multipleConfigs.length; i++) {
			String[][] testCmds = remoteCmds[i];
			for (int j = 0; j < outs.length; j++) {
				PrintWriter pw = outs[j];
				for (String cmd : testCmds[j])
					pw.println(cmd);
			}	
		}
		System.out.println("Commands sent to all nodes.");
	}
	
	private void startTests() throws IOException {
		currentConfigIndex = 0;
		testsDone = 1;
		for (TestConfigs cfg : multipleConfigs) {
			repairs.setCurrentConfigs(cfg);
			if (cfg.isRepair())
				doRepairTests(!IS_AUTOMATIC);
			else
				doTests();
			currentConfigIndex++;
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
				
			}
		}
		if (coordConfigs.isAutomaticRepair())
			doAutomaticRepair();
		signalClientShutdown();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void doTests() throws IOException {
		TestConfigs configs = multipleConfigs[currentConfigIndex];
		long start, finish;
		for (int i = 0; i < configs.getNTests(); i++, testsDone++) {
			start = System.currentTimeMillis();
			for (int j = 0; j < outs.length; j++)
				outs[j].println(Messages.START_TEST.name() + i);
			System.out.printf("Signals sent, waiting for test %d out of %d [%d/%d] to finish in all nodes\n", i+1, configs.getNTests(), testsDone, totalTests);
			int secondsLeft = configs.getTimeLeft(i);
			System.out.printf("Expected time left until all tests finish: (%s) [%s, predicted: %s]\n", 
					time.getTimeString(secondsLeft), time.getRemainingTimeString(secondsLeft, currentConfigIndex), time.getPredictedTimeLeft(secondsLeft, currentConfigIndex));
			waitForClientsFinish(i, start, configs.getTestTime(), false);
			System.out.printf("Test %d out of %d [%d/%d] has finished successfully in all nodes. Tests failed so far: %d\n", i+1, configs.getNTests(), testsDone, totalTests, testsFailed);
			finish = System.currentTimeMillis();
			time.registerTestTime((int)(finish - start)/1000, configs.getTestTime());
		}
		
		System.out.println("All tests done");
	}
	
	private void doRepairTests(boolean isAutomaticRepair) throws IOException {
		TestConfigs configs = multipleConfigs[currentConfigIndex];
		int nRepairs;
		Iterable<Integer> repairIt;
		if (!isAutomaticRepair) {
			IntRanges repairRange = configs.getRepairRange();
			nRepairs = repairRange.getRangeSize();
			repairIt = repairRange.getIterable();
		} else {
			repairIt = configs.getAutomaticRepairsIterable();
			nRepairs = configs.getNumberAutomaticRepairs();
		}
		int nRepairsDone = 0;
		int lastRepairDone = -1;
		//int i;
		long start, finish;
		//for (i = 0; i < configs.getNTests(); i++) {
			//if (repairRange.isInRange(i)) {

		for (Integer i : repairIt) {
				start = System.currentTimeMillis();
				for (int j = 0; j < outs.length; j++)
					outs[j].println(Messages.START_TEST.name() + i);
				System.out.printf("Signals sent, waiting for repair test %d out of %d (repair %d of %d repairs) [%d/%d] to finish in all nodes\n", i+1, 
						configs.getNTests(), nRepairsDone+1, nRepairs, testsDone, totalTests);
				System.out.printf("Expected time left until all repair tests finish: %s [%s, predicted: %s]\n", time.getTimeString(configs.getTimeLeft(nRepairsDone, nRepairs)), 
						time.getRemainingTimeString(configs.getTimeLeft(nRepairsDone, nRepairs), currentConfigIndex), time.getPredictedTimeLeft(configs.getTimeLeft(nRepairsDone, nRepairs), currentConfigIndex));
				waitForClientsFinish(i, start, configs.getTestTime(), isAutomaticRepair);
				System.out.printf("Test %d out of %d (repair %d of %d repairs) has finished successfully in all nodes. Tests failed so far: %d\n", i+1, configs.getNTests(), nRepairsDone+1, nRepairs,
						testsFailed);
				nRepairsDone++;
				testsDone++;
				lastRepairDone = i;
				finish = System.currentTimeMillis();
				time.registerTestTime((int)(finish - start)/1000, configs.getTestTime());
			//}
		}
		
		if (lastRepairDone + 1 < configs.getNTests())		//If this is false the clients have already finished by themselves.
			for (PrintWriter pw : outs)
				pw.println(Messages.REPAIR_TEST_OVER.name());
		
		System.out.println("All repair tests done");
	}
	
	//TODO: At the moment, auto-repairs won't repair tests that failed again. This would require modifying the list we are iterating or having a new repair list.
	private void waitForClientsFinish(int currentTest, long startTime, int duration, boolean isDoingAutoRepair) throws IOException {
		String read;
		long finishTime;
		if (waitFor != null) {
			//Wait for active nodes first
			boolean failed;
			for (int node : waitFor) {
				read = ins[node].readLine();
				while (!read.startsWith(Messages.ACTIVE_OVER.toString()))
					read = ins[node].readLine();
				if (!isDoingAutoRepair) {
					finishTime = System.currentTimeMillis();
					failed = repairs.registerTest(node, read, currentTest, (int)(finishTime - startTime)/1000, duration);
					if (failed) {
						System.out.printf("[WARNING] - Client %d reported a timeout for test %d!\n", node + 1, currentTest);
						testsFailed++;
					}
				}
			}
			repairs.resetSuspicion();
			System.out.println("Active nodes have finished executing current test.");
			//Give a second for passive clients to catch up
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				
			}
			//Need to send message to everyone
			for (int j = 0; j < outs.length; j++)
				outs[j].println(Messages.DO_STOP);
		}
		for (int j = 0; j < ins.length; j++) {
			read = ins[j].readLine();
			while (!read.equalsIgnoreCase(Messages.TEST_DONE.toString()))
				read = ins[j].readLine();
		}
	}
	
	private void waitUntilStart() {
		long waitMillis = mainConfig.getWaitTime() * 1000 + 5000; //Adds an extra 5s to ensure other clients have already woken up
		if (waitMillis == 5000)
			return;
		long waitedFor = 0;
		long startedSleeping = System.currentTimeMillis();
		System.out.println("Sleeping...");
		while (waitedFor < waitMillis - 1000) {
			try {
				Thread.sleep(waitMillis);
			} catch (InterruptedException e) {}
			waitedFor = System.currentTimeMillis() - startedSleeping;
		}
		System.out.println("Woke up, time to start testing.");
	}
	
	private void doAutomaticRepair() throws IOException {
		int repairTime = repairs.getRepairTime();
		int nTests = repairs.getTotalTestsFailed();
		if (nTests == 0) {
			System.out.println("No automatic repair is needed. Excellent!");
			if (coordConfigs.useOAR())
				OARHandler.deleteJob();
			return;
		}
		
		System.out.printf("Starting automatic repairing of %d tests, which is expected to take %s long.\n", nTests, time.getTimeString(repairTime));

		if (coordConfigs.useOAR())
			try {
				OARHandler.updateWallTime(repairTime, nTests);
			} catch (OARException e) {
				System.err.println("[WARNING]Error while interacting with OAR. Proceeding without walltime extension.");
				e.printStackTrace();
			}
		
		for (PrintWriter out: outs)
			out.println(Messages.START_AUTOMATIC_REPAIR.toString());
		currentConfigIndex = 0;
		
		for (TestConfigs cfg: multipleConfigs) {
			List<Integer> toRepair = repairs.getFailedTests(cfg);
			if (toRepair.isEmpty())
				System.out.println("Nothing to repair for test file " + cfg.getName());
			else {
				System.out.println("Starting automatic repairs for test file " + cfg.getName());
				for (PrintWriter out: outs)
					out.println(Messages.AUTO_REPAIR_CFG_.toString() + currentConfigIndex);
				cfg.setAutomaticRepairs(toRepair);
				doRepairTests(IS_AUTOMATIC);
				repairTime = repairs.getRepairTime(); nTests = repairs.getTotalTestsFailed();
				System.out.printf("Finished automatic repairs for current file. Tests left: %d; Time left: %s; Test repaired name: %s\n", 
						nTests, time.getTimeString(repairTime), cfg.getName());
			}
			currentConfigIndex++;
		}
		
		if (coordConfigs.useOAR())
			OARHandler.deleteJob();
	}
	
	private void signalClientShutdown() {
		for (PrintWriter out: outs)
			out.println(Messages.SHUTDOWN_CLIENT.toString());
		for (PrintWriter out: outs)
			out.flush();
	}
	
}
