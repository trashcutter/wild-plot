/**
 * 
 */
package wildPlot.rendering;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import wildPlot.rendering.interfaces.Function2D;

/**
 * Integral marks the region between x axis and a function or another function for a given interval 
 *
 */
public class Integral implements Drawable {
	
	private Function2D function = null;
	
	private Function2D function2 = null;
	
	
	private PlotSheet plotSheet;
	
	private double start = 0;
	
	private double end = Math.PI;
	
	private Color color = new Color(0.7f, 1f, 0f, 0.4f);
	
	/**
	 * set the color for integral area
	 * @param color integral area color
	 */
	public void setColor(Color color) {
		this.color = color;
	}

	/**
	 * Constructor for Integral object for integral between a function and x-axis
	 * @param function given function for the integral
	 * @param plotSheet the sheet the integral will be drawn onto
	 * @param start starting position
	 * @param end ending position
	 */
	public Integral(Function2D function, PlotSheet plotSheet, double start, double end) {
		super();
		this.function = function;
		this.plotSheet = plotSheet;
		this.start = start;
		this.end = end;
	}
	
	/**
	 * Constructor for Integral object between two functions
	 * @param function given function for the integral
	 * @param function2 second given function for the integral
	 * @param plotSheet the sheet the integral will be drawn onto
	 * @param start starting position
	 * @param end ending position
	 */
	public Integral(Function2D function, Function2D function2, PlotSheet plotSheet, double start, double end) {
		super();
		this.function = function;
		this.function2 = function2;
		this.plotSheet = plotSheet;
		this.start = start;
		this.end = end;
	}


	/* (non-Javadoc)
	 * @see rendering.Drawable#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(Graphics g) {
		
		Color oldColor = g.getColor();
		Rectangle field = g.getClipBounds();
		BufferedImage bimage = new BufferedImage(field.width, field.height, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g2d = bimage.createGraphics();
		g2d.setColor(color);
		
		int[] startPoint 	= plotSheet.toGraphicPoint(this.start, 0, field);
		int[] endPoint 		= plotSheet.toGraphicPoint(this.end, 0, field);
		
		for(int i = startPoint[0]; i<=endPoint[0];i++) {
			double currentX = plotSheet.xToCoordinate(i, field);
			double currentY = function.f(currentX);
			
			if(this.function2 != null){
				double currentY2 = function2.f(currentX);
				g2d.drawLine(plotSheet.xToGraphic(currentX, field), plotSheet.yToGraphic(currentY, field), plotSheet.xToGraphic(currentX, field), plotSheet.yToGraphic(currentY2, field));
			}else {
				g2d.drawLine(plotSheet.xToGraphic(currentX, field), plotSheet.yToGraphic(currentY, field), plotSheet.xToGraphic(currentX, field), plotSheet.yToGraphic(0, field));
			}
			
			
			
		}
		
		g2d = (Graphics2D)g;
		g2d.drawImage(bimage, null, 0, 0);
		
		g.setColor(oldColor);

	}
	
	/*
	 * (non-Javadoc)
	 * @see rendering.Drawable#isOnFrame()
	 */
	public boolean isOnFrame() {
		return false;
	}

	@Override
	public void abortAndReset() {
		// TODO Auto-generated method stub
		
	}
}
