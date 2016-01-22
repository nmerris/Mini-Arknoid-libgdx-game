package com.nate.game;


import static com.nate.game.Arknoid1.logger;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.nate.game.DebugGraphics;

public class Arknoid1 implements ApplicationListener {
	
	// this game uses 2 coordinate systems: one for the box2d physics engine, on for screen pixels
	// the coords used with SCENE_WITDH and SCENE_HEIGHT are for use with the box2d physics engine
	// box2d hits a floating point arithmetical limit if you use 'large' numbers.. large in this case would be 1280 or 720.. basically need to keep things really small
	// other parts of the game need to use actual screen pixel coords
	// use SCREEN_TO_WORLD and WORLD_TO_SCREEN to convert between the two as necessary
	public static final float SCENE_WIDTH = 12.80f; // box2d world width
	public static final float SCENE_HEIGHT = 7.20f; // box2d world height
	public static final float SCREEN_TO_WORLD = 1.0f / 100.0f; // ratio is 1:100
	public static final float WORLD_TO_SCREEN = 1.0f / SCREEN_TO_WORLD; // ratio is 100:1
	public static final int SCREEN_WIDTH = (int) (SCENE_WIDTH * WORLD_TO_SCREEN); // screen width (always will be 1280 pixels wide) differing screen resolutions are handled elsewhere
	public static final int SCREEN_HEIGHT = (int) (SCENE_HEIGHT * WORLD_TO_SCREEN); // screen width (always will be 720 pixels high)
	
	static ApplicationType appType; // need to know if program is running on Android or other b/c Android needs clickable buttons on screen, but everything else uses keyboard and mouse for player control
	
	// the following are common to all game objects..
	static Viewport viewport; // this is what you see on your screen, this viewport is for the game physics world and uses smaller box2d dimensions
	Viewport OSDviewport; // this is only for the on screen display, it uses larger screen pixel dimensions
	Camera camera;
	Camera OSDcamera;
	static SpriteBatch batch;
	static TextureAtlas atlas;
	static Logger logger; // used to output text to console
	static OnScreenDisplay onScreenDisplay; // used to draw on screen info such as live, score, num shots remaining, etc
	
	
	
	private Sprite background;
	//private Vector3 tempVector3; // used to track user input to move vaus

	
	private DebugGraphics debugGraphics; // a DebugGraphics object is used to display debug graphics on screen
	private boolean displayDebugGrid = true; // set to true to display a grid overlay on the game screen
	
	private Walls walls;
	//private String ceilingTextureRegion, wallsTextureRegion; // points to the walls (same texture left/right) and ceiling texture in the texture atlas	
	private Ball ball1;
	private String texturePack; // holds all images for one level, currently only one level
	//private String ball1TextureRegion; // points to the ball texture in the texture atlas
	private Vaus vaus1;
	//private String vaus1TextureRegion; // points to the vaus texture in the texture atlas
	private Bricks bricks;
	private String brickDataFile; // holds all the brick layout/position/etc info
	private ContactProcessor contactProcessor; // deals with all box2dWorld contact logic that is not automatically done by the box2d physics engine itself
	private SoundProcessor sounds; // all sounds effects and background music for a single level of play are handled by a SoundProcessor object
	
	
	private LevelLoader levelLoader; // one fileHandler object deals with all file IO, used to read in level data from a .json file
	
	
	// box2dWorld is common to all game elements that use physics
	// box2dDebugRenderer is only need in this class for debug purposes, should be removed for production
	// box2dWorld is passed to other classes through their respective constructors, just like camera, logger, texture atlas, etc
	// ball, Vaus, bricks, etc all have their own additional box2d implementations
	private Box2DDebugRenderer box2dDebugRenderer; // remove for production
	public static World box2dWorld; // the world all the box2d physics elements live in
	
	
	@Override
	public void create () { // called by libgdx engine at game start
			
		brickDataFile = "WHATEVER.TXT IMPLEMENT ME"; // this will need to change each level
		texturePack = "texturepack.atlas"; // this will need to change each level
		
		logger = new Logger("Arknoidian logger", Logger.INFO); // log stuff to the console to help debug
		
		// viewport/camera stuff..
		camera = new OrthographicCamera();
		OSDcamera = new OrthographicCamera();
		viewport = new StretchViewport(SCENE_WIDTH, SCENE_HEIGHT, camera); 
		OSDviewport = new StretchViewport(SCENE_WIDTH * WORLD_TO_SCREEN, SCENE_HEIGHT * WORLD_TO_SCREEN, OSDcamera);
		camera.position.set(SCENE_WIDTH * 0.5f,  SCENE_HEIGHT * 0.5f, 0);
		OSDcamera.position.set(SCENE_WIDTH * WORLD_TO_SCREEN * 0.5f, SCENE_HEIGHT * WORLD_TO_SCREEN * 0.5f, 0);
		
		
		// batch and texture stuff..
		batch = new SpriteBatch();
		// load the background image Sprite from the texture atlas..
		// atlas is assigned here and passed to all the other objects that use it
		atlas = new TextureAtlas(Gdx.files.internal(texturePack));
		
		// debug for graphics..
		debugGraphics = new DebugGraphics(viewport, logger, displayDebugGrid); // remove for production
		// debug renderer for box2d, remove for production..
		box2dDebugRenderer = new Box2DDebugRenderer(
				true, /* draw bodies */
				true, /* don't draw joints */
				true, /* draw aabbs */
				true, /* draw inactive bodies */
				true, /* draw velocities */
				true /* draw contacts */);
		

		// create physics World using box2d..
		box2dWorld = new World(new Vector2(0, -9.8f), true);

		
		
		
		// create the background..
		// TODO: move all the background stuff to it's own class, for clarity and consistency
		background = new Sprite(atlas.findRegion("background"));
		background.setCenter(0, 0); // centers the background
		
		// create all the other non-background objects..
		// the order of the object instantiation matters here or you get a null pointer exception..
		levelLoader = new LevelLoader();
		// it would be cool to have the level load in a separate thread..
		levelLoader.loadLevel(999); // load level data for level X, everything needed to construct a level retrieved here
		
		sounds = new SoundProcessor (levelLoader.getBackgroundMusic()); // the only audio component that changes per level is the background music, all soundfx are fixed (for now)
		ball1 = new Ball (levelLoader.getBallTexture()); // each level may have it's own unique ball texture
		vaus1 = new Vaus (levelLoader.getVausTexture(), ball1); // each level may have it's own unique vaus texture
		bricks = new Bricks (levelLoader.loadBricks()); // pass a single level's bricks Array to Bricks object
		walls = new Walls (levelLoader.getLeftWallTexture(), levelLoader.getRightWallTexture(), levelLoader.getCeilingTexture()); // each level may have it's own unique wall and ceiling textures
		onScreenDisplay = new OnScreenDisplay(bricks, ball1, vaus1, 180); // start with x lives
		contactProcessor = new ContactProcessor(bricks, sounds); // contactProcessor needs access to the bricks Array and game sounds
		

		
		appType = Gdx.app.getType();
		logger.info("###################### Game is running on: " + appType.toString() + " ######################");
		
		
		logger.info("exiting create() in Arknoid1..");
	} // end create()
	

	
	
	
	
	
	// TODO: got to make everything back so it uses one texture atlas.. need to update that in ALL the draw methods!
	
	@Override
	public void render () {

		// much of the 'updating' is done automatically by the box2d physics engine,
		// but still some custom updating must be done, thus the following methods..
		ball1.update();
		vaus1.update();
		bricks.update();
		
		// clear the screen..
		Gdx.gl.glClearColor(1, 0.5f, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		box2dWorld.step(1/60f, 6, 2); // this is the physics update timestep, not clear about how this works at this time
		
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		
		batch.begin();	
		//background.draw(batch); // background implementation is in this class
		walls.draw();
		ball1.draw();
		vaus1.draw();
		bricks.draw();
		//bonusItems.draw();
		batch.end();
		
		//switch to the OSDcamera to draw the static text on screen (score, lives, etc)..
		OSDcamera.update();
		batch.setProjectionMatrix(OSDcamera.combined);
		batch.begin();
		onScreenDisplay.draw();
		batch.end();
	
		
		// drawDebugGraphics shows useful screen debug graphics like grids, remove for production
		// the only thing implemented right now is the grid, add bounding rectangles?
		//debugGraphics.drawDebugGraphics();
		
		// box2dDebugRenderer shows outlines of the box2d bodies ..
		box2dDebugRenderer.render(box2dWorld, viewport.getCamera().combined);
		
	}


	// resize is called when user resizes window, also once after create() method exits..
	// won't render correctly (or at all) if you don't update the viewports here, even if you don't need to resize the screen
	@Override
	public void resize(int width, int height) {
		viewport.update(width, height); // resize the game world viewport
		OSDviewport.update(width, height); // resize the on screen display viewport
		
	}

	@Override
	public void pause() {
		logger.info("inside pause() in Arknoid1");
		
	}

	@Override
	public void resume() {
		logger.info("exiting resume() in Arknoid1");
		
	}

	@Override
	public void dispose() {
		batch.dispose();
		atlas.dispose();
		debugGraphics.dispose(); // testing only: remove this for production
		box2dDebugRenderer.dispose(); // testing only: remove this for production
		box2dWorld.dispose();
		walls.dispose();
		vaus1.dispose();
		ball1.dispose();
		bricks.dispose(); // nothing happening here, may not need it
		onScreenDisplay.dispose();
		sounds.dispose();
		logger.info("exiting dispose() in Arknoid1..");
	}

}
