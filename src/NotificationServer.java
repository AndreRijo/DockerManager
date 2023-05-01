import java.net.ServerSocket;

public class NotificationServer implements Runnable {
	
	public static final int PUBLIC_PORT_OFFSET = 100;
	public static final int PRIVATE_PORT_OFFSET = 200;
	
	public static final String PUBLIC_TEST_COMPLETE = "TEST_COMPLETE";
	
	//TODO: Well, do!
	private int notificationsPerTest;
	private ServerSocket publicS;
	private ServerSocket privateS;
	
	public NotificationServer(int notificationsPerTest, int basePort) {
		this.notificationsPerTest = notificationsPerTest;
		//publicS = new ServerSocket(basePort + PUBLIC_PORT_OFFSET);
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
