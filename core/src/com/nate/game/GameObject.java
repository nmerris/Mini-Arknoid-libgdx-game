/**
 * 
 */
package com.nate.game;

/**
 *  All game objects must be controlled from the Arknoid1.java main program.
 *  They must each implement a number of methods that allow for basic control,
 *  such as pausing, stopping, drawing, and updating.
 *  @author natenator
 *	@version 1.0
 */
public interface GameObject {
	
	/**
	 * 
	 *
	 */
	void draw();
	
	/**
	 * 
	 *
	 */
	void update();
	
	/**
	 * @param b set to true to stop all processing
	 *
	 */
	void stop();
	
	void start();
	
	void dispose();
	
	
	
}
