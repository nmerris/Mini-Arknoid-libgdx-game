/**
 * 
 */
package com.nate.game;

import static com.nate.game.GameBody.GameBodies;
import static com.nate.game.Ball.ballSpeed;
import static com.nate.game.Ball.ballMaxVx;
import static com.nate.game.Ball.ballMaxVy;
import static com.nate.game.Ball.ballMinVx;
import static com.nate.game.Ball.ballMinVy;
import static com.nate.game.Ball.ballVyAtMaxVx;
import static com.nate.game.Ball.ballVxAtMaxVy;
import static com.nate.game.Ball.ballVyAtMinVx;
import static com.nate.game.Ball.ballVxAtMinVy;

import static com.nate.game.BonusItem.ItemTypes;
import static com.nate.game.SoundProcessor.SoundEffects;

import static com.nate.game.Vaus.box2dVausTotalWidth;
import static com.nate.game.Arknoid1.logger;
import static com.nate.game.Arknoid1.box2dWorld;
import static com.nate.game.Arknoid1.onScreenDisplay;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;
import com.nate.game.Projectile.ProjectileTypes; // why did I not make this static import?  TODO: go through everything and make sure all my imports are consistent in all classes

/**
 * @author natenator
 * objects that extend the libgdx ContactListener interface detect when box2d world bodies collide
 * note that this is different than a user input detector
 * user input processing is implemented in Vaus.java because vaus is the only thing that the user can move
 * however many different bodies from differing classes all can interact, thus a separate class here
 */
public class ContactProcessor implements ContactListener{
	
	private Array<Brick> bombBlastBrickHitList; // holds a list of all the bricks that should be dealt damage when a bomb projectile explodes
	
	
	//private Logger logger; // use the logger from Arknoid1 main program
	//private OnScreenDisplay onScreenDisplay; // we need to access the onScreenDisplay object to update score and lives
	private Bricks bricks; // we need this to update the number of bricks remaining after they have been destroyed
	
	private SoundProcessor soundEffects; // various sound effects are played at various as game is played, call the play() method on it to play a sound effect

	// each body has a custom object tied to it via the userData field of each body
	// the object also contains everything needed to identify what is colliding, how many points, how much damage, etc
	private GameBody<?> gameBodyA, gameBodyB; 
	
	// every collision has two box2d bodies, but the order is arbitrary, call them bodyA and bodyB
	// a ContactPair object contains a ContactPairBelligerents enum value, which represents the order of the two bodies
	// so BALL_BRICK means that bodyA is the ball and bodyB is a brick
	private ContactPair contactPair;
	private Body bodyA, bodyB;

	private float preContactVx, preContactVy; 	// used to temporarily store the incoming velocities of a body before a collision occurs
	private float initialVx, initialVy;			// used to temporarily store the initial velocities of a body after a collision occurs
	private float newVx, newVy;			 		// used to temporarily store the new x and y velocity vectors of a body, to override box2d's values
	private float initialPosBall, initialPosVaus; // used to temporarily store the positions of ball and vaus, could be either X or Y coords

	private float alterBallBounceThreshold; // but it is accurately descriptive	
	private float halfVausWidth;
	//private Filter filterBall; // used to mask collisions with ball as needed
	
	private enum ContactPairBelligerents {
		BALL_BRICK, BALL_FLIPPER, BALL_VAUS, BALL_VAUS_FLAT_TOP, BALL_LEFT_WALL, BALL_RIGHT_WALL, BALL_CEILING, BALL_FLOOR, NO_CONTACT,
		PROJECTILE_BRICK, PROJECTILE_CEILING, BONUS_ITEM_VAUS, BONUS_ITEM_FLOOR;
		
	}
	
	// ContactCategories enum contains all the different collision masking categories
	// box2d uses unsigned bit strings (must be a power of 2) to keep track of what should collide with what on a category basis
	// here's how it works: when two bodies collide, one body's mask is bitwise ANDed with the other body's category
	// if the result is all zero's, NO collision occurs, otherwise the bodies collide
	// the category for a body will likely never change, but the mask can be changed as needed throughout the game
	// this enum keeps all this in one spot and improves readability of the code..
	public static enum ContactCategories{
		VAUS((short)			0b0000000000000001),
		FLIPPERS((short)		0b0000000000000010),
		VAUS_FLAT_TOP((short)	0b0000000000000100),
		BALL((short)			0b0000000000001000),
		BRICK((short)			0b0000000000010000),
		WALLS((short)			0b0000000000100000),
		PROJECTILE((short)		0b0000000001000000),
		BONUS_ITEM((short)		0b0000000010000000),
		SENSOR((short) 			0b0000000100000000);
		
		// each enum value has it's own unique category and mask variables..
		private short category;
		
		// enum constructor..
		ContactCategories(short c) { category = c; }
		
		public short getCategory() { return category; }
		
		// getMask is a convenience method, use it's returned short as the mask for any body 
		// that you don't want colliding with whatever enum you are calling this from
		// for example, set a body's mask to BRICK.getMask() if you want that body to NOT collide with a brick
		// for anything more complicated, you need to manually set the mask as needed
		public short getMask() { return (short)~category; }
		
	}
	
	
	// constructor..
	public ContactProcessor(Bricks b, SoundProcessor s){
		
		
		bombBlastBrickHitList = new Array<Brick>();
		
		box2dWorld.setContactListener(this);
		
		bricks = b;
		soundEffects = s;
		
		contactPair = new ContactPair();
		
		halfVausWidth = box2dVausTotalWidth * 0.5f;
		alterBallBounceThreshold = halfVausWidth * 0.3f; // ie ?% of one half the length of vaus' total width
		

		logger.info("ContactProcessor constructor exiting..");
	}
	
	// inner class.. why not?  only used within it's outer class, and keeps things organized and abstracted
	// a ContactPair object is used in the contactBegin, contactEnd, etc. methods
	// you don't know what order the bodies will be reported by the box2d physics engine,
	// so you need to do some analyzing and sorting, the getBelligerents method returns a ContactPair object
	// that contains the bodies involved, and puts them in the same every time
	// use getBelligerents to find out what collided and in what order
	// then use getters to work on the bodies and their custom objects, they will always be in the same order now
	class ContactPair {
		private ContactPairBelligerents belligerents;
		private Body bodyA, bodyB;
		
		// constructor for good measure, Java's default initializations would likely have been sufficient
		ContactPair(){
			belligerents = ContactPairBelligerents.NO_CONTACT;
			bodyA = bodyB = null;
		}
		
		// getters and setters..
		void setBelligerents(ContactPairBelligerents cpb){this.belligerents = cpb;}
		void setBodyA(Body b){this.bodyA = b;}
		void setBodyB(Body b){this.bodyB = b;}
		ContactPairBelligerents getBelligerents(){return belligerents;}
		Body getBodyA(){return bodyA;}
		Body getBodyB(){return bodyB;}
	}
	
	


	
	
	@Override
	public void beginContact(Contact contact) {
	
		contactPair = analyzeContact(contact); // to find out what two bodies collided, and which is which
		bodyA = contactPair.getBodyA();
		bodyB = contactPair.getBodyB();
		gameBodyA = (GameBody<?>) bodyA.getUserData(); // we do not know what specific bodies are colliding yet, thus <?>
		gameBodyB = (GameBody<?>) bodyB.getUserData();
		
		Brick tempBrick; // temp local variable for a Brick object
		Projectile tempProjectile;

		
		switch(contactPair.getBelligerents()){
		
			case BALL_VAUS_FLAT_TOP:
				logger.info("==========================================================");
				logger.info("inside beginContact, case BALL_VAUS_FLAT_TOP:");
				preContactVx = bodyA.getLinearVelocity().x; // store the incoming ball velocities before collision
				preContactVy = bodyA.getLinearVelocity().y;
				break;
		
			case BALL_BRICK:
				logger.info("==========================================================");
				logger.info("inside beginContact, case BALL_BRICK:");
				preContactVx = bodyA.getLinearVelocity().x; // store the incoming ball velocities before collision
				preContactVy = bodyA.getLinearVelocity().y;
				break;
			
			case BALL_FLIPPER:
				logger.info("==========================================================");
				logger.info("inside beginContact, case BALL_FLIPPER:");
				preContactVx = bodyA.getLinearVelocity().x; // store the incoming ball velocities before collision
				preContactVy = bodyA.getLinearVelocity().y;
				break;
				
			case BALL_FLOOR:
				logger.info("==========================================================");
				logger.info("inside beginContact, case BALL_FLOOR:");
				preContactVx = bodyA.getLinearVelocity().x; // store the incoming ball velocities before collision
				preContactVy = bodyA.getLinearVelocity().y;
				break;
				
			case PROJECTILE_BRICK: // a projectile might be: a laser, a bomb, or a bomb blast sensor
				
				tempBrick = (Brick) gameBodyB.getSpecificBodyObject(); // casting will always be ok because at this point, bodyB can only be a brick
				tempProjectile = (Projectile) gameBodyA.getSpecificBodyObject();
				
				
				logger.info("==========================================================");
				logger.info("inside **BEGIN**Contact, case PROJECTILE_BRICK:");
				logger.info("  and brick ID num = " + tempBrick.id);
				
				// when a bomb or it's blast sensor hits any brick, add it to bombBlastBrickHitList right now
				// if the bomb blast sensor goes past a brick (which is recorded automatically by box2d in endContact below), it will be removed from this array
				// the instant that the bomb projectile itself (as opposed to it's blast sensor) hits a brick, all bricks remaining in bombBlastBrickHitList will then be dealt damage
				// the only bricks in bombBlastBrickHitList at that time should be the ones that the blast sensor is touching, others were simply passed by on the way
				if(tempProjectile.type == ProjectileTypes.BOMB){
					if(!bombBlastBrickHitList.contains(tempBrick, true)){ bombBlastBrickHitList.add(tempBrick); }// add the brick to bombBlastBrickHitList if it's not already there (don't want duplicates, so Bricks.bricksRemaining is accurate)
				}
				
				
			case NO_CONTACT:
				break;
				
			default:
				break;
			
		}
	}
	
	@Override
	public void endContact(Contact contact) {
		
		contactPair = analyzeContact(contact); // to find out what two bodies collided, and which is which
		bodyA = contactPair.getBodyA();
		bodyB = contactPair.getBodyB();
		gameBodyA = (GameBody<?>) bodyA.getUserData(); // we do not know what specific bodies are colliding yet, thus <?>
		gameBodyB = (GameBody<?>) bodyB.getUserData();
		
		Brick tempBrick; // temp local variable for a Brick object
		Projectile tempProjectile;
		
		switch(contactPair.getBelligerents()){
		
			case BALL_VAUS_FLAT_TOP:
				logger.info("==========================================================");
				logger.info("inside beginContact, case BALL_VAUS_FLAT_TOP:");
				preContactVx = bodyA.getLinearVelocity().x; // store the incoming ball velocities before collision
				preContactVy = bodyA.getLinearVelocity().y;
				break;
		
			case BALL_BRICK:
				logger.info("==========================================================");
				logger.info("inside beginContact, case BALL_BRICK:");
				preContactVx = bodyA.getLinearVelocity().x; // store the incoming ball velocities before collision
				preContactVy = bodyA.getLinearVelocity().y;
				break;
			
			case BALL_FLIPPER:
				logger.info("==========================================================");
				logger.info("inside beginContact, case BALL_FLIPPER:");
				preContactVx = bodyA.getLinearVelocity().x; // store the incoming ball velocities before collision
				preContactVy = bodyA.getLinearVelocity().y;
				break;
				
			case BALL_FLOOR:
				logger.info("==========================================================");
				logger.info("inside beginContact, case BALL_FLOOR:");
				preContactVx = bodyA.getLinearVelocity().x; // store the incoming ball velocities before collision
				preContactVy = bodyA.getLinearVelocity().y;
				break;
				
			case PROJECTILE_BRICK: // a projectile might be: a laser, a bomb, or a bomb blast sensor
				
				tempBrick = (Brick) gameBodyB.getSpecificBodyObject(); // casting will always be ok because at this point, bodyB can only be a brick
				tempProjectile = (Projectile) gameBodyA.getSpecificBodyObject();
				
				
				logger.info("==========================================================");
				logger.info("inside **END**Contact, case PROJECTILE_BRICK:");
				logger.info("  and brick ID num = " + tempBrick.id);
				
				
				if(tempProjectile.type == ProjectileTypes.BOMB){
					bombBlastBrickHitList.removeValue(tempBrick, true); // remove this brick from bombBlastBrickHitList array because it has been passed by completely by the bomb and it's blast sensor
				}
				
			case NO_CONTACT:
				break;
				
			default:
				break;
		
		}
		
	}
	
	


	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		
		
		
		
		
		contactPair = analyzeContact(contact); // to find out what two bodies collided, and which is which
		bodyA = contactPair.getBodyA();
		bodyB = contactPair.getBodyB();
		gameBodyA = (GameBody<?>) bodyA.getUserData(); // we do not know what specific bodies are colliding yet, thus <?>
		gameBodyB = (GameBody<?>) bodyB.getUserData();
		
		Brick tempBrick; // temp local variable for a Brick object
		Vaus tempVaus; // temp local variable for a Vaus object
		Projectile tempProjectile;
		BonusItem tempBonusItem;
		
		
		switch(contactPair.getBelligerents()){ // belligerents.. unleashing my inner nerd here!
		
			case BALL_VAUS_FLAT_TOP:
				logger.info("==========================================================");
				logger.info("inside postSolve, case BALL_VAUS_FLAT_TOP:");
				
				soundEffects.playSoundEffect(SoundEffects.BALL_VAUS_CONTACT, 0.1f);
				
				manipulateBounceOffVaus(bodyA, bodyB); // modify the angle at which the ball bounces off vaus, for more fun play control
				checkBallBounce(bodyA);
				
				break;
		
			case BALL_BRICK: // BALL_BRICK means that bodyA is the ball and bodyB is a brick, in that order every time, thanks to analyzeContact() above
				logger.info("==========================================================");
				logger.info("inside postSolve, case BALL_BRICK:");
				
				soundEffects.playSoundEffect(SoundEffects.BALL_BRICK_CONTACT, 1.0f);
				
				tempBrick = (Brick) gameBodyB.getSpecificBodyObject(); // casting will always be ok because at this point, bodyB can only be a brick
				
				
				tempBrick.toughness--; // this particular brick takes a hit and looses one toughness point
				if(tempBrick.toughness <= 0){
					
					soundEffects.playSoundEffect(SoundEffects.BRICK_DESTROYED, 0.1f);// you have to set the volume really low for it to make any difference
					
					tempBrick.isAlive = false; // kill the brick: won't be drawn and won't interact with anything
					logger.info("  set brick isAlive to: " + tempBrick.isAlive);
					onScreenDisplay.updateScore(tempBrick.pointValue); // update the score using the bricks point value
					bricks.bricksRemaining--; // decrement the number of bricks remaining in Bricks class
					
					logger.info("  inside ContactProcessor.postSolve case BALL_BRICK, bricks.bricksRemaining decremented to: " + bricks.bricksRemaining);
					
					// check if this brick had a bonus item..
					checkBrickForBonusItem(tempBrick);
					
				}
				checkBallBounce(bodyA);
				break;
			
			case BALL_FLIPPER: // BALL_FLIPPER means bodyA is the ball and bodyB is the flipper, in that order, every time
				logger.info("==========================================================");
				logger.info("inside postSolve, case BALL_FLIPPER:");
				tempVaus = (Vaus) gameBodyB.getSpecificBodyObject();
				if(tempVaus.isFlipping()){
					logger.info("  and flipper is moving:");
					initialVx = bodyA.getLinearVelocity().x; // record the initial velocities of ball
					initialVy = bodyA.getLinearVelocity().y;
					/* 
					 * we need to scale the x and y velocities of the ball after it is whacked with the flippers
					 * otherwise the ball can greatly speed up just from whacking it, which is not a game dynamic that is desirable
					 * the common scaleFactor is determined using right triangle geometry and some simple algebra
					 * imagine a right triangle where initial vX and vY are the non-hypotenuse sides
					 * the hypotenuse LENGTH represents the velocity of the ball, and we want it to always be the same (Ball.ballSpeed)
					 * scaleFactor = hypotenuse / sqrt(vX^2 + vY^2).. 
					 */
					float scaleFactor = ballSpeed / ((float) Math.sqrt(initialVx * initialVx + initialVy * initialVy));
					bodyA.setLinearVelocity(initialVx * scaleFactor, initialVy * scaleFactor);
				}
				checkBallBounce(bodyA); // it is theoretically possible for the ball to bounce off a flipper perfectly vertically, don't want that
				
				break;
				
			case BALL_FLOOR:
				//Walls w = (Walls) gameBodyB.getSpecificBodyObject(); // no need to manipulate the floor custom object, but you could if you wanted to
				onScreenDisplay.updateLives(-1); // lose one life
				//checkBallBounce(bodyA);
				break;
				
			case PROJECTILE_BRICK:
				// TODO: implement bomb projectile, and what is here is probably not all correct
	
				logger.info("==========================================================");
				logger.info("inside *****post****Solve, case PROJECTILE_BRICK:");
				
				
				tempBrick = (Brick) gameBodyB.getSpecificBodyObject(); // casting will always be ok because at this point, bodyB can only be a brick
				tempProjectile = (Projectile) gameBodyA.getSpecificBodyObject();
				
				logger.info("  and tempProjectile.type = " + tempProjectile.type);
				
				if(tempProjectile.type == ProjectileTypes.LASER){// a laser projectile hit a brick
					logger.info("  laser just hit a brick");
					tempBrick.toughness -= tempProjectile.power; // subtract projectile power from brick toughness
					if(tempBrick.toughness <= 0){
						tempBrick.isAlive = false; // kill the brick: won't be drawn and won't interact with anything
						onScreenDisplay.updateScore(tempBrick.pointValue); // update the score using the bricks point value
						bricks.bricksRemaining--; // decrement the number of bricks remaining in Bricks class
						logger.info("  inside ContactProcessor.postSolve case PROJECTILE_BRICK - LASER, bricks.bricksRemaining decremented to: " + bricks.bricksRemaining);

						
						// check if this brick had a bonus item..
						checkBrickForBonusItem(tempBrick);
					}
				}
				// box2d does not report sensor collisions in postSolve, so only the bomb projectile itself reports contact with other bricks here
				// at this point, bombBlastBrickHitList array only contains bricks that should be dealt damage from the bomb blast
				else if(tempProjectile.type == ProjectileTypes.BOMB){// box2d does not report sensor collisions in postSolve, so only the bomb projectile itself reports contact with other bricks here
					logger.info("  bomb just hit a brick with ID: " + tempBrick.id);
					logger.info("    bombBlastBrickHitList.size = " + bombBlastBrickHitList.size);
					logger.info("      and the ID's of all the bricks to be dealt damage are: ");
					for(Brick b: bombBlastBrickHitList){// check the bomb blast brick hit list
						logger.info("       " + b.id);
					}
					
					
					for(Brick b: bombBlastBrickHitList){
						b.toughness -= tempProjectile.power; // subtract projectile power from brick toughness
						if(b.toughness <= 0){ // brick has sustained mortal damage
							b.isAlive = false; // kill the brick: won't be drawn and won't interact with anything
							onScreenDisplay.updateScore(b.pointValue); // update the score using the bricks point value
							bricks.bricksRemaining--; // decrement the number of bricks remaining in Bricks class
							logger.info("  inside ContactProcessor.postSolve case PROJECTILE_BRICK - BOMB, bricks.bricksRemaining decremented to: " + bricks.bricksRemaining);

							
							// check if this brick had a bonus item..
							checkBrickForBonusItem(b);
						}
					}	
				}

				tempProjectile.isAlive = false; // kill the projectile because it just hit something
				
			
				break;
				
			case PROJECTILE_CEILING:
				logger.info("==========================================================");
				logger.info("inside postSolve, case PROJECTILE_CEILING:");
				tempProjectile = (Projectile) gameBodyA.getSpecificBodyObject();
				tempProjectile.isAlive = false; // kill the projectile.. what a waste of firepower!
				break;
				
			case BONUS_ITEM_VAUS:
				logger.info("==========================================================");
				logger.info("inside postSolve, case BONUS_ITEM_VAUS:");
				tempVaus = (Vaus) gameBodyB.getSpecificBodyObject();
				tempBonusItem = (BonusItem) gameBodyA.getSpecificBodyObject();
			
				if(tempBonusItem.getBonusItemType() != ItemTypes.NO_ITEM){ // you just caught a bonus item
					logger.info("  you just got a power up: " + tempBonusItem.getBonusItemType());
					
					if(tempBonusItem.isProjectile()){ // the bonus item was a projectile weapon
						if(tempBonusItem.isAlive){
							tempVaus.projectileType = tempBonusItem.getProjectileType(); // update vaus with new projectile weapon
							tempVaus.resetShotsRemaining(); // reset the shots remaining to original amount
							tempVaus.setProjectileType(tempBonusItem.getProjectileType());
							logger.info("  the power up is a projectile of type: " + tempBonusItem.getProjectileType());
							logger.info("  numShots reset via tempVaus.resetShotsRemaining()");
						}
					}
					else if(tempBonusItem.getBonusItemType() != ItemTypes.EXTRA_LIFE){ // the bonus item is NOT an extra life and NOT a projectile type weapon
						if(tempBonusItem.isAlive) { tempVaus.bonusItem = tempBonusItem.getBonusItemType(); } // update vaus with your new power up!	
					}
					else if(tempBonusItem.getBonusItemType() == ItemTypes.EXTRA_LIFE){ // you caught an extra life bonus item
						
						//box2d is reporting 2 collisions most of the time, so only add a life to your total if bonusItem isAlive is true..
						if(tempBonusItem.isAlive) { onScreenDisplay.updateLives(1); } // add 1 life to your total!
						logger.info("  the power up was an extra life!");
					}
				}
				tempBonusItem.isAlive = false; // you caught the bonus item, so it's not 'alive' anymore - don't simulate or draw it anymore
				break;
				
			case BONUS_ITEM_FLOOR:
				logger.info("==========================================================");
				logger.info("inside postSolve, case BONUS_ITEM_FLOOR:");
				tempBonusItem = (BonusItem) gameBodyA.getSpecificBodyObject();
				tempBonusItem.isAlive = false; // the bonus item hit the floor and must be eliminated
				break;
				
			case NO_CONTACT:
				break;
				
			default:
				break;
			
		}
		
	}
	
	// this method checks a brick to see if it contains a bonus item
	// if so, it will set the bonus item's isAlive field to true..
	private void checkBrickForBonusItem(Brick b){
		if(b.bonusItem.getBonusItemType() != ItemTypes.NO_ITEM){
			logger.info("  there WAS a bonus item in the brick");
			logger.info("  and the item was: " + b.bonusItem.getBonusItemType());
			b.bonusItem.isAlive = true; // make the bonus item active
			logger.info("  set brick's bonusItem isAlive to: " + b.bonusItem.isAlive);
		}else if(b.bonusItem.getBonusItemType() == ItemTypes.NO_ITEM){
			logger.info("  there was NO bonus item in the brick");
		}
	}
	
	
	/**
	 * This method can be called from anywhere.  It checks the ball's velocity vector x and y components
	 * to determine if the ball is now moving too close to perfectly vertical (not a big deal but can cause brick breaking mayhem),
	 * or too close to perfectly horizontal (a BFD.. if ball gets stuck going perfectly horizontal, game would need to be restarted).
	 * inititialVx and initialVy are the ball vector components that the box2d physics engine has already calculated for us.
	 * Sometimes box2d decides to make the ball go perfectly horizontal or vertical, I do not know why, but the price was right so I'm
	 * not complaining.  This method also uses the velocity vector of the ball BEFORE the collision was processed by box2d, which is 
	 * necessary to determine which direction the ball should be moving after the collision.
	 * ballMaxVx and ballMaxVy represent the closest to perfectly horizontal and vertical the ball should ever be allowed to travel.
	 * Essentially, I am overriding box2d when necessary, otherwise just let the ball bounce as box2d sees fit.
	 * @param ball The ball body whose velocity vector may need to be altered, must always be the ball body
	 */
	private void checkBallBounce(Body ball){
		initialVx = ball.getLinearVelocity().x; // these are the initial velocities that box2d has calculated for me, this method may modify them
		initialVy = ball.getLinearVelocity().y; // box2d tends to end up with perfectly horizontal angles, so we need to make sure that doesn't happen
		
		logger.info("==========================================================");
		logger.info("inside checkBallBounce");
		logger.info("(initVx, initVy) = (" + initialVx + ", " + initialVy + ")");
		logger.info("(preContactVx, preContactVy) = (" + preContactVx + ", " + preContactVy + ")");
		
		// check if ball is moving too close to perfectly horizontal..
		if(initialVx >= ballMaxVx){ // ball is moving RIGHT and it's angle is too close to perfectly horizontal
			logger.info("inside checkBallBounce: moving right too close to horizontal");
			// change outgoing ball Vx and Vy depending on if ball was traveling up or down before collision occurred..
			if(preContactVx > 0){ // and ball was moving RIGHT before collision
				ball.setLinearVelocity(ballMaxVx, preContactVy >= 0 ? -ballVyAtMaxVx : ballVyAtMaxVx); // Mmmmmmm... ternary operator goodness
				logger.info("inside checkBallBounce: moving right and WAS moving right before collision, velocities changed");
				logger.info("  inside checkBallBounce: new (Vx, Vy) = (" + ballMaxVx + ", " + (preContactVy >= 0 ? -ballVyAtMaxVx : ballVyAtMaxVx) + ")");
			}
			else if(preContactVx < 0){ // and ball was moving LEFT before collision
				// if ball was moving perfectly horizontally before collision (preContactVy = 0), just make it start going down..
				ball.setLinearVelocity(ballMaxVx, preContactVy > 0 ? ballVyAtMaxVx : -ballVyAtMaxVx /*case when preContactVy = 0*/);
				logger.info("inside checkBallBounce: moving right and WAS moving left before collision, velocities changed");
				logger.info("  inside checkBallBounce: new (Vx, Vy) = (" + ballVxAtMaxVy + ", " + (preContactVy > 0 ? ballMaxVy : -ballMaxVy) + ")");
			} // it doesn't matter if ball travels perfectly vertically so no preContactVx = 0 case needed
		}
		else if(initialVx <= -ballMaxVx){ // ball is moving LEFT and it's angle is too close to perfectly horizontal
			logger.info("inside checkBallBounce: moving left and too close to horizontal");
			if(preContactVx > 0){ // and ball was moving RIGHT before collision
				// if ball was moving perfectly horizontally before collision (preContactVy = 0), just make it start going down..
				ball.setLinearVelocity(-ballMaxVx, preContactVy > 0 ? ballVyAtMaxVx : -ballVyAtMaxVx /*case when preContactVy = 0*/);
				logger.info("inside checkBallBounce: moving left and WAS moving  right before collision, velocities changed");
				logger.info("  inside checkBallBounce: new (Vx, Vy) = (" + (-ballVxAtMaxVy) + ", " + (preContactVy > 0 ? ballMaxVy : -ballMaxVy) + ")");
			}
			else if(preContactVx < 0){ // and ball is was moving LEFT before collision
				ball.setLinearVelocity(-ballMaxVx, preContactVy >= 0 ? -ballVyAtMaxVx : ballVyAtMaxVx);
				logger.info("inside checkBallBounce: moving left and WAS moving left before collision, velocities changed");
				logger.info("  inside checkBallBounce: new (Vx, Vy) = (" + (-ballMaxVx) + ", " + (preContactVy >= 0 ? -ballVyAtMaxVx : ballVyAtMaxVx) + ")");
			}
		}
		else if(initialVx == 0){ // for whatever reason, the ball is traveling perfectly vertically
			if(preContactVx > 0){ // and ball WAS traveling RIGHT before collision
				ball.setLinearVelocity(-ballMinVx, preContactVy > 0 ? ballVyAtMinVx : -ballVyAtMinVx);
			}
			else if(preContactVx < 0){ // and ball WAS traveling LEFT before collision
				ball.setLinearVelocity(ballMinVx, preContactVy > 0 ? ballVyAtMinVx : -ballVyAtMinVx);
			}
			else{ // ball WAS travelling perfectly vertically before collision
				// this should never happen
			}
		}
		logger.info("exiting checkBallBounce..");
	} // end checkBallBounce
	
	
	
	/**
	 * This method calculates the angle at which the ball bounces off vaus.
	 * The angle depends on how far off center the ball hits: the idea is to give the player some basic control
	 * over the angle at which the ball bounces off vaus when the flippers are not being used.
	 * The adjustment made here depends on which direction
	 * the ball is heading when it hits vaus, if it hits on the left or right side,
	 * and how far from the center of vaus the ball hit.
	 * I am trying to duplicate the action from the original NES Arknoid game.
	 * No changes are made if the ball hits near the center of vaus.
	 * This method should only be called after the ball has collided with vaus' flat top.
	 * @param ball The ball body, it must always be the ball body 
	 * @param vaus Vaus' body, it must always be vaus' body (vaus is what you control in the game - the name vaus is a shout out to the original NES Arknoid game)
	 */
	private void manipulateBounceOffVaus(Body ball, Body vaus){
		initialPosBall = ball.getPosition().x; // store initial ball and vaus x positions
		initialPosVaus = vaus.getPosition().x;
		initialVx = ball.getLinearVelocity().x; // store initial ball velocity components
		initialVy = ball.getLinearVelocity().y;
		
		logger.info("just inside manipulateBounceOffVaus, (initVx, initVy) = (" + initialVx + ", " + initialVy + ")");
		
		logger.info("ballSpeed = " + ballSpeed);
		float multiplier = 2.0f;
		
		if(initialVx > 0){ // ball is moving right
			logger.info("ball is moving RIGHT");
			if(initialPosBall > initialPosVaus + alterBallBounceThreshold + 0.1f){ // and ball hit vaus sufficiently right of center
				logger.info("  and ball hit vaus sufficiently right of center");
				logger.info("  initial ball Vx = " + initialVx + " and initial ball Vy = " + initialVy);
				logger.info("  ballMaxVx = ballMaxVy = " + ballMaxVx);
				// in this case, new ball Vx > initial ball Vx for fun game dynamics, kind of like giving a tennis ball some "english"
				// alter the x component of ball velocity based on percentage away from center of vaus..
				
				newVx = initialVx * (((initialPosBall - initialPosVaus - alterBallBounceThreshold) / (halfVausWidth - alterBallBounceThreshold)) * multiplier + 1.0f); 
				
				logger.info("    offset ratio = " + ((initialPosBall - initialPosVaus - alterBallBounceThreshold) / (halfVausWidth - alterBallBounceThreshold)));
				logger.info("    initialVx multiplier = " + (((initialPosBall - initialPosVaus - alterBallBounceThreshold) / (halfVausWidth - alterBallBounceThreshold)) * multiplier + 1.0f));
				logger.info("    new ball Vx = " + newVx);
				
				if(newVx > ballMaxVx){ newVx = ballMaxVx; }
				else if(newVx < ballMinVx){ newVx = ballMinVx; };
				
				logger.info("    new ball Vx after checking against ballMaxVx = " + newVx);
				
				newVy = (float)Math.sqrt(ballSpeed * ballSpeed - newVx * newVx); // calculate new ball velocity vector y component, must keep ball going the same speed
				
				logger.info("    new ball Vy = " + newVy);
				ball.setLinearVelocity(newVx, newVy); // finally change the ball's complete velocity vector
			}
			else if(initialPosBall < initialPosVaus - alterBallBounceThreshold - 0.1f){ // and ball hit vaus sufficiently left of center
				logger.info("  and ball hit vaus sufficiently left of center");
				logger.info("  initial ball Vx = " + initialVx + " and initial ball Vy = " + initialVy);
				logger.info("  ballMaxVx = ballMaxVy = " + ballMaxVx);
				// in this case, new ball Vy > initial ball Vy for fun game dynamics
				// alter the y component of ball velocity based on percentage away from center of vaus..
				newVy = initialVy * (((initialPosVaus - alterBallBounceThreshold - initialPosBall) / (halfVausWidth - alterBallBounceThreshold)) * multiplier + 1.0f);
				logger.info("    offset ratio = " + ((initialPosVaus - alterBallBounceThreshold - initialPosBall) / (halfVausWidth - alterBallBounceThreshold)));
				logger.info("    initialVy multiplier = " + (((initialPosVaus - alterBallBounceThreshold - initialPosBall) / (halfVausWidth - alterBallBounceThreshold)) * multiplier + 1.0f));
				logger.info("    new ball Vy = " + newVy);
			
				if(newVy > ballMaxVy){ newVy = ballMaxVy; }
				else if(newVy < ballMinVy){ newVy = ballMinVy; }
				logger.info("    new ball Vy after checking against ballMaxVy = " + newVy);
				
				newVx = (float)Math.sqrt(ballSpeed * ballSpeed - newVy * newVy);
				logger.info("    new ball Vx = " + newVx);
				
				ball.setLinearVelocity(newVx, newVy);
			} // end ball moving RIGHT
		}
		else{ // ball is moving left
			logger.info("ball is moving LEFT");
			if(initialPosBall > initialPosVaus + alterBallBounceThreshold + 0.1f){ // and ball hit vaus sufficiently right of center
				logger.info("  and ball hit vaus sufficiently right of center");
				logger.info("  initial ball Vx = " + initialVx + " and initial ball Vy = " + initialVy);
				logger.info("  ballMaxVx = ballMaxVy = " + ballMaxVx);
				// in this case, new ball Vy > initial ball Vy for fun game dynamics
				// alter the y component of ball velocity based on percentage away from center of vaus..
				newVy = initialVy * (((initialPosBall - initialPosVaus - alterBallBounceThreshold) / (halfVausWidth - alterBallBounceThreshold)) * multiplier + 1.0f);
				logger.info("    offset ratio = " + ((initialPosBall - initialPosVaus - alterBallBounceThreshold) / (halfVausWidth - alterBallBounceThreshold)));
				logger.info("    initialVy multiplier = " + (((initialPosBall - initialPosVaus - alterBallBounceThreshold) / (halfVausWidth - alterBallBounceThreshold)) * multiplier + 1.0f));
				logger.info("    new ball Vy = " + newVy);
			
				if(newVy > ballMaxVy){ newVy = ballMaxVy; } 
				else if(newVy < ballMinVy){ newVy = ballMinVy; };
				logger.info("    new ball Vy after checking against ballMaxVy and ballMinVy = " + newVy);
				
				newVx = -(float)Math.sqrt(ballSpeed * ballSpeed - newVy * newVy);
				logger.info("    new ball Vx = " + newVx);
				
				ball.setLinearVelocity(newVx, newVy);
			}
			else if(initialPosBall < initialPosVaus - alterBallBounceThreshold - 0.1f){ // and ball hit vaus sufficiently left of center
				logger.info("  and ball hit vaus sufficiently left of center");
				logger.info("  initial ball Vx = " + initialVx + " and initial ball Vy = " + initialVy);
				logger.info("  ballMaxVx = ballMaxVy = " + ballMaxVx);
				// in this case, new ball Vx > initial ball Vx for fun game dynamics, kind of like giving a tennis ball some "english"
				// alter the x component of ball velocity based on percentage away from center of vaus..
				newVx = initialVx * (((initialPosVaus - alterBallBounceThreshold - initialPosBall) / (halfVausWidth - alterBallBounceThreshold)) * multiplier + 1.0f); 
				logger.info("    offset ratio = " + ((initialPosVaus - alterBallBounceThreshold - initialPosBall) / (halfVausWidth - alterBallBounceThreshold)));
				logger.info("    initialVx multiplier = " + (((initialPosVaus - alterBallBounceThreshold - initialPosBall) / (halfVausWidth - alterBallBounceThreshold)) * multiplier + 1.0f));
				logger.info("    new ball Vx = " + newVx);
				if(newVx > -ballMinVx){ newVx = -ballMinVx; }
				else if(newVx < -ballMaxVx){ newVx = -ballMaxVx; };
				logger.info("    new ball Vx after checking against ballMaxVx = " + newVx);
				newVy = (float)Math.sqrt(ballSpeed * ballSpeed - newVx * newVx); // calculate new ball velocity vector y component, must keep ball going the same speed
				logger.info("    new ball Vy = " + newVy);
				ball.setLinearVelocity(newVx, newVy); // finally change the ball's complete velocity vector
			}
		} // end ball moving LEFT
	} // end manipulateBounceOffVaus method
	
	
	/**
	 * This method takes a Contact object (which is produced in the contact methods from the ContactProcessor interface),
	 * and determines what two objects collided, and sorts them so that they are always in a defined
	 * order.  This info is put into a ContactPair object, which is an inner class in this class.
	 * I am trying to avoid messy repeated but 'inverse' switch statements above.. I want tidy, clean, logical, readable code.
	 * There is never a need for more than one ContactPair object, so it is reused every time.
	 * @param contact The contact object contains both colliding bodies and is automatically generated by libgdx. The two colliding bodies are in arbitrary order.
	 */
	private ContactPair analyzeContact(Contact contact){
		bodyA = contact.getFixtureA().getBody();
		bodyB = contact.getFixtureB().getBody();
		gameBodyA = (GameBody<?>) bodyA.getUserData(); // we do not know what specific bodies are colliding yet, thus <?>
		gameBodyB = (GameBody<?>) bodyB.getUserData();
	
		// start ball contact checks..
		if(gameBodyA.getType() == GameBodies.BALL && gameBodyB.getType() == GameBodies.VAUS){
			Vaus v = (Vaus) gameBodyB.getSpecificBodyObject(); // get the custom object tied to the body
			if(v.isFlipper(bodyB)){ contactPair.setBelligerents(ContactPairBelligerents.BALL_FLIPPER); } // need to know if ball hit either flipper
			else if(v.isVausFlatTop(bodyB)){ contactPair.setBelligerents(ContactPairBelligerents.BALL_VAUS_FLAT_TOP); } // or if ball hit the flat top of vaus
			else { contactPair.setBelligerents(ContactPairBelligerents.NO_CONTACT); } // don't care about the actual vaus body
			contactPair.setBodyA(bodyA);
			contactPair.setBodyB(bodyB);
			return contactPair;
		}else if(gameBodyA.getType() == GameBodies.VAUS && gameBodyB.getType() == GameBodies.BALL){
			Vaus v = (Vaus) gameBodyA.getSpecificBodyObject(); // get the custom object tied to the body
			if(v.isFlipper(bodyA)){ contactPair.setBelligerents(ContactPairBelligerents.BALL_FLIPPER); }
			else if(v.isVausFlatTop(bodyA)){ contactPair.setBelligerents(ContactPairBelligerents.BALL_VAUS_FLAT_TOP); } // or if ball hit the flat top of vaus
			else { contactPair.setBelligerents(ContactPairBelligerents.NO_CONTACT); }
			contactPair.setBodyA(bodyB);
			contactPair.setBodyB(bodyA);
			return contactPair;
		}else if(gameBodyA.getType() == GameBodies.BALL && gameBodyB.getType() == GameBodies.BRICK){ // bodyA is ball and bodyB is a brick
			contactPair.setBelligerents(ContactPairBelligerents.BALL_BRICK);
			contactPair.setBodyA(bodyA);
			contactPair.setBodyB(bodyB);
			return contactPair;
		}else if(gameBodyA.getType() == GameBodies.BRICK && gameBodyB.getType() == GameBodies.BALL){ // bodyA is a brick and bodyB is ball
			contactPair.setBelligerents(ContactPairBelligerents.BALL_BRICK);
			contactPair.setBodyA(bodyB);
			contactPair.setBodyB(bodyA);
			return contactPair;
		}else if(gameBodyA.getType() == GameBodies.BALL && gameBodyB.getType() == GameBodies.WALL){
			Walls w = (Walls) gameBodyB.getSpecificBodyObject();
			if(w.isFloor(bodyB)){ contactPair.setBelligerents(ContactPairBelligerents.BALL_FLOOR); }
			else { contactPair.setBelligerents(ContactPairBelligerents.NO_CONTACT); }
			contactPair.setBodyA(bodyA);
			contactPair.setBodyB(bodyB);
			return contactPair;
		}else if(gameBodyA.getType() == GameBodies.WALL && gameBodyB.getType() == GameBodies.BALL){
			Walls w = (Walls) gameBodyA.getSpecificBodyObject();
			if(w.isFloor(bodyA)){ contactPair.setBelligerents(ContactPairBelligerents.BALL_FLOOR); }
			else { contactPair.setBelligerents(ContactPairBelligerents.NO_CONTACT); }
			contactPair.setBodyA(bodyB);
			contactPair.setBodyB(bodyA);
			return contactPair;
		}
		// end ball contact checks
		
		// start projectile contact checks..
		else if(gameBodyA.getType() == GameBodies.PROJECTILE && gameBodyB.getType() == GameBodies.WALL){ // bodyA is projectile and bodyB is any wall
			Walls w = (Walls) gameBodyB.getSpecificBodyObject();
			if(w.isCeiling(bodyB)){ contactPair.setBelligerents(ContactPairBelligerents.PROJECTILE_CEILING); }
			else { contactPair.setBelligerents(ContactPairBelligerents.NO_CONTACT); }
			contactPair.setBodyA(bodyA);
			contactPair.setBodyB(bodyB);
			return contactPair;
		}else if(gameBodyA.getType() == GameBodies.WALL && gameBodyB.getType() == GameBodies.PROJECTILE){ // bodyA is a any wall and bodyB is projectile
			Walls w = (Walls) gameBodyA.getSpecificBodyObject();
			if(w.isCeiling(bodyA)){ contactPair.setBelligerents(ContactPairBelligerents.PROJECTILE_CEILING); }
			else { contactPair.setBelligerents(ContactPairBelligerents.NO_CONTACT); }
			contactPair.setBodyA(bodyB);
			contactPair.setBodyB(bodyA);
			return contactPair;
		}
		else if(gameBodyA.getType() == GameBodies.PROJECTILE && gameBodyB.getType() == GameBodies.BRICK){ // bodyA is projectile and bodyB is any wall
			contactPair.setBelligerents(ContactPairBelligerents.PROJECTILE_BRICK);
			contactPair.setBodyA(bodyA);
			contactPair.setBodyB(bodyB);
			return contactPair;
		}else if(gameBodyA.getType() == GameBodies.BRICK && gameBodyB.getType() == GameBodies.PROJECTILE){ // bodyB is projectile and bodyA is any wall
			contactPair.setBelligerents(ContactPairBelligerents.PROJECTILE_BRICK);
			contactPair.setBodyA(bodyB);
			contactPair.setBodyB(bodyA);
			return contactPair;
		}
		// end projectile contact checks
		
		// start bonus item contact checks
		else if(gameBodyA.getType() == GameBodies.BONUS_ITEM && gameBodyB.getType() == GameBodies.VAUS){ // bodyA is projectile and bodyB is vaus
			contactPair.setBelligerents(ContactPairBelligerents.BONUS_ITEM_VAUS);
			contactPair.setBodyA(bodyA);
			contactPair.setBodyB(bodyB);
			return contactPair;
		}else if(gameBodyA.getType() == GameBodies.VAUS && gameBodyB.getType() == GameBodies.BONUS_ITEM){ // bodyB is projectile and bodyA is vaus
			contactPair.setBelligerents(ContactPairBelligerents.BONUS_ITEM_VAUS);
			contactPair.setBodyA(bodyB);
			contactPair.setBodyB(bodyA);
			return contactPair;
		}
		else if(gameBodyA.getType() == GameBodies.BONUS_ITEM && gameBodyB.getType() == GameBodies.WALL){ // bodyA is projectile and bodyB is vaus
			Walls w = (Walls) gameBodyB.getSpecificBodyObject();
			if(w.isFloor(bodyB)){ contactPair.setBelligerents(ContactPairBelligerents.BONUS_ITEM_FLOOR); }
			else { contactPair.setBelligerents(ContactPairBelligerents.NO_CONTACT); }
			contactPair.setBodyA(bodyA);
			contactPair.setBodyB(bodyB);
			return contactPair;
		}else if(gameBodyA.getType() == GameBodies.WALL && gameBodyB.getType() == GameBodies.BONUS_ITEM){ // bodyB is projectile and bodyA is vaus
			Walls w = (Walls) gameBodyA.getSpecificBodyObject();
			if(w.isFloor(bodyA)){ contactPair.setBelligerents(ContactPairBelligerents.BONUS_ITEM_FLOOR); }
			else { contactPair.setBelligerents(ContactPairBelligerents.NO_CONTACT); }
			contactPair.setBodyA(bodyB);
			contactPair.setBodyB(bodyA);
			return contactPair;
		}
		// end bonus item contact checks
		
/*		// start bomb blast contact checks (not the same as a bomb projectile)
		else if(gameBodyA.getType() == GameBodies.BOMB_BLAST && gameBodyB.getType() == GameBodies.BRICK){
			contactPair.setBodyA(bodyA);
			contactPair.setBodyB(bodyB);
			return contactPair;
		}
		else if(gameBodyA.getType() == GameBodies.BRICK && gameBodyB.getType() == GameBodies.BOMB_BLAST){
			contactPair.setBodyA(bodyB);
			contactPair.setBodyB(bodyA);
			return contactPair;
		}
		// end bomb blast contact checks
*/		
		
		else{ // nothing important has collided
			contactPair.setBelligerents(ContactPairBelligerents.NO_CONTACT);
			contactPair.setBodyA(bodyA); // won't be used, need it to avoid null pointer when getBodyA is called from inside postSolve method
			contactPair.setBodyB(bodyB);
			return contactPair;
		}
	} // end analyzeContact method
} // end ContactProcessor.java class
