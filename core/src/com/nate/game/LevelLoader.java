/**
 * 
 */
package com.nate.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.gson.Gson;

import static com.nate.game.Arknoid1.logger;

import java.io.BufferedReader;
//import java.io.File;
import java.io.FileNotFoundException;
//import java.io.FileReader;
import java.io.IOException;




/**
 * @author Nathan Merris
 * This class handles all file read and write operations
 * All file IO is handled using Gson: level data is read in from a standard .json text file and converted to POJO's here
 * If I get to the level editor, Gson will also be used to convert POJO's back to .json text files
 * The idea is that one single JSON text file holds all info needed to create one single level of play
 * The file includes info such as: how many bricks, where each brick is, texture image file names (ie the walls may look different from on level to the next), etc
 */
public class LevelLoader {
	
	private Gson gson; // Gson is google's open source JSON to POJO and back library
	private LevelData levelData;
	
	
	// I wanted the .json text file to be as simple and straight forward as possible, so only the absolutely necessary data is in the file itself
	// this class will read the essential data in, and fill in everything else, thus the TempBrick inner class...
	// I may be able to get rid of TempBrick inner class..
	class TempBrick{ // the field names in this inner class EXACTLY match the property names in the .json file for each element in the "bricks" array part of the file
		float positionX;
		float positionY;
		String bonusItem;
		short toughness;
		boolean isIndestructible;
		String textureFile;
		short pointValue;
	}
	
	// a LevelData object (levelData) is populated via gson in the loadLevel method below..
	// everthing needed to create one level of play is stored in levelData
	class LevelData{ // the field names in this inner class EXACTLY match the property names in the .json file, which is just levelNumber and the bricks array right now
		int levelNumber;
		String levelBackgroundMusic;
		String ballTexture;
		String vausTexture;
		String leftWallTexture, rightWallTexture, ceilingTexture;
		
		Array<TempBrick> bricks = new Array<TempBrick>();
	}
	
	
	
	// constructor..
	public LevelLoader(){
		levelData = new LevelData();
		gson = new Gson(); // Gson is google's open source JSON to POJO and back library
		logger.info("LevelLoader constructor exiting..");
	}
	
	// getters..
	public String getBallTexture(){	return levelData.ballTexture; }
	public String getVausTexture(){ return levelData.vausTexture; }
	public String getLeftWallTexture(){ return levelData.leftWallTexture; }
	public String getRightWallTexture(){ return levelData.rightWallTexture; }
	public String getCeilingTexture(){ return levelData.ceilingTexture; }
	public String getBackgroundMusic(){ return levelData.levelBackgroundMusic; }
	
	
	/**
	 * loadBricks should be called from Arknoid1 main program: it returns a ready-to-go Array of bricks to pass to a single level's Bricks object
	 * This method fills and returns an array with Brick objects.
	 * I want to keep the .json level data file as simple as possible, so only the essential brick stuff is in the file, 
	 * this method fills in all the rest and gets the array ready for Bricks to process.
	 * @return An array of Brick objects ready for Bricks class to use, just pass the whole array to the Bricks constructor.
	 */
	public Array<Brick> loadBricks(){
		
		Array<Brick> bricks = new Array<Brick>(); // this will be returned and should be passed to Bricks in Arknoid1
		int id = 0; // each brick has it's own unique ID
		BonusItem.ItemTypes bonusItem; // each brick may contain a bonus item
		
		for(TempBrick b: levelData.bricks){// loop through the simple bricks in the array in levelData
			id++;
			// set the bonusItem.. using if-else ladder for efficiency: most bricks will not have a bonus item (faster than a switch?)
			if(b.bonusItem.equals("none")){ bonusItem = BonusItem.ItemTypes.NO_ITEM; }
			else if(b.bonusItem.equals("laser")){ bonusItem = BonusItem.ItemTypes.LASER; }
			else if(b.bonusItem.equals("bomb")){ bonusItem = BonusItem.ItemTypes.BOMB; }
			else if(b.bonusItem.equals("extralife")){ bonusItem = BonusItem.ItemTypes.EXTRA_LIFE; }
			else { bonusItem = BonusItem.ItemTypes.NO_ITEM; } // Java requires bonusItem to be initialized, this should never be reached unless the .json file is corrupt
			
			// id simply increments for each simple brick in levelData.bricks
			// 5th parameter in Brick constructor is isAlive, all bricks start alive so just set to true for every brick
			bricks.add(new Brick(id, b.pointValue, b.toughness, b.isIndestructible, true, b.textureFile, bonusItem, b.positionX, b.positionY));
			
		}
		
		return bricks;
		
		
	}
	
	
	/**
	 * loadLevel should be called once at the beginning of each level
	 * this method reads in all the level data from a .json file and converts it to POJO's used in this game elsewhere
	 * if you pass in a levelNumber that does not have a matching .json file, an error will occur
	 * @param levelNumber the integer value representing the level to load, file MUST be named as follows: "level-XYZ.json" where XYZ = 001 for level 1, XYZ = 123 for level 123, etc
	 */
	public void loadLevel(int levelNumber){
		logger.info("just entered LevelLoader.loadLevel, about to read in level data from .json file for level: " + levelNumber);
		
		String levelToLoad = "level-" + levelNumber + ".json";
		logger.info("  levelToLoad String is: " + levelToLoad);
		

		try (BufferedReader reader = new BufferedReader(Gdx.files.internal(levelToLoad).reader())) { // try with resources so it closes file automatically
            levelData = gson.fromJson(reader, LevelData.class); // read in from file and populate a LevelData object
            
    		logger.info("  level data was read in from file name: " + levelToLoad);
    		logger.info("  levelData.bricks.size after reading in from file is: " + levelData.bricks.size);

		}
		catch (GdxRuntimeException e){ // libgdx customer exception
			logger.info("  unfortunately there was an exception, GdxRuntimeException was: " + e.getMessage());
		}
		catch (FileNotFoundException e) { // standard Java exception
			logger.info("  unfortunately the file was not found, FileNotFoundException was: " + e.getMessage());
        }
		catch (IOException e) { // standard Java excpetion
			logger.info("  unfortunately an IO exception occurred, IOException was: " + e.getMessage());
        }
		
	}// end loadLevel
	
	
	
	
}
