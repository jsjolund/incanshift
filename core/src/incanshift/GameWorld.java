package incanshift;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
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
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameWorld implements Disposable {
	public Player player;
	String tag = "GameWorld";
	public ModelInstance skybox;

	private AssetManager assets;
	Music music;

	// Game objects
	public Array<GameObject> instances;
	// Collision
	private CollisionHandler collisionHandler;

	private ArrayMap<String, GameObject.Constructor> gameObjectFactory;

	public void loadLevelCSV(String csv) {
		String[] lines = csv.split(System.getProperty("line.separator"));
		for (String line : lines) {
			String name;
			Vector3 pos = null;
			try {
				String[] values = line.split(",");
				name = values[0];
				float x = Float.parseFloat(values[1]);
				float y = Float.parseFloat(values[2]);
				float z = Float.parseFloat(values[3]);
				pos = new Vector3(x, y, z);
			} catch (Exception e) {
				Gdx.app.debug(tag, "Error when parsing csv file.", e);
				continue;
			}
			toGameCoords(pos);

			if (name.equals("mask")) {
				spawnEnemyMask(pos);
			} else if (name.equals("start_position")) {
				player.object.position(pos);

			} else if (gameObjectFactory.containsKey(name)) {

			}

		}
	}

	private Vector3 toGameCoords(Vector3 v) {
		return v.set(v.x, v.z, -v.y);
	}

	public GameWorld(IncanShift game, Viewport viewport, Vector3 screenCenter) {
		assets = new AssetManager();

		assets.load("model/temple.g3db", Model.class);
		assets.load("model/ground.g3db", Model.class);
		assets.load("model/skybox.g3db", Model.class);
		assets.load("model/level.g3db", Model.class);
		assets.load("model/gun.g3db", Model.class);
		assets.load("model/mask.g3db", Model.class);
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

		assets.finishLoading();

		gameObjectFactory = new ArrayMap<String, GameObject.Constructor>();

		Model modelTemple = assets.get("model/temple.g3db", Model.class);
		gameObjectFactory.put("temple", new GameObject.Constructor(modelTemple,
				Bullet.obtainStaticNodeShape(modelTemple.nodes), 0));
		// instances.add(gameObjectFactory.get("temple").construct());
		Gdx.app.debug(tag, "Loaded temple");

		Model modelGround = assets.get("model/ground.g3db", Model.class);
		gameObjectFactory.put("ground", new GameObject.Constructor(modelGround,
				Bullet.obtainStaticNodeShape(modelGround.nodes), 0));
		instances.add(gameObjectFactory.get("ground").construct());
		Gdx.app.debug(tag, "Loaded ground");

		Model modelLevel = assets.get("model/level.g3db", Model.class);
		gameObjectFactory.put("level", new GameObject.Constructor(modelLevel,
				Bullet.obtainStaticNodeShape(modelLevel.nodes), 0));
		instances.add(gameObjectFactory.get("level").construct());
		Gdx.app.debug(tag, "Loaded level");

		Model modelTestScene = assets.get("model/test_scene.g3db", Model.class);
		gameObjectFactory.put(
				"test_scene",
				new GameObject.Constructor(modelTestScene, Bullet
						.obtainStaticNodeShape(modelTestScene.nodes), 0));
		GameObject testscene = gameObjectFactory.get("test_scene").construct();
		testscene.position(-30, 1, 30);
		instances.add(testscene);
		Gdx.app.debug(tag, "Loaded test_scene");

		Model modelSkybox = assets.get("model/skybox.g3db", Model.class);
		skybox = new ModelInstance(modelSkybox);
		Gdx.app.debug(tag, "Loaded skybox");

		// Add a pillar
		Model modelPillar = assets.get("model/pillar.g3db", Model.class);
		gameObjectFactory.put("pillar", new GameObject.Constructor(modelPillar,
				Bullet.obtainStaticNodeShape(modelPillar.nodes), 0));
		GameObject pillar = gameObjectFactory.get("pillar").construct();
		pillar.position(10, 0, 20);
		instances.add(pillar);
		Gdx.app.debug(tag, "Loaded pillar");

		// Add a 3D compass model
		Model modelCompass = buildCompassModel();
		gameObjectFactory.put("compass", new GameObject.Constructor(
				modelCompass, Bullet.obtainStaticNodeShape(modelCompass.nodes),
				0));
		GameObject compass = gameObjectFactory.get("compass").construct();
		compass.position(10, 0.5f, 10);
		instances.add(compass);
		Gdx.app.debug(tag, "Loaded compass");

		// Add all current game instances to the collision world as ground
		for (GameObject obj : instances) {
			collisionHandler.add(obj, CollisionHandler.GROUND_FLAG,
					CollisionHandler.ALL_FLAG);
			obj.body.setContactCallbackFlag(CollisionHandler.GROUND_FLAG);
		}

		// Add shootable spheres
		Model modelSphere = assets.get("model/mask.g3db", Model.class);
		gameObjectFactory.put("mask", new GameObject.Constructor(modelSphere,
				Bullet.obtainStaticNodeShape(modelSphere.nodes), 0));
		Gdx.app.debug(tag, "Loaded mask");

		// Gun
		Model modelGun = assets.get("model/gun.g3db", Model.class);
		BoundingBox gunBB = new BoundingBox();
		Vector3 gunDim = new Vector3();
		modelGun.calculateBoundingBox(gunBB);
		gunBB.getDimensions(gunDim);
		gameObjectFactory.put("gun", new GameObject.Constructor(modelGun,
				new btBoxShape(gunDim), 5f));
		GameObject gun = gameObjectFactory.get("gun").construct();
		gun.body.setFriction(2f);
		gun.body.setContactCallbackFlag(CollisionHandler.OBJECT_FLAG);
		collisionHandler.add(gun, CollisionHandler.OBJECT_FLAG,
				CollisionHandler.GROUND_FLAG);
		instances.add(gun);
		Gdx.app.debug(tag, "Loaded gun");

		// Test crate/box
		Model modelCrate = assets.get("model/crate.g3db", Model.class);
		gameObjectFactory.put("crate", new GameObject.Constructor(modelCrate,
				new btBox2dShape(new Vector3(0.5f, 0.5f, 0.5f)), 1f));

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
		player.setGun(gun);

		loadLevelCSV(Gdx.files.internal("model/test_map.csv").readString());
	}

	void spawnEnemyMask(Vector3 pos) {
		Gdx.app.debug(tag, "Spawning mask at " + pos);
		GameObject sphere = gameObjectFactory.get("mask").construct();
		sphere.position(pos);
		sphere.removable = true;
		instances.add(sphere);
		collisionHandler.add(sphere, CollisionHandler.OBJECT_FLAG,
				CollisionHandler.ALL_FLAG);
		sphere.body.setContactCallbackFlag(CollisionHandler.OBJECT_FLAG);
	}

	void spawnCrate(Vector3 pos) {
		Gdx.app.debug(tag, "Spawning crate at " + pos);
		GameObject crate = gameObjectFactory.get("crate").construct();
		crate.body.setContactCallbackFlag(CollisionHandler.OBJECT_FLAG);
		instances.add(crate);
		crate.position(pos);
		crate.movable = true;
		crate.body.setActivationState(Collision.DISABLE_DEACTIVATION);
		collisionHandler.add(crate, CollisionHandler.OBJECT_FLAG,
				CollisionHandler.ALL_FLAG);
		crate.body.setContactCallbackFlag(CollisionHandler.OBJECT_FLAG);
		Gdx.app.debug(tag, "Loaded crate");
	}

	@Override
	public void dispose() {
		for (GameObject obj : instances)
			obj.dispose();
		instances.clear();

		for (GameObject.Constructor ctor : gameObjectFactory.values())
			ctor.dispose();
		gameObjectFactory.clear();

		collisionHandler.dispose();
		assets.dispose();
	}

	private Model buildCompassModel() {
		float compassScale = 5;
		ModelBuilder modelBuilder = new ModelBuilder();
		Model arrow = modelBuilder.createArrow(Vector3.Zero, Vector3.Y.cpy()
				.scl(compassScale), null, Usage.Position | Usage.Normal);
		modelBuilder.begin();

		Mesh zArrow = arrow.meshes.first().copy(false);
		zArrow.transform(new Matrix4().rotate(Vector3.X, 90));
		modelBuilder.part("part1", zArrow, GL20.GL_TRIANGLES, new Material(
				ColorAttribute.createDiffuse(Color.BLUE)));

		modelBuilder.node();
		Mesh yArrow = arrow.meshes.first().copy(false);
		modelBuilder.part("part2", yArrow, GL20.GL_TRIANGLES, new Material(
				ColorAttribute.createDiffuse(Color.GREEN)));

		modelBuilder.node();
		Mesh xArrow = arrow.meshes.first().copy(false);
		xArrow.transform(new Matrix4().rotate(Vector3.Z, -90));
		modelBuilder.part("part3", xArrow, GL20.GL_TRIANGLES, new Material(
				ColorAttribute.createDiffuse(Color.RED)));

		arrow.dispose();
		return modelBuilder.end();
	}

	public void update(float delta) {
		// Update collisions
		collisionHandler.dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);

		// Update player transform from user input
		player.update(delta);
		player.object.body.getWorldTransform(player.object.transform);

		for (GameObject obj : instances)
			obj.body.getWorldTransform(obj.transform);
	}

	public void music(boolean on) {
		// Play some music
		music = assets.get("sound/music_game.ogg", Music.class);
		music.play();
		music.setVolume(0.3f);
		music.setLooping(true);
	}

}
