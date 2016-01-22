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
import static com.nate.game.Arknoid1.SCREEN_HEIGHT;
import static com.nate.game.Arknoid1.SCREEN_WIDTH;
import static com.nate.game.Arknoid1.WORLD_TO_SCREEN;
import static com.nate.game.Arknoid1.SCREEN_TO_WORLD;
import static com.nate.game.Arknoid1.logger;
import static com.nate.game.CreateBody.createBody;
import static com.nate.game.Arknoid1.batch;
import static com.nate.game.Walls.box2dWallWidth;

/**
 * @author natenator
 * This class implements all on screen display items, including the 'flip' and 'fire' buttons only used with Android
 */

public class OnScreenDisplay implements GameObject {
	
	// the onscreen coordinates of the fire and flipper buttons, only used on Android platforms
	// coordinates represented by these fields locate the top left corner of a square in which a button texture will be drawn
	// they are all in normal screen pixel units because that's what this class uses to draw all the fonts..
	// it does not matter what screen resolution is being used.. translation from different screen resolutions (and therefore coordinates) is taken care of by camera.unproject as needed in Vaus.touchdown method (just like for desktops)
/*	static final float ANDROID_FIRE_BUTTON_X = 64f;
	static final float ANDROID_FIRE_BUTTON_Y = 220f;
	static final float ANDROID_FLIP_BUTTON_X = 1152f;
	static final float ANDROID_FLIP_BUTTON_Y = 220f;*/
	
	// the following 5 fields locate both the buttons on the screen on Android devices, X and Y coords for both buttons are calculated from wall width and button texture width (buttons must always be square)
	static final float OSD_BUTTON_PADDING = 32; // the distance past the wall the Android buttons will be located onscreen
	static float androidFireButtonX; // fire button top left corner X coordinate, these coords are used in Vaus.touchDown to determine if an Android platform user has tapped a button
	static float androidFireButtonY; // flip button top left corner Y
	static float androidFlipButtonX; // flip button X
	static float androidFlipButtonY; // flip button Y
	
	private BitmapFont font;		// the font that will be drawn on screen while playing
	private Bricks bricks;			// need this to determine when level completed so we can draw congratulatory text on the screen
	private Ball ball;				// using this to stop the ball when you run out of lives, not so useful for final game
	private Vaus vaus;
	
	private int score; // exactly what you think it is
	private int lives;		
	private int shots; // how many shots user has left, when it gets to zero, both the shot counter and weapon name are removed from the screen
	
	private Texture fireButtonTexture, flipButtonTexture; // the images for the Android fire and flip buttons
	static int screenButtonWidth; // width and height, in screen units, of Android buttons, which are square and both the same size, so only one dimension needed, static because Vaus class uses it to determine if button tapped, not final because it depends on the texture dimensions
	
	
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
		
		androidFireButtonX = box2dWallWidth + OSD_BUTTON_PADDING;										// fire button is a bit in from left edge of screen past the left wall
		androidFireButtonY = (SCREEN_HEIGHT - screenButtonWidth) * 0.5f;								// set the button Y coordinate to half the screen height - half the button height/width (it's square), because it's drawn from top left corner
		androidFlipButtonX = SCREEN_WIDTH - box2dWallWidth - OSD_BUTTON_PADDING - screenButtonWidth; 	// flip button is a bit in from right edge of screen before the right wall
		androidFlipButtonY = androidFireButtonY; 														// both buttons are at the same elevation, using this additional variable for clarity
		
	}
	
	
	@Override
	public void draw() {
		
		font.draw(batch, "Score: " + String.valueOf(score), 30f, SCENE_HEIGHT * WORLD_TO_SCREEN - 30f); // top left corner: score
		font.draw(batch, "Lives: " + String.valueOf(lives), SCENE_WIDTH * WORLD_TO_SCREEN - 150f, SCENE_HEIGHT * WORLD_TO_SCREEN - 30f); // top right corner: lives

		
		
		// temp text on game screen for play testing purposes, but only want it on non-Android platforms.. (otherwise too much crap at bottom of screen)
		// remove this for production
		if(!Arknoid1.appType.equals(ApplicationType.Android)){
			font.setColor(Color.SKY);
			font.draw(batch, "move: click left mouse, flippers: click right mouse, fire: space, pause ball: 'p'", 150f, 50f); // play instructions at bottom of screen centered
			font.setColor(Color.LIGHT_GRAY);
		}
		
		
		
		if(shots > 0){
			font.draw(batch, "Shots: " + String.valueOf(shots), 30f, 50f); // bottom left corner: number projectile shots remaining
			font.draw(batch, vaus.getProjectileName(), SCENE_WIDTH * WORLD_TO_SCREEN - 100f, 50f);
		}
		
		if(bricks.bricksRemaining <= 0){
			font.draw(batch, "CONGRATULATIONS! YOU KILLED ALL THE BRICKS!", 270f, 300f); // roughly centered
		}
		
		
		
		
/*		if(!Arknoid1.appType.equals(ApplicationType.Android) ){ // TESTING ONLY HERE
			
			batch.draw(fireButtonTexture, ANDROID_FIRE_BUTTON_X, ANDROID_FIRE_BUTTON_Y, screenButtonWidth, screenButtonWidth);
			batch.draw(flipButtonTexture, ANDROID_FLIP_BUTTON_X, ANDROID_FLIP_BUTTON_Y, screenButtonWidth, screenButtonWidth);
			
		}*/
		
		if(Arknoid1.appType.equals(ApplicationType.Android) ){ // if game is running on Android platform
			batch.draw(fireButtonTexture, androidFireButtonX, androidFireButtonY, screenButtonWidth, screenButtonWidth); // draws rectangular texture with bottom left corner at ...X,...Y
			batch.draw(flipButtonTexture, androidFlipButtonX, androidFlipButtonY, screenButtonWidth, screenButtonWidth);
		}
		
		
		
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
