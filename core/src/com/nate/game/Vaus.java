/**
 * 
 */
package com.nate.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Application.ApplicationType;

import static com.nate.game.Arknoid1.SCENE_HEIGHT;
import static com.nate.game.Arknoid1.SCENE_WIDTH;
import static com.nate.game.Arknoid1.VAUS_ELEVATION;
import static com.nate.game.Arknoid1.WORLD_TO_SCREEN;
import static com.nate.game.Walls.box2dWallWidth;
import static com.nate.game.Ball.ballBody;
import static com.nate.game.Arknoid1.batch;
import static com.nate.game.Arknoid1.logger;
import static com.nate.game.Arknoid1.box2dWorld;
import static com.nate.game.Arknoid1.viewport;
import static com.nate.game.Arknoid1.onScreenDisplay;
import static com.nate.game.OnScreenDisplay.ANDROID_FIRE_BUTTON_X;
import static com.nate.game.OnScreenDisplay.ANDROID_FIRE_BUTTON_Y;
import static com.nate.game.OnScreenDisplay.ANDROID_FLIP_BUTTON_X;
import static com.nate.game.OnScreenDisplay.ANDROID_FLIP_BUTTON_Y;
import static com.nate.game.OnScreenDisplay.screenButtonWidth;

import static com.badlogic.gdx.Input.Keys; // represents the keyboard keys that user can press
import static com.nate.game.CreateBody.createBody; // used to create bodies for the box2d physics world
import static com.nate.game.ContactProcessor.ContactCategories; // enum: every body in the box2d world has it's own category used for collision filtering
import static com.nate.game.BonusItem.ItemTypes; // represents all the different bonus items that may fall from bricks for vaus to 'catch'
import static com.nate.game.Projectile.ProjectileTypes; // represents all the different projectileType types that vaus can 'fire'

/**
 * @author Nathan Merris
 *
 */
public class Vaus implements InputProcessor, GameObject {
	private Ball ball;
	private Projectile laser, bomb; // only 2 projectileType weapons for now
	
	private float vausSpeed; // this determines how fast vaus will be able to move
	ItemTypes bonusItem; // lasers, wide-glide, bombs, extra-life, etc
	ProjectileTypes projectileType; // lasers and bombs for now, these are what you actually fire at the bricks
	private String projectileWeaponName;
	private static final float FLIPPER_GRAVITY = 0.5f; // determines how fast the flippers fall back down after use
	private static final float MAX_FLIPPER_GRAVITY = 10f;
		
	private boolean pauseBall; // used only in the keyDown method, it's in this class because it involves user input detection
	
	private Texture vausTex; // texture for the main section (the middle section) of vaus
	
	// box2d stuff..
	// EVERY box2d body is based on the texture sizes, so changing them will change everything..
	public static Body vausBody, leftFlipperBody, rightFlipperBody, flatTopBody, tempFlipperBody; // vaus and her two flippers and other junk to make the ball bounce as expected
	private float box2dVausWidth, box2dVausHeight, box2dFlipperWidth, box2dFlipperHeight; // represents the widths of things in the box2d world, which is much scaled down from the actual pixel dimensions of the textures
	public static float box2dVausTotalWidth; // the width of vaus and her flippers from end to end, in box2d world units
	private float box2dXlastTouched;
	private float leftMostVausX, rightMostVausX; // Vaus should never pass to the left of leftMostVausX or it would go off the screen, similar for rightMost
	private boolean movingRight, movingLeft; // used with collision detection logic/response
	
/******************************************************************************************************/
	
	// constructor..
	public Vaus (String textureFileName, Ball ball) 		// needed to stop and start ball when space bar toggled, pass in ball because this class handles all user input
	{
		logger.info("entering Vaus constructor..");
		
		vausSpeed = 8.0f; // vaus ballSpeed, this could potentially be changed while playing, but just constant for now
		movingRight = movingLeft = false; // used when moving vaus back and forth from user input
		pauseBall = false; // starting game with ball NOT paused
		
		bonusItem = ItemTypes.NO_ITEM; // start with no bonus items, set in ContactProcessor if you catch a bonusItem with vaus
		projectileType = ProjectileTypes.NO_PROJECTILE; // start with no projectileType weapons
		projectileWeaponName = "NO WEAPON"; // should never be shown because OnScreenDisplay should never show weapon name on screen if you don't have a weapon
		
		this.ball = ball;
		vausTex = new Texture(Gdx.files.internal(textureFileName)); // load the texture
		
		laser = new Projectile(ProjectileTypes.LASER); // always create projectileType objects, they may never be used, depends on level and if player obtains the bonus item that contains a projectileType weapon
		bomb = new Projectile(ProjectileTypes.BOMB); // TODO: there's a better way to do this so that new objects are not created until they are needed.. 
		
		// convert from the texture pixel dimensions to the box2d physics world dimensions..
		// everything is based on the vaus main body texture size..
		box2dVausWidth = vausTex.getWidth() * WORLD_TO_SCREEN;
		box2dVausHeight = vausTex.getHeight() * WORLD_TO_SCREEN;
		box2dFlipperWidth = box2dVausWidth * 0.4f;
		box2dFlipperHeight = box2dVausHeight * 0.4f;
		leftMostVausX = box2dVausWidth * 0.5f + box2dFlipperWidth + box2dWallWidth + box2dFlipperHeight * 2f;
		rightMostVausX = SCENE_WIDTH - leftMostVausX; 
		
		box2dVausTotalWidth = box2dVausWidth + box2dFlipperWidth * 2.0f;
		
		Gdx.input.setInputProcessor(this); // allow Vaus to receive user input
		
		// create box2d bodies..
		// all the vaus bodies should not collide with each other, but should collide with the ball, so the mask bits are the bitwise 1's complement of their own category
		vausBody = createBody(BodyType.KinematicBody, 							// body type
				 false, ContactCategories.VAUS, ContactCategories.VAUS.getMask(), 	// is body a sensor?, sensor group bits, mask bits
				 // VAUS: 0000 0000 0000 0001, VAUS mask: 1111 1111 1111 1110, bitwise AND: 0000 0000 0000 0000, so DON'T collide
				 // a collision WILL  occur when the bitwise AND of category and mask are non-zero
				 0, new GameBody<Vaus>(this), // gravity scale, userData
				 SCENE_WIDTH * 0.5f, VAUS_ELEVATION, // initial x and y positions
				 100.0f, 0, 1.0f, // density, restitution, friction
				 box2dVausWidth * 0.5f, box2dVausHeight * 0.5f); // halfWidth, halfHeight
		
		leftFlipperBody = createBody(BodyType.DynamicBody,  // body type
			   	 false, ContactCategories.VAUS, ContactCategories.VAUS.getMask(),  // is body a sensor?, sensor group
			   	 FLIPPER_GRAVITY, new GameBody<Vaus>(this), // gravity scale, userData
				 SCENE_WIDTH * 0.25f, VAUS_ELEVATION, // initial x and y positions, not used because the flippers are attached to vaus and move with vaus
				 10.0f, 0, 0, // density, restitution, friction
				 box2dFlipperWidth * 0.5f, box2dFlipperHeight * 0.5f); // halfWidth, halfHeight based on vaus texture dimensions
	
		rightFlipperBody = createBody(BodyType.DynamicBody, // body type
				 false, ContactCategories.VAUS, ContactCategories.VAUS.getMask(), // is body a sensor?, sensor group
				 FLIPPER_GRAVITY, new GameBody<Vaus>(this), // gravity scale, userData
				 SCENE_WIDTH * 0.75f, VAUS_ELEVATION, // initial x and y positions
				 10.0f, 0, 0, // density, restitution, friction
				 box2dFlipperWidth * 0.5f, box2dFlipperHeight * 0.5f); // halfWidth, halfHeight based on vaus texture dimensions
		
		flatTopBody = createBody(BodyType.KinematicBody, // body type
				 false, ContactCategories.VAUS, ContactCategories.VAUS.getMask(), // is body a sensor?, sensor group
				 0, new GameBody<Vaus>(this), // gravity scale, userData
				 SCENE_WIDTH * 0.5f, VAUS_ELEVATION + box2dVausHeight * 0.5f, // initial x and y positions
				 100.0f, 0, 1.0f, // density, restitution, friction
				 (box2dVausWidth + box2dFlipperWidth + box2dFlipperWidth) * 0.5f, 0); // halfWidth, halfHeight (going for a horizontal line here)
	
		
		
		// define box2d joints..
		RevoluteJointDef rightFlipperJointDef = new RevoluteJointDef();
		rightFlipperJointDef.bodyA = vausBody; // the anchor body
		rightFlipperJointDef.bodyB = rightFlipperBody; // the body that moves on the bodyA pivot
		rightFlipperJointDef.collideConnected = false; // so the connected bodies do not collide with one another
		rightFlipperJointDef.localAnchorA.set(vausBody.getLocalCenter().x + box2dVausWidth * 0.5f, (vausBody.getLocalCenter().y + box2dVausHeight * 0.5f) - box2dVausHeight * 0.05f); // needs adjusted
		rightFlipperJointDef.localAnchorB.set(rightFlipperBody.getLocalCenter().x - box2dFlipperWidth * 0.5f , rightFlipperBody.getLocalCenter().y + box2dVausHeight * 0.2f); // needs adjusted
		rightFlipperJointDef.enableLimit = true; // only want flippers to move between horizontal and about 45deg up, similar to pinball
		rightFlipperJointDef.upperAngle = 45f * MathUtils.degreesToRadians;
		rightFlipperJointDef.lowerAngle = 0.02f; // for some reason flipper doesn't sit perfectly horizontal at 0
		box2dWorld.createJoint(rightFlipperJointDef);
		
		RevoluteJointDef leftFlipperJointDef = new RevoluteJointDef();
		leftFlipperJointDef.bodyA = vausBody; // the anchor body
		leftFlipperJointDef.bodyB = leftFlipperBody; // the body that moves on the bodyA pivot
		leftFlipperJointDef.collideConnected = false; // so the connected bodies do not collide with one another
		leftFlipperJointDef.localAnchorA.set(vausBody.getLocalCenter().x - box2dVausWidth * 0.5f, (vausBody.getLocalCenter().y + box2dVausHeight * 0.5f) - box2dVausHeight * 0.05f);
		leftFlipperJointDef.localAnchorB.set(leftFlipperBody.getLocalCenter().x + box2dFlipperWidth * 0.5f , leftFlipperBody.getLocalCenter().y + box2dVausHeight * 0.2f);
		leftFlipperJointDef.enableLimit = true;
		leftFlipperJointDef.upperAngle = 0;
		leftFlipperJointDef.lowerAngle = -45f * MathUtils.degreesToRadians; // what an unholy PITA it was to figure these angles out!!
		box2dWorld.createJoint(leftFlipperJointDef);
		
		
		logger.info("Vaus constructor exiting..");
	}
	
	@Override
	public void update(){
	
		if(movingLeft & vausBody.getPosition().x <= box2dXlastTouched){ // we have arrived at (or slightly passed) the X coord that the user last clicked, so stop moving
			vausBody.setLinearVelocity(0, 0); // stop
			flatTopBody.setLinearVelocity(0, 0);
			movingRight = movingLeft = false;
		}
		else if(movingRight & vausBody.getPosition().x >= box2dXlastTouched){ // we have arrived at (or slightly passed) the X coord that the user last clicked, so stop moving
			vausBody.setLinearVelocity(0, 0); // stop
			flatTopBody.setLinearVelocity(0, 0);
			movingLeft = movingRight = false;
		}
		else if(movingLeft & vausBody.getPosition().x <= leftMostVausX){
			vausBody.setLinearVelocity(0, 0); // stop
			flatTopBody.setLinearVelocity(0, 0);
			movingLeft = movingRight = false;
		}
		else if(movingRight & vausBody.getPosition().x >= rightMostVausX){
			vausBody.setLinearVelocity(0, 0); // stop
			flatTopBody.setLinearVelocity(0, 0);
			movingLeft = movingRight = false;
		}
		
		updateProjectiles();
	
			
	}
	
	// returns the center of vaus' current position
	public static Vector2 getPosition(){
		return vausBody.getPosition();
	}
	
	// this method calls each projectile object's update method, but only for the one projectile vaus current has
	// updating a projectile means setting the box2d body to active or inactive depending on it's isAlive boolean field
	// this is done in each projectile object individually
	private void updateProjectiles(){
		switch(projectileType){
			case LASER:
				laser.update();
				break;
			
			case BOMB:
				bomb.update();
				break;
				
			default:
				break;
		}
	}
	
	public void setProjectileType(ProjectileTypes type){
		logger.info("inside Vaus.setProjectileType()");
		
		switch(type){
			case LASER:
				logger.info("  case LASER:, calling laser.setProjectileType(LASER)..");
				laser.setProjectileType(type);
				projectileWeaponName = "Laser";
				break;
			
			case BOMB:
				logger.info("  case BOMB:, calling bomb.setProjectileType(BOMB)..");
				bomb.setProjectileType(type);
				projectileWeaponName = "Bomb";
				break;
				
			default:
				logger.info("  case ***default***:");
				break;
		}
	}
	
	public String getProjectileName(){
		return projectileWeaponName;
	}
	
	public void resetShotsRemaining(){
		
		logger.info("inside Vaus.resetShotsRemaining(), projectileType is: " + projectileType);
		
		switch(projectileType){
			case LASER:
				logger.info("  case LASER:");
				laser.resetNumShots();
				break;
			
			case BOMB:
				logger.info("  case BOMB:");
				bomb.resetNumShots();
				break;
				
			default:
				logger.info("  case ***default***:");
				break;
		}
	}
	
	
	// draw should be called from Arknoid1 main class from inside the batch rendering block
	// batches are only for drawing textures on the screen, they have nothing to do with the physics
	@Override
	public void draw(){				
		batch.draw(vausTex, // texture
				   vausBody.getPosition().x - box2dVausWidth * 0.5f, // bottom left x
				   vausBody.getPosition().y - box2dVausHeight * 0.5f, // bottom left y
				   box2dVausWidth * 0.5f, box2dVausHeight * 0.5f, // originX, originY (for translations)
				   box2dVausWidth, box2dVausHeight, // width, height
				   1f, 1f, // scaleX, scaleY
				   0, // rotate, 0 means do not rotate
				   0, 0, // srcX, srcY (?)
				   vausTex.getWidth(), vausTex.getHeight(), // srcWidth, srcHeight
				   false, false // flipX, flipY
				   );
		
		// similar to the way that each brick has a bonusItem tied to it, vaus may have have a projectileType-type bonus item tied to it
		// so the draw() method for a projectileType is called from vaus, similar to how the draw methods for the bonusItem capsules are called from each brick's draw method
		if(projectileType != ProjectileTypes.NO_PROJECTILE){ // we only need to draw the projectileType-type bonus items, so ignored anything that's not a projectileType
			switch(projectileType){
				case LASER:
					laser.draw();
					break;
					
				case BOMB:
					bomb.draw();
					break;
					
				default:
					break;
			}
		}
	}
	
	// isFlipper returns true if the Body passed to it is either flipper body
	public boolean isFlipper(Body b){
		if (b.equals(leftFlipperBody) || b.equals(rightFlipperBody)) return true;
		else return false;
	}
	
	public boolean isVausFlatTop(Body b){
		if (b.equals(flatTopBody)) return true;
		else return false;
	}
	
	public boolean isFlipping(){
		if(leftFlipperBody.getLinearVelocity().y > 0.05f || rightFlipperBody.getLinearVelocity().y > 0.05f){
			return true;
		}
		else return false;
	}
	
	
	
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	
	// start user input detection methods..
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) { // desktop: left mouse button, android: tap screen (both work as-is)
		
		// translate touched/clicked coords to box2dWorld coords and store in point(x,y,z)
		Vector3 point = new Vector3(); // used to temporarily store the translated touched/clicked coords to world coords
		viewport.getCamera().unproject(point.set(screenX, screenY, 0)); // translate touched/clicked coords to box2dWorld coords and store in point(x,y,z)
		
		if(button == Input.Buttons.LEFT){ // left mouse button
			box2dXlastTouched = point.x;
			
			if(box2dXlastTouched < vausBody.getPosition().x){ // user touched/clicked left of current vaus position
				leftFlipperBody.setGravityScale(MAX_FLIPPER_GRAVITY); // this is here to prevent the flippers from flopping around when you are just moving vaus
				rightFlipperBody.setGravityScale(MAX_FLIPPER_GRAVITY);
				
				vausBody.setLinearVelocity(-vausSpeed, 0); // move vaus left instantly
				flatTopBody.setLinearVelocity(-vausSpeed, 0); // move flatTopBody left instantly
				
				movingLeft = true;
				movingRight = false;
			}
			else if(box2dXlastTouched > vausBody.getPosition().x){ // user touched/clicked right of current vaus position
				leftFlipperBody.setGravityScale(MAX_FLIPPER_GRAVITY);
				rightFlipperBody.setGravityScale(MAX_FLIPPER_GRAVITY);
				
				vausBody.setLinearVelocity(vausSpeed, 0); // move vaus right instantly
				flatTopBody.setLinearVelocity(vausSpeed, 0); // move flatTopBody left instantly
				
				movingLeft = false;
				movingRight = true;
			}
			
			
			
			// check if either the 'fire' or 'flip' buttons were pressed and take appropriate action, only check on Android platforms because everything else has keyboard and mouse..
/*			if(Arknoid1.appType.equals(ApplicationType.Android)){
				processAndroidButtonTap(screenX, screenY); // for Android button tap, we use screen coords, not box2d world coords
			}*/
		
			if(!Arknoid1.appType.equals(ApplicationType.Android)){ // TESTING ONLY HERE
				
				logger.info("inside Vaus.touchDown, inside Android tap if statement...");
				logger.info("  and screenX, screenY = " + screenX + ", " + screenY);
				
				processAndroidButtonTap(screenX, screenY); // for Android button tap, we use screen coords, not box2d world coords
			}
			
			
			
		}
		else if(button == Input.Buttons.RIGHT){ // whack the ball with the flippers! (right mouse button), does nothing in android
			
			leftFlipperBody.setGravityScale(FLIPPER_GRAVITY); // set to "normal" gravity so the applied forces below have an effect
			rightFlipperBody.setGravityScale(FLIPPER_GRAVITY);
					
			leftFlipperBody.applyForceToCenter(0, 100f, true); // this causes the flipper to swing up
			rightFlipperBody.applyForceToCenter(0, 100f, true);
			
		}
		return false;
	}
	
	// TODO: the coords are not right, need to figure it out...........
	private void processAndroidButtonTap(int screenX, int screenY) {
		int buttonPadding = 10; // it always annoys me when you have to super-precisely tap something on a touchscreen, so I'm giving it some padding pixels so you just have to be close enough
		if(screenX > ANDROID_FIRE_BUTTON_X - buttonPadding && screenX < ANDROID_FIRE_BUTTON_X + screenButtonWidth + buttonPadding){ // player tapped screen within fire button X range
			if(screenY > SCENE_HEIGHT - ANDROID_FIRE_BUTTON_Y - screenButtonWidth - buttonPadding && screenY < SCENE_HEIGHT - ANDROID_FIRE_BUTTON_Y - buttonPadding){ // and player tapped screen with fire button Y range
				// player just tapped the fire button in Android platform..
				switch(projectileType){ // fire projectile weapon if player has one
					case LASER:
						logger.info("  firing laser...");
						laser.fire(vausBody.getPosition().x, vausBody.getPosition().y + box2dVausHeight);
						break;
						
					case BOMB:
						logger.info("  firing bomb...");
						bomb.fire(vausBody.getPosition().x, vausBody.getPosition().y + box2dVausHeight);
						break;
				
					case NO_PROJECTILE:
						break;
						
					default:
						break;
				}
			}
		} // end Android fire button detection
		
		else if(screenX > ANDROID_FLIP_BUTTON_X - buttonPadding && screenX < ANDROID_FLIP_BUTTON_X + screenButtonWidth + buttonPadding){ // player tapped screen within fire button X range
			if(screenY > SCENE_HEIGHT - ANDROID_FLIP_BUTTON_Y - screenButtonWidth - buttonPadding && screenY < SCENE_HEIGHT - ANDROID_FLIP_BUTTON_Y - buttonPadding){ // and player tapped screen with fire button Y range
		
				leftFlipperBody.setGravityScale(FLIPPER_GRAVITY); // set to "normal" gravity so the applied forces below have an effect
				rightFlipperBody.setGravityScale(FLIPPER_GRAVITY);
						
				leftFlipperBody.applyForceToCenter(0, 100f, true); // this causes the flipper to swing up
				rightFlipperBody.applyForceToCenter(0, 100f, true);
			}
		} // end Android flip button detection
	} // end processAndroidButtonTap

	
	//private float tempVx, tempVy; // only used in keyDown...
	@Override
	public boolean keyDown(int keycode) {
		
		switch(keycode){
			case Keys.P:
				logger.info("you pushed 'P' or 'p' key to toggle ball pause");
				if(!pauseBall){				
					ball.stop();
					pauseBall = true;
				}
				else{
					ball.start();
					pauseBall = false;
				}
				break;
				
			case Keys.SPACE:
				logger.info("you pushed SPACE to fire your projectileType weapon");
				switch(projectileType){ // Mmmmmmm... nested switches
					case LASER:
						logger.info("  firing laser...");
						laser.fire(vausBody.getPosition().x, vausBody.getPosition().y + box2dVausHeight);
						break;
						
					case BOMB:
						logger.info("  firing bomb...");
						bomb.fire(vausBody.getPosition().x, vausBody.getPosition().y + box2dVausHeight);
						break;
				
					case NO_PROJECTILE:
						break;
						
					default:
						break;
				}
				break;
				
			default:
				break;
		}
		
		
		return false;
	}
	

	@Override
	public boolean keyUp(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void dispose(){
		vausTex.dispose();
		logger.info("Vaus dispose() exiting..");
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}


}
