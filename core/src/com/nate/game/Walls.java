/**
 * 
 */
package com.nate.game;


import static com.nate.game.Arknoid1.SCENE_HEIGHT;
import static com.nate.game.Arknoid1.SCENE_WIDTH;
import static com.nate.game.Arknoid1.WORLD_TO_SCREEN;
import static com.nate.game.CreateBody.createBody;
import static com.nate.game.Arknoid1.batch;
import static com.nate.game.Arknoid1.logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.utils.Logger;



/**
 * @author Nathan Merris
 *
 */
public class Walls implements GameObject{
	//private Logger logger; // use the logger from Arknoid1 main program
	//private SpriteBatch batch; // use the same batch everywhere
	
	public static Body leftWallBody, rightWallBody, ceilingBody, floorBody; // each wall and the ceiling are fixed box2d bodies
	
	// the textures dimensions are scaled down to the size needed by box2d physics engine..
	// used in the draw method in this class, these are just to keep things organized and readable
	private float box2dCeilingWidth, box2dCeilingHeight, box2dWallHeight;
	public static float box2dWallWidth; // need this to compute range of motion of vaus in Vaus.java, that's why it's public static
	
	private Texture ceilingTex, wallTex; // the left and right walls are the same texture for now
	
	// constructor..
	public Walls (String leftWallTextureFileName, String rightWallTextureFileName, String ceilingTextureFileName) // there is no floor in normal gameplay
	{
		// load the textures..
		ceilingTex = new Texture(Gdx.files.internal(ceilingTextureFileName)); // need to read from a file, string of filename should be passed in from Arknoid1 at start
		wallTex = new Texture(Gdx.files.internal(leftWallTextureFileName)); // same texture for both walls for now
		
		// the width of the textures scaled down to use with box2d..
		box2dCeilingWidth = ceilingTex.getWidth() * WORLD_TO_SCREEN; 	// ceiling and floor are the same dimensions
		box2dCeilingHeight = ceilingTex.getHeight() * WORLD_TO_SCREEN;
		box2dWallWidth = wallTex.getWidth() * WORLD_TO_SCREEN; 			// left and right walls are the same dimensions
		box2dWallHeight = wallTex.getHeight() * WORLD_TO_SCREEN;
		
		// create the bodies..
		leftWallBody = createBody(BodyType.StaticBody,  			// body type
			   	 false, ContactProcessor.ContactCategories.WALLS, (short)~0x0000, // is body a sensor?, sensor group, collision mask (collide with everything for walls)
			   	 0, new GameBody<Walls>(this), 						// gravity scale, userData
			   	 box2dWallWidth * 0.5f, SCENE_HEIGHT * 0.5f, 		// initial x and y positions
				 0, 0, 0, 											// density, restitution, friction
				 box2dWallWidth * 0.5f, box2dWallHeight * 0.5f); 	// halfWidth, halfHeight based on vaus texture dimensions
	
		rightWallBody = createBody(BodyType.StaticBody,  		// body type
			   	 false, ContactProcessor.ContactCategories.WALLS, (short)~0x0000, // is body a sensor?, sensor group, collision mask (collide with everything for walls)
			   	 0, new GameBody<Walls>(this),  					// gravity scale, userData
			   	 SCENE_WIDTH - box2dWallWidth * 0.5f, SCENE_HEIGHT * 0.5f, // initial x and y positions, not used because the flippers are attached to vaus and move with vaus
				 0, 0, 0, 											// density, restitution, friction
				 box2dWallWidth * 0.5f, box2dWallHeight * 0.5f);	// halfWidth, halfHeight based on vaus texture dimensions
		
		ceilingBody = createBody(BodyType.StaticBody,  			// body type
			   	 false, ContactProcessor.ContactCategories.WALLS, (short)~0x0000, // is body a sensor?, sensor group, collision mask (collide with everything for walls)
			   	 0, new GameBody<Walls>(this),  					// gravity scale, userData
			   	 SCENE_WIDTH * 0.5f, SCENE_HEIGHT - box2dCeilingHeight * 0.5f, // initial x and y positions, not used because the flippers are attached to vaus and move with vaus
				 0, 0, 0, 											// density, restitution, friction
				 box2dCeilingWidth * 0.5f, box2dCeilingHeight * 0.5f);	// halfWidth, halfHeight based on vaus texture dimensions
	
		floorBody = createBody(BodyType.StaticBody,  			// body type
			   	 false, ContactProcessor.ContactCategories.WALLS, (short)~0x0000, // is body a sensor?, sensor group, collision mask (collide with everything for walls)
			   	 0, new GameBody<Walls>(this), 						// gravity scale, userData
			   	 SCENE_WIDTH * 0.5f, box2dCeilingHeight * 0.5f, 	// initial x and y positions, not used because the flippers are attached to vaus and move with vaus
				 0, 0, 0, 											// density, restitution, friction
				 box2dCeilingWidth * 0.5f, box2dCeilingHeight * 0.5f);	// halfWidth, halfHeight based on vaus texture dimensions
	
				
		
		logger.info("exiting Walls constructor..");
	} // end constructor
	
	
	// draw should be called from Arknoid1 main class from inside the batch rendering block
	// the same batch is used everywhere, openGL likes that and it is much faster
	public void draw(){
		// apparently you need to use the 16 parameter draw method to draw things that use box2d because..
		// box2d will not work with large numbers (by large I mean 1280, 720..) it wants numbers closer to
		// 12.8 or 7.2, otherwise it hits a floating point arithmetical limit and your max ballSpeed is way too slow
		// but the textures to draw on the screen are in "normal" screen size dimensions (like 1280 x 40 pixels)
		// so you have to basically do EVERYTHING at the smaller scale, and use the following draw method
		// which takes in the texture (NOT a textureRegion, there is no draw method like this that takes in a textureRegion)
		// but uses the positioning values from the box2d physics engine, it must understand how to draw the
		// "normal" sized texture using the smaller scale size box2d physics coordinates, what a PITA, there is probably a 
		// better way, but I'm going with this for now..
		
		batch.draw(ceilingTex, // texture
				   ceilingBody.getPosition().x - SCENE_WIDTH * 0.5f, // bottom left x
				   ceilingBody.getPosition().y - box2dCeilingHeight * 0.5f, // bottom left y
				   box2dCeilingWidth * 0.5f, box2dCeilingHeight * 0.5f, // originX, originY (for translations)
				   box2dCeilingWidth, box2dCeilingHeight, // width, height
				   1f, 1f, // scaleX, scaleY
				   0, // rotate, 0 means do not rotate
				   0, 0, // srcX, srcY (?)
				   ceilingTex.getWidth(), ceilingTex.getHeight(), // srcWidth, srcHeight
				   false, false // flipX, flipY
				   );
		
		// the floor texture will probably not be used in the final game because you typically want a pit for the ball to fall in..
		batch.draw(ceilingTex, // using ceiling texture for floor for now
				   floorBody.getPosition().x - SCENE_WIDTH * 0.5f, // bottom left x
				   floorBody.getPosition().y - box2dCeilingHeight * 0.5f, // bottom left y
				   box2dCeilingWidth * 0.5f, box2dCeilingHeight * 0.5f, // originX, originY (for translations)
				   box2dCeilingWidth, box2dCeilingHeight, // width, height
				   1f, 1f, // scaleX, scaleY
				   0, // rotate, 0 means do not rotate
				   0, 0, // srcX, srcY (?)
				   ceilingTex.getWidth(), ceilingTex.getHeight(), // srcWidth, srcHeight
				   false, false // flipX, flipY
				   );
		
		
		batch.draw(wallTex, // texture
				   leftWallBody.getPosition().x - box2dWallWidth * 0.5f, // bottom left x
				   leftWallBody.getPosition().y - box2dWallHeight * 0.5f, // bottom left y
				   box2dWallWidth * 0.5f, box2dWallHeight * 0.5F, // originX, originY (for translations)
				   box2dWallWidth, box2dWallHeight, // width, height
				   1f, 1f, // scaleX, scaleY
				   0, // rotate, 0 means do not rotate
				   0, 0, // srcX, srcY (?)
				   wallTex.getWidth(), wallTex.getHeight(), // srcWidth, srcHeight
				   false, false // flipX, flipY
				   );
		
		batch.draw(wallTex, // texture
				   rightWallBody.getPosition().x - box2dWallWidth * 0.5f, // bottom left x
				   rightWallBody.getPosition().y - box2dWallHeight * 0.5f, // bottom left y
				   box2dWallWidth * 0.5f, box2dWallHeight * 0.5F, // originX, originY (for translations)
				   box2dWallWidth, box2dWallHeight, // width, height
				   1f, 1f, // scaleX, scaleY
				   0, // rotate, 0 means do not rotate
				   0, 0, // srcX, srcY (?)
				   wallTex.getWidth(), wallTex.getHeight(), // srcWidth, srcHeight
				   false, false // flipX, flipY
				   );
		
		} // end draw()
	
	// isFloor returns true if the Body passed to it is floorBody, to determine when you DIE!!
	public boolean isFloor(Body b){
		if (b.equals(floorBody)) return true;
		else return false;
	}
	
	public boolean isCeiling(Body b){
		if (b.equals(ceilingBody)) return true;
		else return false;
	}
	

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
	
	public void dispose(){
		ceilingTex.dispose();
		wallTex.dispose();
	

		logger.info("Walls dispose() method exiting..");
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}


	
	
}
