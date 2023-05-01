
public class CoordinatorConfigs {

	private int[] coordWaitFor;
	private boolean isAutomaticRepair;
	private int suspiciousOffset;	//Any test that ends too early on all active nodes will be considered as failed.
	private boolean useOAR;
	private String[] ips;
	
	public CoordinatorConfigs() {
		isAutomaticRepair = false;
		useOAR = false;
		suspiciousOffset = 0;
	}
	
	public void setWaitFor(int[] waitFor) {
		coordWaitFor = waitFor;
	}
	
	public int[] getWaitFor() {
		return coordWaitFor;
	}
	
	public void setAutomaticRepair(boolean value) {
		isAutomaticRepair = value;
	}
	
	public void setUseOAR(boolean value) {
		useOAR = value;
	}
	
	public boolean isAutomaticRepair() {
		return isAutomaticRepair;
	}
	
	public boolean useOAR() {
		return useOAR;
	}
	
	//Returns true if there's a node list to override the ones in the test configurations
	public boolean hasNodeList() {
		return ips != null;
	}
	
	public String[] getNodeList() {
		return ips;
	}
	
	public void setNodeList(String[] ips) {
		this.ips = ips;
	}

	public void setSuspiciousOffset(int offset) {
		suspiciousOffset = offset;
	}
	
	public int getSuspiciousOffset() {
		return suspiciousOffset;
	}
}
