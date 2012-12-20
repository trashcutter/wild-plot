package wildPlot.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JFrame;

import wildPlot.parsing.FunctionParser;
import wildPlot.parsing.SplineInterpolation;
import wildPlot.rendering.BarGraph;
import wildPlot.rendering.FunctionDrawer;
import wildPlot.rendering.Integral;
import wildPlot.rendering.LinesPoints;
import wildPlot.rendering.PlotSheet;
import wildPlot.rendering.PointDrawer2D;
import wildPlot.rendering.ReliefDrawer;
import wildPlot.rendering.XAxis;
import wildPlot.rendering.XAxisHistoGram;
import wildPlot.rendering.XGrid;
import wildPlot.rendering.YAxis;
import wildPlot.rendering.YGrid;
import wildPlot.rendering.interfaces.Function2D;
import wildPlot.rendering.interfaces.Function3D;
import wildPlot.rendering.interfaces.FunctionParserWrapper;
import wildPlot.tools.TableReader;

/**
 * This class encapsulates most of the functionality of the rendering package and can be used together with the parsing 
 * package. It can also be used alone but may not be as versatile as when using the rendering package Classes manually.
 *
 */
public class PlotControl {
	private int funcNr = 0;
	
	private float lineThickness = 1F;
	
	private String plotTitle = "WildPlotTitle";
	private boolean hasTitle = false;
	
	private String xName = "x";
	private String yName = "y";
	
	private double[] xlimits = {-10,10};
	private double[] ylimits = {-10,10};
	
	private int yTicPixelDistance = 50;
	private int xTicPixelDistance = 120;
	
	private int yMinorTicPixelDistance = 15;
	private int xMinorTicPixelDistance = 45;
	
	private double xtics = 1;
	private double ytics = 1;
	
	private double xMinorTics = 1;
	private double yMinorTics = 1;
	
	private boolean xLimitOverride 		= false;
	private boolean yLimitOverride 		= false;
	
	private boolean xticsOverride 		= false;
	private boolean yticsOverride 		= false;
	
	private boolean xMinorTicsOverride 	= false;
	private boolean yMinorTicsOverride 	= false;
	
	private boolean hasGrid 			= false;
	
	private boolean axisOnFrameBorder 	= false;
	
	private boolean hasFrame 			= true;
	private boolean hasFrameBorder 		= true;
	private int frameThickness 			= 100;
	private int borderThickness 		= 1;
	
	// Preparation for legend drawable
	private HashMap<Object, String> NameList = new HashMap<Object, String>();
	
	private static final Color[] gradientColors = {
		Color.red, Color.green.darker(), Color.blue, Color.magenta, Color.cyan, Color.orange, Color.pink, Color.YELLOW
	};
	private int colorCnt = 0;
	
	private double yAxisOffset = 0;
	private double xAxisOffset = 0;
	
	private double func3DScaleOrder = 1;
	
	PlotSheet plotSheet = null;
	
	private FunctionParser funcParse;
	private Vector<Function2D> func2DVector 				= new Vector<Function2D>();
	private Vector<double[][]> pointVector 					= new Vector<double[][]>();
	private Vector<double[][]> histogramPointVector 		= new Vector<double[][]>();
	private Vector<double[][]> linesPointVector 			= new Vector<double[][]>();
	private Vector<double[][]> barGraphVector	 			= new Vector<double[][]>();
	private Vector<Function2D[]> integralVector 			= new Vector<Function2D[]>();
	private HashMap<Function2D[], double[]> integralLim 	= new HashMap<Function2D[], double[]>();
	private HashMap<double[][], Boolean> isSpline			= new HashMap<double[][], Boolean>();
	private HashMap<Object, Color> colorDef 				= new HashMap<Object, Color>();
	private Function3D func3D = null;
	private Thread plotThread = null;
	private PlotCanvas window = null;
	
	/**
	 * Constructor of PlotControl 
	 * @param funcParse FunctionParser object used to parse given functions
	 */
	public PlotControl(FunctionParser funcParse) {
		super();
		this.funcParse = funcParse;
	}
	
	/**
	 * Constructor of PlotControl, using this Constructor creates a new FunctionParser object which can 
	 * be used using the parse() method of this class' object
	 */
	public PlotControl() {
		super();
		this.funcParse = new FunctionParser();
	}
	
	/**
	 * add a function from the parser to the plot
	 * @param functionName name of the function needed for function parser
	 * @param leftBound left Bound that should be plotted for this function
	 * @param rightBound right Bound that should be plotted for this function
	 * @return true if everything went OK, else false
	 */
	public boolean plot(String functionName, double leftBound, double rightBound) {
		FunctionParserWrapper func = new FunctionParserWrapper(funcParse, functionName);
		return plot(func, leftBound, rightBound, functionName+"(x)");
	}
	
	/**
	 * add a direct Function2D object to the plot
	 * @param func the function that should be plotted
	 * @param leftBound left Bound that should be plotted for this function
	 * @param rightBound right Bound that should be plotted for this function
	 * @param name name of function for legend
	 * @return true if everything went OK, else false
	 */
	public boolean plot(Function2D func, double leftBound, double rightBound, String name) {
		
		//only set the limits, if they are not set by user directly because these 
		//bounds given here are(or will be) calculated by the program parser
		if(!this.xLimitOverride){
			if(leftBound<xlimits[0])
				xlimits[0] = leftBound;
			
			if(rightBound>xlimits[1])
				xlimits[1] = rightBound;
			xlimits[1] = rightBound;
		}

		//TODO: abrastern vorher (Dopplungen)
		func2DVector.add(func);
		NameList.put(func, name);
		this.colorDef.put(func, gradientColors[colorCnt++%(gradientColors.length)]);
		return true;
	}
	
	/**
	 * This is a test method for using 3D functions as relief plot
	 * this can be considered a hack as 3d functions are not yet fully supported and must be defined
	 * as normal 2d function with the additional variable 'y'
	 * @param functionName function name used in parser object for the 3d function that shall be plotted
	 * @param leftBound left bound of plot (x-axis)
	 * @param rightBound right bound of plot (x-axis)
	 * @return true and nothing else at the moment
	 */
	public boolean splot(String functionName, double leftBound, double rightBound) {
		FunctionParserWrapper func = new FunctionParserWrapper(funcParse, functionName, true);
		return splot(func, leftBound, rightBound, functionName+"(x,y)");
	}
	
	/**
	 * This is a test method for using 3D functions as relief plot
	 * this can be considered a hack as 3d functions are not yet fully supported and must be defined
	 * as normal 2d function with the additional variable 'y'
	 * @param func
	 * @param leftBound
	 * @param rightBound
	 * @param name
	 * @return
	 */
	public boolean splot(Function3D func, double leftBound, double rightBound, String name) {
		
		//only set the limits, if they are not set by user directly because these 
		//bounds given here are(or will be) calculated by the program parser
		if(!this.xLimitOverride){
			if(leftBound<xlimits[0])
				xlimits[0] = leftBound;
			
			if(rightBound>xlimits[1])
				xlimits[1] = rightBound;
			xlimits[1] = rightBound;
		}

		//TODO: abrastern vorher (Dopplungen)
		func3D = func;
		NameList.put(func, name);
		return true;
	}
	
	/**
	 * start plotting on plot window with all prior given elements
	 */
	public void start() {
		//TODO Bounds aus vectoren auslesen, range errechnen
		
		
		if(plotThread != null && plotThread.isAlive()) {
			window.setVisible(false);
			window.setPlotRunning(false);
			try {
				plotThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		plotSheet = new PlotSheet(xlimits[0], xlimits[1], ylimits[0], ylimits[1]);
		if(this.hasTitle){
			plotSheet.setTitle(this.plotTitle);
		}
		
		
		XAxis xAxis;
		
		if(this.xticsOverride) {
			xAxis = new XAxis(plotSheet, 0, xtics, xMinorTics);
		} else{
			xAxis = new XAxis(plotSheet, 0, xTicPixelDistance, xMinorTicPixelDistance);
		}
		
		YAxis yAxis;
		
		if(this.yticsOverride) {
			yAxis = new YAxis(plotSheet, 0, ytics, yMinorTics);
		} else {
			yAxis = new YAxis(plotSheet, 0, yTicPixelDistance, yMinorTicPixelDistance);
		}
		
		if(this.hasFrame){
			plotSheet.setFrameThickness(this.frameThickness);
			plotSheet.setBorderThickness(this.borderThickness);
			if(!this.hasFrameBorder) {
				plotSheet.unsetBorder();
			}
		}
		
		xAxis.setyOffset(xAxisOffset);
		
		
		yAxis.setxOffset(yAxisOffset);
		yAxis.setIntersectionOffset(xAxisOffset);
		yAxis.setName(yName);
		xAxis.setName(xName);
		if(this.axisOnFrameBorder){
			xAxis.setOnFrame();
			yAxis.setOnFrame();
		}
		
		
		if(this.hasGrid){
			YGrid yGrid;
			if(this.xticsOverride) {
				yGrid = new YGrid(plotSheet, 0,xtics);
			} else {
				yGrid = new YGrid(plotSheet, 0,xTicPixelDistance);
			}
			XGrid xGrid;
			if(this.yticsOverride) {
				xGrid = new XGrid(plotSheet, 0, ytics);
			} else {
				xGrid = new XGrid(plotSheet, 0, yTicPixelDistance);
			}
			plotSheet.addDrawable(yGrid);
			plotSheet.addDrawable(xGrid);
		}
		
		for(Function2D[] func:this.integralVector){
			Color thisColor = colorDef.get(func);
			Integral integral;
			double[] limits = this.integralLim.get(func);
			if(func.length == 1) {
				integral = new Integral(func[0], plotSheet, limits[0], limits[1]);
			} else {
				integral = new Integral(func[0], func[1], plotSheet,limits[0], limits[1]);
			}
			thisColor = thisColor.brighter();
			thisColor = new Color(thisColor.getRed(), thisColor.getGreen(),thisColor.getBlue(),127);
			integral.setColor(thisColor.brighter());
			plotSheet.addDrawable(integral);
		}
		for(double[][] points:barGraphVector){
			Color thisColor = colorDef.get(points);
			BarGraph barGraph = new BarGraph(plotSheet, 1, points, thisColor);
			barGraph.setFilling(true);
			thisColor = thisColor.brighter();
			thisColor = new Color(thisColor.getRed(), thisColor.getGreen(),thisColor.getBlue(),127);
			barGraph.setFillColor(thisColor);
			plotSheet.addDrawable(barGraph);
		}
		for(double[][] points:histogramPointVector){
			Color thisColor = colorDef.get(points);
			XAxisHistoGram histogram = new XAxisHistoGram(plotSheet, points, 0, 1, thisColor);
			histogram.setFilling(true);
			thisColor = thisColor.brighter();
			thisColor = new Color(thisColor.getRed(), thisColor.getGreen(),thisColor.getBlue(),127);
			histogram.setFillColor(thisColor);
			plotSheet.addDrawable(histogram);
		}
		for(double[][] points:this.linesPointVector){
			Color thisColor = colorDef.get(points);
			LinesPoints linesPoints = new LinesPoints(plotSheet, points, thisColor);
			plotSheet.addDrawable(linesPoints);
		}
		for(Function2D func:func2DVector){
			FunctionDrawer functionDrawer = new FunctionDrawer(func, plotSheet, colorDef.get(func));
			functionDrawer.setSize(lineThickness);
			plotSheet.addDrawable(functionDrawer);
		}
		
		for(double[][] points:pointVector){
			if(this.isSpline.get(points)) {
				SplineInterpolation interpol = new SplineInterpolation(points[0], points[1]);
				double leftLimit = points[0][0];
				double rightLimit = points[0][0];
				for(int i = 0; i< points[0].length;i++){
					if(leftLimit > points[0][i]){
						leftLimit = points[0][i];
					}
					if(rightLimit < points[0][i]){
						rightLimit = points[0][i];
					}
					
				}
				
				FunctionDrawer functionDrawer = new FunctionDrawer(interpol, plotSheet, colorDef.get(points), leftLimit, rightLimit);
				functionDrawer.setSize(lineThickness);
				plotSheet.addDrawable(functionDrawer);
				
			}else{
				PointDrawer2D pointDrawer = new PointDrawer2D(plotSheet, points, colorDef.get(points));
				plotSheet.addDrawable(pointDrawer);
				}
		}
		//relief plot
		if(this.func3D != null) {
			//set good parameters to use relief better
			xAxis.setOnFrame();
			yAxis.setOnFrame();
			plotSheet.setFrameThickness(100); //to be able to show legend
			
			ReliefDrawer reliefDrawer = new ReliefDrawer(func3DScaleOrder, 200, func3D, plotSheet, true);
			plotSheet.addDrawable(reliefDrawer);
			plotSheet.addDrawable(reliefDrawer.getLegend());
		}
		
		plotSheet.addDrawable(yAxis);
		plotSheet.addDrawable(xAxis);
		
		window = new PlotCanvas(plotSheet);
		plotThread = new Thread(window);
		plotThread.setPriority(Thread.MIN_PRIORITY);
		plotThread.start();
		//EventQueue.invokeLater(window);
	}
	
	/**
	 * manually set x-limits for the plot, overrides all automatic calculations
	 * @param x0 left bound 
	 * @param x1 right bound
	 */
	public void setXlim(double x0, double x1){
		this.xLimitOverride = true;
		this.xlimits[0] = (x0<=x1)?  x0 :x1;
		this.xlimits[1] = (x0<=x1)?  x1 :x0;
		
	}
	
	/**
	 * manually set y-limits for the plot, overrides all automatic calculations 
	 * @param y0 lower bound
	 * @param y1 upper bound
	 */
	public void setYlim(double y0, double y1){
		this.yLimitOverride = true;
		this.ylimits[0] = (y0<=y1)?  y0 :y1;
		this.ylimits[1] = (y0<=y1)?  y1 :y0;
		
	}
	
	/**
	 * disable offset of the x-axis (set it to 0)
	 */
	public void unsetyAxisOffset() {
		this.yAxisOffset = 0;
	}
	
	/**
	 * disable offset of the y-axis (set it to 0)
	 */
	public void unsetxAxisOffset() {
		this.xAxisOffset = 0;
	}
	
	/**
	 * offset to move y-axis along the x-axis
	 * for border axis behavior use setAxisOnFrameBorder()
	 * @param yAxisOffset offset on x-axis
	 */
	public void setyAxisOffset(double yAxisOffset) {
		this.yAxisOffset = yAxisOffset;
	}
	
	/**
	 * offset to move x-axis along the y-axis
	 * for border axis behavior use setAxisOnFrameBorder()
	 * @param xAxisOffset offset on y-axis
	 */
	public void setxAxisOffset(double xAxisOffset) {
		this.xAxisOffset = xAxisOffset;
	}
	
	/**
	 * add a table (stored in a file) of points as simple points on plot
	 * @param file file that stores the table with points
	 */
	public void tablePlot(String file) {
		tablePlot(file, file);
	}
	
	/**
	 * add a table (stored in a file) of points as simple points on plot
	 * @param file file that stores the table with points
	 */
	public void tablePlot(String file, boolean isSpline) {
		tablePlot(file, file, isSpline);
	}
	
	/**
	 * add a table (stored in a file) of points as simple points on plot with a given name for the legend
	 * @param file file that stores the table with points
	 * @param name name on the legend
	 */
	public void tablePlot(String file, String name, boolean isSpline) {
		TableReader tableReader = new TableReader(file);
		try {
			tablePlot(tableReader.getPointArray(), name, isSpline);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * add a table (stored in a file) of points as simple points on plot with a given name for the legend
	 * @param file file that stores the table with points
	 * @param name name on the legend
	 */
	public void tablePlot(String file, String name) {
		TableReader tableReader = new TableReader(file);
		try {
			tablePlot(tableReader.getPointArray(), name, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * add a table of points as simple points on plot with a given name for the legend
	 * @param points table with points in array format
	 * @param name name for legend
	 */
	public void tablePlot(double[][] points, String name, boolean isSpline) {
		this.pointVector.add(points);
		NameList.put(points, name);
		this.isSpline.put(points, isSpline);
		this.colorDef.put(points, gradientColors[colorCnt++%(gradientColors.length)]);
		
	}
	/**
	 * add a table of points as simple points on plot
	 * @param points table with points in array format
	 */
	public void tablePlot(double[][] points) {
		tablePlot(points, points.toString(), false);
	}
	
	/**
	 * add a histogram with given data points from a file
	 * @param file file with data points
	 */
	public void histogram(String file) {
		histogram(file, file);
	}
	/**
	 * add a histogram with given data points from a file and a name for the legend
	 * @param file
	 * @param name
	 */
	public void histogram(String file, String name) {
		TableReader tableReader = new TableReader(file);
		double[][] histoData = null;
		try {
			histoData = tableReader.getPointArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.histogramPointVector.add(histoData);
		NameList.put(histoData, name);
		this.colorDef.put(histoData, gradientColors[colorCnt++%(gradientColors.length)]);
	}
	/**
	 * add a histogram with the given point array and a name for the legend
	 * @param points
	 * @param name
	 */
	public void histogram(double[][] points, String name) {
		this.histogramPointVector.add(points);
		NameList.put(points, name);
	}
	/**
	 * add a histogram with the given point array
	 * @param points
	 */
	public void histogram(double[][] points) {
		histogram(points, points.toString());
	}
	
	/**
	 * draw points and connect them with lines using data from file
	 * @param file file with data points
	 */
	public void linesPoints(String file) {
		linesPoints(file, file);
	}
	
	/**
	 * draw points and connect them with lines using data from file
	 * @param file file with data points
	 * @param name for legend (not yet implemented)
	 */
	public void linesPoints(String file, String name) {
		TableReader tableReader = new TableReader(file);
		try {
			linesPoints(tableReader.getPointArray(), name);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * draw points and connect them with lines using data from array
	 * @param points array with data points
	 * @param name for legend (not yet implemented)
	 */
	public void linesPoints(double[][] points, String name) {
		this.linesPointVector.add(points);
		NameList.put(points, name);
		this.colorDef.put(points, gradientColors[colorCnt++%(gradientColors.length)]);
	}
	
	/**
	 * draw points and connect them with lines using data from array
	 * @param points array with data points
	 */
	public void linesPoints(double[][] points) {
		linesPoints(points, points.toString());
	}
	
	public void barGraph(String file) {
		barGraph(file, file);
	}
	public void barGraph(String file, String name) {
		TableReader tableReader = new TableReader(file);
		try {
			barGraph(tableReader.getPointArray(), name);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void barGraph(double[][] points, String name) {
		this.barGraphVector.add(points);
		NameList.put(points, name);
		this.colorDef.put(points, gradientColors[colorCnt++%(gradientColors.length)]);
	}
	public void barGraph(double[][] points) {
		barGraph(points, "Bar graph: "+points.toString());
	}
	
	public void integral(Function2D func1, double leftLimit, double rightLimit) {
		Function2D[] funcArray = new Function2D[1];
		funcArray[0] = func1;
		this.integralVector.add(funcArray);
		double[] limits = {leftLimit, rightLimit};
		this.integralLim.put(funcArray, limits);
		this.colorDef.put(funcArray, gradientColors[colorCnt++%(gradientColors.length)]);
	}
	
	public void integral(Function2D func1, Function2D func2, double leftLimit, double rightLimit) {
		if(func2 == null)
			integral(func1, leftLimit, rightLimit);
		else {
			Function2D[] funcArray = new Function2D[2];
			funcArray[0] = func1;
			funcArray[1] = func2;
			this.integralVector.add(funcArray);
			double[] limits = {leftLimit, rightLimit};
			this.integralLim.put(funcArray, limits);
			this.colorDef.put(funcArray, gradientColors[colorCnt++%(gradientColors.length)]);
		}	
	}
	
	/**
	 * 
	 * @param functionName1
	 * @param functionName2
	 * @param leftLimit
	 * @param rightLimit
	 */
	public void integral(String functionName1, String functionName2, double leftLimit, double rightLimit) {
		FunctionParserWrapper func1 = new FunctionParserWrapper(funcParse, functionName1);
		FunctionParserWrapper func2 = null;
		if(functionName2 != null)
			func2 = new FunctionParserWrapper(funcParse, functionName2);
		integral(func1, func2, leftLimit, rightLimit);
	}
	
	/**
	 * specify the x-tics manually and turn off automatic calculation for them
	 * @param xtics the choosen x-tics
	 */
	public void setXtics(double xtics) {
		this.xtics = xtics;
		xticsOverride = true;
		if(!this.xMinorTicsOverride)
			this.xMinorTics = xtics;
	}
	
	public void setMinorXtics(double minorXtics) {
		this.xMinorTics = minorXtics;
		this.xMinorTicsOverride = true;
	}
	
	public void unsetMinorXtics() {
		this.xMinorTics = this.xtics;
		this.xMinorTicsOverride = false;
	}
	
	/**
	 * brings back automatic calculation of x-tics
	 */
	public void unsetXtics() {
		xticsOverride = false;
	}

	/**
	 * specify the y-tics manually and turn off automatic calculation for them
	 * @param ytics the chosen y-tixs
	 */
	public void setYtics(double ytics) {
		this.ytics = ytics;
		yticsOverride = true;
	}
	
	public void setMinorYtics(double minorYtics) {
		this.yMinorTics = minorYtics;
		this.yMinorTicsOverride = true;
	}
	
	public void unsetMinorYtics() {
		this.yMinorTics = this.ytics;
		this.yMinorTicsOverride = false;
	}
	
	/**
	 * brings back automatic calculation of y-tics
	 */
	public void unsetYtics() {
		yticsOverride = false;
	}
	
	/**
	 * Helping function to calculate tics for markers on axis
	 * @param deltaRange range used for tics
	 * @param ticlimit number of tics wished
	 * @return ticspan, this may be more ore less as ticlimit but in same region
	 */
	private double ticsCalc(double deltaRange, int ticlimit){
		double tics = Math.pow(10, (int)Math.log10(deltaRange/ticlimit));
		while(2.0*(deltaRange/(tics)) <= ticlimit) {
			tics /= 2.0;
		}
		while((deltaRange/(tics))/2 >= ticlimit) {
			tics *= 2.0;
		}
		return tics;
	}
	
	/**
	 * this sets x and y axis on the frame borders,
	 * it deactivates the lower markers of x axis and left markers of y axis and
	 * aligns the x axis numbering little bit different.
	 * If this is set, than no offset for any axis is regarded for this plot
	 */
	public void setAxisOnFrameBorder() {
		this.axisOnFrameBorder = true;
	}
	
	/**
	 * brings the x-axis back in the plotting field, and brings the possibility back to 
	 * turn on offset on any axis
	 */
	public void unsetAxisOnFrameBorder() {
		this.axisOnFrameBorder = false;
	}
	
	/**
	 * stop window thread and close plot window
	 */
	public void stop() {
		if(plotThread != null && plotThread.isAlive()) {
			window.setVisible(false);
			window.setPlotRunning(false);
			try {
				plotThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * activate grid lines on plot
	 */
	public void setGrid() {
		this.hasGrid = true;
	}
	
	/**
	 * deactivate grid lines on plot (standard behavior)
	 */
	public void unsetGrid() {
		this.hasGrid = false;
	}

	/**
	 * This is a factor for the distribution of colors for function values of 3d functions.
	 * Values below 1 are used for more colors in higher regions and above 1 are more colors
	 * in lower regions and 1 is equally distributed.
	 * @param func3dScaleOrder
	 * @see wildPlot.rendering.ReliefDrawer#gradientCurveFactor
	 */
	public void setFunc3DScaleOrder(double func3dScaleOrder) {
		func3DScaleOrder = func3dScaleOrder;
	}
	
	/**
	 * activate a frame around the plot (standard behavior)
	 * the size of the frame will be 100 pixel
	 */
	public void setFrame() {
		this.hasFrame = true;
		frameThickness = 100;
	}

	/**
	 * activate a frame around the plot with a given size in pixel
	 * @param frameThickness size of frame
	 */
	public void setFrame(int frameThickness) {
		this.hasFrame = true;
		this.frameThickness = frameThickness;
	}
	
	/**
	 * activate the border (size=1px) between frame and plot (standard behavior)
	 */
	public void setFrameBorder() {
		this.hasFrameBorder = true;
		this.borderThickness = 1;
	}
	
	/**
	 * activate the border between plot and frame with the given border size in pixel
	 * @param borderSize border size in pixel
	 */
	public void setFrameBorder(int borderSize) {
		this.hasFrameBorder = true;
		this.borderThickness = borderSize;
	}
	
	/**
	 * deactivate frame around plot
	 */
	public void unsetFrame() {
		this.hasFrame = false;
	}

	/**
	 * deactivate border between frame and plot
	 * Warning!: when the an axis is aligned on border, the axis line will disappear too!
	 */
	public void unsetFrameBorder() {
		this.hasFrameBorder = false;
	}
	
	/**
	 * Parse expression with the internal function parser, when defining mathematical functions with this parse
	 * method, these functions may be used in plot and splot methods.
	 * @param input expression that should be parsed
	 * @return true if parsing was successful otherwise false
	 */
	public boolean parse(String input) {
		return this.funcParse.parse(input);
	}

	public void setxName(String xName) {
		this.xName = xName;
	}

	public void setyName(String yName) {
		this.yName = yName;
	}
	
	public JFrame getJFrame() {
		return (window!=null)? this.window.getContainer():null;
	}

	/**
	 * @param plotTitle the plotTitle to set
	 */
	public void setPlotTitle(String plotTitle) {
		this.plotTitle = plotTitle;
		this.hasTitle = true;
	}
	
	/**
	 * deactivate plotTitle
	 */
	public void unsetPlotTitle() {
		this.hasTitle = false;
	}
	
}
