/**
 * 
 */
package wildPlot.rendering;

import java.awt.Graphics;

/**
 * Classes that implement the Drawable interface have the ability to draw with a provided Graphics object onto
 * a PlotSheet.
 * 
 * @see wildPlot.rendering.PlotSheet
 */
public interface Drawable {
	
	/**
	 * Paint the drawable object
	 * @param g
	 */
	public void paint(Graphics g);
	
	/**
	 * Returns true if this Drawable can draw on the outer frame of the plot
	 * this is necessary because normally everything drawn over the border will be whited out by the PlotSheet object.
	 * If a legend or descriptions shall be drawn onto the outer frame this method of the corresponding Drawables has
	 * to return true. For all other cases it is highly recommended to return false.
	 * @return
	 */
	public boolean isOnFrame();

	public void abortAndReset();
}
