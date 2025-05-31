import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessExecutioner {
	
	private String cmd;
	private Process proc;
	private boolean shouldWait, shouldLog, shouldPrint, failedByTimeout;
	private OutputReader reader;
	
	public ProcessExecutioner(String cmd, boolean shouldWait, boolean shouldLog, boolean shouldPrint) {
		this.cmd = cmd;
		this.shouldWait = shouldWait;
		this.shouldLog = shouldLog;
		this.shouldPrint = shouldPrint;
		failedByTimeout = false;
	}
	
	public void start() {
		try {
			//proc = Runtime.getRuntime().exec(cmd);
			proc = Runtime.getRuntime().exec(new String[] {"sh", "-c", cmd});
			//Runnable runnable = () -> {new BufferedReader(new InputStreamReader(proc.getInputStream())).lines().forEach(System.out::println);};
			reader = new OutputReader(proc.getInputStream(), shouldLog, shouldPrint);
			new Thread(reader).start();
			if (shouldWait)
				proc.waitFor();
		} catch (IOException | InterruptedException e) {
			System.err.println("Error while executing: " + cmd);
			e.printStackTrace();
		}
	}
	
	//Milliseconds
	//Returns true if the command ends normally; false if a timeout or an exception happens.
	public boolean startWithTimeout(int timeout) {
		try {
			proc = Runtime.getRuntime().exec(new String[] {"sh", "-c", cmd});
			reader = new OutputReader(proc.getInputStream(), shouldLog, shouldPrint);
			new Thread(reader).start();
			if (shouldWait) {
				failedByTimeout = !proc.waitFor(timeout, TimeUnit.SECONDS);
				return !failedByTimeout;
			}
		} catch (IOException | InterruptedException e) {
			System.err.println("Error while executing: " + cmd + " in timeout mode.");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public List<String> getLog() {
		return reader.getLog();
	}
	
	public boolean failedByTimeout() {
		return failedByTimeout;
	}
}

class OutputReader implements Runnable {
	
	private InputStream read;
	private boolean shouldLog, shouldPrint;
	private List<String> log;
	
	OutputReader(InputStream stream, boolean shouldLog, boolean shouldPrint) {
		read = stream;
		this.shouldLog = shouldLog;
		this.shouldPrint = shouldPrint;
		log = new LinkedList<>();
	}
	
	public void run() {
		if (shouldLog)
			new BufferedReader(new InputStreamReader(read)).lines().forEach(log::add);
		else if (shouldPrint)
			new BufferedReader(new InputStreamReader(read)).lines().forEach(System.out::println);
		else
			new BufferedReader(new InputStreamReader(read)).lines().forEach(this::ignoreString);
	}
	
	public List<String> getLog() {
		return log;
	}
	
	//Used to ignore the output of lines()
	private void ignoreString(String s) {
		
	}
}
