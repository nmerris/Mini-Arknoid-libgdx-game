/** The Projectile class implements all projectiles that vaus can use to damage bricks.
 *  As this class implements GameObject, it knows how to draw itself.
 *  There can only be one projectileType object on the screen at any given time.
 *  Call a projectileType object's draw() method from Arknoid1.java main class
 */
package com.nate.game;

import static com.nate.game.Arknoid1.SCENE_WIDTH;
import static com.nate.game.Arknoid1.VAUS_ELEVATION;
import static com.nate.game.Arknoid1.WORLD_TO_SCREEN;
import static com.nate.game.Arknoid1.batch;
import static com.nate.game.CreateBody.createBody;
import static com.nate.game.ContactProcessor.ContactCategories;
import static com.nate.game.Vaus.getPosition;
import static com.nate.game.BonusItem.ItemTypes;
import static com.nate.game.Arknoid1.logger;
import static com.nate.game.Arknoid1.onScreenDisplay;
import static com.nate.game.CreateBody.createBody; // used to create bodies for the box2d physics world
import static com.nate.game.Ball.box2dBallRadius; // bombBlastCircleRadius is based off ball radius

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Logger;

/**
 * @author natenator
 *
 */
public class Projectile implements GameObject {
	
	public enum ProjectileTypes{ // enum constructor parameters: ENUM_VALUE(numShots, power, speed)
		NO_PROJECTILE(0, 0, 0f), LASER(5, 1, 8.0f), BOMB(2, 4, 3.0f);
		
		private int numShots, power; // each enum variable has numShots, power, and speed values
		private float speed;
		
		// enum constructor..
		ProjectileTypes(int n, int p, float s){ // numshots, power, speed
			numShots = n;
			power = p;
			speed = s;
		}
		
		public int getNumShots() { return numShots; }
		public int getPower() { return power; }
		
	}
	
	int power; // how much damage each projectileType does to the brick it hits
	boolean isAlive; // true when projectile is on screen
	//boolean bombBlastIsAlive; // true when bomb is exploding
	float speed; // the rate of change of the projectileType, probably only ever going to go perfectly vertical
	ProjectileTypes type; // represents the different types of projectiles vaus can shoot
	private Body body; // the projectile's box2d body
	
	private FixtureDef bombProjectileBlastSensor; // a bomb projectile has a sensor attached to it, anything the sensor touches at the instant a bomb body contacts a brick, will take some damage
	
	private Texture tex;
	float width, height;
	int shotsRemaining; // Projectile can only be fired a number of times
	//float bombBlastRadius;
	
	//private BombBlast bombBlast; // a bomb type projectile has a bombBlast, it's only detonated if a bomb hits a brick
	
	// constructor should be called to create a new projectileType object when a bonus item dropped from a brick contacts vaus
	// only need to create a new object if the new projectileType in the bonus item differs from current projectileType (if any)
	// otherwise just keep reusing the old object as needed
	public Projectile (ProjectileTypes type){
		
		//bombBlast = new BombBlast(); // create a bombBlast object, but it will only be 'detonated' as needed
		// laser does not need a seperate object because it just hits a brick and goes away, no blast radius to deal with
		// however, either projectile will trigger cool particle effects as needed, which is just visual
		
		this.type = type;
		
		switch(type){
			case LASER:
				tex = new Texture(Gdx.files.internal("projectile_laser.png")); // load the texture
				width = tex.getWidth() * WORLD_TO_SCREEN;
				height = tex.getHeight() * WORLD_TO_SCREEN;
				power = ProjectileTypes.LASER.power;
				speed = ProjectileTypes.LASER.speed;
				shotsRemaining = ProjectileTypes.LASER.numShots;
				break;
				
			case BOMB:
				tex = new Texture(Gdx.files.internal("projectile_bomb.png")); // load the texture
				width = tex.getWidth() * WORLD_TO_SCREEN;
				height = tex.getHeight() * WORLD_TO_SCREEN;
				power = ProjectileTypes.BOMB.power;
				speed = ProjectileTypes.BOMB.speed;
				shotsRemaining = ProjectileTypes.BOMB.numShots;
				break;
				
			default:
				break;
		}
		
		// projectiles should never hit ball, other projectiles, nor bonus items (while they are falling from bricks downward)..
		short mask = (short)(ContactCategories.PROJECTILE.getMask() & ContactCategories.BALL.getMask() & ContactCategories.BONUS_ITEM.getMask()); 
		
		// create a projectile body..
		body =   createBody(BodyType.DynamicBody,  				// body type
			   	 false, ContactCategories.PROJECTILE, mask, 	// is body a sensor?, sensor group, sensor mask (do not collide with ball)
			   	 0, new GameBody<Projectile>(this),				// gravity scale, userData
			   	 -10f, -10f,									// initial x and y positions
				 0, 0, 0, 										// density, restitution, friction
				 width * 0.5f, height * 0.5f); 					// halfWidth, halfHeight based on brick texture dimensions
		
		isAlive = false; // start inactive because we need to wait for player to fire the projectileType before it should be added to world and drawn
		body.setActive(false); // do not start simulating the body yet, wait until user "fires" the projectileType
		

		
		// create and attach sensor fixture def for bomb bodies..
		if(type == ProjectileTypes.BOMB) {
			CircleShape circle = new CircleShape();
			circle.setRadius(width * 2.0f); // set bomb's blast radius based on bomb body width
			bombProjectileBlastSensor = new FixtureDef();
			bombProjectileBlastSensor.isSensor = true; // a sensor will generate contact info, but will not cause any actual reactions with other bodies
			bombProjectileBlastSensor.shape = circle;
			body.createFixture(bombProjectileBlastSensor); // attach the bomb sensor fixture (like a 'blast radius') to the projectile body 
			circle.dispose(); // DO NOT PUT THIS BEFORE YOU ADD IT TO body or you will get a nebulous error message, confusing like a nebula of course, if you are an employer considering hiring me, I'm just being goofy, I am not trying to show off by using big words
		
		
			
		}

		
	}
	
	
	public void resetNumShots(){
		logger.info("just entered Projectile.resetShotsRemaining()");
		switch(type){
			case LASER:
				shotsRemaining = ProjectileTypes.LASER.numShots;
				logger.info("  inside Projectile.resetNumShots(), case LASER, shotsRemaining set to: " + shotsRemaining);
				break;
				
			case BOMB:
				shotsRemaining = ProjectileTypes.BOMB.numShots;
				logger.info("  inside Projectile.resetNumShots(), case BOMB, shotsRemaining set to: " + shotsRemaining);
				break;
				
			default:
				logger.info("  inside Projectile.resetNumShots(), ***case default***");
				break;
		}
		onScreenDisplay.updateShots(shotsRemaining); // update the on screen display
		logger.info("  just before exiting, shotsRemaining is: " + shotsRemaining);
	}
	
	
	// called from Vaus' input listener methods to fire a projectileType..
	// x and y parameters indicate what coords from which to shoot the projectileType from vaus
	public void fire(float x, float y){
		
		logger.info("inside Projectile.fire(), shotsRemaining: " + shotsRemaining);
		
		if(shotsRemaining > 0){ // only fire if you have shots remaining
			if(!isAlive){ // only want to fire a new projectileType if there is currently not one on the screen
				isAlive = true; // set it to active, will remain alive until it hits a brick or the ceiling (see ContactProcessor)
				body.setActive(true); // start simulating it in the box2d world
				body.setTransform(x, y, 0); // start the projectileType from where vaus is currently located
				body.setLinearVelocity(0, speed); // make the projectileType start moving straight up
				shotsRemaining--; // decrement shot counter
			}	
		}
		
		onScreenDisplay.updateShots(shotsRemaining); // update the on screen display
	}
	
	
	
	// sets the type of projectile..
	public void setProjectileType(ProjectileTypes type){
		this.type = type;
	}
	
	
	
	/* (non-Javadoc)
	 * @see com.nate.game.GameObject#draw()
	 */
	@Override
	public void draw() {
		
		// draw textures for projectiles here
		
		
	}

	/* (non-Javadoc)
	 * @see com.nate.game.GameObject#update()
	 */
	@Override
	public void update() {
		body.setActive(isAlive); // isAlive is set elsewhere as needed during game play (mostly in ContactProcessor class)
		// vaus.update() is called from Arknoid1 main program
		// projectile.update() is called from vaus.update()
	}

	/* (non-Javadoc)
	 * @see com.nate.game.GameObject#stop()
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.nate.game.GameObject#start()
	 */
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.nate.game.GameObject#dispose()
	 */
	@Override
	public void dispose() {

	}

}
