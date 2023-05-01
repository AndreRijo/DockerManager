import java.util.List;

public class Variable {

	private String name;
	private RepeatType repeat;
	private List<String> values;
	private int loopLevel;
	
	public Variable(String name, RepeatType repeat, List<String> values) {
		this.name = name;
		this.repeat = repeat;
		this.values = values;
		setLoopLevel();
	}
	
	private void setLoopLevel() {
		if (repeat.equals(RepeatType.LOOP))
			loopLevel = 0;
		else if (repeat.name().startsWith("LOOP_"))
			loopLevel = repeat.name().charAt(repeat.name().length() - 1) - '0';
		else
			loopLevel = -1;
	}
	
	public String getVarName() {
		return name;
	}
	
	public RepeatType getRepeatType() {
		return repeat;
	}
	
	/**
	 * Note: testNumber can be higher than the number of different values - this method will calculate the right position.
	 */
	public String getValue(int testNumber, int maxTests, int[] loopLevelsBase) {
		if (loopLevel == 0)
			return values.get(testNumber % values.size());
		else if (loopLevel == -1) {
			int split = maxTests/values.size();
			return values.get(testNumber/split);
		}
		return values.get(testNumber / loopLevelsBase[loopLevel] % values.size());
	}
	
	public int getNumberOfValues() {
		return values.size();
	}
	
}
