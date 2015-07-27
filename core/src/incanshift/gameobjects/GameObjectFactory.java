package incanshift.gameobjects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btConeShape;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

import incanshift.gameobjects.GameObject.Constructor;
import incanshift.world.GameSettings;

public class GameObjectFactory implements Disposable {

	public final static String tag = "GameObjectFactory";

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

	/**
	 * Object factory blueprint for a 3D model along with an appropriate
	 * collision shape.
	 */
	private static void createFactoryDefs(AssetManager assets,
										  ArrayMap<String, GameObject.Constructor> gameObjectMap) {
		Gdx.app.debug(tag, "Building factory def models.");

		Model modelCompass = buildCompassModel();

		gameObjectMap.put("compass", new GameObject.Constructor(modelCompass,
				Bullet.obtainStaticNodeShape(modelCompass.nodes), 0));
		Gdx.app.debug(tag, "Loaded compass model");

		Model modelBlowpipe = assets.get("model/blowpipe.g3db", Model.class);
		gameObjectMap.put("blowpipe", new GameObject.Constructor(modelBlowpipe,
				new btBoxShape(getBoundingBoxHalfExtents(modelBlowpipe)), 5f));
		Gdx.app.debug(tag, "Loaded blowpipe model");

		Model modelHook = assets.get("model/grappling_hook.g3db", Model.class);
		gameObjectMap.put("hook", new GameObject.Constructor(modelHook,
				new btBoxShape(getBoundingBoxHalfExtents(modelHook)), 1f));
		Gdx.app.debug(tag, "Loaded hook model");

		Model modelHookTrail = assets.get("model/grappling_hook_trail.g3db",
				Model.class);
		gameObjectMap.put("hook_trail",
				new GameObject.Constructor(modelHookTrail,
						new btBoxShape(
								getBoundingBoxHalfExtents(modelHookTrail)),
						5f));
		Gdx.app.debug(tag, "Loaded hook trail model");

		Model modelBox = assets.get("model/box.g3db", Model.class);
		gameObjectMap.put("box", new GameObject.Constructor(modelBox,
				new btBoxShape(getBoundingBoxHalfExtents(modelBox)), 1f));
		// new btBox2dShape(new Vector3(0.5f, 0.5f, modelBox)), 1f));
		Gdx.app.debug(tag, "Loaded box model");

		Model modelGun = assets.get("model/gun.g3db", Model.class);
		gameObjectMap.put("gun", new GameObject.Constructor(modelGun,
				new btBoxShape(getBoundingBoxHalfExtents(modelGun)), 5f));
		Gdx.app.debug(tag, "Loaded gun model");

		Model modelSphere = assets.get("model/mask.g3db", Model.class);
		gameObjectMap.put("mask", new GameObject.Constructor(modelSphere,
				Bullet.obtainStaticNodeShape(modelSphere.nodes), 0));
		Gdx.app.debug(tag, "Loaded mask model");

		Model modelHookTarget = assets.get("model/hook_target.g3db",
				Model.class);
		gameObjectMap.put("hook_target",
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
		gameObjectMap.put("player",
				new GameObject.Constructor(modelPlayer,
						new btCapsuleShape(GameSettings.PLAYER_RADIUS,
								GameSettings.PLAYER_HEIGHT
										- 2 * GameSettings.PLAYER_RADIUS),
						100));
		Gdx.app.debug(tag, "Loaded player model");

		Model modelShard = assets.get("model/shard.g3db", Model.class);
		gameObjectMap.put("shard", new GameObject.Constructor(modelShard,
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

	private ArrayMap<String, GameObject.Constructor> gameObjectMap;

	AssetManager assets;

	public GameObjectFactory() {
		this.assets = new AssetManager();

		assets.load("model/blowpipe.g3db", Model.class);
		assets.load("model/grappling_hook.g3db", Model.class);
		assets.load("model/grappling_hook_trail.g3db", Model.class);
		assets.load("model/box.g3db", Model.class);
		assets.load("model/gun.g3db", Model.class);
		assets.load("model/mask.g3db", Model.class);
		assets.load("model/skybox.g3db", Model.class);
		assets.load("model/shard.g3db", Model.class);
		assets.load("model/hook_target.g3db", Model.class);

		Gdx.app.debug(tag, String.format("Trying to load assets..."));
		try {
			assets.finishLoading();
		} catch (GdxRuntimeException e) {
			Gdx.app.debug(tag, "Could not load assets, ", e);
		}
		Gdx.app.debug(tag, String.format("Assets finished loading."));

		gameObjectMap = new ArrayMap<String, GameObject.Constructor>();
		createFactoryDefs(assets, gameObjectMap);
	}

	public GameObject construct(String name) {
		return gameObjectMap.get(name).construct();
	}

	public boolean containsKey(String name) {
		return gameObjectMap.containsKey(name);
	}

	public void remove(String name) {
		gameObjectMap.removeKey(name);
	}

	@Override
	public void dispose() {
		for (GameObject.Constructor ctor : gameObjectMap.values())
			ctor.dispose();
		gameObjectMap.clear();
		assets.dispose();
	}

	public Constructor get(String name) {
		return gameObjectMap.get(name);
	}

	public void put(String name, Constructor ctor) {
		gameObjectMap.put(name, ctor);
	}
}