import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestConfigs {

	public static final int TIME = 0;
	public static final int ACTIVE = 1;
	public static final int PASSIVE = 2;
	
	//TODO: Some configs can be moved to CoordinatorConfigs, like ips.
	
	private String[] ips;
	private String configName;
	//Seconds
	private int testTime;
	private int waitTime;
	private int nTests;
	private int node;
	
	private List<Integer>[] testToNodes;
	private Map<String, Variable> vars;
	private boolean isRepair;
	private IntRanges repairRange;
	private boolean remoteConfigs;
	private int ownPort;
	private int[] loopLevelsBase;	//Number of tests each loop level must wait before passing to the next value.
	private boolean shouldLog;
	private int notificationPort;	//Port for the client to run a notification server
	private int operationMode;
	private List<Integer> automaticRepairs;
	
	//Not yet used atm
	
	public TestConfigs() {
		vars = new HashMap<String, Variable>();
		isRepair = false;
		remoteConfigs = false;
		shouldLog = false;
		notificationPort = -1;
		operationMode = TIME;
	}
	
	public void setGlobalConfigs(String[] ips, int testT, int nTests) {
		this.ips = ips;
		testTime = testT;
		this.nTests = nTests;
	}
	
	public void setLoopLevels(int[] loopLevels) {
		loopLevelsBase = loopLevels;
	}
	
	public int[] getLoopLevels() {
		return loopLevelsBase;
	}
	
	public void setTestTime(int testT) {
		testTime = testT;
	}
	
	public String[] getIPs() {
		return ips;
	}
	
	public int getPort(int node) {
		return Integer.parseInt(ips[node].substring(ips[node].indexOf(':')+1));
	}
	
	public int getNNodes() {
		return ips.length;
	}

	public int getNTests() {
		return nTests;
	}
	
	public int getTestTime() {
		return testTime;
	}

	public void setTestsPerNode(List<Integer>[] testToNodes) {
		this.testToNodes = testToNodes;
	}
	
	public void setNode(int nodeN) {
		node = nodeN;
	}
	
	public int getNode() {
		return node;
	}
	
	public void setName(String path) {
		configName = path;
	}
	
	public String getName() {
		return configName;
	}
	
	public boolean shouldRunTest(int node, int test) {
		if (test < 0 || test >= testToNodes.length) {
			return false;
		}
		return testToNodes[test].contains(node);
	}

	public void addVar(String varName, RepeatType repeat, List<String> values) {
		vars.put(varName, new Variable(varName, repeat, values));
	}
	
	public Map<String, String> getTestValues(int testNumber) {
		Map<String, String> map = new HashMap<>();
		for (Variable var: vars.values())
			map.put(var.getVarName(), var.getValue(testNumber, nTests, loopLevelsBase));
		return map;
	}
	
	public Map<String, Variable> getVars() {
		return vars;
	}
	
	public int getTimeLeft(int currentTest) {
		return (nTests - currentTest) * testTime;
	}
	
	public int getTimeLeft(int currentTest, int totalTests) {
		return (totalTests - currentTest) * testTime;
	}

	public void addRepairRange(IntRanges repairRange) {
		isRepair = true;
		this.repairRange = repairRange;
	}
	
	public boolean isRepair() {
		return isRepair;
	}
	
	public IntRanges getRepairRange() {
		return repairRange;
	}

	public void setWaitTime(int wait) {
		waitTime = wait;
	}
	
	public int getWaitTime() {
		return waitTime;
	}
	
	public void useRemoteConfigs() {
		remoteConfigs = true;
	}
	
	public boolean isRemoteConfigs() {
		return remoteConfigs;
	}
	
	public void setOwnPort(int port) {
		ownPort = port;
	}
	
	public int getOwnPort() {
		if (ownPort != 0)
			return ownPort;
		return getPort(node);
	}
	
	public void setShouldLog(boolean doLog) {
		shouldLog = doLog;
	}
	
	public boolean shouldLog() {
		return shouldLog;
	}
	
	public void setNotificationPort(int notifyPort) {
		notificationPort = notifyPort;
	}
	
	public int getNotificationPort() {
		return notificationPort;
	}
	
	public boolean shouldRunNotificationServer() {
		return notificationPort != -1;
	}

	public void setMode(String modeS) {
		switch (modeS.trim().toLowerCase()) {
		case ClientMain.MODE_TIME:
			operationMode = TIME;
			break;
		case ClientMain.MODE_ACTIVE:
			operationMode = ACTIVE;
			break;
		case ClientMain.MODE_PASSIVE:
			operationMode = PASSIVE;
			break;
		default:
			operationMode = TIME;
			System.out.println("[WARNING]Unknown operation mode - defaulting to time.");
		}
	}
	
	public int getMode() {
		return operationMode;
	}
	
	public void setAutomaticRepairs(List<Integer> repairs) {
		automaticRepairs = repairs;
	}
	
	public Iterable<Integer> getAutomaticRepairsIterable() {
		return automaticRepairs;
	}
	
	public int getNumberAutomaticRepairs() {
		return automaticRepairs.size();
	}
	
	//Replaces the internal list with the one in the argument.
	//Pre: the node list in the argument must have the same length or higher than the original one
	public void replaceIPs(String[] newIPs) {
		if (newIPs.length == ips.length)
			ips = newIPs;
		else
			for (int i = 0; i < ips.length; i++) {
				ips[i] = newIPs[i];
			}
	}
}
