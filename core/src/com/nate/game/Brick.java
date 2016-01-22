/**
 * 
 */
package com.nate.game;

import static com.nate.game.Arknoid1.SCENE_HEIGHT;
import static com.nate.game.Arknoid1.SCREEN_TO_WORLD;
import static com.nate.game.CreateBody.createBody;
import static com.nate.game.BonusItem.ItemTypes;
import static com.nate.game.Arknoid1.batch;
import static com.nate.game.Arknoid1.logger;
import static com.nate.game.ContactProcessor.ContactCategories;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.nate.game.BonusItem;

/**
 * @author natenator
 * this class constructor is called from Bricks.java when the brick array is loaded for each level
 * each Brick object has it's own box2d body to go with it, which is created here in constructor
 */
public class Brick { // each Brick object represents a single brick and has a BonusItem (which may be NO_ITEM)

	
	int id;						// each brick has it's own unique id per level
	short toughness;			// how many hits does it take to destroy brick
	short pointValue;			// how many points is this brick worth
	boolean isIndestructible;	// set to true if the brick can not be destroyed
	boolean isAlive; 			// set to false when brick has been destroyed
	//ItemTypes bonusItemType;				// what item type, if any, is hidden inside the brick
	BonusItem bonusItem;		// the actual BonusItem object
	Body brickBody;				// each brick is it's own individual box2d body including width, height, and position in box2d world
	Texture brickTex;			// each brick has it's own individual texture to draw on screen
	float width, height;		// each brick has it's own width and height in box2dworld units, always calculated from texture size
	
	
	
	// constructor is called from Bricks.java when level info is read in from a file..
	public Brick(int id, short p, short t, boolean ind, boolean a, String textureFileName, ItemTypes bonusItemType, float x, float y){
		
		bonusItem = new BonusItem(bonusItemType, x, y);
		
		this.id = id;
		this.pointValue = p;
		this.toughness = t;
		this.isIndestructible = ind;
		this.isAlive = a;
			
		this.brickTex = new Texture(Gdx.files.internal(textureFileName)); // load the texture
		this.width = brickTex.getWidth() * SCREEN_TO_WORLD;
		this.height = brickTex.getHeight() * SCREEN_TO_WORLD;
		
		this.brickBody = createBody(BodyType.StaticBody,  			// body type
			   	 false, ContactCategories.BRICK, (short)~0x0000, 	// is body a sensor?, sensor group, sensor mask (collide with everything)
			   	 0, new GameBody<Brick>(this),							// gravity scale, userData
			   	 x, y,													// initial x and y positions
				 0, 0, 0, 												// density, restitution, friction
				 width * 0.5f, height * 0.5f); 							// halfWidth, halfHeight based on brick texture dimensions
		
	}
	
	public void draw(){
		batch.draw(brickTex, // texture
				   brickBody.getPosition().x - width * 0.5f, // bottom left x
				   brickBody.getPosition().y - height * 0.5f, // bottom left y
				   brickBody.getLocalCenter().x, brickBody.getLocalCenter().y, // originX, originY (for translations)
				   width, height, // width, height
				   1f, 1f, // scaleX, scaleY
				   0, // rotate, 0 means do not rotate
				   0, 0, // srcX, srcY (?)
				   brickTex.getWidth(), brickTex.getHeight(), // srcWidth, srcHeight (in real world dimensions, not in scaled down box2d world dimensions
				   false, false // flipX, flipY
				   );
	}

}
