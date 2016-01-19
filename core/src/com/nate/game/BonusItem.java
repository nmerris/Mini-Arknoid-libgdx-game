/** A BonusItem object represents a bonus item that has fallen from a brick for you to 'catch' with vaus
 *  
 */
package com.nate.game;

import static com.nate.game.Arknoid1.WORLD_TO_SCREEN;
import static com.nate.game.CreateBody.createBody;
import static com.nate.game.Arknoid1.batch;
import static com.nate.game.Arknoid1.logger;

import static com.nate.game.Projectile.ProjectileTypes; // holds all the different projectileType types that can be fired from vaus

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.nate.game.BonusItem.ItemTypes;
import com.nate.game.ContactProcessor.ContactCategories;

/**
 * @author natenator
 *
 */
public class BonusItem implements GameObject{

	public enum ItemTypes{ // items can be hidden inside bricks, some good, some bad
		NO_ITEM, VAUS_WIDE, VAUS_SMALL, LASER, BOMB, BALLS_2, BALLS_3, BALL_BIG, GRAVITY, EXTRA_LIFE
	}
	
	boolean isAlive; // true when on screen
	private float speed; // the rate of change of the bonus item as it drops from it's brick
	private float startingX, startingY; // the x,y coords where the bonus item should start falling
	private ItemTypes bonusItem; // represents the different types of bonus items that can be 'caught' by vaus
	private Body body;
	private Texture tex;
	private float width, height;
	
	// constructor..
	// pass in the item type and the x,y coords where the ball hit the brick to reveal the bonus item
	public BonusItem (ItemTypes type, float x, float y){
		this.bonusItem = type;
		this.startingX = x;
		this.startingY = y;
		
		speed = 1.5f;
		isAlive = false; 
		
		switch(bonusItem){
			case LASER:
				this.tex = new Texture(Gdx.files.internal("item_laser.png")); // load the texture
				this.width = tex.getWidth() * WORLD_TO_SCREEN;
				this.height = tex.getHeight() * WORLD_TO_SCREEN;
				break;
			case BOMB:
				this.tex = new Texture(Gdx.files.internal("item_bomb.png")); // load the texture
				this.width = tex.getWidth() * WORLD_TO_SCREEN;
				this.height = tex.getHeight() * WORLD_TO_SCREEN;
				break;
			case EXTRA_LIFE:
				this.tex = new Texture(Gdx.files.internal("item_extralife.png")); // load the texture
				this.width = tex.getWidth() * WORLD_TO_SCREEN;
				this.height = tex.getHeight() * WORLD_TO_SCREEN;
				break;
			case NO_ITEM:
				break;
			default:
				break;
		}
		
		if(bonusItem != ItemTypes.NO_ITEM){
			// we do not want the bonus item to collide with the ball, bricks, or projectiles
			// to set the correct mask, bitwise AND the masks of the other objects you do not want to collide with..
			short mask = (short)(ContactCategories.BRICK.getMask() & ContactCategories.BALL.getMask() & ContactCategories.PROJECTILE.getMask());
				
			
			this.body = createBody(BodyType.DynamicBody,  			// body type
				   	 false, ContactCategories.BONUS_ITEM, mask, 	// is body a sensor?, sensor category, sensor mask
				   	 0.1f, new GameBody<BonusItem>(this),			// gravity scale, userData
				   	 x, y,											// initial x and y positions
					 1.0f, 0, 0, 									// density, restitution, friction
					 width * 0.5f, height * 0.5f); 					// halfWidth, halfHeight
			
			body.setActive(false); // start inactive
		}

	
	
	}
	
	public ItemTypes getBonusItemType(){
		return bonusItem;
	}
	
	public BonusItem getBonusItem(){
		return this;
	}
	
	public boolean isProjectile(){
		if(bonusItem == ItemTypes.LASER || bonusItem == ItemTypes.BOMB){
			return true;
		}
		else return false;
	}
	
	// returns a ProjectileTypes value depending on what bonusItem is present, if any..
	public ProjectileTypes getProjectileType(){
		switch(bonusItem){
			case LASER:
				return ProjectileTypes.LASER;
			case BOMB:
				return ProjectileTypes.BOMB;
			default:
				return ProjectileTypes.NO_PROJECTILE;
		}
	}
	
	
	

	// as opposed to the other draw() methods, this one is only called from Bricks.java
	// because each brick may or may not have a bonus item.. it makes intuitive sense, and it eliminates
	// the need for a separate class and an associated array that draws each bonus item.. there is already
	// an array in Bricks.java that loops through all the bricks, why not use it to call the BonusItem draw method
	@Override
	public void draw() {
		
		// I wanted to put the setActive(boolean) methods in ContactProcessor class, but libgdx gets angry when you try to modify bodies in
		// certain ways (like making them inactive) while they are in the process of detecting and reacting to collisions
		if(bonusItem != ItemTypes.NO_ITEM){ // check if this brick has a bonus item
			if(isAlive){ // the bonus item is alive, it 'dies' when you catch it with vaus, or it falls past the bottom of the screen
				body.setActive(true); // set the box2d physics engine body to active
				
				batch.draw(tex, // texture
					   	   body.getPosition().x - width * 0.5f, // bottom left x
						   body.getPosition().y - height * 0.5f, // bottom left y
						   body.getLocalCenter().x, body.getLocalCenter().y, // originX, originY (for translations)
						   width, height, // width, height
						   1f, 1f, // scaleX, scaleY
						   0, // rotate, 0 means do not rotate
						   0, 0, // srcX, srcY (?)
						   tex.getWidth(), tex.getHeight(), // srcWidth, srcHeight
						   false, false // flipX, flipY
						   );
			} else {
				body.setActive(false); // remove this inactive bonus item from the box2d simulation
			}
		}
		
	}
	



	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}
	
	// TODO: need to call this dispose() method after it's isAlive field is set to false after it has already dropped..
	public void dispose() {
		tex.dispose();

	}

}
