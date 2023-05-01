import java.time.LocalTime;

public class TimeManager {

	private static final int[] SECOND_FACTORS = {86400,3600,60,1};
	
	private int totalTime;
	private int totalTests;
	private TestConfigs[] cfgs;
	private int timeTaken;
	private int timeShouldHadTaken;
	
	public TimeManager(TestConfigs[] cfgs) {
		this.cfgs = cfgs;
		calculateTotals();
		timeTaken = 0;
		timeShouldHadTaken = 0;
	}
	
	private void calculateTotals() {
		for (TestConfigs cfg: cfgs)
			if (!cfg.isRepair()) {
				totalTime += cfg.getTestTime()*cfg.getNTests();
				totalTests += cfg.getNTests();
			}
			else {
				IntRanges range = cfg.getRepairRange();
				totalTime += cfg.getTimeLeft(0, range.getRangeSize());
				totalTests += range.getRangeSize();
			}
	}
	
	public int getTotalTime() {
		return totalTime;
	}
	
	public int getTotalTests() {
		return totalTests;
	}
	
	public void registerTestTime(int timeElapsed, int timeExpected) {
		timeTaken += timeElapsed;
		timeShouldHadTaken += timeExpected;
	}
	
	public static String staticGetTimeString(int timeSeconds) {
		String result = "";
		if (timeSeconds > 86400)	//1 days
			result += timeSeconds / 86400 + ":";
		result += LocalTime.MIN.plusSeconds(timeSeconds).toString();
		if (timeSeconds % 60 == 0) //no seconds
			result += ":00";
		return result;
		
	}
	
	public String getTimeString(int timeSeconds) {
		return TimeManager.staticGetTimeString(timeSeconds);
	}
	
	public String getRemainingTimeString(int currLeft, int currentConfigIndex) {
		return getTimeString(getTotalSecondsLeft(currLeft, currentConfigIndex));
	}
	
	public String getPredictedTimeLeft(int currLeft, int currentConfigIndex) {
		if (timeTaken == 0)
			return "unknown";
		int total = getTotalSecondsLeft(currLeft, currentConfigIndex);
		double factor = (double)timeTaken/(double)timeShouldHadTaken;
		return getTimeString((int) (total * factor));
	}
	
	private int getTotalSecondsLeft(int currLeft, int currentConfigIndex) {
		int total = currLeft;
		for (int i = currentConfigIndex + 1; i < cfgs.length; i++) {
			TestConfigs curr = cfgs[i];
			if (curr.isRepair())
				total += curr.getTimeLeft(0, curr.getRepairRange().getRangeSize());
			else
				total += (curr.getTestTime() * curr.getNTests());
		}
		return total;
	}
	
	//Format: dd:hh:mm:ss. All but ss are optional, will always assume that the ones missing are the most significant ones.
	public static int stringToSeconds(String time) {
		String[] parts = time.split(":");
		int total = 0;
		for (int i = parts.length, j = SECOND_FACTORS.length; i > 0; i--, j--)
			total += Integer.parseInt(parts[i].trim()) * SECOND_FACTORS[j];
		return total;
	}
}
