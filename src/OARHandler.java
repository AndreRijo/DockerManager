import java.util.List;

import exceptions.OARException;
import exceptions.OARReplyException;
import exceptions.OARTimeoutException;

//This is useless as nodes don't have access to OAR commands lol.

public class OARHandler {
	
	public static final int REQUEST_TIMEOUT = 20;	//seconds. Useful in case OAR has an issue
	public static final String REQUEST_USED_TIME = "oarstat -j %d";
	public static final String REQUEST_TOTAL_TIME = "oarwalltime %d";
	public static final String REQUEST_TIME_EXTENSION = "oarwalltime %d +%s";		//Maybe I can use one of TimeManager formatting options.
	public static final String REQUEST_JOB_DELETION = "oardel %d";
	public static final String OARHANDLER = "[OARHandler]";
	
	private static int JOB_ID;
	private static String JOB_ID_STRING;
	
	public static void setJobID(int jobId) {
		JOB_ID = jobId;
		JOB_ID_STRING = "" + jobId;
	}

	public static void updateWallTime(int secondsNeeded, int nTests) throws OARException {
		//TODO: A global exception catcher; if it's a OARException throw it above, any other handle it here and ignore.
		try {
			System.out.println("Requesting OAR for time left on job " + JOB_ID);
			int usedTime = getUsedTime();
			int wallTime = getTotalTime();
			int secondsLeft = wallTime - usedTime;
			int needToRequest = secondsNeeded - secondsLeft;
			needToRequest += 180;	//A bit of extra room
			String leftS = TimeManager.staticGetTimeString(secondsLeft);
			String neededS = TimeManager.staticGetTimeString(secondsNeeded);
			if (needToRequest > 0) {
				String requestS = TimeManager.staticGetTimeString(needToRequest);
				System.out.printf("Job %d has %s time left, but %s time is needed. Requesting a walltime extension of %s.\n", JOB_ID, leftS, neededS, requestS);
				requestTimeExtension(neededS);
			} else
				System.out.printf("There's enough time left on job %d. Time left: %s. Time needed: %s\n", JOB_ID, leftS, neededS);
		} catch (OARException e) {
			throw e;
		} catch (Exception e) {
			System.out.println("An unexpected exception has occoured. This is likely a bug on the program or a change to the OAR commands.");
			e.printStackTrace();
		}
	}
	
	public static void deleteJob() {
		String cmd = String.format(REQUEST_JOB_DELETION, JOB_ID);
		System.out.println(OARHANDLER + "Requesting deletion of job " + JOB_ID);
		try {
			executeProcess(cmd, false);
		}
		catch (OARTimeoutException e) {
			//Never happens as we don't wait
		}
	}

	private static int getUsedTime() throws OARException {
		String cmd = String.format(REQUEST_USED_TIME, JOB_ID);
		System.out.println("Sending cmd: " + cmd);
		List<String> log = executeProcess(cmd);
		try {
		String info = log.get(log.size() - 1).trim();
		String jobId = info.substring(0, JOB_ID_STRING.length());
		
		//sanity check - we probably got an error message if this doesn't happen.
		if (!jobId.equals(JOB_ID_STRING))
			throw new OARReplyException(cmd, info);
		info = info.substring(jobId.length());
		
		return TimeManager.stringToSeconds(getDurationFromOarStat(info));
		
		} catch (Exception e) {
			System.err.println("Error in getUsedTime(). Log:");
			for (String s: log) {
				System.out.println(s);
			}
			throw e;
		}
	}
	
	private static int getTotalTime() throws OARException {
		String cmd = String.format(REQUEST_TOTAL_TIME, JOB_ID);
		List<String> log = executeProcess(cmd);
		if (log.size() < 2)
			throw new OARReplyException(cmd, log.get(0));
		
		String info = null;
		//A for prepared in case OAR ever changes the print order.
		for (String line : log) {
			//We could theorically skip the first line
			if (line.toLowerCase().contains("Current")) {
				info = line;
				break;
			}
		}
		if (info == null)
			throw new OARReplyException(cmd, listStringToString(log));
		
		info = info.substring(info.indexOf(':') + 1).trim();
		return TimeManager.stringToSeconds(info);
	}

	private static void requestTimeExtension(String timeNeeded) throws OARException {
		String cmd = String.format(REQUEST_TIME_EXTENSION, JOB_ID, timeNeeded);
		List<String> log = executeProcess(cmd);
		System.out.println(OARHANDLER + log.get(log.size() - 1));
	}
	
	//Note: shouldWait is true
	private static List<String> executeProcess(String cmd) throws OARTimeoutException {
		return executeProcess(cmd, true);
	}
	
	private static List<String> executeProcess(String cmd, boolean shouldWait) throws OARTimeoutException {
		ProcessExecutioner ps = new ProcessExecutioner(cmd, shouldWait, true, false);
		boolean success = ps.startWithTimeout(REQUEST_TIMEOUT);
		if (!success)
			throw new OARTimeoutException(cmd);
		return ps.getLog();
	}
	/*
	1) oarstat -j JOB_ID
	2) oarwalltime JOB_ID
	3) oarwalltime JOB_ID +??:??:??
 */

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
	
	private static String getDurationFromOarStat(String s) throws OARException {
		char test = 'a';
		int firstPos, secondPos;
		for (firstPos = 0; firstPos < s.length(); firstPos++) {
			test = s.charAt(firstPos);
			if (test >= '0' && test <= '9')
				break;
		}
		if (test == 'a') {
			//error
		}
		for (secondPos = firstPos + 1; secondPos < s.length(); secondPos++) {
			test = s.charAt(firstPos);
			if (test < '0' || test > '9')
				break;
		}
		return s.substring(firstPos, secondPos);
	}
	
	private static String listStringToString(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (String s : list) {
			sb.append(s);
			sb.append('\n');
		}
		return sb.toString();
	}
}

