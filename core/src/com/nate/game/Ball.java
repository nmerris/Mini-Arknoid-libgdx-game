/**
 * 
 */
package com.nate.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.nate.game.ContactProcessor.ContactCategories;

import static com.nate.game.Arknoid1.SCENE_HEIGHT;
import static com.nate.game.Arknoid1.SCENE_WIDTH;
import static com.nate.game.Vaus.VAUS_ELEVATION;
import static com.nate.game.Arknoid1.SCREEN_TO_WORLD;
import static com.nate.game.CreateBody.createBody;
import static com.nate.game.Walls.box2dWallWidth;
import static com.nate.game.Arknoid1.batch;
import static com.nate.game.Arknoid1.logger;

/**
 * @author natenator
 *
 */
public class Ball implements GameObject {

	private int power; 						// power of ball
	public static float ballSpeed; 			// how fast the ball moves
	private float startingVx, startingVy;
	public static float ballMaxVx, ballMaxVy, ballMinVx, ballMinVy; // the max x and y components of the ball's velocity vector, need this to avoid ball going too close to completely vertical or horizontal
	public static float ballVxAtMaxVy, ballVyAtMaxVx, ballVyAtMinVx, ballVxAtMinVy;
	private boolean paused; 				// if true only ball does not move
	private Texture ballTex;				// texture to draw the ball in the draw() method
	public static Body ballBody; 			// it's static so that Vaus.java can conveniently modify the ball's velocity without having to create getter and setter methods
	public static float box2dBallRadius;
	
	// constructor..
	public Ball(String textureFileName){ // the texture file name string is read passed in from Arknoid1 main program when level starts
				
		// think of a right triangle: the hypotenuse represents the ball's travel vector
		// the hypotenuse can be broken down to it's x and y components, call them Vx and Vy, using the  Pythagorean theorem
		// the length of the hypotenuse represents the speed of the ball, and should never change regardless of the direction the ball is heading
		// so essentially ballSpeed = hypotenuse.. since we are dealing with right triangles, we can use basic geometry to
		// manipulate the ball's Vx and Vy, whilst keeping the hypotenuse (aka ballSpeed) the SAME
		// this is used in the ContactProcessor class to alter the bounce-off angle of the ball depending on it's direction of travel and where on Vaus the ball hits
		// but for this game, we don't want the ball to ever travel perfectly vertically or horizontally,
		// ballMaxVx and ballMaxVy set the bounds for how close to perfectly vert/horiz the ball may ever travel
		ballSpeed = 4.0f;
		//ballMaxVx = /*ballSpeed * 0.98f*/ 10.0f;
		//ballMaxVy = /*ballSpeed * 0.98f*/ 10.0f;
		ballMaxVx = ballSpeed * 0.97f;
		ballMaxVy = ballSpeed * 0.97f;
		ballMinVx = ballSpeed * 0.15f;
		ballMinVy = ballSpeed * 0.15f;
		ballVxAtMaxVy = (float)Math.sqrt(ballSpeed * ballSpeed - ballMaxVy * ballMaxVy);
		ballVyAtMaxVx = (float)Math.sqrt(ballSpeed * ballSpeed - ballMaxVx * ballMaxVx);
		ballVyAtMinVx = (float)Math.sqrt(ballSpeed * ballSpeed - ballMinVx * ballMinVx);
		ballVxAtMinVy = (float)Math.sqrt(ballSpeed * ballSpeed - ballMinVy * ballMinVy);
		
		
	
		startingVx = 1.85f; // the starting ball velocity vector x component ***MUST NOT BE > ballSpeed***
		startingVy = -(float)Math.sqrt(ballSpeed * ballSpeed - startingVx * startingVx);
		
		logger.info("ballSpeed = " + ballSpeed);
		logger.info("ballMaxVx = ballMaxVy = " + ballMaxVx);
		logger.info("startingVx = " + startingVx + " and startingVy = " + startingVy);
		if(startingVx >= ballMaxVx){ logger.info("starting ball Vx exceeded ballMaxVx!!"); }
		if(startingVy >= ballMaxVy){ logger.info("starting ball Vy exceeded ballMaxVy!!"); }
		
		power = 1;
	
		// load the texture..
		ballTex = new Texture(Gdx.files.internal(textureFileName));
		box2dBallRadius = ballTex.getWidth() * SCREEN_TO_WORLD * 0.5f;
		
		// using overloaded createBody constructor for circular body creation..
		ballBody = createBody(BodyType.DynamicBody, 						// body type
				 false, ContactCategories.BALL, (short)~0x0000,				// is body a sensor?, sensor group bits, mask bits
				 0, new GameBody<Ball>(this), 								// gravity scale, userData
				 SCENE_WIDTH * 0.5f, Vaus.VAUS_ELEVATION * 4.5f, 					// initial x and y positions
				 1.0f, 1.0f, 0.0f, 											// density, restitution, friction
				 box2dBallRadius); 											// radius of circular body to be created
		
		ballBody.setLinearVelocity(startingVx, startingVy); // make the ball start moving
	
		logger.info("Ball constructor exiting..");
	}
	
	@Override
	public void update(){
		
		// for some reason, box2d is not detecting when the ball collides with the wall or ceiling when
		// the ball hits the wall/ceiling at too shallow an angle, I feel this is a glitch in the physics engine
		// although I am abusing it somewhat by forcing things to have zero friction and gravity.. m.e.h.
		// so to work around it, I am just manually changing the ball velocity vector as needed..
		// won't work without the 1.1 multiplier
		// this seems too computationally expensive... would be better to figure out how to make this happen inside the ContactProcessor class..
		// TODO put all this in ContactProcessor class
		if(ballBody.getPosition().x >= SCENE_WIDTH - box2dWallWidth - box2dBallRadius * 1.1f){ // ball hit right wall
			ballBody.setLinearVelocity(-ballBody.getLinearVelocity().x, ballBody.getLinearVelocity().y); // reverse only the x direction when bouncing off either wall
		}
		else if(ballBody.getPosition().x <= box2dWallWidth + box2dBallRadius * 1.1f){ // ball hit left wall
			ballBody.setLinearVelocity(-ballBody.getLinearVelocity().x, ballBody.getLinearVelocity().y); // reverse x component
		}
		else if(ballBody.getPosition().y >= SCENE_HEIGHT - box2dWallWidth - box2dBallRadius * 1.1f){ // ball hit ceiling
			ballBody.setLinearVelocity(ballBody.getLinearVelocity().x, -ballBody.getLinearVelocity().y); // reverse y component
		}
		else if(ballBody.getPosition().y <= box2dWallWidth + box2dBallRadius * 1.1f){ // ball hit floor
			ballBody.setLinearVelocity(ballBody.getLinearVelocity().x, -ballBody.getLinearVelocity().y); // reverse y component
		}
		
	}
	
	@Override
	public void draw(){
		
		batch.draw(ballTex, // texture
				   ballBody.getPosition().x - box2dBallRadius, // bottom left x
				   ballBody.getPosition().y - box2dBallRadius, // bottom left y
				   box2dBallRadius, box2dBallRadius, // originX, originY (for translations)
				   box2dBallRadius * 2.0f, box2dBallRadius * 2.0f, // width, height
				   1f, 1f, // scaleX, scaleY
				   0, // rotate, 0 means do not rotate
				   0, 0, // srcX, srcY (?)
				   ballTex.getWidth(), ballTex.getHeight(), // srcWidth, srcHeight
				   false, false // flipX, flipY
				   );
		
	}
	
	float tempVx, tempVy; // only used in stop() and start()..
	@Override
	public void stop() {
		tempVx = ballBody.getLinearVelocity().x;
		tempVy = ballBody.getLinearVelocity().y;
		ballBody.setLinearVelocity(0, 0); // stop the ball instantly
	}
	
	@Override
	public void start(){
		ballBody.setLinearVelocity(tempVx, tempVy); // resume with the same velocity before being stopped
	}
	
	@Override
	public void dispose(){
		ballTex.dispose();
		logger.info("Ball dispose() method exiting..");
	}



	
}
