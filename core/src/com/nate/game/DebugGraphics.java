package com.nate.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.viewport.Viewport;

import static com.nate.game.Arknoid1.SCENE_WIDTH; // for convenience
import static com.nate.game.Arknoid1.SCENE_HEIGHT;

/** Renders debugging graphics on screen
 *  the single static method drawDebugGraphics is meant to be called from the render() method
 *  of Arknoid1, but do not call it from inside batch.begin() and batch.end()
 *  
 *  it receives the OrthographciCamera from Arknoid1
 *  each debug graphic can be turned on or off initially by the boolean arguments
 * 
 *  @author Nathan Merris but I did model it on the libgdx ShapeRenderer.java sample
 * 
 **/

 public class DebugGraphics {


	private Logger logger; // use the logger from Arknoid1 main program
	private ShapeRenderer shapeRenderer; // used to render useful debugging graphics on the screen, this only lives here
	//private OrthographicCamera camera; // use the camera from Arknoid1 main program (do I even need this if I'm using the viewport instead?)
	private Viewport viewport; // use the viewport from Arknoid1 main program
	private boolean drawGrid;
	private boolean instructionsDisplayed; // used so that the instructions in console are only displayed the first time render() is called in Arknoid1
	
	// constructor
	public DebugGraphics (Viewport viewport, Logger logger, boolean drawGrid){
		this.viewport = viewport;
		this.logger = logger;
		this.drawGrid = drawGrid;
		shapeRenderer = new ShapeRenderer();
		instructionsDisplayed = false;
	}
	
	// this class needs it's own dispose() method because the main Arknoid1 class does not use the same shapeRenderer (or maybe not at all)
	public void dispose(){
		shapeRenderer.dispose();
		logger.info("DebugGraphics dispose() method exiting..");
	}
	
	
	
	public void drawDebugGraphics() {
		
		if(!instructionsDisplayed){
			//logger.info("Press 'g' to toggle onscreen grid");
			instructionsDisplayed = true;
		}
		
		shapeRenderer.setProjectionMatrix(viewport.getCamera().combined); // viewport comes in from Arknoid1.java
		
		// Draw grid
		if (drawGrid) {
			shapeRenderer.begin(ShapeType.Line);

			shapeRenderer.setColor(Color.RED);
			shapeRenderer.line(-SCENE_WIDTH, 0.0f, SCENE_WIDTH, 0.0f);
			shapeRenderer.line(0.0f, -SCENE_HEIGHT, 0.0f, SCENE_HEIGHT);
			
			shapeRenderer.setColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, Color.WHITE.a);
			
			for (int i = (int) (-SCENE_HEIGHT * 0.5); i <= (int) (SCENE_HEIGHT * 0.5); i += 50) {
				if (i == 0)
					continue;
				
				//shapeRenderer.line(-SCENE_WIDTH, i, SCENE_WIDTH, i); // draw horizontal lines
			}
			
			for (int i = (int) (-SCENE_WIDTH * 0.5); i <= (int) (SCENE_WIDTH * 0.5); i += 50) {
				if (i == 0)
					continue;
				
				//shapeRenderer.line(i, -SCENE_HEIGHT, i, SCENE_HEIGHT); // draw vertical lines
			}
			
			shapeRenderer.end();
		}

		

		
	/*	if (drawRectangles) {
			shapeRenderer.begin(ShapeType.Filled);
			shapeRenderer.setColor(Color.GREEN);
						
			shapeRenderer.rect(7.2f, 2.4f, 3.3f, 2.8f, 0.0f, 0.0f, 45.0f);
			shapeRenderer.rect(-8.4f, 3.8f, 6.1f, 2.3f, 0.0f, 0.0f, 75.0f);
			shapeRenderer.rect(-4.2f, -3.4f, 3.3f, 2.8f, 0.0f, 0.0f, 25.0f);
			shapeRenderer.rect(3.2f, -6.4f, 3.9f, 1.8f, 0.0f, 0.0f, 60.0f);
			
			shapeRenderer.end();
		}
	*/
		
	/*	if (drawPoints) {
			shapeRenderer.begin(ShapeType.Line);
			shapeRenderer.setColor(Color.MAGENTA);
			
			shapeRenderer.x(-5.0f, 0.0f, 0.25f);
			shapeRenderer.x(3.0f, 8.0f, 0.25f);
			shapeRenderer.x(-7.0f, 2.0f, 0.25f);
			shapeRenderer.x(7.0f, -3.0f, 0.25f);
			
			shapeRenderer.end();
		}
	*/

	} // end drawDebugGraphics
} // end class DebugGraphics
