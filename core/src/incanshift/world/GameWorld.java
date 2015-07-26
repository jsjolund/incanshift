package incanshift.world;

import java.util.Arrays;
import java.util.Random;

import incanshift.IncanShift;
import incanshift.gameobjects.Billboard;
import incanshift.gameobjects.BillboardOverlay;
import incanshift.gameobjects.EnvTag;
import incanshift.gameobjects.GameObject;
import incanshift.gameobjects.TextParser;
import incanshift.player.Player;
import incanshift.player.PlayerSound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
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
	public ArrayMap<GameObject, BillboardOverlay> billboardOverlays;
	public Array<EnvTag> envTags;

	public String[] levels = { //
			// "model/outside_level.csv", //
			"model/inside_level10_throw_out_the_bodies.csv", //
			"model/inside_level1.csv", //
			"model/inside_level2.csv", //
			"model/inside_level4_chair.csv", //
			"model/inside_level8_ant_hive.csv", //
			"model/inside_level3.csv", //
			"model/inside_level9_pillars_in_a_hill_of_stairs.csv", //
			"model/inside_level6_ziggurat_room.csv", //
			"model/inside_level7_ziggurat_dissolved.csv", //
			// "model/inside_level5_l.csv", //
	};
	public int currentLevel = 0;

	public ModelInstance skybox;
	public Player player;

	public boolean xRayMask = false;

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

		// Load the 3D models used by the game
		assets.load("model/blowpipe.g3db", Model.class);
		assets.load("model/grappling_hook.g3db", Model.class);
		assets.load("model/grappling_hook_trail.g3db", Model.class);
		assets.load("model/box.g3db", Model.class);
		assets.load("model/gun.g3db", Model.class);
		assets.load("model/mask.g3db", Model.class);
		assets.load("model/skybox.g3db", Model.class);
		assets.load("model/shard.g3db", Model.class);
		assets.load("model/hook_target.g3db", Model.class);

		// Sounds
		assets.load("sound/jump.wav", Sound.class);
		assets.load("sound/jump1.wav", Sound.class);
		assets.load("sound/jump2.wav", Sound.class);
		assets.load("sound/jump3.wav", Sound.class);
		assets.load("sound/jump4.wav", Sound.class);

		assets.load("sound/shatter.wav", Sound.class);
		assets.load("sound/mask_hit1.wav", Sound.class);
		assets.load("sound/mask_hit2.wav", Sound.class);
		assets.load("sound/mask_hit3.wav", Sound.class);
		assets.load("sound/mask_hit4.wav", Sound.class);

		assets.load("sound/wall_hit1.wav", Sound.class);
		assets.load("sound/wall_hit2.wav", Sound.class);
		assets.load("sound/wall_hit3.wav", Sound.class);
		assets.load("sound/wall_hit4.wav", Sound.class);
		assets.load("sound/wall_hit5.wav", Sound.class);
		assets.load("sound/wall_hit6.wav", Sound.class);
		assets.load("sound/wall_hit7.wav", Sound.class);

		assets.load("sound/mask_pickup.wav", Sound.class);

		assets.load("sound/shoot.wav", Sound.class);
		assets.load("sound/run.wav", Sound.class);
		assets.load("sound/walk.wav", Sound.class);
		assets.load("sound/climb.wav", Sound.class);
		assets.load("sound/music_game.ogg", Music.class);

		Bullet.init();
		instances = new ArrayMap<String, Array<GameObject>>();
		billboards = new Array<Billboard>();
		billboards.ordered = true;
		billboardOverlays = new ArrayMap<GameObject, BillboardOverlay>();
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
		skybox = new ModelInstance(
				assets.get("model/skybox.g3db", Model.class));

		// Create a player, a gun and load the level from CSV
		player = spawnPlayer(game, viewport, screenCenter);
		loadLevel(currentLevel);

	}

	public void loadLevel(int level) {
		for (Entry<String, Array<GameObject>> entry : instances) {
			for (GameObject obj : entry.value) {
				collisionHandler.dynamicsWorld.removeCollisionObject(obj.body);
			}
		}
		if (taskRemoveShards != null && taskRemoveShards.isScheduled()) {
			taskRemoveShards.cancel();
			Gdx.app.debug(tag, "Canceled remove shards task");
		}
		// for (Entry<String, Array<GameObject>> entry : instances) {
		// for (GameObject obj : entry.value) {
		// System.out.println("destroy "+obj.id);
		// destroy(obj);
		// }
		// }
		instances.clear();
		// for (Entry<GameObject, BillboardOverlay> entry : billboardOverlays) {
		// entry.value.dispose();
		// }
		billboardOverlays.clear();
		// for (Billboard b : billboards) {
		// b.dispose();
		// }
		billboards.clear();
		envTags.clear();
		player.reset();
		loadLevelCSV(levels[level]);

		// GameObject gun = spawn("gun", player.position.cpy(), new Vector3(),
		// false, false, false, CollisionHandler.OBJECT_FLAG,
		// CollisionHandler.GROUND_FLAG);
		// player.setGun(gun);

		GameObject hook = spawn("hook", player.position.cpy(), new Vector3(),
				false, false, true, true, CollisionHandler.OBJECT_FLAG,
				CollisionHandler.GROUND_FLAG);
		addInstance(hook);
		player.addToInventory(hook);

		GameObject blowpipe = spawn("blowpipe", player.position.cpy(),
				new Vector3(), false, false, false, false,
				CollisionHandler.OBJECT_FLAG, CollisionHandler.GROUND_FLAG);
		addInstance(blowpipe);
		player.addToInventory(blowpipe);

		player.equipFromInventory("blowpipe");
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
			boolean callback, short belongsToFlag, short collidesWithFlag) {

		if (!gameObjectFactory.containsKey(name)) {
			String filePath = String.format("model/%s.g3db", name);
			Gdx.app.debug(tag,
					String.format("Creating collision shape for %s", filePath));

			assets.load(filePath, Model.class);
			try {
				assets.finishLoading();
			} catch (Exception e) {
				Gdx.app.debug(tag, "Could not load assets ", e);
			}

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

		if (callback) {
			obj.body.setCollisionFlags(obj.body.getCollisionFlags()
					| btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		}

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
	private void loadLevelCSV(String csvPath) {
		ArrayMap<String, Array<String>> textMap = TextParser
				.parse(Gdx.files.internal("text/billboards.txt"));

		Gdx.app.debug(tag, String.format("Loading CSV data from %s", csvPath));
		String csv = Gdx.files.internal(csvPath).readString();
		Gdx.app.debug(tag, String.format("Content: \n%s", csv));

		// String[] lines = csv.split(System.getProperty("line.separator"));
		String[] lines = csv.split("\n");
		for (String line : lines) {
			String tagName;
			int tagIndex;
			Vector3 pos = new Vector3();
			Vector3 rot = new Vector3();
			String[] values = line.split(";");
			Gdx.app.debug(tag, "Parsing: " + Arrays.toString(values));
			try {
				tagName = values[0];
				Gdx.app.debug(tag, "name: " + tagName);
				tagIndex = Integer.parseInt(values[1]);
				Gdx.app.debug(tag, "index: " + tagIndex);
				pos.set(Float.parseFloat(values[2]),
						Float.parseFloat(values[3]),
						Float.parseFloat(values[4]));
				Gdx.app.debug(tag, "pos: " + pos);
				rot.set(Float.parseFloat(values[5]),
						Float.parseFloat(values[6]),
						Float.parseFloat(values[7]));
				Gdx.app.debug(tag, "rot: " + rot);
			} catch (Exception e) {
				Gdx.app.debug(tag, "Error when parsing csv file.", e);
				Gdx.app.exit();
				continue;
			}

			blenderToGameCoords(rot);
			blenderToGameCoords(pos);

			Gdx.app.debug(tag, "Loading object " + tagName);

			if (tagName.equals("mask")) {
				GameObject mask = spawn(tagName, pos, rot, false, true, false,
						false, CollisionHandler.OBJECT_FLAG,
						CollisionHandler.ALL_FLAG);
				billboardOverlays.put(mask, new BillboardOverlay(pos, 3f, 3f, 0,
						"shader/common.vert", "shader/sun.frag"));

			} else if (tagName.equals("box")) {
				spawn(tagName, pos, rot, true, false, true, false,
						CollisionHandler.OBJECT_FLAG,
						CollisionHandler.ALL_FLAG);

			} else if (tagName.equals("start_position")) {
				player.playerObject.position(pos);

			} else if (tagName.startsWith("text_tag")) {

				String textTagName = tagName.split("_")[2];

				if (!textMap.containsKey(textTagName)
						|| textMap.get(textTagName).size - 1 < tagIndex) {
					Gdx.app.debug(tag,
							String.format("Could not find text for %s index %s",
									textTagName, tagIndex));
				} else {
					String textTagText = textMap.get(textTagName).get(tagIndex);
					spawnBillboard(pos.sub(0, 1, 0), textTagText);
				}

			} else if (tagName.equals("fog_tag")) {
				envTags.add(new EnvTag(pos, 80, 30, Color.GRAY, 30));

			} else if (tagName.startsWith("sound_tag")) {

			} else if (tagName.equals("sun_tag")) {
				envTags.add(new EnvTag(pos, 100, 20, Color.WHITE, 30));

			} else if (tagName.equals("empty")) {

			} else {
				spawn(tagName, pos, rot, false, false, false, false,
						CollisionHandler.GROUND_FLAG,
						CollisionHandler.ALL_FLAG);
			}
		}
		Gdx.app.debug(tag, "Finished loading CSV.");
	}

	public void spawnBillboard(Vector3 pos, String text) {
		Color textColor = new Color(Color.WHITE).mul(1f, 1f, 1f, 1f);
		Color bkgColor = new Color(Color.GRAY).mul(1f, 1f, 1f, 0f);

		Gdx.app.debug(tag, String.format("Spawning billboard at %s", pos));
		billboards.add(new Billboard(pos, 2f, 2f, 20f, text, textColor,
				bkgColor, font));

	}

	/**
	 * Object factory blueprint for a 3D model along with an appropriate
	 * collision shape.
	 */
	private static void createFactoryDefs(AssetManager assets,
			ArrayMap<String, GameObject.Constructor> gameObjectFactory) {

		Model modelCompass = buildCompassModel();
		gameObjectFactory.put("compass",
				new GameObject.Constructor(modelCompass,
						Bullet.obtainStaticNodeShape(modelCompass.nodes), 0));
		Gdx.app.debug(tag, "Loaded compass model");

		Model modelBlowpipe = assets.get("model/blowpipe.g3db", Model.class);
		gameObjectFactory.put("blowpipe", new GameObject.Constructor(
				modelBlowpipe,
				new btBoxShape(getBoundingBoxHalfExtents(modelBlowpipe)), 5f));
		Gdx.app.debug(tag, "Loaded blowpipe model");

		Model modelHook = assets.get("model/grappling_hook.g3db", Model.class);
		gameObjectFactory.put("hook", new GameObject.Constructor(modelHook,
				new btBoxShape(getBoundingBoxHalfExtents(modelHook)), 1f));
		Gdx.app.debug(tag, "Loaded hook model");

		Model modelHookTrail = assets.get("model/grappling_hook_trail.g3db",
				Model.class);
		gameObjectFactory.put("hook_trail",
				new GameObject.Constructor(modelHookTrail,
						new btBoxShape(
								getBoundingBoxHalfExtents(modelHookTrail)),
						5f));
		Gdx.app.debug(tag, "Loaded hook trail model");

		Model modelBox = assets.get("model/box.g3db", Model.class);
		gameObjectFactory.put("box", new GameObject.Constructor(modelBox,
				new btBox2dShape(new Vector3(0.5f, 0.5f, 0.5f)), 1f));
		Gdx.app.debug(tag, "Loaded box model");

		Model modelGun = assets.get("model/gun.g3db", Model.class);
		gameObjectFactory.put("gun", new GameObject.Constructor(modelGun,
				new btBoxShape(getBoundingBoxHalfExtents(modelGun)), 5f));
		Gdx.app.debug(tag, "Loaded gun model");

		Model modelSphere = assets.get("model/mask.g3db", Model.class);
		gameObjectFactory.put("mask", new GameObject.Constructor(modelSphere,
				Bullet.obtainStaticNodeShape(modelSphere.nodes), 0));
		Gdx.app.debug(tag, "Loaded mask model");

		Model modelHookTarget = assets.get("model/hook_target.g3db",
				Model.class);
		gameObjectFactory.put("hook_target",
				new GameObject.Constructor(modelHookTarget,
						Bullet.obtainStaticNodeShape(modelHookTarget.nodes),
						0));
		Gdx.app.debug(tag, "Loaded hook target model");

		Model modelPlayer = new ModelBuilder()
				.createCapsule(GameSettings.PLAYER_RADIUS,
						GameSettings.PLAYER_HEIGHT
								- 2 * GameSettings.PLAYER_RADIUS,
						4, GL20.GL_TRIANGLES, new Material(),
						Usage.Position | Usage.Normal);
		gameObjectFactory.put("player",
				new GameObject.Constructor(modelPlayer,
						new btCapsuleShape(GameSettings.PLAYER_RADIUS,
								GameSettings.PLAYER_HEIGHT
										- 2 * GameSettings.PLAYER_RADIUS),
						100));
		Gdx.app.debug(tag, "Loaded player model");

		Model modelShard = assets.get("model/shard.g3db", Model.class);
		gameObjectFactory.put("shard", new GameObject.Constructor(modelShard,
				new btConeShape(0.2f, 0.4f), 1));

		Gdx.app.debug(tag, "Loaded shard model");
	}

	private static Vector3 getBoundingBoxHalfExtents(Model model) {
		BoundingBox bBox = new BoundingBox();
		Vector3 dim = new Vector3();
		model.calculateBoundingBox(bBox);
		bBox.getDimensions(dim);
		return dim.scl(0.5f);
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
		for (Entry<GameObject, BillboardOverlay> entry : billboardOverlays) {
			entry.value.dispose();
		}
		billboardOverlays.clear();
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

		GameObject obj = spawn("player", new Vector3(), new Vector3(), false,
				false, true, true, CollisionHandler.PLAYER_FLAG,
				(short) (CollisionHandler.GROUND_FLAG
						| CollisionHandler.OBJECT_FLAG));

		instances.removeKey("player");

		obj.body.setAngularFactor(Vector3.Y);

		PlayerSound sound = new PlayerSound(assets);

		Player player = new Player(game, obj, screenCenter, viewport, this,
				sound);

		return player;
	}

	public btRigidBody rayTest(Ray ray, Vector3 point, short mask,
			float maxDistance) {
		return collisionHandler.rayTest(ray, point, mask, maxDistance);
	}

	public void update(float delta) {
		// Update collisions
		collisionHandler.dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);

		// Update player transform from user input
		player.update(delta);
		player.playerObject.body
				.getWorldTransform(player.playerObject.transform);

		for (Entry<String, Array<GameObject>> entry : instances) {
			for (GameObject obj : entry.value) {
				obj.body.getWorldTransform(obj.transform);
			}
		}
		for (Billboard b : billboards) {
			b.update(viewport.getCamera());
		}

	}

	public GameObject getGameObject(btCollisionObject co) {
		if (co == null) {
			return null;
		}
		GameObject go = null;
		for (Entry<String, Array<GameObject>> entry : instances) {
			for (GameObject obj : entry.value) {
				btCollisionObject o = (btCollisionObject) obj.body;
				if (o.equals(co)) {
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
					false, false, CollisionHandler.OBJECT_FLAG,
					CollisionHandler.ALL_FLAG);
			obj.body.setAngularFactor(Vector3.X);
			obj.body.setLinearVelocity(randAndNor(lin_vel).scl(50));
			shards.add(obj);
		}
		taskRemoveShards = Timer.schedule(new Task() {

			@Override
			public void run() {
				for (GameObject obj : shards) {
					destroy(obj);
				}
			}
		}, 2);
	}

	private Task taskRemoveShards;

	public void destroy(GameObject obj) {
		collisionHandler.dynamicsWorld.removeCollisionObject(obj.body);
		instances.get(obj.id).removeValue(obj, true);

		Vector3 pos = new Vector3();
		obj.transform.getTranslation(pos);
		Gdx.app.debug(tag, String.format("Destroyed %s at %s, %s remaining.",
				obj.id, pos, numberSpawned(obj.id)));

		if (obj.id.equals("mask")) {
			shatter(pos);
			BillboardOverlay o = billboardOverlays.get(obj);
			billboardOverlays.removeKey(obj);
			o.dispose();
			if (numberSpawned("mask") == 0) {
				loadNextLevel();
			}
		}
		// obj.dispose();
	}

	public void loadNextLevel() {
		currentLevel++;
		if (currentLevel == levels.length) {
			currentLevel = 0;
		}
		Gdx.app.debug(tag, "Loading level " + currentLevel);
		loadLevel(currentLevel);
		Gdx.app.debug(tag, "Finished loading level " + currentLevel);
	}

	public void loadPrevLevel() {
		currentLevel--;
		if (currentLevel == -1) {
			currentLevel = levels.length - 1;
		}
		Gdx.app.debug(tag, "Loading level " + currentLevel);
		loadLevel(currentLevel);
		Gdx.app.debug(tag, "Finished loading level " + currentLevel);
	}

	public void reloadLevel() {
		Gdx.app.debug(tag, "Loading level " + currentLevel);
		loadLevel(currentLevel);
		Gdx.app.debug(tag, "Finished loading level " + currentLevel);
	}

	/**
	 * Create a model of a 3D compass with arrows along the three axis.
	 * 
	 * @return
	 */
	private static Model buildCompassModel() {
		float compassScale = 5;
		ModelBuilder modelBuilder = new ModelBuilder();
		Model arrow = modelBuilder.createArrow(Vector3.Zero,
				Vector3.Y.cpy().scl(compassScale), null,
				Usage.Position | Usage.Normal);
		modelBuilder.begin();

		Mesh zArrow = arrow.meshes.first().copy(false);
		zArrow.transform(new Matrix4().rotate(Vector3.X, 90));
		modelBuilder.part("part1", zArrow, GL20.GL_TRIANGLES,
				new Material(ColorAttribute.createDiffuse(Color.BLUE)));

		modelBuilder.node();
		Mesh yArrow = arrow.meshes.first().copy(false);
		modelBuilder.part("part2", yArrow, GL20.GL_TRIANGLES,
				new Material(ColorAttribute.createDiffuse(Color.GREEN)));

		modelBuilder.node();
		Mesh xArrow = arrow.meshes.first().copy(false);
		xArrow.transform(new Matrix4().rotate(Vector3.Z, -90));
		modelBuilder.part("part3", xArrow, GL20.GL_TRIANGLES,
				new Material(ColorAttribute.createDiffuse(Color.RED)));

		arrow.dispose();
		return modelBuilder.end();
	}
}
