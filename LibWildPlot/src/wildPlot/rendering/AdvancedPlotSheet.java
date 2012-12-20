/**
 * 
 */
package wildPlot.rendering;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;



/**
 * This is a sheet that is used to plot mathematical functions including coordinate systems and optional extras like 
 * legends and descriptors. Additionally all conversions from image to plot coordinates are done here
 * @see wildPlot.rendering.Drawable Drawable interface for objects that can be drawn onto a PlotSheet
 */
public class AdvancedPlotSheet extends PlotSheet implements Runnable{
	
	private ReentrantLock reentrantLock = new ReentrantLock();
	private boolean pictureIsConstructed = false;
	BufferedImage plotImage = null;
	private boolean operationIsAborted = false;
	private Rectangle field;
	private boolean isLogX = false;
	
	private boolean isLogY = false;
	private boolean hasTitle = false;
	private boolean hasFirstPixelSkipSet = false;
	private int firstPixelSkip = 20;
	private ReliefDrawer reliefDrawer = null;
	
	/**
	 * title of plotSheet
	 */
	private String title = "PlotSheet";
	
	/**
	 * not yet implemented
	 */
	private boolean isMultiMode = false;
	
	/**
	 * thickness of frame in pixel
	 */
	private int frameThickness = 0;
	
	/**
	 * states if there is a border between frame and plot
	 */
	private boolean isBordered = true;
	
	/**
	 * thickness of border in pixel, until now more than 1 may bring problems for axis drawing
	 */
	private int borderThickness = 1;
	
	//if class shold be made threadable for mulitplot mode, than
	//this must be done otherwise
	/**
	 * screen that is currently rendered
	 */
	private int currentScreen = 0;
	
	/**
	 * the ploting screens, screen 0 is the only one in single mode
	 */
	Vector<MultiScreenPart> screenParts = new Vector<MultiScreenPart>();
	
	/**
	 * Create a virtual sheet used for the plot
	 * @param xStart the start of the x-range
	 * @param xEnd the end of the x-range
	 * @param yStart the start of the y-range
	 * @param yEnd the end of the y-range
	 * @param drawables list of Drawables that shall be drawn onto the sheet
	 */
	public AdvancedPlotSheet(double xStart, double xEnd, double yStart, double yEnd, Vector<Drawable> drawables) {
		super(xStart, xEnd, yStart, yEnd, drawables);
		double[] xRange = {xStart, xEnd};
		double[] yRange = {yStart, yEnd};
		screenParts.add(0, new MultiScreenPart(xRange, yRange, drawables));
	}
	
	/**
	 * 
	 * Create a virtual sheet used for the plot
	 * @param xStart the start of the x-range
	 * @param xEnd the end of the x-range
	 * @param yStart the start of the y-range
	 * @param yEnd the end of the y-range
	 */
	public AdvancedPlotSheet(double xStart, double xEnd, double yStart, double yEnd) {
		super(xStart, xEnd, yStart, yEnd);
		double[] xRange = {xStart, xEnd};
		double[] yRange = {yStart, yEnd};
		screenParts.add(0, new MultiScreenPart(xRange, yRange));
		
	}
	
	public void setClip(Rectangle field){
		this.field = field;
	}
	
	@Override
	public void run() {
		operationIsAborted = false;
		pictureIsConstructed = false;
		HashMap<Thread, DrawableDrawingRunnable> allDrawableDirectory = new HashMap<Thread, AdvancedPlotSheet.DrawableDrawingRunnable>();
		this.currentScreen = 0;
		
		Vector<DrawableDrawingRunnable> offFrameDrawables = new Vector<DrawableDrawingRunnable>();
		Vector<DrawableDrawingRunnable> onFrameDrawables = new Vector<DrawableDrawingRunnable>();
		BufferedImage bufferedFrameImage = new BufferedImage(field.width, field.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gFrame = bufferedFrameImage.createGraphics();
		gFrame.setClip(field);
		gFrame.setColor(Color.BLACK);
		Thread[] threads = new Thread[this.screenParts.get(currentScreen).getDrawables().size()]; 
		int i = 0;
		
		if(this.screenParts.get(currentScreen).getDrawables() != null && this.screenParts.get(currentScreen).getDrawables().size() != 0) {
			for(Drawable draw : this.screenParts.get(currentScreen).getDrawables()) {
				DrawableDrawingRunnable drawableDrawingRunnable = new DrawableDrawingRunnable(draw, field);
				threads[i] = new Thread( drawableDrawingRunnable);
				allDrawableDirectory.put(threads[i],drawableDrawingRunnable );
				threads[i++].start();
				if(!draw.isOnFrame()) {
					offFrameDrawables.add(drawableDrawingRunnable);
				} else {
					onFrameDrawables.add(drawableDrawingRunnable);
				}
			}
		}
		
		//paint white frame to over paint everything that was drawn over the border 
		Color oldColor = gFrame.getColor();
		if(this.frameThickness>0){
			gFrame.setColor(Color.WHITE);
			//upper frame
			gFrame.fillRect(0, 0, field.width, this.frameThickness);

			//left frame
			gFrame.fillRect(0, this.frameThickness, this.frameThickness, field.height);
			
			//right frame
			gFrame.fillRect(field.width+1-this.frameThickness, this.frameThickness,this.frameThickness+2, field.height-this.frameThickness);
			
			//bottom frame
			gFrame.fillRect(this.frameThickness, field.height-this.frameThickness, field.width-this.frameThickness,this.frameThickness+1);
			
			//make small black border frame
			if(isBordered){
				gFrame.setColor(Color.black);
				//upper border
				gFrame.fillRect(this.frameThickness-borderThickness+1, this.frameThickness-borderThickness+1, field.width-2*this.frameThickness+2*borderThickness-2, borderThickness);
				
				//lower border
				gFrame.fillRect(this.frameThickness-borderThickness+1, field.height-this.frameThickness, field.width-2*this.frameThickness+2*borderThickness-2, borderThickness);
				
				//left border
				gFrame.fillRect(this.frameThickness-borderThickness+1, this.frameThickness-borderThickness+1, borderThickness, field.height-2*this.frameThickness+2*borderThickness-2);
				
				//right border
				gFrame.fillRect(field.width-this.frameThickness, this.frameThickness-borderThickness+1, borderThickness, field.height-2*this.frameThickness+2*borderThickness-2);
				
			}
			
			gFrame.setColor(oldColor);
			
			Font oldFont = gFrame.getFont();
			gFrame.setFont(oldFont.deriveFont(20.0f));
			FontMetrics fm = gFrame.getFontMetrics();
			int height = fm.getHeight();
			
			int width = fm.stringWidth(this.title);
			gFrame.drawString(this.title, field.width/2 -width/2, this.frameThickness - 10 - height);
			gFrame.setFont(oldFont);
		}
		gFrame.dispose();
		
		boolean finished = true;
		do{
			//TODO give this abort message to all drawingRunnable for them to handle this event
			if(operationIsAborted){
				if(hasFirstPixelSkipSet){
					this.reliefDrawer.setPixelSkip(firstPixelSkip);
					this.reliefDrawer.abortAndReset();
				}
				for(Drawable draw : this.screenParts.get(currentScreen).getDrawables()) {
					draw.abortAndReset();
				}
				for(i=0; i<threads.length; i++ ){
					try {
						threads[i].join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return;
			}
			
			finished = true;
			for(i=0; i<threads.length; i++ ){
				DrawableDrawingRunnable currentDrawableRunnable = allDrawableDirectory.get(threads[i]);
				if (currentDrawableRunnable.hasFinished()) {
					if(!currentDrawableRunnable.hasJoined()){
						Drawable drawable = currentDrawableRunnable.getDrawable();
						if(drawable instanceof ReliefDrawer){
							reliefDrawer = (ReliefDrawer)drawable;
							if(!this.hasFirstPixelSkipSet){
								this.firstPixelSkip = reliefDrawer.getPixelSkip();
								hasFirstPixelSkipSet = true;
							}
							if(reliefDrawer.getPixelSkip() == 1){
								try {
									threads[i].join();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								currentDrawableRunnable.hasJoined = true;
							} else {
								reliefDrawer.setPixelSkip(reliefDrawer.getPixelSkip() -1);
								try {
									threads[i].join();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								BufferedImage oldReliefImage = currentDrawableRunnable.getBufferedDrawableImage();
								currentDrawableRunnable.setBufferedOldDrawableImage(oldReliefImage);
								threads[i] = new Thread(currentDrawableRunnable);
								threads[i].start();
								allDrawableDirectory.put(threads[i], currentDrawableRunnable);
								finished = false;
							}
							
						}else{
							try {
								threads[i].join();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							currentDrawableRunnable.hasJoined = true;
						}
					}
					
				} else {
					finished = false;
				}
			}
			BufferedImage buffTempImage = new BufferedImage(field.width, field.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g =  buffTempImage.createGraphics();
			g.setClip(field);
			
			for(DrawableDrawingRunnable offFrameDrawable : offFrameDrawables){
				if(offFrameDrawable.hasJoined)
					g.drawImage(offFrameDrawable.getBufferedDrawableImage(), null, 0, 0);
				else if(offFrameDrawable.getBufferedOldDrawableImage() != null)
					g.drawImage(offFrameDrawable.getBufferedOldDrawableImage(), null, 0, 0);
				
					
			}
			g.drawImage(bufferedFrameImage, null, 0, 0);
			for(DrawableDrawingRunnable onFrameDrawable : onFrameDrawables){
				if(onFrameDrawable.hasJoined)
					g.drawImage(onFrameDrawable.getBufferedDrawableImage(), null, 0, 0);
				else if(onFrameDrawable.getBufferedOldDrawableImage() != null)
					g.drawImage(onFrameDrawable.getBufferedOldDrawableImage(), null, 0, 0);
			}
			g.dispose();
			
			reentrantLock.lock();
			this.plotImage = buffTempImage;
			reentrantLock.unlock();
			
		}while(!finished);
		if(hasFirstPixelSkipSet){
			this.reliefDrawer.setPixelSkip(firstPixelSkip);
		}
		pictureIsConstructed = true;
		
	}
	public void abortOperation(){
		this.operationIsAborted = true;
	}
	
	/**
	 * update the x-Range of this PlotSheet
	 * @param xStart left beginning of plot
	 * @param xEnd right end of plot
	 */
    @Override
	public void updateX(double xStart, double xEnd) {
    	double[] xRange = {xStart, xEnd};
        this.screenParts.get(0).setxRange(xRange);
    }
    
    /**
	 * update the y-Range of this PlotSheet
	 * @param yStart bottom beginning of plot
	 * @param yEnd upper end of plot
	 */
    @Override
	public void updateY(double yStart, double yEnd) {
    	double[] yRange = {yStart, yEnd};
        this.screenParts.get(0).setyRange(yRange);
    }
	
	/**
	 * add another Drawable object that shall be drawn onto the sheet
	 * this adds only drawables for the first screen in multimode plots for
	 * 
	 * @param draw Drawable object which will be addet to plot sheet
	 */
	@Override
	public void addDrawable(Drawable draw) {
		this.screenParts.get(0).addDrawable(draw);
	}
	

	/**
	 * converts a given x coordinate from ploting field coordinate to a graphic field coordinate
	 * @param x given graphic x coordinate
	 * @param field the graphic field
	 * @return the converted x value
	 */
	@Override
	@Deprecated
	public int xToGraphic(double x, Rectangle field) {

		return (this.isLogX)?xToGraphicLog(x,field):xToGraphicLinear(x,field);
	}
	private int xToGraphicLinear(double x, Rectangle field) {
		double xQuotient = (field.width - 2*frameThickness) / (Math.abs(this.screenParts.get(currentScreen).getxRange()[1] - this.screenParts.get(currentScreen).getxRange()[0]));
		double xDistanceFromLeft = x - this.screenParts.get(currentScreen).getxRange()[0];
		
		return field.x + frameThickness + (int)Math.round(xDistanceFromLeft * xQuotient);
	}
	private int xToGraphicLog(double x, Rectangle field) {
		double range = Math.log10(this.screenParts.get(currentScreen).getxRange()[1]) - Math.log10(this.screenParts.get(currentScreen).getxRange()[0]);
		
		return (int) Math.round(field.x + this.frameThickness + (Math.log10(x) - Math.log10(this.screenParts.get(currentScreen).getxRange()[0]))/(range) * (field.width - 2*frameThickness));
	}
	
	/**
	 * 
	 * converts a given y coordinate from ploting field coordinate to a graphic field coordinate
	 * @param y given graphic y coordinate
	 * @param field the graphic field
	 * @return the converted y value
	 */
	@Override
	@Deprecated
	public int yToGraphic(double y, Rectangle field) {
		return (this.isLogY)?yToGraphicLog(y,field):yToGraphicLinear(y,field);
	}
	
	
	private int yToGraphicLinear(double y, Rectangle field) {
		double yQuotient = (field.height -2*frameThickness) / (Math.abs(this.screenParts.get(currentScreen).getyRange()[1] - this.screenParts.get(currentScreen).getyRange()[0]));
		double yDistanceFromTop = this.screenParts.get(currentScreen).getyRange()[1] - y;
		
		return field.y + frameThickness + (int)Math.round(yDistanceFromTop * yQuotient);
	}
	private int yToGraphicLog(double y, Rectangle field) {
		
		
		return (int) Math.round((((Math.log10(y)-Math.log10(this.screenParts.get(currentScreen).getyRange()[0]))/(Math.log10(this.screenParts.get(currentScreen).getyRange()[1]) - Math.log10(this.screenParts.get(currentScreen).getyRange()[0]))) *(field.height-2*this.frameThickness) - (field.height-2*this.frameThickness))*(-1) + this.frameThickness   );
	}
	
	/**
	 * Convert a coordinate system point to a point used for graphical processing (with hole pixels) 
	 * @param x given x-coordinate
	 * @param y given y-coordinate
	 * @param field clipping bounds for drawing
	 * @return the point in graphical coordinates
	 */
	@Override
	public int[] toGraphicPoint(double x, double y, Rectangle field) {
		int[] graphicPoint = {xToGraphic(x, field), yToGraphic(y, field)};
		return graphicPoint;
	}
	
	/**
	 * Transforms a graphical x-value to a x-value from the plotting coordinate system.
	 * This method should not be used for future compatibility as transformations in more complex coordinate systems 
	 * cannot be done by only giving one coordinate
	 * @param x graphical x-coordinate
	 * @param field clipping bounds
	 * @return x-coordinate in plotting coordinate system
	 */
	@Override
	@Deprecated
	public double xToCoordinate(int x, Rectangle field) {
		
		
		return (this.isLogX)?xToCoordinateLog(x,field):xToCoordinateLinear(x,field);
	}
	
	private double xToCoordinateLinear(int x, Rectangle field) {
		double xQuotient = (Math.abs(this.screenParts.get(currentScreen).getxRange()[1] - this.screenParts.get(currentScreen).getxRange()[0])) / (field.width-2*frameThickness);
		double xDistanceFromLeft = field.x - frameThickness + x;
		
		return this.screenParts.get(currentScreen).getxRange()[0] + xDistanceFromLeft*xQuotient;
	}
	
	private double xToCoordinateLog(int x, Rectangle field) {
		double range = Math.log10(this.screenParts.get(currentScreen).getxRange()[1]) - Math.log10(this.screenParts.get(currentScreen).getxRange()[0]);
		
		return Math.pow(10, ((x- (field.x + this.frameThickness))*1.0*(range) )/(field.width - 2.0*frameThickness) + Math.log10(this.screenParts.get(currentScreen).getxRange()[0]) ) ;
	}
	
	
	/**
	 * Transforms a graphical y-value to a y-value from the plotting coordinate system.
	 * This method should not be used for future compatibility as transformations in more complex coordinate systems 
	 * cannot be done by only giving one coordinate
	 * @param y graphical y-coordinate
	 * @param field clipping bounds
	 * @return y-coordinate in plotting coordinate system
	 */
	@Override
	@Deprecated
	public double yToCoordinate(int y, Rectangle field) {
		
		
		return (this.isLogY)?yToCoordinateLog(y, field):yToCoordinateLinear(y, field);
	}
	
	@Override
	public double yToCoordinateLinear(int y, Rectangle field) {
		double yQuotient = (Math.abs(this.screenParts.get(currentScreen).getyRange()[1] - this.screenParts.get(currentScreen).getyRange()[0])) / (field.height -2*frameThickness);
		double yDistanceFromBottom = field.y + field.height - 1 - y -frameThickness;
		
		return this.screenParts.get(currentScreen).getyRange()[0] + yDistanceFromBottom*yQuotient;
	}
	
	@Override
	public double yToCoordinateLog(int y, Rectangle field) {

		return Math.pow(10, ((y - this.frameThickness + (field.height-2*this.frameThickness))*(-1))/((field.height-2*this.frameThickness))*((Math.log10(this.screenParts.get(currentScreen).getyRange()[1]) - Math.log10(this.screenParts.get(currentScreen).getyRange()[0]))) +Math.log10(this.screenParts.get(currentScreen).getyRange()[0]));
	}
	
	/**
	 * Convert a graphical coordinate-system point to a point used for plotting processing 
	 * @param x given graphical x
	 * @param y given graphical y
	 * @param field clipping bounds for drawing
	 * @return the point in plotting coordinates
	 */
	@Override
	public double[] toCoordinatePoint(int x, int y, Rectangle field) {
		double[] coordinatePoint = {xToCoordinate(x, field), yToCoordinate(y, field)};
		
		return coordinatePoint;
	}

	
	/**
	 * the x-range for the plot
	 * @return double array in the lenght of two with the first element beeingt left and the second element beeing the right border
	 */
	@Override
	public double[] getxRange() {
		return this.screenParts.get(0).getxRange();
	}
	
	/**
	 * sets new bounds for x coordinates on the plot
	 * @param xRange double array in the length of two with the first element beeingt left and the second element beeing the right border
	 */
	@Override
	public void setxRange(double[] xRange) {
		this.screenParts.get(0).setxRange(xRange);
	}
	
	/**
	 * the <-range for the plot
	 * @return double array in the lenght of two with the first element being lower and the second element being the upper border
	 */
	@Override
	public double[] getyRange() {
		return this.screenParts.get(0).getyRange();
	}
	
	/**
	 * sets new bounds for y coordinates on the plot
	 * @param yRange double array in the length of two with the first element beeingt left and the second element beeing the right border
	 */
	@Override
	public void setyRange(double[] yRange) {
		this.screenParts.get(0).setyRange(yRange);
	}
	
	/**
	 * returns the size in pixel of the outer frame
	 * @return the size of the outer frame in pixel
	 */
	@Override
	public int getFrameThickness() {
		return (isMultiMode)? 0:frameThickness;
	}
	
	/**
	 * set the size of the outer frame in pixel
	 * @param frameThickness new size for the outer frame in pixel
	 */
	@Override
	public void setFrameThickness(int frameThickness) {
		if(frameThickness < 0){
			System.err.println("PlotSheet:Error::Wrong Frame size (smaller than 0)");
			System.exit(-1);
		}
		this.frameThickness = frameThickness;
	}
	
	/**
	 * sets the size of the border between plot and outer frame in pixel
	 * @param borderThickness size of border in pixel
	 */
	@Override
	public void setBorderThickness(int borderThickness) {
		this.borderThickness = borderThickness;
		this.isBordered = true;
	}
	
	/**
	 * activates the border between outer frame and plot
	 */
	@Override
	public void setBorder() {
		this.isBordered = true;
	}
	
	/**
	 * deactivates the border between outer frame and plot
	 */
	@Override
	public void unsetBorder() {
		this.isBordered = false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see rendering.Drawable#isOnFrame()
	 */
	@Override
	public boolean isOnFrame() {
		return false;
	}
	
	/**
	 * this function calculates the best approximation for a 10based tic distance based on a given pixeldistance for x-axis tics
	 * @param pixelDistance
	 * @param field
	 * @return
	 */
	@Override
	public double ticsCalcX(int pixelDistance, Rectangle field){
		double deltaRange = this.screenParts.get(currentScreen).getxRange()[1] - this.screenParts.get(currentScreen).getxRange()[0];
		int ticlimit = field.width/pixelDistance;
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
	 * this function calculates the best approximation for a 10based tic distance based on a given pixeldistance for y-axis tics
	 * @param pixelDistance
	 * @param field
	 * @return
	 */
	@Override
	public double ticsCalcY(int pixelDistance, Rectangle field){
		double deltaRange = this.screenParts.get(currentScreen).getyRange()[1] - this.screenParts.get(currentScreen).getyRange()[0];
		int ticlimit = field.height/pixelDistance;
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
	 * set the title of the plot
	 * @param title title string shown above plot
	 */
	@Override
	public void setTitle(String title){
		this.title = title;
		this.hasTitle = true;
	}

	/**
	 * @return the isMultiMode
	 */
	@Override
	public boolean isMultiMode() {
		return isMultiMode;
	}
	
	@Override
	public void setLogX() {
		this.isLogX = true;
	}

	@Override
	public void setLogY() {
		this.isLogY = true;
	}
	
	@Override
	public void unsetLogX() {
		this.isLogX = false;
	}

	@Override
	public void unsetLogY() {
		this.isLogY = false;
	}
	
	public BufferedImage getPlotImage(){
		reentrantLock.lock();
		try{
			return plotImage;
		} finally {
			reentrantLock.unlock();
		}
	}
	
	public boolean isFinished(){
		return pictureIsConstructed;
	}
	
	private class DrawableDrawingRunnable implements Runnable {
		
		private Drawable drawable;
		private boolean hasFinished = false;
		private boolean hasJoined = false;
		
		private BufferedImage bufferedDrawableImage;
		private BufferedImage bufferedOldDrawableImage = null;
		private Rectangle field;

		public DrawableDrawingRunnable(Drawable drawable, Rectangle field) {
			super();
			this.drawable = drawable;
			this.field = field;
			
		}


		@Override
		public void run() {
			bufferedDrawableImage = new BufferedImage(field.width, field.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = bufferedDrawableImage.createGraphics();
			g.setClip(field);
			g.setColor(Color.BLACK);
			drawable.paint(g);
			g.dispose();
			this.hasFinished = true;
			
		}

		public Drawable getDrawable() {
			return drawable;
		}

		public boolean hasFinished(){
			return hasFinished;
		}
		public boolean hasJoined(){
			return hasJoined;
		}
		public BufferedImage getBufferedDrawableImage() {
			return bufferedDrawableImage;
		}


		public BufferedImage getBufferedOldDrawableImage() {
			return bufferedOldDrawableImage;
		}


		public void setBufferedOldDrawableImage(BufferedImage bufferedOldDrawableImage) {
			this.bufferedOldDrawableImage = bufferedOldDrawableImage;
		}
		
		
		
	}

	
}
