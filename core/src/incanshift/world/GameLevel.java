package incanshift.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import incanshift.gameobjects.*;
import incanshift.screen.AbstractScreen;

import java.util.Arrays;

public class GameLevel implements Disposable {


	public static final String tag = "GameLevel";
	public Array<Billboard> billboards;
	public ArrayMap<GameObject, BillboardOverlay> billboardOverlays;
	public Array<EnvTag> envTags;
	public Array<SoundTag> soundTags;
	public ArrayMap<String, Array<GameObject>> instances;
	// public ModelInstance skybox;
	public Vector3 playerStartPosition = new Vector3();
	GameObjectFactory gameObjectFactory;
	AssetManager assets = new AssetManager();

	public int numberOfMasksAtCreation = 0;

	public GameLevel(String csvPath, GameObjectFactory gameObjectFactory) {
		this.gameObjectFactory = gameObjectFactory;
		instances = new ArrayMap<String, Array<GameObject>>();
		billboards = new Array<Billboard>();
		billboards.ordered = true;
		billboardOverlays = new ArrayMap<GameObject, BillboardOverlay>();
		envTags = new Array<EnvTag>();
		soundTags = new Array<SoundTag>();


		loadLevelCSV(csvPath);
		// skybox = new ModelInstance(
		// assets.get("model/skybox.g3db", Model.class));
		Gdx.app.debug(tag, "Finished constructing level from " + csvPath);
	}

	private static Vector3 blenderToGameCoords(Vector3 v) {
		return v.set(v.x, v.z, -v.y);
	}

	private void addInstance(GameObject obj) {
		if (!instances.containsKey(obj.id)) {
			instances.put(obj.id, new Array<GameObject>());
		}
		instances.get(obj.id).add(obj);
	}

	@Override
	public void dispose() {
		for (Entry<String, Array<GameObject>> entry : instances) {
			for (GameObject obj : entry.value) {
				obj.dispose();
			}
		}
		for (Entry<GameObject, BillboardOverlay> entry : billboardOverlays) {
			entry.value.dispose();
		}
		for (Billboard b : billboards) {
			b.dispose();
		}
		for (SoundTag tag : soundTags) {
			tag.sound.dispose();
		}
		assets.dispose();
		gameObjectFactory.clearNonFactoryDef();
	}

	private Array<BlenderTag> readtags(String csvPath) {
		Array<BlenderTag> tags = new Array<GameLevel.BlenderTag>();
		Gdx.app.debug(tag, String.format("Loading CSV data from %s", csvPath));
		String csv = Gdx.files.internal(csvPath).readString();
		// Gdx.app.debug(tag, String.format("Content: \n%s", csv));
		String[] lines = csv.split("\n");

		for (String line : lines) {
			BlenderTag btag = new BlenderTag();
			String[] values = line.split(";");
			Gdx.app.debug(tag, "Parsing: " + Arrays.toString(values));
			try {
				btag.name = values[0];
				// Gdx.app.debug(tag, "name: " + tagName);
				btag.index = Integer.parseInt(values[1]);
				// Gdx.app.debug(tag, "index: " + tagIndex);
				btag.pos.set(Float.parseFloat(values[2]),
						Float.parseFloat(values[3]),
						Float.parseFloat(values[4]));
				// Gdx.app.debug(tag, "pos: " + pos);
				btag.rot.set(Float.parseFloat(values[5]),
						Float.parseFloat(values[6]),
						Float.parseFloat(values[7]));
				// Gdx.app.debug(tag, "rot: " + rot);
			} catch (Exception e) {
				Gdx.app.debug(tag, "Error when parsing csv file.", e);
				Gdx.app.exit();
				continue;
			}
			blenderToGameCoords(btag.rot);
			blenderToGameCoords(btag.pos);
			tags.add(btag);
		}
		return tags;
	}

	/**
	 * Read a CSV file and create the objects listed in it.
	 *
	 * @param csvPath
	 */
	private void loadLevelCSV(String csvPath) {
		ArrayMap<String, Array<String>> textMap = TextParser
				.parse(Gdx.files.internal("text/billboards.txt"));

		Array<BlenderTag> tags = readtags(csvPath);

		for (BlenderTag btag : tags) {
			Gdx.app.debug(tag, "Loading object " + btag.name);

			if (btag.name.equals("mask")) {
				GameObject mask = spawn(btag.name, btag.pos, btag.rot, false,
						true, false, false, CollisionHandler.OBJECT_FLAG,
						CollisionHandler.ALL_FLAG);
				billboardOverlays.put(mask, new BillboardOverlay(btag.pos, 3f,
						3f, 0, "shader/common.vert", "shader/sun.frag"));
				numberOfMasksAtCreation++;
				continue;
			}
			if (btag.name.equals("box")) {
				spawn(btag.name, btag.pos, btag.rot, true, false, true, false,
						CollisionHandler.OBJECT_FLAG,
						CollisionHandler.ALL_FLAG);
				continue;
			}
			if (btag.name.equals("start_position")) {
				playerStartPosition.set(btag.pos);
				continue;
			}
			if (btag.name.startsWith("text_tag")) {

				String textTagName = btag.name.split("_")[2];

				if (!textMap.containsKey(textTagName)
						|| textMap.get(textTagName).size - 1 < btag.index) {
					Gdx.app.debug(tag,
							String.format("Could noNt find text for %s index %s",
									textTagName, btag.index));
				} else {
					String textTagText = textMap.get(textTagName)
							.get(btag.index);
					spawnBillboard(btag.pos.sub(0, 1, 0), textTagText);
				}
				continue;
			}
			if (btag.name.equals("fog_tag")) {
				envTags.add(new EnvTag(btag.pos, 130, 30, Color.GRAY, 40));
				Gdx.app.debug(tag, "Added fog tag at " + btag.pos);
				continue;
			}
			if (btag.name.startsWith("sound_tag")) {
				String name = "test";
				soundTags.add(new SoundTag("sound/" + name + ".ogg", btag.pos, 10));
				Gdx.app.debug(tag, "Added sound tag at " + btag.pos);
				continue;
			}
			if (btag.name.equals("sun_tag")) {
				envTags.add(new EnvTag(btag.pos, 100, 20, Color.WHITE, 40));
				continue;
			}
			if (btag.name.equals("empty")) {
				continue;
			}
			// Object not predefined, spawn as ground.
			spawn(btag.name, btag.pos, btag.rot, false, false, false, false,
					CollisionHandler.GROUND_FLAG,
					CollisionHandler.ALL_FLAG);
		}
		Gdx.app.debug(tag, "Finished loading CSV.");

	}

	public int numberSpawned(String id) {
		if (!instances.containsKey(id)) {
			return 0;
		}
		return instances.get(id).size;
	}

	public void remove(GameObject obj) {
		instances.get(obj.id).removeValue(obj, true);
	}

	public void removeOverlay(GameObject obj) {
		if (!billboardOverlays.containsKey(obj)) {
			return;
		}
		BillboardOverlay maskOverlay = billboardOverlays.get(obj);
		maskOverlay.dispose();
		billboardOverlays.removeKey(obj);
	}


	/**
	 * Spawn a game object from the factory and add it to the world. If not
	 * defined in factory, load the model from file system and generate a static
	 * collision shape for it.
	 *
	 * @param name             Factory name for the object.
	 * @param pos              Object position.
	 * @param rot              Object rotation.
	 * @param movable          True if the player can move this object.
	 * @param removable        True if the player can destroy this object.
	 * @param noDeactivate     True if collision simulation should never be suspended for
	 *                         this object.
	 * @param callback         If true, use a contact contact callback flag for this object.
	 * @param belongsToFlag    Collision flag/mask for the group this object belongs to.
	 * @param collidesWithFlag Collision flag/mask for the group this object can collide
	 *                         with.
	 * @return The created game object.
	 */
	public GameObject spawn(String name, Vector3 pos, Vector3 rot,
							boolean movable, boolean removable, boolean noDeactivate,
							boolean callback, short belongsToFlag, short collidesWithFlag) {
		GameObject obj = gameObjectFactory.build(name, pos, rot, movable, removable, noDeactivate, callback, belongsToFlag, collidesWithFlag);
		addInstance(obj);
		return obj;
	}

	public void spawnBillboard(Vector3 pos, String text) {
		Color textColor = new Color(Color.WHITE).mul(1f, 1f, 1f, 1f);
		Color bkgColor = new Color(Color.GRAY).mul(1f, 1f, 1f, 0f);

		Gdx.app.debug(tag, String.format("Spawning billboard at %s", pos));
		billboards.add(new Billboard(pos, 2f, 2f, 20f, text, textColor,
				bkgColor, AbstractScreen.sansHuge));

	}

	private class BlenderTag {
		String name;
		int index;
		Vector3 pos = new Vector3();
		Vector3 rot = new Vector3();
	}

}
