/**
 * 
 */
package wildPlot.rendering;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import wildPlot.rendering.interfaces.Function3D;

/**
 * Draws a relief of a three dimensional function on a two dimensional plot sheet. The relief is drawn either with borders
 * or with a color gradient.
 * 
 * @author Michael Goldbach
 * @see wildPlot.rendering.Drawable
 * @see wildPlot.rendering.PlotSheet
 * @see wildPlot.rendering.interfaces.Function3D 
 */
public class ReliefDrawer implements Drawable {
	
	
	private int pixelSkip = 6;
	private boolean abortPaint = false;
	private boolean depthSearchAborted = false;
	
	private int threadCnt = 1;
	
	/**
	 * x-bounds of relief
	 */
	private double[] xrange = {0,0};
	
	/**
	 * y-bounds of relief
	 */
	private double[] yrange = {0,0};
	
	/**
	 * this variable will be used to store the lowest function value in the ploting range in the starting resolution
	 */
	private double f_xLowest 	= 0;
	
	/**
	 * this variable will be used to store the highest function value in the ploting range in the starting resolution
	 */
	private double f_xHighest 	= 0;
	
	/**
	 * to make the gradient colors non linear to the function value, this exponential factor can be used
	 */
	private double gradientCurveFactor = 1;
	
	/**
	 * the count of different height regions, can be dependent on the count of gradient colors
	 */
	private int heightRegionCount = 10;
	
	/**
	 * The gradient colors used for colored relief, can be dynamically expanded
	 */
//	private Color[] gradientColors = {new Color(0, 0, 143), new Color(0, 15, 200), new Color(0, 32, 255), new Color(0, 115, 255),
//			new Color(0, 170, 255), new Color(0, 231, 255), new Color(73, 242, 190),new Color(147, 255, 108), new Color(100, 230, 87),new Color(48, 213, 75),
//			new Color(98, 217, 62), new Color(148, 223, 50),new Color(188, 233, 35), new Color(235, 246, 20),new Color(245, 180, 10),new Color(255, 127, 0),
//			new Color(255, 88, 0),new Color(255, 39, 0), new Color(192, 19, 0), new Color(128, 0, 0) };
	
	private Color[] gradientColors = {Color.white, Color.GREEN.darker(), Color.GREEN.darker().darker(), Color.BLACK};
	
	/**
	 * The border function value between where borders are drawn. On colored plot each region between two borders gets
	 * a unique color.
	 */
	private double[] borders = null;
	private boolean depthScanningIsFinished = false;
	
	/**
	 * determines if this ReliefDrawer draws only one colored borders ore uses color gradient for the relief
	 */
	private boolean colored = true;
	
	/**
	 * the function for which the relief plot is drawn
	 */
	private Function3D function;
	
	/**
	 * the PlotSheet object on which the relief is drawn onto
	 */
	private PlotSheet plotSheet;
	
	/**
	 * border color for non-colored plots
	 */
	private Color color = new Color(255,0,0);
	
	/**
	 * Creates a new ReliefDrawer object
	 * @param gradientCurveFactor factor for non linear gradients (1=linear, <1 finer resolution for higher values, >1 finer resolution for lower values)
	 * @param heightRegionCount number of gradient regions, if colored this is set to the number of gradient colors, if the count is less than the colors, 
	 * if it is higher than the number of colors the color array will be expanded
	 * @param function the three dimensional function plotted with this relief drawer
	 * @param plotSheet this is where the relief is drawn upon
	 * @param colored true if a color gradient should be used, false if borders shall be used
	 */
	public ReliefDrawer(double gradientCurveFactor, int heightRegionCount, Function3D function, PlotSheet plotSheet, boolean colored) {
		super();
		this.gradientCurveFactor = gradientCurveFactor;
		this.heightRegionCount = heightRegionCount;
		this.function = function;
		this.plotSheet = plotSheet;
		this.colored = colored;
		if(colored){
			if(this.gradientColors.length >= heightRegionCount){
				this.heightRegionCount = this.gradientColors.length;
			} else {
				Vector<Color> colorVector = new Vector<Color>(Arrays.asList(this.gradientColors));
				this.gradientColors = wildPlot.tools.RelativeColorGradient.makeGradient(colorVector, this.heightRegionCount);
				this.heightRegionCount = this.gradientColors.length;
			}
		}
	}
	
	/**
	 * Creates a new ReliefDrawer object for colored gradients
	 * @param gradientCurveFactor factor for non linear gradients 
	 * (1=linear, <1 finer resolution for higher values, >1 finer resolution for lower values)
	 * @param function the three dimensional function plotted with this relief drawer
	 * @param plotSheet this is where the relief is drawn upon
	 */
	public ReliefDrawer(double gradientCurveFactor, Function3D function, PlotSheet plotSheet) {
		super();
		this.gradientCurveFactor = gradientCurveFactor;
		this.function = function;
		this.plotSheet = plotSheet;		
		this.heightRegionCount = this.gradientColors.length;

	}
	
	/**
	 * 
	 * @param gradientCurveFactor factor for non linear gradients 
	 * (1=linear, <1 finer resolution for higher values, >1 finer resolution for lower values)
	 * @param heightRegionCount number of gradient regions, if colored this is set to the number of gradient colors, if the count is less than the colors, 
	 * if it is higher than the number of colors the color array will be expanded
	 * @param function the three dimensional function plotted with this relief drawer
	 * @param plotSheet this is where the relief is drawn upon
	 * @param colored true if a color gradient should be used, false if borders shall be used
	 * @param color color of borders if non colored plot is used
	 */
	public ReliefDrawer(double gradientCurveFactor, int heightRegionCount, Function3D function, PlotSheet plotSheet,boolean colored, Color color) {
		super();
		this.gradientCurveFactor = gradientCurveFactor;
		this.heightRegionCount = heightRegionCount;
		this.function = function;
		this.plotSheet = plotSheet;
		this.color = color;
		this.colored = colored;
		
		if(colored){
			if(this.gradientColors.length >= heightRegionCount){
				this.heightRegionCount = this.gradientColors.length;
			} else {
				Vector<Color> colorVector = new Vector<Color>(Arrays.asList(this.gradientColors));
				this.gradientColors = wildPlot.tools.RelativeColorGradient.makeGradient(colorVector, this.heightRegionCount);
				this.heightRegionCount = this.gradientColors.length;
			}
		}
	}

	/* (non-Javadoc)
	 * @see rendering.Drawable#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(Graphics g) {
		abortPaint = false;
		Color oldColor = g.getColor();
		Rectangle field = g.getClipBounds();
		
		
		if(rangeHasChanged()){
			try {
				scanDepth(field);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(abortPaint)
			return;
		
		this.depthScanningIsFinished = true;
		if(this.colored){
			try {
				drawColoredRelief(g);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else{
			g.setColor(color);
			drawBorders(g);
		}
		
		
		
		g.setColor(oldColor);
	}
	
	/**
	 * draws relief with color gradient
	 * @param g graphic object used to draw relief
	 */
	private void drawColoredRelief(Graphics g) throws InterruptedException {
		Rectangle field = g.getClipBounds();
		
		BufferedImage[] bimages = new BufferedImage[threadCnt];
		for(int i = 0; i< bimages.length; i++){
			bimages[i] = new BufferedImage(field.width, field.height, BufferedImage.TYPE_INT_ARGB);
		}
		
//		double[] thisCoordinate = plotSheet.toCoordinatePoint(0, 0, field);
//		
//		double thisF_xy;
//		for(int i = field.x+plotSheet.getFrameThickness() ; i < field.x + field.width -plotSheet.getFrameThickness(); i++) {
//			for(int j = field.y + +plotSheet.getFrameThickness() ; j < field.y +field.height -plotSheet.getFrameThickness(); j++) {
//				thisCoordinate = plotSheet.toCoordinatePoint(i, j, field);
//				thisF_xy = function.f(thisCoordinate[0], thisCoordinate[1]);
//				g.setColor(getColor(thisF_xy));
//				g.drawLine(i, j, i, j);
//				
//			}
//		}
		
		int length = (field.x + field.width-plotSheet.getFrameThickness()) - (field.x+plotSheet.getFrameThickness());
		Thread[] threads = new Thread[threadCnt];
		
		PartRenderer[] partRenderer = new PartRenderer[threadCnt];
		
		Graphics gnew = bimages[0].getGraphics();
		gnew.setClip(field);
		partRenderer[0] = new PartRenderer(gnew, field.x+plotSheet.getFrameThickness(), field.x + plotSheet.getFrameThickness()+ length/threadCnt);
		threads[0] = new Thread(partRenderer[0]);
		for(int i = 1; i< threads.length-1; i++){
			gnew = bimages[i].getGraphics();
			gnew.setClip(field);
			partRenderer[i] = new PartRenderer(gnew, field.x + plotSheet.getFrameThickness() + length*i/threadCnt +1, field.x+ plotSheet.getFrameThickness() + length*(i+1)/threadCnt);
			threads[i] = new Thread(partRenderer[i]);
		}
		if(threadCnt > 1){
		gnew = bimages[threadCnt-1].getGraphics();
		gnew.setClip(field);
		partRenderer[threadCnt-1] = new PartRenderer(gnew, field.x + plotSheet.getFrameThickness() + length*(threadCnt-1)/threadCnt +1, field.x+ plotSheet.getFrameThickness() + length);
		threads[threadCnt-1] = new Thread(partRenderer[threadCnt-1]);
		}
		for(Thread thread : threads) {
			thread.start();
		}
		
		for(Thread thread : threads) {
			thread.join();
		}
		
		for(BufferedImage bimage: bimages){
			((Graphics2D)g).drawImage(bimage, null, 0, 0);
		}
		
		//
	}
	
	/**
	 * draws bordered relief plot
	 * @param g graphic object used to draw relief
	 */
	private void drawBorders(Graphics g) {
		Rectangle field = g.getClipBounds();
		double[] thisCoordinate = plotSheet.toCoordinatePoint(0, 0, field);
		double[] upToThisCoordinate = plotSheet.toCoordinatePoint(0, 0, field);
		double[] leftToThisCoordinate = plotSheet.toCoordinatePoint(0, 0, field);
		
		double thisF_xy;
		double upToThisF_xy;
		double leftToThisF_xy;
		
		for(int i = field.x+plotSheet.getFrameThickness() + 1; i < field.x + field.width-plotSheet.getFrameThickness(); i++) {
			for(int j = field.y+plotSheet.getFrameThickness() + 1; j < field.y +field.height-plotSheet.getFrameThickness(); j++) {
				thisCoordinate = plotSheet.toCoordinatePoint(i, j, field);
				upToThisCoordinate = plotSheet.toCoordinatePoint(i, j-1, field);
				leftToThisCoordinate = plotSheet.toCoordinatePoint(i-1, j, field);
				thisF_xy = function.f(thisCoordinate[0], thisCoordinate[1]);
				upToThisF_xy = function.f(upToThisCoordinate[0], upToThisCoordinate[1]);
				leftToThisF_xy = function.f(leftToThisCoordinate[0], leftToThisCoordinate[1]);
				
				if(onBorder(thisF_xy, upToThisF_xy) || onBorder(thisF_xy, leftToThisF_xy)) {
					g.drawLine(i, j, i, j);
				}
				
			}
		}
	}
	
	/**
	 * if the bounds have changed the min and max height of relief has to be determined anew
	 * @return
	 */
	private boolean rangeHasChanged() {
		boolean tester = true;
		
		tester &= plotSheet.getxRange()[0] == this.xrange[0];
		tester &= plotSheet.getxRange()[1] == this.xrange[1];
		tester &= plotSheet.getyRange()[0] == this.yrange[0];
		tester &= plotSheet.getyRange()[1] == this.yrange[1];
		
		if(!tester) {
			this.xrange = plotSheet.getxRange().clone();
			this.yrange = plotSheet.getyRange().clone();
		}
		
		return !tester || this.depthSearchAborted;
	}
	
	/**
	 * scan depth of relief to determine distance between borders
	 * @param field bounds of plot
	 */
	private void scanDepth(Rectangle field) throws InterruptedException {
		depthSearchAborted = true;
		double[] coordinate = plotSheet.toCoordinatePoint(0, 0, field);
		double f_xy = function.f(coordinate[0], coordinate[1]);
		this.f_xHighest = f_xy;
		this.f_xLowest 	= f_xy;
		
		int length = (field.x + field.width-plotSheet.getFrameThickness()) - (field.x+plotSheet.getFrameThickness());
		Thread[] threads = new Thread[threadCnt];
		
		int stepSize = length/threadCnt;
		
		DepthSearcher[] dSearcher = new DepthSearcher[threadCnt];
		
		int leftLim = field.x+plotSheet.getFrameThickness();
		int rightLim = (field.x + plotSheet.getFrameThickness()+ (stepSize));
		dSearcher[0] = new DepthSearcher(field,leftLim ,rightLim );
		threads[0] = new Thread(dSearcher[0]);
		for(int i = 1; i< threads.length-1; i++){
			dSearcher[i] = new DepthSearcher(field, field.x + plotSheet.getFrameThickness() + stepSize*i +1, field.x+ plotSheet.getFrameThickness() + stepSize*(i+1));
			threads[i] = new Thread(dSearcher[i]);
		}
		if(threadCnt>1){
			dSearcher[threadCnt-1] = new DepthSearcher(field, field.x + plotSheet.getFrameThickness() + stepSize*(threadCnt-1) +1, field.x+ plotSheet.getFrameThickness() + length);
			threads[threadCnt-1] = new Thread(dSearcher[threadCnt-1]);
		}
		for(Thread thread : threads) {
			thread.start();
		}
		
		for(Thread thread : threads) {
			thread.join();
		}
		
		for(DepthSearcher searcher : dSearcher ){
			if(searcher.getF_xHighest() > this.f_xHighest)
				this.f_xHighest = searcher.getF_xHighest();
			if(searcher.getF_xLowest() < this.f_xLowest)
				this.f_xLowest = searcher.getF_xLowest();
		}
		
		//System.err.println(this.f_xHighest + " : " + this.f_xLowest);
		//create borders based on heigth gradient
		borders = new double[this.heightRegionCount];
		double steps = (this.f_xHighest - this.f_xLowest)/this.heightRegionCount;
		
		for(int i = 0; i < borders.length ; i++) {
			borders[i] =  this.f_xLowest +  (this.f_xHighest - this.f_xLowest)*Math.pow((1.0/this.heightRegionCount)*(i+1.0), gradientCurveFactor);
			//System.err.println(borders[i]+" " + (this.f_xHighest - this.f_xLowest)*Math.pow((1.0/this.heightRegionCount)*(i+1.0), gradientCurveFactor));
		}
		if(!this.abortPaint){
			depthSearchAborted = false;
			depthScanningIsFinished = true;
		}
	}
	
	/**
	 * returns true if a pixel on plot is directly on a border which has to be drawn
	 * @param f_xy the current function value
	 * @param f_xyNext the function value of a neighbor
	 * @return true if between those two function values a border has to be drawn
	 */
	private boolean onBorder(double f_xy, double f_xyNext) {
		double lowerBorder = this.f_xLowest;
		double higherBorder = this.f_xHighest;
		
		for(int i = 0 ; i< borders.length; i++) {
			higherBorder = borders[i];
			if((f_xy >= lowerBorder && f_xy < higherBorder) || (f_xyNext >= lowerBorder && f_xyNext < higherBorder)) {
				
				if((f_xy >= lowerBorder && f_xy < higherBorder) && (f_xyNext >= lowerBorder && f_xyNext < higherBorder)) {
					
					return false;
				}
				return true;
			}
			lowerBorder = higherBorder;
		}
		
		return true;
		
	}
	
	/**
	 * get the gradient color for the corresponding function value
	 * @param f_xy function value
	 * @return color that corresponds to the function value
	 */
	private Color getColor(double f_xy) {
		double lowerBorder = this.f_xLowest;
		double higherBorder = this.f_xHighest;
		try{
			if(borders == null){
				System.err.println("!!!!!!!!!!!!!!!!!!! Borders null");
			}
			for(int i = 0 ; i< borders.length; i++) {
				higherBorder = borders[i];
				if((f_xy >= lowerBorder && f_xy < higherBorder)) {
					return this.gradientColors[i];
					
				}
				lowerBorder = higherBorder;
			}
		} catch(NullPointerException e){
			e.printStackTrace();
			System.err.println("!!!!!!!!!!!!: " + borders.length);
			System.exit(-1);
		}
		
		
		return (f_xy < borders[0])? this.gradientColors[0] : this.gradientColors[this.gradientColors.length-1];
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see rendering.Drawable#isOnFrame()
	 */
	public boolean isOnFrame() {
		return false;
	}
	
	public Drawable getLegend() {
		return new ReliefLegend();
	}
	
	
	
	
	public void setThreadCnt(int threadCnt) {
		this.threadCnt = threadCnt;
	}




	/**
	 * Legend for ReliefDrawer as Drawable implementing inner class
	 * @author Michael Goldbach
	 *
	 */
	private class ReliefLegend implements Drawable{
		
		/**
		 * Format that is used to print numbers under markers
		 */
		private DecimalFormat df =   new DecimalFormat( "##0.00#" );	
		private DecimalFormat dfScience =   new DecimalFormat( "0.0###E0" );
		private boolean isAborted = false;
		private boolean isScientific = false;
		
		/*
		 * (non-Javadoc)
		 * @see rendering.Drawable#paint(java.awt.Graphics)
		 */
		public void paint(Graphics g) {
			isAborted = false;
			while(!ReliefDrawer.this.depthScanningIsFinished || rangeHasChanged()){
				if(this.isAborted){
					System.err.println("no relief legend will be drawn!");
					return;
				}
				try {
					
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			ReliefDrawer.this.depthScanningIsFinished = false;
			Color oldColor = g.getColor();
			Rectangle field = g.getClipBounds();
			double deltaZ = (ReliefDrawer.this.yrange[1] - ReliefDrawer.this.yrange[0])/ReliefDrawer.this.borders.length;
			
			
			@SuppressWarnings("deprecation")
			int leftStart = plotSheet.xToGraphic(ReliefDrawer.this.xrange[1], field) + 10;
			
			double lowerStart = ReliefDrawer.this.f_xLowest;
			double upperEnd = 0;
			double currentHeight = ReliefDrawer.this.yrange[0];
			double yToZQuotient = Math.abs(ReliefDrawer.this.yrange[1] - ReliefDrawer.this.yrange[0])/(ReliefDrawer.this.f_xHighest - ReliefDrawer.this.f_xLowest);
			
			//draw colors for legend
			for(int i = 0; i < ReliefDrawer.this.borders.length; i++) {
				
				upperEnd = ReliefDrawer.this.borders[i];
				deltaZ = yToZQuotient*(upperEnd-lowerStart);
				Color regionColor = ReliefDrawer.this.getColor((lowerStart + upperEnd)/2);
				
				g.setColor(regionColor);
				g.fillRect(leftStart, plotSheet.yToGraphic(currentHeight+deltaZ, field), 10, plotSheet.yToGraphic(currentHeight, field) - plotSheet.yToGraphic(currentHeight+deltaZ, field));
				
				currentHeight += deltaZ;
				lowerStart = upperEnd;
			}
			
			g.setColor(Color.black);
			double ztics = ticsCalc(ReliefDrawer.this.f_xHighest - ReliefDrawer.this.f_xLowest, 12);
			
			
			double startY = ReliefDrawer.this.yrange[0];
			double startZ = ReliefDrawer.this.f_xLowest;
			
			double currentY = startY;
			double currentZ = startZ;
			
			if(ztics < 1e-2 || ztics > 1e3)
				this.isScientific = true;
			
			//draw numbering left to the color bar
			while(currentY <= ReliefDrawer.this.yrange[1]) {
				if(this.isScientific)
					g.drawString(df.format(currentZ), leftStart + 22, plotSheet.yToGraphic(currentY, field));
				else
					g.drawString(dfScience.format(currentZ), leftStart + 22, plotSheet.yToGraphic(currentY, field));
				currentZ+=ztics;
				currentY += yToZQuotient*ztics;
			}

			g.setColor(oldColor);
		}
		
		/**
		 * calculate nice logical tics
		 * @param deltaRange range
		 * @param ticlimit number of maximal tics in given range
		 * @return tics for the specified parameters
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

		/*
		 * (non-Javadoc)
		 * @see rendering.Drawable#isOnFrame()
		 */
		public boolean isOnFrame() {
			return true;
		}

		@Override
		public void abortAndReset() {
			isAborted = true;
			
		}
		
	}
	private class DepthSearcher implements Runnable{

		double f_xHighest = 0;
		double f_xLowest = 0;
		
		Rectangle field = null;
		int leftLim = 0;
		int rightLim = 0;

		public DepthSearcher(Rectangle field, int leftLim, int rightLim) {
			super();
			this.field = field;
			this.leftLim = leftLim;
			this.rightLim = rightLim;
		}


		@Override
		public void run() {
			double[] coordinate = plotSheet.toCoordinatePoint(0, 0, field);
			double f_xy = function.f(coordinate[0], coordinate[1]);
			this.f_xHighest = f_xy;
			this.f_xLowest 	= f_xy;
			
			//scan for minimum and maximum f(x,y) in the given range
			for(int i = leftLim; i <= rightLim; i+=pixelSkip) {
				for(int j = field.y+plotSheet.getFrameThickness(); j < field.y +field.height-plotSheet.getFrameThickness(); j+=pixelSkip) {
					if(abortPaint){
						return;
					}
					coordinate = plotSheet.toCoordinatePoint(i, j, field);
					f_xy = function.f(coordinate[0], coordinate[1]);
					if(f_xy < this.f_xLowest && f_xy != Double.NaN && f_xy != Double.NEGATIVE_INFINITY && f_xy != Double.POSITIVE_INFINITY ) {
						this.f_xLowest 	= f_xy;
					} 
					if(f_xy > this.f_xHighest && f_xy != Double.NaN && f_xy != Double.NEGATIVE_INFINITY && f_xy != Double.POSITIVE_INFINITY ) {
						this.f_xHighest 	= f_xy;
					}
					
					
				}
			}
			
		}

		public double getF_xHighest() {
			return f_xHighest;
		}


		public double getF_xLowest() {
			return f_xLowest;
		}

	}
	
	private class PartRenderer implements Runnable{

		Graphics g = null;
		Rectangle field = null;
		int leftLim = 0;
		int rightLim = 0;

		public PartRenderer(Graphics g, int leftLim, int rightLim) {
			super();
			this.field = g.getClipBounds();
			this.leftLim = leftLim;
			this.rightLim = rightLim;
			this.g = g;
		}


		@Override
		public void run() {
			double[] thisCoordinate = plotSheet.toCoordinatePoint(0, 0, field);
			
			double thisF_xy;
			for(int i = leftLim ; i <= rightLim; i+=pixelSkip) {
				for(int j = field.y + +plotSheet.getFrameThickness() ; j < field.y +field.height -plotSheet.getFrameThickness(); j+=pixelSkip) {
					if(abortPaint)
						return;
					thisCoordinate = plotSheet.toCoordinatePoint(i, j, field);
					thisF_xy = function.f(thisCoordinate[0], thisCoordinate[1]);
					g.setColor(getColor(thisF_xy));
					g.fillRect(i, j, pixelSkip, pixelSkip);
//					g.drawLine(i, j, i, j);
					
				}
			}
			
		}

	}

	@Override
	public void abortAndReset() {
		abortPaint = true;
		
	}

	public int getPixelSkip() {
		return pixelSkip;
	}

	public void setPixelSkip(int pixelSkip) {
		this.pixelSkip = pixelSkip;
	}
	
	
}
