package exceptions;

public class OARTimeoutException extends OARException {

	private static final long serialVersionUID = 1L;

	public OARTimeoutException(String cmd) {
		super("Timeout when executing the following OAR command: " + cmd);
	}

	
}
