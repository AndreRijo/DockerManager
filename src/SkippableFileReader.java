

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class SkippableFileReader extends BufferedReader {

	public static final String COMMENT_TYPE1 = "//";
	public static final String COMMENT_TYPE2 = "#";
	
	//TODO: Support multi-line comments?
	
	public SkippableFileReader(Reader in) {
		super(in);
	}

	public SkippableFileReader(Reader in, int size) {
		super(in, size);
	}
	
	/**
	 * Returns the next line that isn't a comment or an empty line
	 * @return the next line of text that isn't a comment (i.e., doesn't start with "//" or "#"), or null when EOF is reached.
	 * @throws IOException if super.readLine() would throw an exception.
	 */
	public String readLineNotComment() throws IOException {
		String line = super.readLine();
		while (line != null && (line.startsWith(COMMENT_TYPE1) || line.startsWith(COMMENT_TYPE2) || line.equals(""))) {
			line = super.readLine();
		}
		return line;
	}
}
