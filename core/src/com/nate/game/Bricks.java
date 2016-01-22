/**
 * 
 */
package com.nate.game;

import static com.nate.game.Arknoid1.SCENE_HEIGHT;
import static com.nate.game.Arknoid1.SCENE_WIDTH;
import static com.nate.game.Arknoid1.SCREEN_TO_WORLD;
import static com.nate.game.Arknoid1.logger;

import com.badlogic.gdx.utils.Array;
import com.nate.game.BonusItem.ItemTypes;

/**
 * @author Nathan Merris
 * The Bricks class contains functionality to draw ALL bricks as needed.
 * Each individual brick is it's own Brick object.
 * All bricks for one level are passed in from LevelLoader.loadBricks.
 * 
 */
public class Bricks implements GameObject { // the Bricks class represents all bricks on a single level, each individual brick is a Brick object
	
	private int numBricks;		// the total number of all bricks, including indestructible bricks
	int bricksRemaining; // when this reaches zero, you have destroyed all the bricks that can be destroyed
	private Array<Brick> bricks; // holds all bricks, each is an individual Brick object
	
	// constructor..
	public Bricks (Array<Brick> bricks) {
		
		this.bricks = bricks;
		numBricks = bricks.size;
		bricksRemaining = numBricks; // this would not always equal numBricks b/c some may be indestructible (need to implement this)
		
		logger.info("inside Bricks constructor, Bricks.numBricks = " + numBricks);
		logger.info("inside Bricks constructor, Bricks.bricksRemaining = " + bricksRemaining);
		logger.info("Bricks constructor exiting..");
	}
	

	public void update(){
		
	}
	
	
	public void draw(){ 
		// isAlive for both the brick and it's bonus item are changed in ContactProcessor.java as needed
		// each brick has it's own bonusItem, which may by NO_ITEM
		// each game cycle, the bricks array is looped through once
		// first we check to see if the brick is still alive, if it is just draw it
		// if the brick has been destroyed, then we check to see if it's bonus item is still alive
		// when both the brick and it's bonus item are 'not alive', that brick is removed from the loop/game/simulation
		
		for(Brick b : bricks){
			if(b.isAlive){ // this particular brick has not been destroyed yet
				b.draw(); // draw the brick
			}else{ // this particular brick has been destroyed		
				if(b.bonusItem.getBonusItemType() == ItemTypes.NO_ITEM){ bricks.removeValue(b, true); }
				else if(!b.bonusItem.isAlive){ bricks.removeValue(b, true); } // brick is not alive AND it's bonus item is not alive, so remove it from array
				b.bonusItem.draw(); // the draw method for each bonus item is only called here, because a bonus item is tied to a brick, nothing drawn if item is NO_ITEM
				b.brickBody.setActive(false); // remove this destroyed brick from the box2d simulation
			}
		}
	}
	

	
	@Override
	public void dispose(){
		
	}


	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}


	
	
}
