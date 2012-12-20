package wildPlot.gui;

import java.awt.*;
import java.awt.BufferCapabilities.FlipContents;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import wildPlot.rendering.PlotSheet;


/**
 * Canvas based plotting canvas
 * @author Michael Goldbach
 *
 */
public class PlotCanvas extends Canvas implements Runnable {
	
	private boolean wasCursorOnSaveButton = false;
	private long timestamp = 0;
	
	private int unitsToScroll = 0;
	private boolean hasScrolled = false;
	
	private boolean hasClicked = false;
	
	/**
	 * Probably needed for something
	 */
	private static final long serialVersionUID = -5160512933071989649L;
	
	
	private DecimalFormat dfExponent =   new DecimalFormat( "##0.###E0" );
	/**
	 * DecimalFormat used to format x and y coordinates on canvas
	 */
	private DecimalFormat dfNormal =   new DecimalFormat( "##0.###" );
	
	/**
	 * this plot is refreshed manually for that purpose we use BufferStrategy to get graphic acceleration
	 */
	private BufferStrategy strategy;

	/**
	 * logic loop boolean 
	 */
	private boolean plotRunning = true;

	/**
	 * is set to true when window is resized
	 */
	private boolean resized = true;
	
	/**
	 * to access this object in eventhandler this variable is used
	 */
	private Canvas thisCanvas = null;
	
	/**
	 * this PlotSheet is drawn onto the canvas
	 */
	private PlotSheet plotSheet = null;
	
	/**
	 * the plot image is stored in this bufferd image for reuse in other cycles
	 */
	private BufferedImage bimage = null;
	
	private int width 	= 0;
	private int height 	= 0;
	
	JFrame container = null;
	
	public PlotCanvas(PlotSheet plotSheet) {
		
		thisCanvas = this;
		this.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent arg0) {
				PlotCanvas.this.hasScrolled = true;
				PlotCanvas.this.unitsToScroll += arg0.getUnitsToScroll()/arg0.getScrollAmount();
				
			}
		});
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				PlotCanvas.this.hasClicked = true;
			}
		});
		
		
		//Frame window this canvas is on
		container = new JFrame("WildPlot");
		container.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		//size of window, canvas will be little bit smaller
		container.setBounds(100, 100, 1024, 1024);
		
		//standard color of canvas is system based so set this to white
		setBackground(Color.WHITE);
		//setBounds(0,0, 1024, 768);
		
		//center location is best for using of whole window 
		container.getContentPane().add(this, BorderLayout.CENTER);
		
		//to speed up and usage of system acceleration
		setIgnoreRepaint(true);
		
		//make window visible
		container.setVisible(true);
		
		//for safety, if window is closed, end this application
		container.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				plotRunning = false;
				//System.exit(0);
			}
		});
		
		//if window is resized remember that for use in loop logic
		container.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent arg0) {
				resized = true;
			}
		});
		
		//it would be nice if plotting window would be focused on plotting start, but this does not seem to work
		//on windows
		requestFocus();
		
		//get a bufferstrategy with two buffers for this canvas for accelerated buffered graphic processing 
		createBufferStrategy(2);
		strategy = getBufferStrategy();
		
		this.plotSheet = plotSheet;
		
		//some debug stuff, may be interesting to turn this on on first try on other operating systems
		//System.err.println(strategy.getCapabilities().isPageFlipping()?"flipping available":"flipping unavailable");
//		System.err.println(strategy.getCapabilities().getBackBufferCapabilities().isAccelerated()?"Backbuffer accelerated":"Backbuffer not accelerated");
//		System.err.println(strategy.getCapabilities().getFrontBufferCapabilities().isAccelerated()?"Frontbuffer accelerated":"Frontbuffer not accelerated");
//		FlipContents fC = strategy.getCapabilities().getFlipContents();
	}

	public JFrame getContainer() {
		return container;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		//logic loop
		int cnt = 0;
		while(plotRunning){
			//use the canvas bound as drawing bounds
			Rectangle field = this.getBounds();
			
			
			if(this.hasScrolled) {
				PointerInfo info = MouseInfo.getPointerInfo();

				int x = info.getLocation().x - this.getLocationOnScreen().x;
				int y = info.getLocation().y - this.getLocationOnScreen().y;
				double[] coordinates = plotSheet.toCoordinatePoint(x, y, field);
				
				if(this.unitsToScroll != 0) {
					double modifier = 0.1*this.unitsToScroll;
					if(this.unitsToScroll < 0){
						
						double xSize = (plotSheet.getxRange()[1]-plotSheet.getxRange()[0]) + modifier*(plotSheet.getxRange()[1]-plotSheet.getxRange()[0]);
						double ySize = (plotSheet.getyRange()[1]-plotSheet.getyRange()[0]) + modifier*(plotSheet.getyRange()[1]-plotSheet.getyRange()[0]);
						
						double[] xRange = {coordinates[0] - xSize/2, coordinates[0] + xSize/2};
						double[] yRange = {coordinates[1] - ySize/2, coordinates[1] + ySize/2};
						plotSheet.setxRange(xRange);
						plotSheet.setyRange(yRange);
					}else {
						double xSizeIncrease = 0.5*(modifier*(plotSheet.getxRange()[1]-plotSheet.getxRange()[0]));
						double ySizeIncrease = 0.5*(modifier*(plotSheet.getyRange()[1]-plotSheet.getyRange()[0]));
						double[] xRange = {plotSheet.getxRange()[0]-xSizeIncrease, plotSheet.getxRange()[1]+xSizeIncrease};
						double[] yRange = {plotSheet.getyRange()[0]-ySizeIncrease, plotSheet.getyRange()[1]+ySizeIncrease};
						plotSheet.setxRange(xRange);
						plotSheet.setyRange(yRange);
					}
					resized=true;
				}
			}

			//well, this really should not happen, if it happens something with the canvas itself is wrong
			if(field == null) {
				System.err.println("This should not happen!");
				System.exit(-1);
			}
			
			boolean reconstruct = false;
			//if there is not an buffered image or the window was resized, than a new image must be made
			//resize event is not always monitored correctly so watch width and height manually
			if(bimage == null || resized || field.width != this.width || field.height != this.height){
				bimage = new BufferedImage(field.width, field.height, BufferedImage.TYPE_INT_ARGB);
				this.width =  field.width;
				this.height = field.height;
				reconstruct = true;
			}
			
			//the graphic object of the back buffer
			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			//white out existing image, TODO: change this to only whiteout place with x,y coordinates to save redraw of
			//whole plot, even when using bufferd image of it
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			
			//use black as standard color
			g.setColor(Color.black);
			
			//graphic object of out bufferd plotimage, redrawn only after resize event (and for the first time)
			Graphics2D g2d = bimage.createGraphics();
			
			//the plot should be as big as the canvas for practical reasons
			
			
			//render plot anew if window is resized (otherwise the old image will be used for refresh)
			if (reconstruct) {
				g2d.setClip(field);
				g2d.setColor(Color.WHITE);
				g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
				//timeless simple black
				g2d.setColor(Color.BLACK);
				plotSheet.paint(g2d);
			}
			
			//draw the bufferd image in our backbuffer
			g.drawImage(bimage, null, 0, 0);
			
			//it is not neccessary to draw on plot image, dispose and free memory of graphic object for it
			g2d.dispose();
			
			//to get informations of mouse coursor position we need this
			//unfortunately this only gives us screen coordinates not the coordinates on the canvas
			PointerInfo info = MouseInfo.getPointerInfo();
			
			//therefore we use the positioning info of our canvas to calculate mouse positions over
			//the canvas
			int x = info.getLocation().x - this.getLocationOnScreen().x;
			int y = info.getLocation().y - this.getLocationOnScreen().y;
			
			//and because no one wants to know pixel coordinates we transfer them to plotSheet coordinates
			double[] coordinates = plotSheet.toCoordinatePoint(x, y, field);
			
			//and for fancyness we only draw them on the canvas if the mouse is over it
			if(x >= 0 && x <= field.width && y>=0 && y <= field.height){
				String xText = (Math.abs(plotSheet.getxRange()[1] - plotSheet.getxRange()[0]) < 1e-2 || Math.abs(plotSheet.getxRange()[1] - plotSheet.getxRange()[0]) > 1e3)? dfExponent.format(coordinates[0]): dfNormal.format(coordinates[0]);
				String yText = (Math.abs(plotSheet.getyRange()[1] - plotSheet.getyRange()[0]) < 1e-2 || Math.abs(plotSheet.getyRange()[1] - plotSheet.getyRange()[0]) > 1e3)? dfExponent.format(coordinates[1]): dfNormal.format(coordinates[1]);
				String drawString = "x: " + xText + " y: " + yText + " ScU: " + this.unitsToScroll;
				//FontMetrics fm = g.getFontMetrics();
				g.drawString(drawString, 11, 11);
			}
			
			//save Button:
			
			int saveXPos = field.width-30;
			int saveYPos = 30;
			int saveRadius = 20;
			Color oldColor = g.getColor();
			
			
			
			g.setColor(Color.gray);
			//is mouse in radius of save button zone:
			if(Math.sqrt(Math.pow(x-saveXPos, 2) + Math.pow(y-saveYPos,2)) <=saveRadius  ) {					
				Color thisColor = Color.blue.brighter().brighter();
				thisColor = new Color(thisColor.getRed(), thisColor.getGreen(),thisColor.getBlue(),127);
				g.setColor(thisColor);
				g.fillRoundRect(saveXPos-12, saveYPos-12, 24, 24, 3, 3);
				
				g.setColor(Color.white);
				g.fillRect(saveXPos-7, saveYPos-12, 14, 12);
				g.setColor(Color.gray);
				
				
				
				long thisTimestamp = System.nanoTime();
				if(!wasCursorOnSaveButton)
					this.timestamp = thisTimestamp;
				
				int radAnimationStart = (int)((thisTimestamp - this.timestamp)/10000000);
				for(int i = 0; i< 12; i++)
					g.drawArc(saveXPos-saveRadius, saveYPos-saveRadius, saveRadius*2, saveRadius*2, -radAnimationStart+i*30, 15);
				wasCursorOnSaveButton = true;
				
				if(this.hasClicked){
					JFileChooser fc = new JFileChooser();
					int result = fc.showSaveDialog(this.container);
					
					if(JFileChooser.APPROVE_OPTION == result) {
						try {
						    // retrieve image
						    ImageIO.write(bimage, "png", fc.getSelectedFile());
						} catch (IOException e) {
						    e.printStackTrace();
						}
					}
				}
			} else
				wasCursorOnSaveButton = false;
			
			g.setColor(oldColor);
			
			//disc symbol outline:
			g.drawRoundRect(saveXPos-12, saveYPos-12, 24, 24, 3, 3);
			g.drawRect(saveXPos-7, saveYPos-12, 14, 12);
			
			
			//now we are finished with drawing on the backbuffer so dispose of the graphic object
			g.dispose();
			
			//flip/blitt buffers
//			FlipContents fC = strategy.getCapabilities().getFlipContents();
//			System.out.println(fC);
			strategy.show();
			
			//reset resize information
			this.hasClicked = false;
			resized = false;
			this.hasScrolled = false;
			this.unitsToScroll = 0; 
			//sleep some time cause no one needs 100fps plots with 100% CPU-usage 
			try { Thread.sleep(50); } catch (Exception e) {}
			cnt++;
		}
		container.setVisible(false);
		//container.dispose();
	}

	public boolean isPlotRunning() {
		return plotRunning;
	}

	public void setPlotRunning(boolean plotRunning) {
		this.plotRunning = plotRunning;
	}
	
}
