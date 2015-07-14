package incanshift;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBox2dShape;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;

public class GameScreen extends AbstractScreen implements Screen {

	String tag = "GameScreen";
	private Player player;

	private ModelBatch modelBatch;
	private AssetManager assets;
	private Music music;

	// Lights and stuff
	private Environment environment;
	private ShaderProgram shaderSun;
	private Texture sunTexture;
	private Vector3 sunPosition;
	private Vector3 sunPositionProj;
	private float sunRadius;
	private ShapeRenderer shapeRenderer;
	private ModelInstance skybox;
	private Matrix4 uiMatrix;

	// Game objects
	private Array<GameObject> instances;
	private ArrayMap<String, GameObject.Constructor> gameObjectFactory;

	// Collision
	private CollisionHandler collisionHandler;

	// Gun positioning
	private GameObject gun;
	private Matrix4 gunBaseTransform = new Matrix4();
	private Vector3 gunFrontBackPosition = new Vector3();
	private Vector3 gunLeftRightPosition = new Vector3();
	private Vector3 gunUpDownPosition = new Vector3();

	private String msg = new String();

	private Vector3 lastCameraDirection = new Vector3();
	private Vector3 screenCenter = new Vector3();

	public GameScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight);

		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		// Load game assets
		assets = new AssetManager();
		assets.load("model/temple.g3db", Model.class);
		assets.load("model/ground.g3db", Model.class);
		assets.load("model/skybox.g3db", Model.class);
		assets.load("model/level.g3db", Model.class);
		assets.load("model/gun.g3db", Model.class);
		assets.load("model/sphere.g3db", Model.class);
		assets.load("model/pillar.g3db", Model.class);
		assets.load("model/crate.g3db", Model.class);
		assets.load("model/test_scene.g3db", Model.class);

		assets.load("sound/jump.wav", Sound.class);
		assets.load("sound/shatter.wav", Sound.class);
		assets.load("sound/shoot.wav", Sound.class);
		assets.load("sound/run.wav", Sound.class);
		assets.load("sound/walk.wav", Sound.class);
		assets.load("sound/climb.wav", Sound.class);
		assets.load("sound/music_game.ogg", Music.class);

		// Collision handler
		Bullet.init();
		instances = new Array<GameObject>();
		collisionHandler = new CollisionHandler(instances);

		// Various environment graphics stuff
		shapeRenderer = new ShapeRenderer();
		environment = new Environment();
		sunTexture = new Texture(512, 512, Format.RGBA8888);
		sunPosition = new Vector3(500, 1200, 700);
		sunPositionProj = sunPosition.cpy();
		sunRadius = 500000f;
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f,
				0.3f, 0.3f, 1f));
		environment.add(new DirectionalLight().set(Color.WHITE,
				sunPosition.scl(-1)));
		loadShaders();

		// Create game instances
		modelBatch = new ModelBatch();
		assets.finishLoading();

		gameObjectFactory = new ArrayMap<String, GameObject.Constructor>();

		Model modelTemple = assets.get("model/temple.g3db", Model.class);
		gameObjectFactory.put("temple", new GameObject.Constructor(modelTemple,
				Bullet.obtainStaticNodeShape(modelTemple.nodes), 0));
		// instances.add(gameObjectFactory.get("temple").construct());
		Gdx.app.debug(tag, "loaded temple");

		Model modelGround = assets.get("model/ground.g3db", Model.class);
		gameObjectFactory.put("ground", new GameObject.Constructor(modelGround,
				Bullet.obtainStaticNodeShape(modelGround.nodes), 0));
		instances.add(gameObjectFactory.get("ground").construct());
		Gdx.app.debug(tag, "loaded ground");

		Model modelLevel = assets.get("model/level.g3db", Model.class);
		gameObjectFactory.put("level", new GameObject.Constructor(modelLevel,
				Bullet.obtainStaticNodeShape(modelLevel.nodes), 0));
		instances.add(gameObjectFactory.get("level").construct());
		Gdx.app.debug(tag, "loaded level");

		Model modelTestScene = assets.get("model/test_scene.g3db", Model.class);
		gameObjectFactory.put(
				"test_scene",
				new GameObject.Constructor(modelTestScene, Bullet
						.obtainStaticNodeShape(modelTestScene.nodes), 0));
		GameObject testscene = gameObjectFactory.get("test_scene").construct();
		testscene.position(-30, 0, -30);
		instances.add(testscene);
		Gdx.app.debug(tag, "loaded test_scene");

		Model modelSkybox = assets.get("model/skybox.g3db", Model.class);
		skybox = new ModelInstance(modelSkybox);
		Gdx.app.debug(tag, "loaded skybox");

		// Add a pillar
		Model modelPillar = assets.get("model/pillar.g3db", Model.class);
		gameObjectFactory.put("pillar", new GameObject.Constructor(modelPillar,
				Bullet.obtainStaticNodeShape(modelPillar.nodes), 0));
		GameObject pillar = gameObjectFactory.get("pillar").construct();
		pillar.position(10, 0, 20);
		instances.add(pillar);
		Gdx.app.debug(tag, "loaded pillar");

		// Add a 3D compass model
		Model modelCompass = buildCompassModel();
		gameObjectFactory.put("compass", new GameObject.Constructor(
				modelCompass, Bullet.obtainStaticNodeShape(modelCompass.nodes),
				0));
		GameObject compass = gameObjectFactory.get("compass").construct();
		compass.position(10, 0.5f, 10);
		instances.add(compass);
		Gdx.app.debug(tag, "loaded compass");

		// Add all current game instances to the collision world as ground
		for (GameObject obj : instances) {
			collisionHandler.add(obj, CollisionHandler.GROUND_FLAG,
					CollisionHandler.ALL_FLAG);
			obj.body.setContactCallbackFlag(CollisionHandler.GROUND_FLAG);
		}

		// Add shootable spheres
		Model modelSphere = assets.get("model/sphere.g3db", Model.class);
		gameObjectFactory.put("sphere", new GameObject.Constructor(modelSphere,
				Bullet.obtainStaticNodeShape(modelSphere.nodes), 0));
		Gdx.app.debug(tag, "loaded sphere");

		// Blender sphere coordinates
		Vector3[] spherePos = { new Vector3(-2, 5, 7), new Vector3(-4, 1, 0),
				new Vector3(2, 1, 0), new Vector3(7, -3, 7),
				new Vector3(-2, -8, 7), new Vector3(0, -8, 7), };

		for (Vector3 pos : spherePos) {
			pos.set(pos.x, pos.z, -pos.y);
			GameObject sphere = gameObjectFactory.get("sphere").construct();
			sphere.position(pos);
			sphere.removable = true;
			instances.add(sphere);
			collisionHandler.add(sphere, CollisionHandler.OBJECT_FLAG,
					CollisionHandler.ALL_FLAG);
			sphere.body.setContactCallbackFlag(CollisionHandler.OBJECT_FLAG);
		}

		// Gun
		Model modelGun = assets.get("model/gun.g3db", Model.class);
		BoundingBox gunBB = new BoundingBox();
		Vector3 gunDim = new Vector3();
		modelGun.calculateBoundingBox(gunBB);
		gunBB.getDimensions(gunDim);
		gameObjectFactory.put("gun", new GameObject.Constructor(modelGun,
				new btBoxShape(gunDim), 5f));
		gun = gameObjectFactory.get("gun").construct();
		gun.position(GameSettings.PLAYER_START_POS);
		gun.body.setFriction(2f);
		gun.body.setContactCallbackFlag(CollisionHandler.OBJECT_FLAG);
		collisionHandler.add(gun, CollisionHandler.OBJECT_FLAG,
				CollisionHandler.GROUND_FLAG);
		Gdx.app.debug(tag, "loaded gun");

		// Test crate/box
		Model modelCrate = assets.get("model/crate.g3db", Model.class);
		gameObjectFactory.put("crate", new GameObject.Constructor(modelCrate,
				new btBox2dShape(new Vector3(0.5f, 0.5f, 0.5f)), 1f));
		GameObject crate = gameObjectFactory.get("crate").construct();
		crate.body.setContactCallbackFlag(CollisionHandler.OBJECT_FLAG);
		instances.add(crate);
		crate.position(15, 10, 15);
		crate.movable = true;
		crate.body.setActivationState(Collision.DISABLE_DEACTIVATION);
		collisionHandler.add(crate, CollisionHandler.OBJECT_FLAG,
				CollisionHandler.ALL_FLAG);
		crate.body.setContactCallbackFlag(CollisionHandler.OBJECT_FLAG);
		Gdx.app.debug(tag, "loaded crate");

		// Player
		Model modelPlayer = new ModelBuilder().createCapsule(
				GameSettings.PLAYER_RADIUS, GameSettings.PLAYER_HEIGHT - 2
						* GameSettings.PLAYER_RADIUS, 4, GL20.GL_TRIANGLES,
				new Material(), Usage.Position | Usage.Normal);
		gameObjectFactory.put("player", new GameObject.Constructor(modelPlayer,
				new btCapsuleShape(GameSettings.PLAYER_RADIUS,
						GameSettings.PLAYER_HEIGHT - 2
								* GameSettings.PLAYER_RADIUS), 100));
		GameObject playerObject = gameObjectFactory.get("player").construct();
		playerObject.position(GameSettings.PLAYER_START_POS);
		playerObject.visible = false;
		playerObject.body.setAngularFactor(Vector3.Y);
		// playerObject.body.setFriction(3);
		playerObject.body.setCollisionFlags(playerObject.body
				.getCollisionFlags()
				| btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		playerObject.body.setContactCallbackFlag(CollisionHandler.PLAYER_FLAG);

		PlayerSound sound = new PlayerSound(assets);

		player = new Player(game, playerObject, screenCenter, viewport,
				collisionHandler, sound);
		player.object.body.setActivationState(Collision.DISABLE_DEACTIVATION);
		collisionHandler
				.add(playerObject,
						CollisionHandler.PLAYER_FLAG,
						(short) (CollisionHandler.GROUND_FLAG | CollisionHandler.OBJECT_FLAG));
		Gdx.app.debug(tag, "loaded player");
	}

	private Model buildCompassModel() {
		float compassScale = 5;
		ModelBuilder modelBuilder = new ModelBuilder();
		Model arrow = modelBuilder.createArrow(Vector3.Zero, Vector3.Y.cpy()
				.scl(compassScale), null, Usage.Position | Usage.Normal);
		modelBuilder.begin();
		Mesh xArrow = arrow.meshes.first().copy(false);
		xArrow.transform(new Matrix4().rotate(Vector3.X, 90));
		modelBuilder.part("part1", xArrow, GL20.GL_TRIANGLES, new Material(
				ColorAttribute.createDiffuse(Color.RED)));
		modelBuilder.node();
		Mesh yArrow = arrow.meshes.first().copy(false);
		modelBuilder.part("part2", yArrow, GL20.GL_TRIANGLES, new Material(
				ColorAttribute.createDiffuse(Color.GREEN)));
		modelBuilder.node();
		Mesh zArrow = arrow.meshes.first().copy(false);
		zArrow.transform(new Matrix4().rotate(Vector3.Z, 90));
		modelBuilder.part("part3", zArrow, GL20.GL_TRIANGLES, new Material(
				ColorAttribute.createDiffuse(Color.BLUE)));
		arrow.dispose();
		return modelBuilder.end();
	}

	@Override
	public void dispose() {
		super.dispose();

		for (GameObject obj : instances)
			obj.dispose();
		instances.clear();

		for (GameObject.Constructor ctor : gameObjectFactory.values())
			ctor.dispose();
		gameObjectFactory.clear();

		collisionHandler.dispose();
		player.dispose();

		modelBatch.dispose();
		assets.dispose();

		shaderSun.dispose();
		sunTexture.dispose();

	}

	@Override
	public void hide() {
		lastCameraDirection.set(camera.direction);
		Gdx.input.setCursorCatched(false);
		music.stop();
	}

	private void loadShaders() {
		String vert = Gdx.files.local("shader/sun.vert").readString();
		String frag = Gdx.files.local("shader/sun.frag").readString();
		shaderSun = new ShaderProgram(vert, frag);
		ShaderProgram.pedantic = false;
		if (!shaderSun.isCompiled()) {
			Gdx.app.debug("Shader:", shaderSun.getLog());
			Gdx.app.exit();
		}
		if (shaderSun.getLog().length() != 0) {
			Gdx.app.debug("Shader:", shaderSun.getLog());
		}
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	@Override
	public void render(float delta) {
		delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());
		super.render(delta);

		// Update collisions
		collisionHandler.dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);

		// Update player transform from user input
		player.update(delta);
		player.object.body.getWorldTransform(player.object.transform);

		// Update gun position/rotation relative to camera
		player.direction.nor();
		gunLeftRightPosition.set(player.direction).crs(Vector3.Y).nor()
				.scl(0.075f);
		gunFrontBackPosition.set(player.direction).nor().scl(0.1f);
		gunUpDownPosition.set(player.direction).nor().crs(gunLeftRightPosition)
				.scl(0.75f);
		gunBaseTransform.set(camera.view).inv();
		gun.body.setWorldTransform(gunBaseTransform);
		gun.body.translate(gunLeftRightPosition);
		gun.body.translate(gunFrontBackPosition);
		gun.body.translate(gunUpDownPosition);
		gun.body.setLinearVelocity(player.object.body.getLinearVelocity());
		gun.body.getWorldTransform(gun.transform);

		for (GameObject obj : instances)
			obj.body.getWorldTransform(obj.transform);

		// Render the models
		modelBatch.begin(camera);
		modelBatch.render(skybox);
		modelBatch.end();

		// Draw the sun
		spriteBatch.begin();
		spriteBatch.setProjectionMatrix(uiMatrix);
		shaderSun.begin();
		Vector3 s_pos_sun = viewport.project(sunPositionProj.set(sunPosition));
		s_pos_sun.y = s_pos_sun.y - viewport.getScreenHeight() / 2;
		shaderSun.setUniformf("pos_sun", s_pos_sun);
		shaderSun.setUniformf("resolution", viewport.getScreenWidth(),
				viewport.getScreenHeight());
		shaderSun.end();
		float dst = camera.position.dst(sunPosition);
		spriteBatch.setShader(shaderSun);
		float sw = sunRadius / dst;
		float sh = sw;
		spriteBatch.draw(sunTexture, s_pos_sun.x - sw / 2,
				s_pos_sun.y - sh / 2, sw, sh);
		spriteBatch.setShader(null);
		spriteBatch.end();

		// Render the game level models and player gun
		modelBatch.begin(camera);
		for (GameObject obj : instances) {
			if (obj.visible) {
				modelBatch.render(obj, environment);
			}
		}
		modelBatch.render(gun, environment);
		modelBatch.end();

		// Draw collision debug wireframe
		// collisionHandler.debugDrawWorld(camera);

		// Draw player coordinates
		msg = String
				.format("Blender: x=%.2f, y=%.2f, z=%.2f\nGame: x=%.2f, y=%.2f, z=%.2f, v=%.2f",
						player.position.x, -player.position.z,
						player.position.y - GameSettings.PLAYER_HEIGHT / 2,

						player.position.x, player.position.y
								- GameSettings.PLAYER_HEIGHT / 2,
						player.position.z, player.object.body
								.getLinearVelocity().len());
		spriteBatch.setShader(null);
		spriteBatch.setProjectionMatrix(uiMatrix);
		spriteBatch.begin();
		fontTiny.draw(spriteBatch, msg, 10, 30);
		spriteBatch.end();

		// Draw crosshair
		shapeRenderer.setProjectionMatrix(uiMatrix);
		shapeRenderer.begin(ShapeType.Line);
		float xc = viewport.getScreenWidth() / 2;
		float yc = viewport.getScreenHeight() / 2;
		shapeRenderer.setColor(Color.GRAY);
		shapeRenderer.line(xc + 1, yc - GameSettings.CROSSHAIR, xc + 1, yc
				+ GameSettings.CROSSHAIR);
		shapeRenderer.line(xc - GameSettings.CROSSHAIR, yc - 1, xc
				+ GameSettings.CROSSHAIR, yc - 1);

		shapeRenderer.setColor(Color.WHITE);
		shapeRenderer.line(xc, yc - GameSettings.CROSSHAIR, xc, yc
				+ GameSettings.CROSSHAIR);
		shapeRenderer.line(xc - GameSettings.CROSSHAIR, yc, xc
				+ GameSettings.CROSSHAIR, yc);
		shapeRenderer.end();

	}

	@Override
	public void resize(int width, int height) {

		super.resize(width, height);

		float vw = viewport.getScreenWidth();
		float vh = viewport.getScreenHeight();

		screenCenter.set(width / 2, height / 2, 1);

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, vw, vh);

		camera = new PerspectiveCamera(GameSettings.CAMERA_FOV,
				viewport.getScreenWidth(), viewport.getScreenHeight());
		camera.near = 1E-2f;
		camera.far = 1.5E3f;
		camera.lookAt(lastCameraDirection);

		camera.update(true);

		viewport.setCamera(camera);

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(player.controller);
		Gdx.input.setCursorCatched(true);

		// Play some music
		music = assets.get("sound/music_game.ogg", Music.class);
		music.play();
		music.setVolume(0.3f);
		music.setLooping(true);

	}

}
