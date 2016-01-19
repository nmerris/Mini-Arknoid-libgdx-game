/**
 * 
 */
package com.nate.game;

/**
 * @author natenator
 *
 */
public class GameBody<T> { // horray for generics!
	
	public static enum GameBodies{ // a list of all the different types of bodies in the box2d world
		VAUS, FLIPPER, BALL, WALL, BRICK, PROJECTILE, BONUS_ITEM, BOMB_BLAST, ANDROID_BUTTON
	}
	private GameBodies bodyType; // each GameBody object has it's own type, used in collision detection
	private T specificBodyObject; // each GameBody has it's own unique object that contains ALL the data needed for game play processing
	// for example, each Brick body has it's own toughness and bonus item
	
	// constructor..
	public GameBody(T object){
		specificBodyObject = object;
		String bodyClass = object.getClass().getName();
		switch(bodyClass){
			case "com.nate.game.Ball":
				bodyType = GameBodies.BALL;
				break;
			case "com.nate.game.Brick":
				bodyType = GameBodies.BRICK;
				break;
			case "com.nate.game.Vaus":
				bodyType = GameBodies.VAUS;
				break;
			case "com.nate.game.Walls":
				bodyType = GameBodies.WALL;
				break;
			case "com.nate.game.Projectile":
				bodyType = GameBodies.PROJECTILE;
				break;
			case "com.nate.game.BonusItem":
				bodyType = GameBodies.BONUS_ITEM;
				break;
			case "com.nate.game.OnScreenDisplay":
				bodyType = GameBodies.ANDROID_BUTTON;
			default:
				break;
		}
	}
	
	public T getSpecificBodyObject(){
		return specificBodyObject;
	}
	
	
	public GameBodies getType(){
		return bodyType;
	}
	
}
