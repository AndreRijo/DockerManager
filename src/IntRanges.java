import java.util.LinkedList;
import java.util.List;

public class IntRanges {

	private int[] mins;
	private int[] maxs;
	
	//Same sizes. Each position must correspond to a range, i.e., min[0]-max[0], min[1]-max[1], etc.
	public IntRanges(int[] mins, int[] maxs) {
		this.mins = mins;
		this.maxs = maxs;
	}
	
	public static IntRanges stringToRanges(String[] stringRanges) {
		int[] mins = new int[stringRanges.length];
		int[] maxs = new int[stringRanges.length];
		
		for (int i = 0; i < stringRanges.length; i++) {
			//No test
			if (stringRanges[i].charAt(0) == '-') {
				mins[i] = -1;
				maxs[i] = -1;
				continue;
			}
			String[] parts = stringRanges[i].split("-");
			if (parts.length == 1) {
				int n = Integer.parseInt(parts[0].trim());
				mins[i] = n;
				maxs[i] = n;
			} else {
				mins[i] = Integer.parseInt(parts[0].trim());
				maxs[i] = Integer.parseInt(parts[1].trim());
			}
		}
		
		return new IntRanges(mins, maxs);
	}
	
	//Returns an IntRange of 0-max
	public static IntRanges getFullRange(int max) {
		int[] mins = {0};
		int[] maxs = {max};
		return new IntRanges(mins, maxs); 
	}
	
	public boolean isInRange(int number) {
		int i;
		for (i = 0; i < maxs.length; i++)
			if (maxs[i] >= number)
				break;
		if (i == maxs.length) {
			//Too big
			return false;
		}
		
		return mins[i] <= number;
	}
	
	public int getRangeSize() {
		int size = 0;
		for (int i = 0; i < maxs.length; i++) {
			size += maxs[i] - mins[i] + 1;
		}
		return size;
	}
	
	public Iterable<Integer> getIterable() {
		List<Integer> list = new LinkedList<>();
		int min, max;
		for (int i = 0; i < mins.length; i++) {
			min = mins[i]; max = maxs[i];
			for (int j = min; j <= max; j++)
				list.add(j);
		}
		
		return list;
	}
}
