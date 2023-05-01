import java.util.Scanner;

public class TestOarMain {

	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		System.out.println("Job ID?");
		OARHandler.setJobID(Integer.parseInt(in.nextLine()));
		System.out.println("Pretended job time (seconds)?");
		int targetSeconds = Integer.parseInt(in.nextLine());
		System.out.println("Requesting job extension...");
		try {
			OARHandler.updateWallTime(targetSeconds, 1);
			System.out.println("Job extended.");
		} catch (Exception e) {
			System.out.println("Exception while attempting an extension.");
			e.printStackTrace();
		}
		System.out.println("Delete job? (true/false)");
		boolean shouldDelete = Boolean.parseBoolean(in.nextLine());
		if (shouldDelete) {
			System.out.println("Requesting job deletion...");
			OARHandler.deleteJob();
			System.out.println("Job deletion request sent. Exitting program.");
		} else
			System.out.println("Exitting without deleting job.");
		in.close();
	}

}
