package incanshift.world;

import java.util.Random;

import incanshift.IncanShift;
import incanshift.gameobjects.Billboard;
import incanshift.gameobjects.EnvTag;
import incanshift.gameobjects.GameObject;
import incanshift.player.Player;
import incanshift.player.PlayerSound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
import com.badlogic.gdx.physics.bullet.collision.btConeShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
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

	Viewport viewport;
	IncanShift game;

	private AssetManager assets;
	public CollisionHandler collisionHandler;

	private ArrayMap<String, GameObject.Constructor> gameObjectFactory;
	public ArrayMap<String, Array<GameObject>> instances;

	public Array<Billboard> billboards;
	public Array<EnvTag> envTags;

	public String[] levels = { "model/outside_level.csv", "model/level1.csv",
			"model/level2.csv" };
	public int currentLevel = 0;

	public ModelInstance skybox;
	public Player player;

	public Music music;

	private BitmapFont font;

	private void addInstance(GameObject obj) {
		if (!instances.containsKey(obj.id)) {
			instances.put(obj.id, new Array<GameObject>());
		}
		instances.get(obj.id).add(obj);
	}

	public GameWorld(IncanShift game, Viewport viewport, Vector3 screenCenter,
			BitmapFont font) {
		Gdx.app.debug(tag, String.format("Creating game world"));
		this.font = font;
		this.viewport = viewport;
		this.game = game;
		assets = new AssetManager();

		// Load the 3D models and sounds used by the game
		assets.load("model/blowpipe.g3db", Model.class);
		assets.load("model/box.g3db", Model.class);
		// assets.load("model/gun.g3db", Model.class);
		assets.load("model/mask.g3db", Model.class);
		assets.load("model/skybox.g3db", Model.class);
		assets.load("model/shard.g3db", Model.class);

		assets.load("sound/jump.wav", Sound.class);
		assets.load("sound/shatter.wav", Sound.class);
		assets.load("sound/shoot.wav", Sound.class);
		assets.load("sound/run.wav", Sound.class);
		assets.load("sound/walk.wav", Sound.class);
		assets.load("sound/climb.wav", Sound.class);
		assets.load("sound/music_game.ogg", Music.class);

		Bullet.init();
		instances = new ArrayMap<String, Array<GameObject>>();
		billboards = new Array<Billboard>();
		envTags = new Array<EnvTag>();
		collisionHandler = new CollisionHandler();
		gameObjectFactory = new ArrayMap<String, GameObject.Constructor>();

		Gdx.app.debug(tag, String.format("Trying to load assets..."));
		try {
			assets.finishLoading();
		} catch (GdxRuntimeException e) {
			Gdx.app.debug(tag, "Could not load assets, ", e);
		}
		Gdx.app.debug(tag, String.format("Assets finished loading."));
		createFactoryDefs(assets, gameObjectFactory);
		skybox = new ModelInstance(assets.get("model/skybox.g3db", Model.class));

		// Create a player, a gun and load the level from CSV
		player = spawnPlayer(game, viewport, screenCenter);
		// GameObject gun = spawn("gun", player.position.cpy(), false, false,
		// false, CollisionHandler.OBJECT_FLAG,
		// CollisionHandler.GROUND_FLAG);
		// player.setGun(gun);

		loadLevel(currentLevel);

		// GameObject blowpipe = spawn("blowpipe", player.position.cpy(),
		// new Vector3(), false, false, false,
		// CollisionHandler.OBJECT_FLAG, CollisionHandler.GROUND_FLAG);
		// player.setGun(blowpipe);
		// addInstance(blowpipe);
	}

	public void loadLevel(int level) {
		for (Entry<String, Array<GameObject>> entry : instances) {
			for (GameObject obj : entry.value) {
				collisionHandler.dynamicsWorld.removeCollisionObject(obj.body);
			}
		}
		instances.clear();
		billboards.clear();
		envTags.clear();
		loadLevelCSV(levels[level]);
	}

	/**
	 * Spawn a game object from the factory and add it to the world. If not
	 * defined in factory, load the model from file system and generate a static
	 * collision shape for it.
	 * 
	 * @param name
	 *            Factory name for the object.
	 * @param pos
	 *            Object position.
	 * @param rot
	 *            Object rotation.
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
	public GameObject spawn(String name, Vector3 pos, Vector3 rot,
			boolean movable, boolean removable, boolean noDeactivate,
			short belongsToFlag, short collidesWithFlag) {

		if (!gameObjectFactory.containsKey(name)) {
			String filePath = String.format("model/%s.g3db", name);
			Gdx.app.debug(tag,
					String.format("Creating collision shape for %s", filePath));
			assets.load(filePath, Model.class);
			assets.finishLoading();
			Model model = assets.get(filePath);
			gameObjectFactory.put(name, new GameObject.Constructor(model,
					Bullet.obtainStaticNodeShape(model.nodes), 0));
		}

		GameObject obj = gameObjectFactory.get(name).construct();
		obj.id = name;
		Gdx.app.debug(tag, String.format("Spawning %s at %s", name, pos));

		obj.transform.rotate(Vector3.Y, rot.y);
		obj.transform.rotate(Vector3.X, rot.x);
		obj.transform.rotate(Vector3.Z, rot.z);
		obj.transform.setTranslation(pos);
		obj.body.setWorldTransform(obj.transform);

		obj.movable = movable;
		obj.removable = removable;
		if (noDeactivate) {
			obj.body.setActivationState(Collision.DISABLE_DEACTIVATION);
		}
		obj.body.setContactCallbackFlag(belongsToFlag);
		collisionHandler.add(obj, belongsToFlag, collidesWithFlag);

		addInstance(obj);

		return obj;
	}

	/**
	 * Read a CSV file and create the objects listed in it.
	 * 
	 * @param csv
	 */
	public void loadLevelCSV(String csvPath) {
		Gdx.app.debug(tag, String.format("Loading CSV data from %s", csvPath));
		String csv = Gdx.files.internal(csvPath).readString();
		Gdx.app.debug(tag, String.format("Content: \n%s", csv));
		String[] lines = csv.split(System.getProperty("line.separator"));

		Array<Vector3> billboardPos = new Array<Vector3>();

		for (String line : lines) {
			String name;
			Vector3 pos = new Vector3();
			Vector3 rot = new Vector3();
			try {
				String[] values = line.split(";");
				name = values[0];
				pos.set(Float.parseFloat(values[1]),
						Float.parseFloat(values[2]),
						Float.parseFloat(values[3]));
				rot.set(Float.parseFloat(values[4]),
						Float.parseFloat(values[5]),
						Float.parseFloat(values[6]));
			} catch (Exception e) {
				Gdx.app.debug(tag, "Error when parsing csv file.", e);
				continue;
			}
			blenderToGameCoords(rot);
			blenderToGameCoords(pos);

			if (name.equals("mask")) {
				spawn(name, pos, rot, false, true, false,
						CollisionHandler.OBJECT_FLAG, CollisionHandler.ALL_FLAG);

			} else if (name.equals("box")) {
				spawn(name, pos, rot, true, false, true,
						CollisionHandler.OBJECT_FLAG, CollisionHandler.ALL_FLAG);

			} else if (name.equals("start_position")) {
				player.object.position(pos);

			} else if (name.equals("text_tag")) {
				billboardPos.add(pos);

			} else if (name.equals("fog_tag")) {
				envTags.add(new EnvTag(pos, 80, 40, Color.GRAY, 30));

			} else if (name.equals("sun_tag")) {
				envTags.add(new EnvTag(pos, 60, 30, Color.WHITE, 30));

			} else {
				spawn(name, pos, rot, false, false, false,
						CollisionHandler.GROUND_FLAG, CollisionHandler.ALL_FLAG);
			}
		}
		spawnBillboards(billboardPos, Gdx.files.internal("text/billboards.txt"));
	}

	public void spawnBillboards(Array<Vector3> pos, FileHandle textFile) {
		Color textColor = new Color(Color.WHITE).mul(1f, 1f, 1f, 1f);
		Color bkgColor = new Color(Color.GRAY).mul(1f, 1f, 1f, 0f);

		String text = textFile.readString();
		String[] lines = text.split(System.getProperty("line.separator"));
		Array<String> sections = new Array<String>();

		String msg = "";
		for (String line : lines) {
			if (line.equals("#") && msg.length() != 0) {
				sections.add(msg);
				msg = "";
			} else if (!line.equals("#") && msg.length() != 0) {
				msg += String.format("\n%s", line);
			} else if (!line.equals("#")) {
				msg += String.format("%s", line);
			}
		}
		for (int i = 0; i < Math.min(pos.size, sections.size); i++) {
			Gdx.app.debug(tag,
					String.format("Spawning billboard at %s", pos.get(i)));
			billboards.add(new Billboard(pos.get(i), 4f, 4f, 10f, msg,
					textColor, bkgColor, font));
		}

	}

	// public void spawnBillboard(Vector3 pos) {
	// billboards.add(new Billboard(new Vector3(-20, 2, 10), 1, 1, 0,
	// "shader/common.vert", "shader/test.frag"));
	// }

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
		Gdx.app.debug(tag, "Loaded compass model");

		Model modelBlowpipe = assets.get("model/blowpipe.g3db", Model.class);
		gameObjectFactory.put("blowpipe", new GameObject.Constructor(
				modelBlowpipe, new btBoxShape(
						getBoundingBoxDimensions(modelBlowpipe)), 5f));
		Gdx.app.debug(tag, "Loaded blowpipe model");

		Model modelBox = assets.get("model/box.g3db", Model.class);
		gameObjectFactory.put("box", new GameObject.Constructor(modelBox,
				new btBox2dShape(new Vector3(0.5f, 0.5f, 0.5f)), 1f));
		Gdx.app.debug(tag, "Loaded box model");

		// Model modelGun = assets.get("model/gun.g3db", Model.class);
		// gameObjectFactory.put("gun", new GameObject.Constructor(modelGun,
		// new btBoxShape(getBoundingBoxDimensions(modelGun)), 5f));
		// Gdx.app.debug(tag, "Loaded gun model");

		Model modelSphere = assets.get("model/mask.g3db", Model.class);
		gameObjectFactory.put("mask", new GameObject.Constructor(modelSphere,
				Bullet.obtainStaticNodeShape(modelSphere.nodes), 0));
		Gdx.app.debug(tag, "Loaded mask model");

		Model modelPlayer = new ModelBuilder().createCapsule(
				GameSettings.PLAYER_RADIUS, GameSettings.PLAYER_HEIGHT - 2
						* GameSettings.PLAYER_RADIUS, 4, GL20.GL_TRIANGLES,
				new Material(), Usage.Position | Usage.Normal);
		gameObjectFactory.put("player", new GameObject.Constructor(modelPlayer,
				new btCapsuleShape(GameSettings.PLAYER_RADIUS,
						GameSettings.PLAYER_HEIGHT - 2
								* GameSettings.PLAYER_RADIUS), 100));
		Gdx.app.debug(tag, "Loaded player model");

		Model modelShard = assets.get("model/shard.g3db", Model.class);
		gameObjectFactory.put("shard", new GameObject.Constructor(modelShard,
				new btConeShape(0.2f, 0.4f), 1));

		Gdx.app.debug(tag, "Loaded shard model");
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
		for (Entry<String, Array<GameObject>> entry : instances) {
			for (GameObject obj : entry.value) {
				obj.dispose();
			}
		}
		instances.clear();

		for (GameObject.Constructor ctor : gameObjectFactory.values())
			ctor.dispose();
		gameObjectFactory.clear();

		collisionHandler.dispose();
		assets.dispose();

		for (Billboard b : billboards) {
			b.dispose();
		}

		player.dispose();
	}

	public void music(boolean on) {
		// Play some music
		music = assets.get("sound/music_game.ogg", Music.class);
		music.play();
		music.setVolume(0.3f * GameSettings.MUSIC_VOLUME);
		music.setLooping(true);
	}

	private int numberSpawned(String id) {
		return instances.get(id).size;
	}

	public Player spawnPlayer(IncanShift game, Viewport viewport,
			Vector3 screenCenter) {

		GameObject obj = spawn(
				"player",
				new Vector3(),
				new Vector3(),
				false,
				false,
				true,
				CollisionHandler.PLAYER_FLAG,
				(short) (CollisionHandler.GROUND_FLAG | CollisionHandler.OBJECT_FLAG));

		instances.removeKey("player");

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

		for (Entry<String, Array<GameObject>> entry : instances) {
			for (GameObject obj : entry.value) {
				obj.body.getWorldTransform(obj.transform);
			}
		}
		for (Billboard b : billboards) {
			b.update(viewport.getCamera());
		}

		// if (numberSpawned("mask") == 0) {
		// currentLevel++;
		// if (currentLevel == levels.length) {
		// currentLevel = 0;
		// }
		// loadLevel(currentLevel);
		// game.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		// }

	}

	public GameObject getGameObject(btRigidBody co) {
		GameObject go = null;

		for (Entry<String, Array<GameObject>> entry : instances) {
			for (GameObject obj : entry.value) {
				if (obj.body.equals(co)) {
					go = obj;
					Gdx.app.debug(tag,
							String.format("Found object id: " + obj.id));
					break;
				}
			}
		}
		return go;
	}

	private Vector3 randAndNor(Vector3 v) {
		Random rand = new Random();
		float sign;
		sign = (rand.nextFloat() > 0.5) ? -1 : 1;
		v.x = rand.nextFloat() * sign;
		sign = (rand.nextFloat() > 0.5) ? -1 : 1;
		v.y = rand.nextFloat() * sign;
		sign = (rand.nextFloat() > 0.5) ? -1 : 1;
		v.z = rand.nextFloat() * sign;
		v.nor();
		return v;
	}

	private void shatter(Vector3 pos) {
		Vector3 lin_vel = new Vector3();
		final Array<GameObject> shards = new Array<GameObject>();
		for (int i = 0; i < 25; i++) {
			GameObject obj = spawn("shard", pos, new Vector3(), true, false,
					false, CollisionHandler.OBJECT_FLAG,
					CollisionHandler.ALL_FLAG);
			obj.body.setAngularFactor(Vector3.X);
			obj.body.setLinearVelocity(randAndNor(lin_vel).scl(50));
			shards.add(obj);
		}
		Timer.schedule(new Task() {

			@Override
			public void run() {
				for (GameObject obj : shards) {
					destroy(obj);
				}
			}
		}, 2);
	}

	public void destroy(GameObject obj) {
		collisionHandler.dynamicsWorld.removeCollisionObject(obj.body);
		instances.get(obj.id).removeValue(obj, true);
		obj.dispose();
		Vector3 pos = new Vector3();
		obj.transform.getTranslation(pos);
		Gdx.app.debug(tag, String.format("Destroyed %s at %s, %s remaining.",
				obj.id, pos, numberSpawned(obj.id)));
		if (obj.id.equals("mask")) {
			shatter(pos);
		}
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
