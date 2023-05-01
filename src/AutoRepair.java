import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AutoRepair {
	
	public static final String TIMEOUT = "TIMEOUT";
	public static final String SAVE_FOLDER = "DM_Logs/";
	public static final String LOG = ".log";

	private Map<TestConfigs,List<Integer>> toRepair;
	private List<Integer> currentTest;
	private TestConfigs currentCfg;
	private List<String> logOutput;
	private StringBuilder currentTestString;
	//private String fileName;
	private File file;
	private int nTestsFailed, repairTime, suspiciousOffset, nActive, suspiciousCount;
	
	public AutoRepair(TestConfigs[] allCfgs, int suspiciousOffset, int nActive) {
		toRepair = new HashMap<>();
		for (TestConfigs cfg : allCfgs)
			toRepair.put(cfg, new LinkedList<>());
		logOutput = new LinkedList<>();
		prepareFile();
		nTestsFailed = 0; repairTime = 0; this.suspiciousOffset = suspiciousOffset; this.nActive = nActive; suspiciousCount = 0;
	}
	
	private void prepareFile() {
		logOutput.add("*****List of tests that went through repairs*****");
		logOutput.add("");

		String fileName = SAVE_FOLDER + "testLog" + java.time.LocalDate.now().toString() + "_" + java.time.LocalTime.now().toString() + LOG;
		file = new File(fileName);
		if (!file.exists()) {
			try {
				file.createNewFile();
				System.out.println("Coordinator log file successfully initialized at " + file.getAbsolutePath());
			} catch (IOException e) {
				System.out.println("[WARNING]Failed to create log file for coordinator.");
			}
		}
	}
	
	public void setCurrentConfigs(TestConfigs cfg) {
		if (currentTest != null)
			if (currentTest.isEmpty())
				logOutput.add(currentTestString.append("all tests successfull.").toString());
		currentTest = toRepair.get(cfg);
		currentCfg = cfg;
		addLogConfig(cfg);
	}
	
	//Returns true if the test failed.
	public boolean registerTest(int node, String clientMsg, int testNumber, int time, int expectedDuration) {
		if (testNumber == -1) {	//No test, skipping this config.
			return true;
		}
		if (clientMsg.trim().endsWith(TIMEOUT)) {
			System.out.printf("%s from node%d for test %d was a failed test.\n", clientMsg, node+1, testNumber);
			if (currentTest.size() > 0 && currentTest.get(currentTest.size() - 1) == testNumber) {
				//A test may fail in multiple nodes and, thus, be attempted to be registered multiple times
				System.out.println("Repeated fail, thus ignored.");
				return false;
			}
			return registerTestHelper(node, testNumber, "");
		} else if (expectedDuration - time >= suspiciousOffset) {
			//Commented this as it would trigger on every single dataload.
			//System.out.printf("%s from node%d for test %d ended too early! It may be a failed test.");
			suspiciousCount++;
			if (suspiciousCount == nActive) {
				System.out.printf("[WARNING] - Test %d ended too early in all nodes! Marking it as a failed test.\n", testNumber);
				return registerTestHelper(node, testNumber, "[Early end]");
			}
		} else
			System.out.printf("%s from node%d for test %d was not a failed test.\n", clientMsg, node+1, testNumber);
		return false;
	}
	
	//Used when we change to a new test
	public void resetSuspicion() {
		suspiciousCount = 0;
	}
	
	private boolean registerTestHelper(int node, int testNumber, String logHeader) {
		nTestsFailed++;
		currentTest.add(testNumber);
		repairTime += currentCfg.getTestTime();
		currentTestString.append(String.format("%s%d (node %d), ", logHeader, testNumber, node + 1));
		saveLog();
		return true;
	}
	
	private void addLogConfig(TestConfigs cfg) {
		currentTestString = new StringBuilder();
		currentTestString.append(cfg.getName() + ": ");
	}
	
	private void saveLog() {
		if (!file.exists()) {
			System.out.println("[WARNING]Did not log test failure as file creation has failed.");
			return;
		}
		try {
			PrintWriter pw = new PrintWriter(new FileOutputStream(file));
			for (String s : logOutput)	
				pw.println(s);
			pw.println(currentTestString.toString());
			pw.close();
			System.out.println("Failure log written successfully.");
		} catch (IOException e) {
			//Should never happen
			System.err.println("[ERROR]Failed to write automatic repairs log file.");
			e.printStackTrace();
		}
	}
	
	public List<Integer> getFailedTests(TestConfigs cfg) {
		return toRepair.get(cfg);
	}
	
	public int getTotalTestsFailed() {
		return nTestsFailed;
	}
	
	public int getRepairTime() {
		return repairTime;
	}
	
	public void setConfigAsRepaired(TestConfigs cfg) {
		List<Integer> repaired = toRepair.remove(cfg);
		nTestsFailed -= repaired.size();
		repairTime -= (cfg.getTestTime() * nTestsFailed);
	}
	
}
