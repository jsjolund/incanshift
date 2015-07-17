package incanshift;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
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
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBox2dShape;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

/*
 * Contains 3d model and collision body definitions for the objects
 * in the world. Loads information about numbers of objects and their
 * positions from a CSV file.
 * 
 * The CSV file is generated by a python script run inside Blender.
 * 
 * An example CSV file is in assets/model/map_template.csv,
 * which was created with the blend file assets/model/map_template.blend.
 */
public class GameWorld implements Disposable {

	final static String tag = "GameWorld";

	private AssetManager assets;
	private CollisionHandler collisionHandler;
	private ArrayMap<String, GameObject.Constructor> gameObjectFactory;

	public Array<GameObject> instances;
	public ModelInstance skybox;
	public Player player;

	Music music;

	/**
	 * Spawn a game object from the factory and add it to the world. If not
	 * defined in factory, load the model from file system and generate a static
	 * collision shape for it.
	 * 
	 * @param name
	 *            Factory name for the object.
	 * @param pos
	 *            Starting position.
	 * @param movable
	 *            True if the player can move this object.
	 * @param removable
	 *            True if the player can destroy this object.
	 * @param noDeactivate
	 *            True if collision simulation should never be suspended for
	 *            this object.
	 * @param belongsToFlag
	 *            Collision flag/mask for the group this object belongs to.
	 * @param collidesWithFlag
	 *            Collision flag/mask for the group this object can collide
	 *            with.
	 * @return The created game object.
	 */
	public GameObject spawn(String name, Vector3 pos, boolean movable,
			boolean removable, boolean noDeactivate, short belongsToFlag,
			short collidesWithFlag) {

		if (!gameObjectFactory.containsKey(name)) {
			String filePath = String.format("model/%s.g3db", name);
			assets.load(filePath, Model.class);
			assets.finishLoading();
			Model model = assets.get(filePath);
			gameObjectFactory.put(name, new GameObject.Constructor(model,
					Bullet.obtainStaticNodeShape(model.nodes), 0));
		}

		GameObject obj = gameObjectFactory.get(name).construct();
		Gdx.app.debug(tag, String.format("Spawning %s at %s", name, pos));
		obj.position(pos);
		obj.movable = movable;
		obj.removable = removable;
		if (noDeactivate) {
			obj.body.setActivationState(Collision.DISABLE_DEACTIVATION);
		}
		obj.body.setContactCallbackFlag(belongsToFlag);
		collisionHandler.add(obj, belongsToFlag, collidesWithFlag);
		instances.add(obj);
		return obj;
	}

	public GameWorld(IncanShift game, Viewport viewport, Vector3 screenCenter) {
		assets = new AssetManager();

		// Load the 3D models and sounds used by the game
		assets.load("model/blowpipe.g3db", Model.class);
		assets.load("model/box.g3db", Model.class);
		assets.load("model/gun.g3db", Model.class);
		assets.load("model/mask.g3db", Model.class);
		assets.load("model/skybox.g3db", Model.class);

		assets.load("sound/jump.wav", Sound.class);
		assets.load("sound/shatter.wav", Sound.class);
		assets.load("sound/shoot.wav", Sound.class);
		assets.load("sound/run.wav", Sound.class);
		assets.load("sound/walk.wav", Sound.class);
		assets.load("sound/climb.wav", Sound.class);
		assets.load("sound/music_game.ogg", Music.class);

		Bullet.init();
		instances = new Array<GameObject>();
		collisionHandler = new CollisionHandler();
		gameObjectFactory = new ArrayMap<String, GameObject.Constructor>();
		assets.finishLoading();
		createFactoryDefs(assets, gameObjectFactory);
		skybox = new ModelInstance(assets.get("model/skybox.g3db", Model.class));

		// Create a player, a gun and load the level from CSV
		player = spawnPlayer(game, viewport, screenCenter);
		// GameObject gun = spawn("gun", player.position.cpy(), false, false,
		// false, CollisionHandler.OBJECT_FLAG,
		// CollisionHandler.GROUND_FLAG);
		// player.setGun(gun);

		GameObject blowpipe = spawn("blowpipe", player.position.cpy(), false,
				false, false, CollisionHandler.OBJECT_FLAG,
				CollisionHandler.GROUND_FLAG);
		player.setGun(blowpipe);

		loadLevelCSV(Gdx.files.internal("model/christoffer.csv").readString());
	}

	/**
	 * Read a CSV file and create the objects listed in it.
	 * 
	 * @param csv
	 */
	public void loadLevelCSV(String csv) {
		String[] lines = csv.split(System.getProperty("line.separator"));
		for (String line : lines) {
			String name;
			Vector3 pos = null;
			try {
				String[] values = line.split(";");
				name = values[0];
				float x = Float.parseFloat(values[1]);
				float y = Float.parseFloat(values[2]);
				float z = Float.parseFloat(values[3]);
				pos = new Vector3(x, y, z);
			} catch (Exception e) {
				Gdx.app.debug(tag, "Error when parsing csv file.", e);
				continue;
			}
			blenderToGameCoords(pos);

			if (name.equals("mask")) {
				spawn(name, pos, false, true, false,
						CollisionHandler.OBJECT_FLAG, CollisionHandler.ALL_FLAG);

			} else if (name.equals("box")) {
				spawn(name, pos, true, false, true,
						CollisionHandler.OBJECT_FLAG, CollisionHandler.ALL_FLAG);

			} else if (name.equals("start_position")) {
				player.object.position(pos);

			} else if (name.equals("tag")) {
				// Add a billboard

			} else {
				spawn(name, pos, false, false, false,
						CollisionHandler.GROUND_FLAG, CollisionHandler.ALL_FLAG);
			}

		}

	}

	/**
	 * Object factory blueprint for a 3D model along with an appropriate
	 * collision shape.
	 */
	private static void createFactoryDefs(AssetManager assets,
			ArrayMap<String, GameObject.Constructor> gameObjectFactory) {

		Model modelCompass = buildCompassModel();
		gameObjectFactory.put("compass", new GameObject.Constructor(
				modelCompass, Bullet.obtainStaticNodeShape(modelCompass.nodes),
				0));

		Model modelBlowpipe = assets.get("model/blowpipe.g3db", Model.class);
		gameObjectFactory.put("blowpipe", new GameObject.Constructor(
				modelBlowpipe, new btBoxShape(
						getBoundingBoxDimensions(modelBlowpipe)), 5f));

		Model modelBox = assets.get("model/box.g3db", Model.class);
		gameObjectFactory.put("box", new GameObject.Constructor(modelBox,
				new btBox2dShape(new Vector3(0.5f, 0.5f, 0.5f)), 1f));

		Model modelGun = assets.get("model/gun.g3db", Model.class);
		gameObjectFactory.put("gun", new GameObject.Constructor(modelGun,
				new btBoxShape(getBoundingBoxDimensions(modelGun)), 5f));

		Model modelSphere = assets.get("model/mask.g3db", Model.class);
		gameObjectFactory.put("mask", new GameObject.Constructor(modelSphere,
				Bullet.obtainStaticNodeShape(modelSphere.nodes), 0));

		Model modelPlayer = new ModelBuilder().createCapsule(
				GameSettings.PLAYER_RADIUS, GameSettings.PLAYER_HEIGHT - 2
						* GameSettings.PLAYER_RADIUS, 4, GL20.GL_TRIANGLES,
				new Material(), Usage.Position | Usage.Normal);
		gameObjectFactory.put("player", new GameObject.Constructor(modelPlayer,
				new btCapsuleShape(GameSettings.PLAYER_RADIUS,
						GameSettings.PLAYER_HEIGHT - 2
								* GameSettings.PLAYER_RADIUS), 100));
	}

	public static Vector3 getBoundingBoxDimensions(Model model) {
		BoundingBox bBox = new BoundingBox();
		Vector3 dim = new Vector3();
		model.calculateBoundingBox(bBox);
		bBox.getDimensions(dim);
		return dim;
	}

	private static Vector3 blenderToGameCoords(Vector3 v) {
		return v.set(v.x, v.z, -v.y);
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

	public void music(boolean on) {
		// Play some music
		music = assets.get("sound/music_game.ogg", Music.class);
		music.play();
		music.setVolume(0.3f);
		music.setLooping(true);
	}

	public Player spawnPlayer(IncanShift game, Viewport viewport,
			Vector3 screenCenter) {

		GameObject obj = spawn(
				"player",
				new Vector3(),
				false,
				false,
				true,
				CollisionHandler.PLAYER_FLAG,
				(short) (CollisionHandler.GROUND_FLAG | CollisionHandler.OBJECT_FLAG));

		instances.removeValue(obj, true);

		obj.visible = false;
		obj.body.setAngularFactor(Vector3.Y);
		obj.body.setCollisionFlags(obj.body.getCollisionFlags()
				| btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);

		PlayerSound sound = new PlayerSound(assets);

		Player player = new Player(game, obj, screenCenter, viewport, this,
				sound);

		return player;
	}

	public btRigidBody rayTest(Ray ray, short mask, float maxDistance) {
		return collisionHandler.rayTest(ray, mask, maxDistance);
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

	public GameObject getGameObject(btRigidBody co) {
		GameObject go = null;
		for (GameObject obj : instances) {
			if (obj.body.equals(co)) {
				go = obj;
				break;
			}
		}
		return go;
	}

	public void removeGameObject(GameObject obj) {
		obj.visible = false;
		collisionHandler.dynamicsWorld.removeCollisionObject(obj.body);
	}

	/**
	 * Create a model of a 3D compass with arrows along the three axis.
	 * 
	 * @return
	 */
	private static Model buildCompassModel() {
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
}
