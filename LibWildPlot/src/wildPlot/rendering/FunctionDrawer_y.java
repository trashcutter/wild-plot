/**
 * 
 */
package wildPlot.rendering;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

import wildPlot.rendering.interfaces.Function2D;
import wildPlot.rendering.interfaces.StepFunction2D;

/**
 * FunctionDrawer is used to draw a mathematical function on a given PlotSheet
 * 
 * 
 */
public class FunctionDrawer_y implements Drawable {
	private double binSize = 1;
	private boolean isStepFunction = false;
	
	private double extraScaleFactor = 1;
	
	private boolean autoscale = false;
	
	private double scaleFactor = 10;
	
	private boolean isOnFrame = false;
	
	private double xOffset = 0;
	
	private float size = 1;
	
	private double leftLimit = 0;
	private double rightLimit = 0;
	
	private boolean hasLimit = false;
	
	/**
	 * true when warning for pole positions is allready given
	 */
	private boolean warned = false;
	
	/**
	 * the function which will be plotted with this FunctionDrawer
	 */
	private Function2D function;
	
	/**
	 * the PlotSheet on which this FunctionDrawer is drawing upon
	 */
	private PlotSheet plotSheet;
	
	/**
	 * the color of the function graph
	 */
	private Color color = new Color(255,0,0);;
	
	/**
	 * Constructor for a FunctionDrawer object
	 * @param function given function which is drawn
	 * @param plotSheet the sheet the function will be drawn onto
	 * @param color color of the function
	 */
	public FunctionDrawer_y(Function2D function, PlotSheet plotSheet, Color color) {
		this.function = function;
		this.plotSheet = plotSheet;
		this.color = color;
	}
	
	/**
	 * Constructor for a FunctionDrawer object
	 * @param function given function which is drawn
	 * @param plotSheet the sheet the function will be drawn onto
	 * @param color color of the function
	 */
	public FunctionDrawer_y(Function2D function, PlotSheet plotSheet, Color color, double leftLimit, double rightLimit) {
		this.function = function;
		this.plotSheet = plotSheet;
		this.color = color;
		this.hasLimit = true;
		this.leftLimit = leftLimit;
		this.rightLimit = rightLimit;
	}
	
	/* (non-Javadoc)
	 * @see rendering.Drawable#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(Graphics g) {
		if(function instanceof StepFunction2D) {
			this.isStepFunction = true;
		}
		 Graphics2D g2D = (Graphics2D) g;     
		 Stroke oldStroke = g2D.getStroke();
		    g2D.setStroke(new BasicStroke(this.size));  // set stroke width of 10
		 
		Color oldColor = g.getColor();
		Rectangle field = g.getClipBounds();
		g.setColor(color);
		
		
		if(autoscale){
			double[] start = this.plotSheet.toCoordinatePoint(0, 0, field);
			double[] end = this.plotSheet.toCoordinatePoint(0+this.plotSheet.getFrameThickness(), 0, field);
			
			this.scaleFactor = Math.abs(end[0] - start[0]);
//			this.scaleFactor *= binSize;
		} else {
			this.scaleFactor = 1.0;
		}
		
		if(this.isOnFrame)
			xOffset = plotSheet.getxRange()[0];
		
		double[] drawingPoint = plotSheet.toCoordinatePoint(field.x,field.height,field);
		if(this.isOnFrame)
			drawingPoint = plotSheet.toCoordinatePoint(field.x,field.height-this.plotSheet.getFrameThickness(),field);
		
		double f_y = function.f(drawingPoint[1])*scaleFactor*extraScaleFactor;
		double f_y_old = f_y;
		
		int[] coordStart = plotSheet.toGraphicPoint(f_y,drawingPoint[1],field);
		if(this.isOnFrame)
			coordStart = plotSheet.toGraphicPoint(this.xOffset-f_y,drawingPoint[1],field);
		
		int[] coordEnd = coordStart;
		
		int leftStart = field.height+field.y;
		int rightEnd = field.y;
		if(this.isOnFrame){
			leftStart = field.height+field.y-this.plotSheet.getFrameThickness();
			rightEnd = field.y+this.plotSheet.getFrameThickness();
		}
		
		if(this.hasLimit){
			leftStart = plotSheet.yToGraphic(leftLimit, field);
			rightEnd = plotSheet.yToGraphic(rightLimit, field);
		}
		
		for(int i = leftStart; i> rightEnd; i--) {
			drawingPoint = plotSheet.toCoordinatePoint(0,i,field);
			
			coordEnd = coordStart;
			
			f_y_old = f_y;
			f_y = function.f(drawingPoint[1])*scaleFactor*extraScaleFactor;
			coordStart = plotSheet.toGraphicPoint(f_y,drawingPoint[1],field);
			if(this.isOnFrame)
				coordStart = plotSheet.toGraphicPoint(this.xOffset-f_y,drawingPoint[1],field);
			
			double overlap = 0.2 * (plotSheet.getxRange()[1] - plotSheet.getxRange()[0]);
			
			if(f_y_old != Double.NaN && f_y!= Double.NaN &&
					f_y_old != Double.NEGATIVE_INFINITY && f_y!= Double.NEGATIVE_INFINITY &&
					f_y_old != Double.POSITIVE_INFINITY && f_y!= Double.POSITIVE_INFINITY &&
					f_y_old <= plotSheet.getxRange()[1] + overlap && f_y_old >= plotSheet.getxRange()[0] - overlap &&
					f_y <= plotSheet.getxRange()[1] + overlap && f_y >= plotSheet.getxRange()[0] - overlap) {
				if(!this.isStepFunction)
					g.drawLine(coordStart[0], coordStart[1], coordEnd[0], coordEnd[1]);
				else
					g.drawLine(coordStart[0], coordStart[1], coordEnd[0], coordStart[1]);
			} else if(!warned) {
				System.err.println("Could not draw part of function, possible pole or out of reach");
				warned = true;
			}
			
		}
		g2D.setStroke(oldStroke);
		g.setColor(oldColor);

	}
	
	
	public double getMaxValue(int pixelResolution){
		Rectangle field = new Rectangle(pixelResolution, pixelResolution);
		double[] drawingPoint = plotSheet.toCoordinatePoint(field.x,0,field);
		double max = Double.NEGATIVE_INFINITY;
		
		for(int i = 0; i< pixelResolution; i++) {
			drawingPoint = plotSheet.toCoordinatePoint(i,0,field);
			double f_x = function.f(drawingPoint[0]);
			if(f_x > max)
				max=f_x;
		}
		
		return max;
	}
	/**
	 * @param size the size to set
	 */
	public void setSize(float size) {
		this.size = size;
	}

	public boolean isOnFrame() {
		return this.isOnFrame;
	}
	
	/**
	 * unset the axis to draw on the border between outer frame and plot
	 */
	public void unsetOnFrame() {
		this.isOnFrame = false;
		xOffset = 0;
	}
	
	/**
	 * set the axis to draw on the border between outer frame and plot
	 */
	public void setOnFrame(double extraSpace) {
		this.isOnFrame = true;
		xOffset = plotSheet.getxRange()[0]+ extraSpace;
	}
	
	public void setOnFrame() {
		setOnFrame(0);
	}
	
	public void setAutoscale(double binWidth) {
		this.binSize = binWidth;
		this.autoscale = true;
	}
	public void unsetAutoscale() {
		this.autoscale = false;
	}

	public double getExtraScaleFactor() {
		return extraScaleFactor;
	}

	public void setExtraScaleFactor(double extraScaleFactor) {
		this.extraScaleFactor = extraScaleFactor;
	}

	@Override
	public void abortAndReset() {
		// TODO Auto-generated method stub
		
	}
	
	
}
