package incanshift.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.Viewport;
import incanshift.IncanShift;
import incanshift.gameobjects.*;
import incanshift.player.Player;
import incanshift.player.PlayerSound;

import java.util.Random;

public class GameWorld implements Disposable {

	public String[] levels = { //
			"model/outside_level.csv", //
			"model/inside_level1_jump_and_shoot.csv", //
			"model/inside_level2_three_levels.csv", //--
			"model/inside_level10_throw_out_the_bodies.csv", //
			"model/inside_level4_chair.csv", //
			"model/inside_level8_ant_hive.csv", //
			"model/inside_level3_3d_space.csv", //
			"model/inside_level9_pillars_in_a_hill_of_stairs.csv", //
			"model/inside_level6_ziggurat_room.csv", //
			"model/inside_level7_ziggurat_dissolved.csv", //
			"model/forest.csv", //
			// "model/inside_level5_l.csv", //
	};

	final static String tag = "GameWorld";
	public CollisionHandler collisionHandler;
	public int currentLevelIndex = 0;
	public GameObjectFactory gameObjectFactory;
	public Music music;
	public Player player;
	public boolean xRayMask = false;
	GameLevel currentGameLevel;
	IncanShift game;
	Viewport viewport;
	private AssetManager assets;
	private Array<Task> removeShardsTasks = new Array<Timer.Task>();

	public GameWorld(IncanShift game, Viewport viewport, Vector3 screenCenter,
					 BitmapFont font) {
		Gdx.app.debug(tag, String.format("Creating game world"));
		// this.font = font;
		this.viewport = viewport;
		this.game = game;

		// Sounds
		assets = new AssetManager();
		assets.load("sound/ambience_music.ogg", Music.class);

		Bullet.init();
		gameObjectFactory = new GameObjectFactory();
		collisionHandler = new CollisionHandler();

		Gdx.app.debug(tag, String.format("Trying to load assets..."));
		try {
			assets.finishLoading();
		} catch (GdxRuntimeException e) {
			Gdx.app.debug(tag, "Could not load assets, ", e);
		}
		Gdx.app.debug(tag, String.format("Assets finished loading."));

		music = assets.get("sound/ambience_music.ogg", Music.class);

		// Create a player, weapons, and load the level from CSV
		player = spawnPlayer(game, viewport, screenCenter);
		GameObject hook = gameObjectFactory.build("hook", player.position.cpy(), new Vector3(),
				false, false, true, true, CollisionHandler.OBJECT_FLAG,
				CollisionHandler.GROUND_FLAG);
		player.addToInventory(hook);

		GameObject blowpipe = gameObjectFactory.build("blowpipe",
				player.position.cpy(), new Vector3(), false, false, false,
				false, CollisionHandler.OBJECT_FLAG,
				CollisionHandler.GROUND_FLAG);
		player.addToInventory(blowpipe);

		player.equipFromInventory("blowpipe");

		collisionHandler.add(player);
		loadLevel(currentLevelIndex);
		Gdx.app.debug(tag, "GameWorld constructor finished.");
	}

	public void destroy(GameObject obj) {

		if (!obj.id.equals("mask")) {
			return;
		}

		currentGameLevel.remove(obj);
		currentGameLevel.removeOverlay(obj);
		collisionHandler.remove(obj);

		Vector3 pos = new Vector3();
		obj.transform.getTranslation(pos);
		shatter(pos);

	}

	@Override
	public void dispose() {
		currentGameLevel.dispose();
		collisionHandler.dispose();
		assets.dispose();
		player.dispose();
		gameObjectFactory.dispose();
	}

	public ArrayMap<GameObject, BillboardOverlay> getBillboardOverlays() {
		return currentGameLevel.billboardOverlays;
	}

	public Array<Billboard> getBillboards() {
		return currentGameLevel.billboards;
	}

	public Array<EnvTag> getEnvTags() {
		return currentGameLevel.envTags;
	}

	public GameObject getGameObject(btCollisionObject co) {
		if (co == null) {
			return null;
		}
		GameObject go = null;
		for (Entry<String, Array<GameObject>> entry : currentGameLevel.instances) {
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

	public ArrayMap<String, Array<GameObject>> getInstances() {
		return currentGameLevel.instances;
	}

	public void loadLevel(int index) {
		Gdx.app.debug(tag, "Loading level " + index);

		for (Task removeTask : removeShardsTasks) {
			if (removeTask.isScheduled()) {
				removeTask.run();
				removeTask.cancel();
				Gdx.app.debug(tag, "Forcibly ran remove shards task");
			}
		}
		removeShardsTasks.clear();

		if (currentGameLevel != null) {

			for (Entry<String, Array<GameObject>> entry : currentGameLevel.instances) {
				for (GameObject obj : entry.value) {
					collisionHandler.remove(obj);
				}
			}
			currentGameLevel.dispose();
		}

		currentGameLevel = new GameLevel(levels[index], gameObjectFactory);
		player.resetActions();
		player.position(currentGameLevel.playerStartPosition);

		for (Entry<String, Array<GameObject>> entry : currentGameLevel.instances) {
			for (GameObject obj : entry.value) {
				collisionHandler.add(obj);
			}
		}


		Gdx.app.debug(tag, "Finished loading level " + currentLevelIndex);
	}

	public void loadNextLevel() {
		currentLevelIndex++;
		if (currentLevelIndex == levels.length) {
			currentLevelIndex = 0;
		}
		loadLevel(currentLevelIndex);
	}

	public void loadPrevLevel() {
		currentLevelIndex--;
		if (currentLevelIndex == -1) {
			currentLevelIndex = levels.length - 1;
		}
		loadLevel(currentLevelIndex);
	}

	public void music(boolean on) {
		// Play some music
		if (music != null) {
			music.play();
			music.setVolume(1f * GameSettings.MUSIC_VOLUME);
			music.setLooping(true);
		}
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

	public btRigidBody rayTest(Ray ray, Vector3 point, short mask,
							   float maxDistance) {
		return collisionHandler.rayTest(ray, point, mask, maxDistance);
	}

	public void reloadLevel() {
		loadLevel(currentLevelIndex);
	}

	public void shatter(Vector3 pos) {
		Vector3 lin_vel = new Vector3();
		final Array<GameObject> shards = new Array<GameObject>();
		for (int i = 0; i < 25; i++) {
			GameObject obj = currentGameLevel.spawn("shard", pos, new Vector3(),
					true, false, false, false, CollisionHandler.OBJECT_FLAG,
					CollisionHandler.ALL_FLAG);
			collisionHandler.add(obj);
			obj.body.setAngularFactor(Vector3.X);
			obj.body.setLinearVelocity(randAndNor(lin_vel).scl(50));
			shards.add(obj);
		}
		Task removeTask = new Task() {

			@Override
			public void run() {
				Gdx.app.debug(tag, "Clearing shards.");
				for (GameObject obj : shards) {
					Gdx.app.debug(tag, obj.toString());
					// destroy(obj);
					collisionHandler.remove(obj);
					currentGameLevel.instances.get(obj.id).removeValue(obj, true);
				}
			}
		};
		Timer.schedule(removeTask, 2);
		removeShardsTasks.add(removeTask);
	}

	public Player spawnPlayer(IncanShift game, Viewport viewport,
							  Vector3 screenCenter) {
		Gdx.app.debug(tag, "Creating player");
		PlayerSound sound = new PlayerSound();

		Model model = gameObjectFactory.get("player").model;

		btRigidBody.btRigidBodyConstructionInfo constructionInfo = gameObjectFactory
				.get("player").constructionInfo;

		Player obj = new Player(model, constructionInfo, game, screenCenter,
				viewport, this, sound);

		obj.id = "player";
		obj.body.setCollisionFlags(obj.body.getCollisionFlags()
				| btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		obj.body.setActivationState(Collision.DISABLE_DEACTIVATION);
		obj.body.setContactCallbackFlag(CollisionHandler.PLAYER_FLAG);
		obj.belongsToFlag = CollisionHandler.PLAYER_FLAG;
		obj.collidesWithFlag = (short) (CollisionHandler.GROUND_FLAG
				| CollisionHandler.OBJECT_FLAG);

		obj.body.setAngularFactor(Vector3.Y);

		Gdx.app.debug(tag, "Finished creating player");
		return obj;
	}

	public void update(float delta) {
		if (currentGameLevel.numberOfMasksAtCreation != 0 && currentGameLevel.numberSpawned("mask") == 0) {
			loadNextLevel();
		}

		collisionHandler.stepSimulation(delta);

		for (Entry<String, Array<GameObject>> entry : currentGameLevel.instances) {
			for (GameObject obj : entry.value) {
				obj.body.getWorldTransform(obj.transform);
			}
		}
		for (Billboard board : currentGameLevel.billboards) {
			board.update(viewport.getCamera());
		}
		for (SoundTag tag : currentGameLevel.soundTags) {
			if (!tag.finishedPlaying && (player.position.dst(tag.position) < tag.distance)) {
				tag.play();
			}
		}

		player.update(delta);
		player.body.getWorldTransform(player.transform);

	}

}
