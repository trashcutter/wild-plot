package wildPlot.parsing;

import java.util.Scanner;
import java.util.Vector;

import javax.swing.JFrame;

import wildPlot.gui.PlotControl;

/**
 * As a centerpiece of the "WildPlot" project this class provides a menu prompt for term calculation, function plotting and table plotting. 
 * User input is parsed and internal methods are executed according to the entered commands. Calculations are confined to real number values.
 * This class utilizes the PlotControl and the FunctionParser classes.
 * @author C. Otto, R. Meier
 *
 */
public class ProgramParser {

	/* The internal FunctionParser object used to store function information and perform computations. */
	private FunctionParser fParse;
	
	/* The internal PlotControl object used to create graphs. */
	private PlotControl pCtrl;
	
	/* The number of unnamed functions that are used in the plot */
	private int temporaryFunctionCount=0;
	
	/* Determines whether the process should keep / is running or not. */
	private static boolean programRunning = true;
	
	/* Scanner object used to handle user console input. */
	private static Scanner inputScan = new Scanner(System.in);
	
	/* Info String that will be printed if the help command is called by the user.*/
	private static final String infoText = 
			"\n####################"+
					"\n# list of commands #" +
					"\n####################"+
					"\nexit\t\tshut down program" +
					"\n'x'('t')='y'\tdefine a function (for example: f(x)=x^2+5)" +
					"\nplot(args)\tcreate a plot" +
					"\nplot(\"file\")\tcreate a point-plot" +
					"\ninterpol(\"file\")\tcreate a plot with spline interpolated points" +
					"\nxlim(x0,x1)\tchange x-limits of the plot" +
					"\nylim(y0,y1)\tchange y-limits of the plot" +
					"\nsetgrid()\tactivate grid" +
					"\nunsetgrid()\tdeactivate grid" +
					"\nsetAxisOnFrameBorder()\tset both x- and y-axis on frame border" +
					"\nunsetAxisOnFrameBorder()\treset axis on border behavior" +
					"\nxtics(size)\tset size between marks on x-axis" +
					"\nunsetxtics()\tlet the program calculate x-tics" +
					"\nytics(size)\tset size between marks on y-axis" +
					"\nunsetytics()\tlet the program calculate y-tics" +
					"\nsetyAxisOffset(offset)\talign the y-axis given x-axis point" +
					"\nsetxAxisOffset(offset)\talign the x-axis given <-axis point" +
					"\nunsetxAxisOffset()\tset x-axis to go through y=0" +
					"\nunsetyAxisOffset()\tset y-axis to go through x=0" +
					"\nlinespoints(\"file\")\t plot file with point data as points connected with lines" +
					"\nhistogram(\"file\")\t plot data from file as histogram, only y values will be used, file still needs two collums " +
					"\nbargraph(\"file\")\t plot file with point data as bars" +
					"\nsplot(functionName)\t experimental relief plot of 2d functions, define functions using additional y variables" +
					"\nintegral(functionName1, functionName2, leftLimit, rightLimit) or integral(functionName, leftLimit, rightLimit)" +
					"\n\t-> plot integral between two functions or function and x-axis" +
					"\nsetFrame() or setFrame(size)\tactivate frame with 100 pixel or activate it with given size in px" +
					"\nunsetFrame()\tdeactivate frame around plot" +
					"\nsetFrameBorder() or setFrameBorder(size)\t activate frame border or activate it with given size in px" +
					"\nunsetFrameBorder()\tmake frame border disappear" +
					"\nsetxName(\"name\")\tset x-axis name" +
					"\nsetyName(\"name\")\tset y-axis name" +
					"\nsetPlotTitle(\"name\")\tset title of plot"+
					"\nunsetPlotTitle(\"name\")\tunset title of plot"+
					"\nxminortics(value)\tset minor xtics"+
					"\nyminortics(value)\tset minor ytics"+
					"\n";
	
	/**
	 * Constructor to initialize a new ProgrammParser object
	 */
	public ProgramParser(){
		fParse = new FunctionParser();
		pCtrl = new PlotControl(fParse);
	}
	
	/**
	 * Returns the PlotController
	 * @return
	 */
	public PlotControl plotCtrl(){
		return pCtrl;
	}
	
	/**
	 * WildPlot main, top level, executable method.
	 * @param args
	 */
	public static void main(String[] args) {
		ProgramParser fp = new ProgramParser();
		
		/* tests an examples */
//		fp.parse("f(s)=s^2");
//		fp.parse("z(t)=sin(t)");
//		fp.parse("michaKannProggen(u)=-log(u)");
//		fp.parse("plot(z,f,michaKannProggen)");
//		fp.parse("x(s)=s^3");
//		double[] borders = fp.fParse.monothonyAnalysis("x");
//		System.out.println( borders[0]+" :TO: "+borders[1] );
		/* ----------------- */
		
		System.out.println(
			"#######################\n"+
			"# Welcome to WildPlot #\n"+
			"#######################\n"
		);
		while(programRunning){
			System.out.print("<wp>: ");
			fp.parse( fineForm(inputScan.nextLine()) );
		}
	}
	
	/**
	 * This method obtains an input string and returns a fine form that is required for the FunctionParser and
	 * TermParser to work. All whitespace characters outside of "..." blocks are deleted.
	 * @param input string
	 * @return
	 */
	private static String fineForm(String input){
		String fineForm="";
		boolean literalExpression=false;
		for(int i=0; i<input.length(); i++){
			char current=input.charAt(i);
			if(current=='\"') {
				if(literalExpression) literalExpression=false;
				else literalExpression=true;
			}
			else if(current==' ' && !literalExpression) continue;
			fineForm+=current;
		}
		// FIXME: TermParser::pot() method has to be changed OR this method must ensure brackets in "x^y_z" expressions.
		// "x^y_z" ==> "x^(y_z)"		"x_y^z^a_b" ==> "x_(y^(z^(a_b)))"		???
		return fineForm;
	}
	
	/**
	 * The top level parsing method handling input strings in fine form. If the input is no bracket-less command
	 * the program tries to execute it as bracket-dependent command. If this fails the input is handled as a new
	 * input function or term.
	 * @param input
	 */
	public void parse(String input){
		if(input == null || input.length()<1) return;
		if(input.equals("quit") || input.equals("exit")) {
			programRunning = false;
			pCtrl.stop();
			JFrame container = pCtrl.getJFrame();
			if(container != null)
				container.dispose();
		}
		else if(input.equals("replot")){
			pCtrl.stop();
			JFrame container = pCtrl.getJFrame();
			if(container != null)
				container.dispose();
			pCtrl.start();
		}
		else if(input.equals("help")) System.out.println(infoText);
		else if(!processCommand(input)){
			if(!fParse.parse(input)) System.err.println("Invalid input expression.");
		}
	}

	/**
	 * This method tries to handle the input as a bracket-dependent command and reads out the information contained in the brackets.
	 * If a bracket is missing and the method fails to process the command, it will return false. Otherwise it processes the command and returns true.
	 * @param cmd
	 * @return
	 */
	private boolean processCommand(String cmd){
		String command="", argument="";
		boolean readArgument=false, corrupt=true;
		for(int i=0; i<cmd.length()-1; i++){
			if(!readArgument && cmd.charAt(i) == '(') readArgument=true;
			else if(readArgument) argument += cmd.charAt(i);
			else command += cmd.charAt(i);
		}
		if(cmd.charAt(cmd.length()-1) == ')') corrupt=false;
		if(!corrupt) {
			if( process(command, argument) ) return true;
		}
		return false;
	}
	
	/**
	 * This method processes an input command and its arguments if its String representation is valid.
	 * If it fails to recognize the command String, it tries to compute the image value of the (assumed existing) 
	 * function with the name of the command String at the domain value specified by the argument String.
	 * If any of these operations was successful "true" is returned, else "false" is returned.
	 * @param command
	 * @param argument
	 * @return whether input operation was successful or not
	 */
	private boolean process(String command, String argument){
		String[] arguments = argument.split(",");
		
		if(command.equals("plot")){
			for(String arg:arguments){
				if(arg.contains("\"")){
					pCtrl.tablePlot(arg.replace("\"", ""), false);
				}
				else{
					if( fParse.containsFunction(arg) ){
						double[] border = fParse.monotonyAnalysis(arg);
						pCtrl.plot(arg,border[1],border[0]);
					} else {
						if(!fParse.parse("TEMP"+temporaryFunctionCount+"(x)="+arg)) 	// try to apply temporaryFunction
							System.err.println("Invalid input expression.");
						else{		// if successful plot the temporary function
							double[] border = fParse.monotonyAnalysis("TEMP"+temporaryFunctionCount);
							pCtrl.plot("TEMP"+temporaryFunctionCount,border[1],border[0]);
							temporaryFunctionCount++;
						}
					}
				}
			}
			pCtrl.stop();
			JFrame container = pCtrl.getJFrame();
			if(container != null)
				container.dispose();
			pCtrl.start();
			return true;
		}
		else if(command.equals("interpol")){
			for(String arg:arguments){
				if(arg.contains("\"")){
					pCtrl.tablePlot(arg.replace("\"", ""), true);
				}
				else{
					//TODO a warning might be in order
				}
			}
			pCtrl.stop();
			JFrame container = pCtrl.getJFrame();
			if(container != null)
				container.dispose();
			pCtrl.start();
			return true;
		}
		else if(command.equals("xlim")){
			double x0, x1;
			try{
				x0 = Double.parseDouble(arguments[0]); 
				x1 = Double.parseDouble(arguments[1]);
			} catch(NumberFormatException e){
				System.err.println("Invalid x-limits.");
				return false;
			}
			pCtrl.setXlim(x0, x1);
			return true;
		}
		else if(command.equals("ylim")){
			double y0, y1;
			try{
				y0 = Double.parseDouble(arguments[0]); 
				y1 = Double.parseDouble(arguments[1]);
			} catch(NumberFormatException e){
				System.err.println("Invalid y-limits.");
				return false;
			}
			pCtrl.setYlim(y0, y1);
			return true;
		}
		// commands requested by M. Goldbach
		else if(command.equalsIgnoreCase("setgrid")) {
			pCtrl.setGrid();
			return true;
		}
		else if(command.equalsIgnoreCase("unsetgrid")) {
			pCtrl.unsetGrid();
			return true;
		}
		else if(command.equalsIgnoreCase("setAxisOnFrameBorder".toLowerCase())) {
			pCtrl.setAxisOnFrameBorder();
			return true;
		}
		else if(command.equalsIgnoreCase("unsetAxisOnFrameBorder".toLowerCase())) {
			pCtrl.unsetAxisOnFrameBorder();
			return true;
		}
		else if(command.equalsIgnoreCase("xtics")){
			double x0;
			try{
				x0 = Double.parseDouble(arguments[0]); 
			} catch(NumberFormatException e){
				System.err.println("Invalid x-tics.");
				return false;
			}
			pCtrl.setXtics(x0);
			return true;
		}
		else if(command.equalsIgnoreCase("unsetxtics")){
			pCtrl.unsetXtics();
			return true;
		}
		else if(command.equalsIgnoreCase("ytics")){
			double x0;
			try{
				x0 = Double.parseDouble(arguments[0]); 
			} catch(NumberFormatException e){
				System.err.println("Invalid y-tics.");
				return false;
			}
			pCtrl.setYtics(x0);
			return true;
		}
		else if(command.equalsIgnoreCase("yminortics")){
			double x0;
			try{
				x0 = Double.parseDouble(arguments[0]); 
			} catch(NumberFormatException e){
				System.err.println("Invalid y-tics.");
				return false;
			}
			pCtrl.setMinorYtics(x0);
			return true;
		}
		else if(command.equalsIgnoreCase("xminortics")){
			double x0;
			try{
				x0 = Double.parseDouble(arguments[0]); 
			} catch(NumberFormatException e){
				System.err.println("Invalid y-tics.");
				return false;
			}
			pCtrl.setMinorXtics(x0);
			return true;
		}
		else if(command.equalsIgnoreCase("unsetytics")){
			pCtrl.unsetYtics();
			return true;
		}
		else if(command.equalsIgnoreCase("unsetminorytics")){
			pCtrl.unsetMinorYtics();
			return true;
		}
		else if(command.equalsIgnoreCase("unsetminorxtics")){
			pCtrl.unsetMinorXtics();
			return true;
		}
		else if(command.equalsIgnoreCase("setyAxisOffset")){
			double x0;
			try{
				x0 = Double.parseDouble(arguments[0]); 
			} catch(NumberFormatException e){
				System.err.println("Invalid y-tics.");
				return false;
			}
			pCtrl.setyAxisOffset(x0);
			return true;
		}
		else if(command.equalsIgnoreCase("setxAxisOffset")){
			double x0;
			try{
				x0 = Double.parseDouble(arguments[0]); 
			} catch(NumberFormatException e){
				System.err.println("Invalid x-offset.");
				return false;
			}
			pCtrl.setxAxisOffset(x0);
			return true;
		}
		else if(command.equalsIgnoreCase("unsetxAxisOffset")){
			pCtrl.unsetxAxisOffset();
			return true;
		}
		else if(command.equalsIgnoreCase("unsetyAxisOffset")){
			pCtrl.unsetyAxisOffset();
			return true;
		}
		else if(command.equalsIgnoreCase("linespoints")){
			for(String arg:arguments){
				if(arg.contains("\""))	pCtrl.linesPoints(arg.replace("\"", ""));
				else					return false;
			}
			pCtrl.stop();
			JFrame container = pCtrl.getJFrame();
			if(container != null)
				container.dispose();
			pCtrl.start();
			return true;
		}
		else if(command.equalsIgnoreCase("histogram")){
			for(String arg:arguments){
				if(arg.contains("\""))	pCtrl.histogram(arg.replace("\"", ""));
				else					return false;
			}
			pCtrl.stop();
			JFrame container = pCtrl.getJFrame();
			if(container != null)
				container.dispose();
			pCtrl.start();
			return true;
		}
		else if(command.equalsIgnoreCase("bargraph")){
			for(String arg:arguments){
				if(arg.contains("\""))	pCtrl.barGraph(arg.replace("\"", ""));
				else					return false;
			}
			pCtrl.stop();
			JFrame container = pCtrl.getJFrame();
			if(container != null)
				container.dispose();
			pCtrl.start();
			return true;
		}
		else if(command.equalsIgnoreCase("splot")){
			for(String arg:arguments){
				pCtrl.splot(arg,-10,10);
			}
			pCtrl.stop();
			JFrame container = pCtrl.getJFrame();
			if(container != null)
				container.dispose();
			pCtrl.start();
			return true;
		}
		else if(command.equalsIgnoreCase("integral")){
			if(arguments.length == 4){
				
				double y0, y1;
				try{
					y0 = Double.parseDouble(arguments[2]); 
					y1 = Double.parseDouble(arguments[3]);
				} catch(NumberFormatException e){
					System.err.println("Invalid limits.");
					return false;
				} 
				pCtrl.integral(arguments[0], arguments[1], y0, y1);
			} else if(arguments.length == 3){
				double y0, y1;
				try{
					y0 = Double.parseDouble(arguments[1]); 
					y1 = Double.parseDouble(arguments[2]);
				} catch(NumberFormatException e){
					System.err.println("Invalid limits.");
					return false;
				} 
				pCtrl.integral(arguments[0], null, y0, y1);
			} else {
				System.err.println("Invalid number of arguments");
				return false;
			}
			pCtrl.stop();
			JFrame container = pCtrl.getJFrame();
			if(container != null)
				container.dispose();
			pCtrl.start();
			return true;
		}
		else if(command.equalsIgnoreCase("setFrame")){
			if(!argument.equals("")){
				int x0;
				try{
					x0 = Integer.parseInt(argument); 
				} catch(NumberFormatException e){
					System.err.println("Invalid pixel size for frame border");
					return false;
				}
				pCtrl.setFrame(x0);
				return true;
			}else {
				pCtrl.setFrame();
				return true;
			}
		}
		else if(command.equalsIgnoreCase("unsetFrame")){
			pCtrl.unsetFrame();
			return true;
		}
		else if(command.equalsIgnoreCase("setFrameBorder")){
			if(!argument.equals("")){
				int x0;
				try{
					x0 = Integer.parseInt(argument); 
				} catch(NumberFormatException e){
					System.err.println("Invalid pixel size for frame border");
					return false;
				}
				pCtrl.setFrameBorder(x0);
				return true;
			}else {
				pCtrl.setFrameBorder();
				return true;
			}
		}
		else if(command.equalsIgnoreCase("unsetFrameBorder")){
			pCtrl.unsetFrameBorder();
			return true;
		}
		else if(command.equalsIgnoreCase("setxName")){

				if(argument.contains("\""))	pCtrl.setxName(argument.replace("\"", ""));
				else{
					System.err.println("No name for axis specified or invalid expression");
					return false;
				}

			return true;
		}
		else if(command.equalsIgnoreCase("setyName")){

			if(argument.contains("\""))	pCtrl.setyName(argument.replace("\"", ""));
			else{
				System.err.println("No name for axis specified or invalid expression");
				return false;
			}

		return true;
		}else if(command.equalsIgnoreCase("setPlotTitle")){

			if(argument.contains("\""))	pCtrl.setPlotTitle(argument.replace("\"", ""));
			else{
				System.err.println("No name for plot specified or invalid expression");
				return false;
			}

		return true;
		}else if(command.equalsIgnoreCase("unsetPlotTitle")){

			pCtrl.unsetPlotTitle();
			

		return true;
		}
		// end of commands requested by M. Goldbach
		else{
			if(argument.contains(")=")) return false; // function assignments won't be handled here
			else return fParse.parseGetFX(command, arguments);
		}
	}
}
