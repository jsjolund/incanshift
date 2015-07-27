package incanshift.world;

import java.util.Arrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap.Entry;

import incanshift.gameobjects.Billboard;
import incanshift.gameobjects.BillboardOverlay;
import incanshift.gameobjects.EnvTag;
import incanshift.gameobjects.GameObject;
import incanshift.gameobjects.GameObjectFactory;
import incanshift.gameobjects.TextParser;
import incanshift.screen.AbstractScreen;

public class Level implements Disposable {

	public static final String tag = "Level";

	private static Vector3 blenderToGameCoords(Vector3 v) {
		return v.set(v.x, v.z, -v.y);
	}

	public Array<Billboard> billboards;
	public ArrayMap<GameObject, BillboardOverlay> billboardOverlays;
	public Array<EnvTag> envTags;
	public ArrayMap<String, Array<GameObject>> instances;

	private Array<String> nonFactoryDef;

	// public ModelInstance skybox;
	public Vector3 playerStartPosition = new Vector3();
	GameObjectFactory gameObjectFactory;

	AssetManager assets = new AssetManager();

	public Level(String csvPath, GameObjectFactory gameObjectFactory) {
		this.gameObjectFactory = gameObjectFactory;
		instances = new ArrayMap<String, Array<GameObject>>();
		billboards = new Array<Billboard>();
		billboards.ordered = true;
		billboardOverlays = new ArrayMap<GameObject, BillboardOverlay>();
		envTags = new Array<EnvTag>();
		nonFactoryDef = new Array<String>();
		loadLevelCSV(csvPath);
		// skybox = new ModelInstance(
		// assets.get("model/skybox.g3db", Model.class));
		Gdx.app.debug(tag, "Finished constructing level from " + csvPath);
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
		assets.dispose();
		for (String id : nonFactoryDef) {
			Gdx.app.debug(tag, "Removing custom factory def " + id);
			gameObjectFactory.remove(id);
		}
	}

	private class BlenderTag {
		String name;
		int index;
		Vector3 pos = new Vector3();
		Vector3 rot = new Vector3();
	}

	private Array<BlenderTag> readtags(String csvPath) {
		Array<BlenderTag> tags = new Array<Level.BlenderTag>();
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
	 * @param csv
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

			} else if (btag.name.equals("box")) {
				spawn(btag.name, btag.pos, btag.rot, true, false, true, false,
						CollisionHandler.OBJECT_FLAG,
						CollisionHandler.ALL_FLAG);

			} else if (btag.name.equals("start_position")) {
				playerStartPosition.set(btag.pos);

			} else if (btag.name.startsWith("text_tag")) {

				String textTagName = btag.name.split("_")[2];

				if (!textMap.containsKey(textTagName)
						|| textMap.get(textTagName).size - 1 < btag.index) {
					Gdx.app.debug(tag,
							String.format("Could not find text for %s index %s",
									textTagName, btag.index));
				} else {
					String textTagText = textMap.get(textTagName)
							.get(btag.index);
					spawnBillboard(btag.pos.sub(0, 1, 0), textTagText);
				}

			} else if (btag.name.equals("fog_tag")) {
				envTags.add(new EnvTag(btag.pos, 80, 30, Color.GRAY, 30));

			} else if (btag.name.startsWith("sound_tag")) {

			} else if (btag.name.equals("sun_tag")) {
				envTags.add(new EnvTag(btag.pos, 100, 20, Color.WHITE, 30));

			} else if (btag.name.equals("empty")) {

			} else {
				spawn(btag.name, btag.pos, btag.rot, false, false, false, false,
						CollisionHandler.GROUND_FLAG,
						CollisionHandler.ALL_FLAG);
			}
		}
		Gdx.app.debug(tag, "Finished loading CSV.");

	}

	public int numberSpawned(String id) {
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
			nonFactoryDef.add(name);

		} else {
			Gdx.app.debug(tag, name + " exists in factory.");
		}

		GameObject obj = gameObjectFactory.construct(name);
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

		obj.belongsToFlag = belongsToFlag;
		obj.collidesWithFlag = collidesWithFlag;

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

}
