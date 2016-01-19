/**
 * 
 */
package com.nate.game;

import static com.nate.game.Arknoid1.logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;

/**
 * @author ENNE EIGHT
 * This class implements all background music and sound.
 * All sounds are free-use as far as I can tell.
 * Most sounds were obtained from http://www.freesound.org.
 */
public class SoundProcessor {
	
	private String levelBackgroundMusic; // each level may have it's own unique background music
	private IntMap<Sound> soundEffects; // holds all the sound effects, not the background music

	enum SoundEffects { // every sound effect in the game is represented by this enum, at this time these sound effects are the same for every level
		// for testing purposes, a test sound file (FILENAME.wav) will be used, it is a funny sound..
		// libgdx Sound class specifies that max sound effect size is 'about 1 MB' uncompressed, not sure what's going on here, but it would not load a 450KB .wav file (possibly not related to file size.. a bit of a mystery at this point)
		// it simply would not load the  450kb file, it couldn't even find it, so I used a smaller file with the same name and it works fine
		
		BRICK_DESTROYED(1, "fx-brickdestroyed.wav"),
		BALL_BRICK_CONTACT(2, "fx-ballbrick.wav"),
		BALL_VAUS_CONTACT(3, "fx-ballvaus.wav"),
		BALL_FLIPPER_CONTACT(4, "FILENAME.wav"),
		LASER_BRICK_CONTACT(5, "FILENAME.wav"),
		LASER_CEILING_CONTACT(6, "FILENAME.wav"),
		BOMB_BRICK_CONTACT(7, "FILENAME.wav"),
		BOMB_CEILING_CONTACT(8, "FILENAME.wav"),
		BOMBGAME_OVER(9, "FILENAME.wav"),
		DEAD_BALL(10, "FILENAME.wav"),
		LEVEL_START(11, "FILENAME.wav"),
		LEVEL_END(12, "FILENAME.wav"),
		PLAYER_TAP(13, "FILENAME.wav"),
		FIRE_LASER(14, "FILENAME.wav"),
		FIRE_BOMB(15, "FILENAME.wav"),
		EXTRA_LIFE(16, "FILENAME.wav"),
		BONUS_ITEM_APPEARS(17, "FILENAME.wav"),
		BONUS_ITEM_OBTAINED(18, "FILENAME.wav"),
		FLIPPER_MOVE(19, "FILENAME.wav");
		
		
		private int id; // each enum value has a unique int tied to it, this int is the 'key' in soundEffects IntMap<>
		private String fileName;
		
		// enum constructor..
		SoundEffects(int i, String f){ 
			id = i;
			fileName = f;
		} 
	}
	
	private enum BackgroundMusic { // every background music in the game is represneted by this enum, each level can have it's own music, file name is read in from .json levelData file in LevelLoader
		MENU_SCREEN, GAME_START, LEVEL1, LEVEL2, LEVEL3, LEVEL4, LEVEL5;
	}
	
	// constructor..
	// pass in the file name of each level's background music
	public SoundProcessor(String s){
		
		levelBackgroundMusic = s; // file name for each level's background music is passed in from levelData.getBackgroundMusic() in Arknoid1 main class
		logger.info("inside SoundProcessor constructor, level background music file name is: " + s);
		
		soundEffects = new IntMap<Sound>();
		
		for(SoundEffects sfx : SoundEffects.values()) { // very cool: for-each loop for enums
			try { 
				soundEffects.put(sfx.id, Gdx.audio.newSound(Gdx.files.internal(sfx.fileName))); // fill the soundEffects IntMap<>
			}
			catch(GdxRuntimeException e) { 
				logger.info("  inside SoundProcessor constructor, unfortunately an exception occurred while loading sound effects files: " + e.getMessage());	
			}
		}
		
		logger.info("SoundProcessor constructor exiting..");
	}
	
	
	// play a sound effect: pass in the enum value of the effect you want to play, and pass in the volume you desire (0..1.0)
	// you have to set the volume very low for it to make a difference
	public void playSoundEffect(SoundEffects sfx, float v){
		soundEffects.get(sfx.id).play(v);
	}
	
	
	
	public void dispose(){
		for(Sound sfx : soundEffects.values()){
			sfx.dispose();
		}
	}
	
	
	
}
