package exceptions;

public class OARReplyException extends OARException {

	private static final long serialVersionUID = 1L;

	public OARReplyException(String cmd, String reply) {
		super("Unexpected reply for command " + cmd + ": " + reply);
	}

}
