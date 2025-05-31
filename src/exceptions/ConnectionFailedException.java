package exceptions;

import java.io.IOException;

public class ConnectionFailedException extends Exception {

	private static final long serialVersionUID = 1L;
	private IOException e;
	
	public ConnectionFailedException(IOException e) {
		this.e = e;
	}
	
	public IOException GetOriginalException() {
		return e;
	}
}
