/**
 * 
 */
package com.nate.game;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
//import com.badlogic.gdx.physics.box2d.Shape;
import com.nate.game.ContactProcessor.ContactCategories;
//import com.nate.game.ContactProcessor.GameBodies;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

import static com.nate.game.Arknoid1.box2dWorld; // use the same box2d physics world for everything
/**
 * @author natenator
 *
 */

// TODO: update Ball.java and Walls.java to use this method instead of doing it sloppily inside each class
// TODO: it would be great to have an overloaded creatPolygon method to be able to create the ball body (so it would use a circle instead of a square)
public class CreateBody {
	
	// createBody creates a rectangular body to use in the box2d physics world..
	// it's in it's own class to keep things organized
	// constructor 1 of 2..
	public static Body createBody(BodyType type, 						// body type
			boolean isSensor, ContactCategories sensorCategory, short sensorMask, 	// used with collision filtering
			float gravScale, GameBody<?> ob, 							// gravity, customer userData
			float x, float y, 											// initial x and y coords
			float d, float r, float f, 									// density, restitution (bounce), friction
			float halfwidth, float halfheight) {						// dimensions of fixtureDef rectangle
		
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = type;
		bodyDef.position.set(x, y);
		bodyDef.angle = 0;
		Body square = box2dWorld.createBody(bodyDef);
		
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.isSensor = isSensor;
		fixtureDef.filter.categoryBits = sensorCategory.getCategory();
		fixtureDef.filter.maskBits = sensorMask;
 		fixtureDef.density = d;
 		fixtureDef.restitution = r;
 		fixtureDef.friction = f;
 		fixtureDef.shape = new PolygonShape();
 		((PolygonShape) fixtureDef.shape).setAsBox(halfwidth, halfheight);
 		
 		square.createFixture(fixtureDef); // this creates the body
 		square.setGravityScale(gravScale);
 		square.setUserData(ob);
		fixtureDef.shape.dispose();
	
		return square;
	}
	
	// overloaded constructor, has one less parameter, used to create a circle
	// constructor 2 of 2..
	public static Body createBody(BodyType type, 					// body type
			boolean isSensor, ContactCategories sensorCategory, short sensorMask, 	// used with collision filtering
			float gravScale, GameBody<?> ob, 							// gravity, customer userData
			float x, float y, 											// initial x and y coords
			float d, float r, float f, 									// density, restitution (bounce), friction
			float radius) {												// radius of circle body to be created here
		
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = type;
		bodyDef.position.set(x, y);
		bodyDef.angle = 0;
		Body circle = box2dWorld.createBody(bodyDef);
		
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.isSensor = isSensor;
		fixtureDef.filter.categoryBits = sensorCategory.getCategory();
		fixtureDef.filter.maskBits = sensorMask;
 		fixtureDef.density = d;
 		fixtureDef.restitution = r;
 		fixtureDef.friction = f;
 		fixtureDef.shape = new CircleShape();
 		((CircleShape) fixtureDef.shape).setRadius(radius);
 		
 		circle.createFixture(fixtureDef); // this creates the body
 		circle.setGravityScale(gravScale);
 		circle.setUserData(ob);
		fixtureDef.shape.dispose();
	
		return circle;
	}
	
	

}
