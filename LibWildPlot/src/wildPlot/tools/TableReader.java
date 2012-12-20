/**
 * 
 */
package wildPlot.tools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class can read Data files for providing 2xn or 3xn array for use in PointDrawer2D and LinesPoints drawer
 * @author Michael Goldbach
 *
 */
public class TableReader {
	private double[][] table = new double[3][];
	
	private String fileName = "";

	/**
	 * Constructor of TableReader
	 * @param fileName file that contains table with point or BarGraph data
	 */
	public TableReader(String fileName) {
		super();
		this.fileName = fileName;
	}
	
	/**
	 * This method reads from table file and returns an array containing points data
	 * @return array with point data
	 * @throws IOException 
	 */
	public double[][] getPointArray() throws IOException{

			BufferedReader br = new BufferedReader(new FileReader(fileName));
			//check line count
			int lines = 0;
			int collumns = 0;
			String line = "";
			line = br.readLine();
			if(line != null) {
				lines++;
				collumns = line.split("\\t").length;
			}
			while((line = br.readLine())!= null){
				lines++;
			}
			br.close();
			table = new double[collumns][];
			//now read data
			br = new BufferedReader(new FileReader(fileName));
			for(int i = 0; i<collumns; i++){
				table[i] = new double[lines];
			}
			
			int lineNr = 0;
			while((line = br.readLine())!= null){
				String[] dataExtraction = line.split("\\t");
				
				for(int i = 0; i<collumns; i++){
					table[i][lineNr] = Double.parseDouble(dataExtraction[i]);
				}
				lineNr++;
			}
			br.close();

		
		return table;
	}
	
	
}
