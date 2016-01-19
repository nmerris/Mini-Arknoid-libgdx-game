/**
 * 
 */
package com.nate.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.utils.Logger;
import com.nate.game.ContactProcessor.ContactCategories;
import com.badlogic.gdx.Application.ApplicationType;

import static com.nate.game.Arknoid1.SCENE_HEIGHT;
import static com.nate.game.Arknoid1.SCENE_WIDTH;
import static com.nate.game.Arknoid1.SCREEN_TO_WORLD;
import static com.nate.game.Arknoid1.WORLD_TO_SCREEN;
import static com.nate.game.Arknoid1.logger;
import static com.nate.game.CreateBody.createBody;
import static com.nate.game.Arknoid1.batch;;
/**
 * @author natenator
 * This class implements all on screen display items, including the 'flip' and 'fire' buttons only used with Android
 */

public class OnScreenDisplay implements GameObject {
	
	// the onscreen coordinates of the fire and flipper buttons, only used on Android platforms
	// coordinates represented by these fields locate the bottom left corner of a square in which a button texture will be drawn
	// they are all in normal screen pixel units because that's what this class uses to draw all the fonts..
/*	static final float ANDROID_FIRE_BUTTON_X = 64f;
	static final float ANDROID_FIRE_BUTTON_Y = 220f;
	static final float ANDROID_FLIP_BUTTON_X = 1152f;
	static final float ANDROID_FLIP_BUTTON_Y = 220f;*/
	
	static final float ANDROID_FIRE_BUTTON_X = 64f;
	static final float ANDROID_FIRE_BUTTON_Y = 436f;
	static final float ANDROID_FLIP_BUTTON_X = 1152f;
	static final float ANDROID_FLIP_BUTTON_Y = 220f;
	

	private BitmapFont font;		// the font that will be drawn on screen while playing
	private Bricks bricks;			// need this to determine when level completed so we can draw congratulatory text on the screen
	private Ball ball;				// using this to stop the ball when you run out of lives, not so useful for final game
	private Vaus vaus;
	
	private int score;	
	private int lives;		
	private int shots;
	
	//private Body fireButtonBody, flipButtonBody; // only used on Android platforms, needed so user can tap an onscreen button to fire weapon or activate flippers, both are invisible to the box2d contact processing system, they are purely visual
	private Texture fireButtonTexture, flipButtonTexture; // the images for the Android fire and flip buttons
	static int screenButtonWidth; // width and height, in screen units, of Android buttons, which are square and both the same size, so only one dimension needed, static because Vaus class uses it to determine if button tapped, not final because it depends on the texture dimensions
	//private float box2dButtonWidth; // same as above but in scaled down box2d world units
	
	// constructor..
	public OnScreenDisplay(Bricks b, Ball ball, Vaus v, int lives) {
		
		bricks = b;
		this.ball = ball;
		vaus = v;
		font = new BitmapFont(Gdx.files.internal("osd1.fnt")); // load the font file
		font.setColor(Color.LIGHT_GRAY);
		logger.info("OnScreenDisplay constructor exiting..");
		score = 0;
		this.lives = lives;
		
		fireButtonTexture = new Texture(Gdx.files.internal("firebutton.png")); // load the Android button file textures, loaded every time but only used on Android platforms
		flipButtonTexture = new Texture(Gdx.files.internal("flipbutton.png"));
		screenButtonWidth = fireButtonTexture.getWidth(); // it does not matter which texture is used to get this width since both should have identical dimensions
		
		
	}
	
	
	@Override
	public void draw() {
		
		font.draw(batch, "Score: " + String.valueOf(score), 30f, SCENE_HEIGHT * SCREEN_TO_WORLD - 30f); // top left corner: score
		font.draw(batch, "Lives: " + String.valueOf(lives), SCENE_WIDTH * SCREEN_TO_WORLD - 150f, SCENE_HEIGHT * SCREEN_TO_WORLD - 30f); // top right corner: lives

		
		
		// temp text on game screen for play testing purposes, but only want it on non-Android platforms.. (otherwise too much crap at bottom of screen)
		// remove this for production
		if(!Arknoid1.appType.equals(ApplicationType.Android)){
			font.setColor(Color.SKY);
			font.draw(batch, "move: click left mouse, flippers: click right mouse, fire: space, pause ball: 'p'", 150f, 50f); // play instructions at bottom of screen centered
			font.setColor(Color.LIGHT_GRAY);
		}
		
		
		
		if(shots > 0){
			font.draw(batch, "Shots: " + String.valueOf(shots), 30f, 50f); // bottom left corner: number projectile shots remaining
			font.draw(batch, vaus.getProjectileName(), SCENE_WIDTH * SCREEN_TO_WORLD - 100f, 50f);
		}
		
		if(bricks.bricksRemaining <= 0){
			font.draw(batch, "CONGRATULATIONS! YOU KILLED ALL THE BRICKS!", 270f, 300f); // roughly centered
		}
		
		
		
		
		if(!Arknoid1.appType.equals(ApplicationType.Android) ){ // TESTING ONLY HERE
			
			batch.draw(fireButtonTexture, ANDROID_FIRE_BUTTON_X, ANDROID_FIRE_BUTTON_Y, screenButtonWidth, screenButtonWidth);
			batch.draw(flipButtonTexture, ANDROID_FLIP_BUTTON_X, ANDROID_FLIP_BUTTON_Y, screenButtonWidth, screenButtonWidth);
			
		}
		
/*		if(Arknoid1.appType.equals(ApplicationType.Android) ){ // if game is running on Android platform
			batch.draw(fireButtonTexture, ANDROID_FIRE_BUTTON_X, ANDROID_FIRE_BUTTON_Y, screenButtonWidth, screenButtonWidth); // draws rectangular texture with bottom left corner at ...X,...Y
			batch.draw(flipButtonTexture, ANDROID_FLIP_BUTTON_X, ANDROID_FLIP_BUTTON_Y, screenButtonWidth, screenButtonWidth);
		}
*/		
		
		
		if(lives <= 0){
			font.draw(batch, "YOU HAVE DIED!!", 520f, 400f);
			ball.stop(); // stop the ball, why not?  maybe then make the ball explode!!
		}
		
	}// end draw()

	public int getScore() {
		return score;
	}

	public void updateScore(int pointValue) { // point value can be negative
		score += pointValue;
	}

	public int getLives() {
		return lives;
	}

	public void updateLives(int lifeNumValue) { // life value can be negative
		lives += lifeNumValue;
	}
	
	public void updateShots(int numShots) {
		shots = numShots;
	}
	

	@Override
	public void update() {
		// TODO Auto-generated method stub

	}

	
	@Override
	public void dispose(){
		font.dispose();
		fireButtonTexture.dispose();
		flipButtonTexture.dispose();
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
