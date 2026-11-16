package it.unisa.ocelot.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.genetic.VariableTranslator;
import it.unisa.ocelot.suites.benchmarks.BenchmarkCalculator;
import jmetal.core.Solution;

/**
 * Provides static methods to do general things.
 * 
 * @author simone
 */

public class Utils {
	private static final String ENCODING = "UTF-8";
	
	public static final Debugger debugger = new Debugger();

	/**
	 * Returns the content of the file with the specified path.
	 * 
	 * @param pFilename the path of the file.
	 * @return the content of the file.
	 */
	public static String readFile(String pFilename) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(pFilename));
		return Charset.forName(ENCODING).decode(ByteBuffer.wrap(encoded))
				.toString();
	}

	public static boolean arrayContains(Object[] pArray, Object pContent) {
		for (int i = 0; i < pArray.length; i++) {
			if (pArray[i].equals(pContent)) {
				return true;
			}
		}
		return false;
	}
	
	public static void waitForEnterKey() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			in.readLine();
		} catch (Exception e) {
			//nothing to do
		}
	}

	/**
	 * Writes the specified content to the file with the specified path.
	 * 
	 * @param pFilename the path of the file.
	 * @param pContent the content to write on the file.
	 */
	public static void writeFile(String pFilename, String pContent)
			throws IOException {
		PrintWriter writer = new PrintWriter(pFilename, ENCODING);
		writer.print(pContent);
		writer.close();
	}

	public static List<String[]> makeSortedCombinations(String[] pList) {
		List<String[]> result = new ArrayList<>();
		for (int i = 0; i < pList.length; i++) {
			for (int j = i + 1; j < pList.length; j++) {
				result.add(new String[] { pList[j], pList[i] });
			}
		}

		return result;
	}
	
	public static Object[][][] getArguments(Solution solution, CType[] parametersTypes) {
		VariableTranslator translator = new VariableTranslator(solution);
		Object[][][] result = translator.translateArray(parametersTypes);
		return result;
	}
	
	public static String printArguments(Object[][][] pArguments) {
		String result = "";
		if (pArguments[0][0].length != 0) {
			result += "Values\t" + Arrays.toString(pArguments[0][0]) + "\n";
		}
		
		if (pArguments[1].length != 0) {
			result += "Arrays\n";
			for (int i = 0; i < pArguments[1].length; i++) {
				result += "\t" + Arrays.toString(pArguments[1][i]) + "\n";
			}
		}
		
		if (pArguments[2][0].length != 0) {
			result += "Pointers\t" + Arrays.toString(pArguments[2][0]) + "\n";
		}
		
		return result;
	}
	
	public static double safeDouble(double pValue) {
		//caps
		if (Double.isNaN(pValue) || Double.isInfinite(pValue)) {
			return Double.MAX_VALUE;
		}
		
		return pValue;
	}

	/**
	 * Calculates a normalized (branch distance) value as proposed by Arcuri in
	 * "It does matter how you normalize the branch distance in search 
	 * based software testing".
	 * 
	 * @return a {@code double} value of normalized branch distance.
	 */
	public static double normalize(double pValue) {
		return pValue / (pValue + 1);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void exportBenchmarksCSV(String csvFilename, List<BenchmarkCalculator<?>> benchmarks) throws IOException {
		if (benchmarks.size() == 0) {
			return;
		}
		
		List<String> labels = benchmarks.get(0).getLabels();
		List<Map<String, Object>> cumulativeResults = new ArrayList<>();
		List<Map<String, Object>> results = new ArrayList<>();
		
		String csv = "";
		String csvCumulative = "";
		
		String[] row = new String[benchmarks.size() + 1];
		String[] rowCumulative = new String[benchmarks.size() + 1];
		
		row[0] = "labels";
		int i = 1;
		for (BenchmarkCalculator benchmark : benchmarks) {
			cumulativeResults.add(benchmark.getCumulativeResults());
			results.add(benchmark.getResults());
			row[i] = benchmark.getName();
			i++;
		}
		
		csv = StringUtils.join(row, ",") + "\n";
		csvCumulative = StringUtils.join(row, ",") + "\n";
		for (String label : labels) {
			row[0] = label;
			rowCumulative[0] = label;
			
			for (i = 0; i < results.size(); i++) {
				row[i + 1] = results.get(i).get(label).toString();
				rowCumulative[i + 1] = cumulativeResults.get(i).get(label).toString();
			}
			
			csv += StringUtils.join(row, ",") + "\n";
			csvCumulative += StringUtils.join(rowCumulative, ",") + "\n";
		}
		
		Utils.writeFile(csvFilename + ".csv", csv);
		Utils.writeFile(csvFilename + "_cumulative.csv", csvCumulative);
	}
}
