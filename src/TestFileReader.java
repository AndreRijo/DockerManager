import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TestFileReader {
	
	public static TestConfigs readInputFile(String location) throws IOException {
		return readInputFile(location, new TestConfigs());
		
	}

	public static TestConfigs readInputFile(String location, TestConfigs initialConfigs) throws IOException {
		File file = new File(location);
		if (!file.exists() || file.isDirectory()) {
			throw new FileNotFoundException("Failed to find input file (or it's a folder) at " + location);
		}
		SkippableFileReader in = new SkippableFileReader(new FileReader(file));
		initialConfigs.setName(location);
		
		readGlobalConfigs(initialConfigs, in);
		readTestsRanges(initialConfigs, in);
		readVars(initialConfigs, in);
		calculateLoopLevels(initialConfigs);
		
		return initialConfigs;
	}
	
	private static void readGlobalConfigs(TestConfigs configs, SkippableFileReader in) throws IOException {
		int nClients = Integer.parseInt(in.readLineNotComment().trim());
		String[] ips = new String[nClients];
		for (int i = 0; i < nClients; i++)
			ips[i] = in.readLineNotComment().trim();
		int testTime = Integer.parseInt(in.readLineNotComment().trim());
		int nTests = Integer.parseInt(in.readLineNotComment().trim());
		configs.setGlobalConfigs(ips, testTime, nTests);
	}
	
	private static void readTestsRanges(TestConfigs configs, SkippableFileReader in) throws IOException {
		int nNodes = configs.getNNodes();
		int nTests = configs.getNTests();
		IntRanges[] ranges = new IntRanges[nNodes];
		String line;
		for (int i = 0; i < nNodes; i++) {
			line = in.readLineNotComment().toLowerCase();
			if (line.contains("all"))
				ranges[i] = IntRanges.getFullRange(nTests-1);
			else
				ranges[i] = IntRanges.stringToRanges(line.split(","));
		}
		
		@SuppressWarnings("unchecked")
		List<Integer>[] testToNodes = new List[nTests];
		for (int i = 0; i < nTests; i++)
			testToNodes[i] = new LinkedList<>();
		for (int i = 0; i < nTests; i++)
			for (int j = 0; j < nNodes; j++)
				if (ranges[j].isInRange(i))
					testToNodes[i].add(j);
		
		configs.setTestsPerNode(testToNodes);
	}
	
	private static void readVars(TestConfigs configs, SkippableFileReader in) throws IOException {
		int nVars = Integer.parseInt(in.readLineNotComment().trim());
		for (int i = 0; i < nVars; i++)
			readVar(configs, in);
	}
	
	private static void readVar(TestConfigs configs, SkippableFileReader in) throws IOException {
		String varName = in.readLineNotComment().trim();
		System.out.println("VarName: " + varName);
		RepeatType repeat = RepeatType.valueOf(in.readLineNotComment().trim());
		int nValues = Integer.parseInt(in.readLineNotComment().trim());
		
		List<String> values = new ArrayList<String>(nValues);
		for (int i = 0; i < nValues; i++)
			values.add(in.readLineNotComment().trim());
		configs.addVar(varName, repeat, values);
	}
	
	private static void calculateLoopLevels(TestConfigs configs) {
		int[] levels = new int[10];
		/*
		levels[0] = 1;
		for (Variable var : configs.getVars().values()) {
			RepeatType type = var.getRepeatType();
			int nValues = var.getNumberOfValues();
			if (!type.name().startsWith("LOOP_") || nValues == 0)
				continue;
			int level = type.name().charAt(type.name().length() - 1) - '0';
			if (level +1 == levels.length)
				continue;
			if (level == 0)
				levels[0] = nValues;
			else
				levels[level + 1] = levels[level] * nValues;
		}
		configs.setLoopLevels(levels);
		*/
		//First step: Find number of values for each level
		for (Variable var : configs.getVars().values()) {
			RepeatType type = var.getRepeatType();
			int nValues = var.getNumberOfValues();
			if (!type.name().startsWith("LOOP_") || nValues == 0)
				continue;
			int level = type.name().charAt(type.name().length() - 1) - '0';
			levels[level+1] = nValues;
		}
		levels[0] = 1;
		//Second step: calculate starting points
		for (int i = 1; i < levels.length; i++)
			levels[i] = levels[i] * levels[i-1];
		//TODO: Remove
		System.out.print("Levels: [");
		for (int i = 0; i < levels.length; i++)
			System.out.print(levels[i] + ", ");
		System.out.println("]");
		configs.setLoopLevels(levels);
	}
}
