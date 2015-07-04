package incanshift;

import sun.security.acl.WorldGroupImpl;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.CollisionObjectWrapper;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionAlgorithm;
import com.badlogic.gdx.physics.bullet.collision.btCollisionAlgorithmConstructionInfo;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld;
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btDispatcherInfo;
import com.badlogic.gdx.physics.bullet.collision.btManifoldResult;
import com.badlogic.gdx.physics.bullet.collision.btPositionAndRadius;
import com.badlogic.gdx.physics.bullet.collision.btShapeHull;
import com.badlogic.gdx.physics.bullet.collision.btSphereBoxCollisionAlgorithm;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MainScreen implements Screen {

	Viewport viewport;
	private PerspectiveCamera camera;
	private FPSInputProcessor camController;

	private ModelBatch modelBatch;
	private AssetManager assets;

	// Lights and stuff
	private Environment environment;

	// Game objects
	Array<GameObject> instances;
	ArrayMap<String, GameObject.Constructor> constructors;

	// Collision
	btCollisionConfiguration collisionConfig;
	btDispatcher dispatcher;
	private btDbvtBroadphase broadphase;
	private btCollisionWorld collisionWorld;
	private DebugDrawer debugDrawer;
	private MyContactListener contactListener;

	// Collision flags
	final static short GROUND_FLAG = 1 << 8;
	final static short OBJECT_FLAG = 1 << 9;
	final static short ALL_FLAG = -1;

	class MyContactListener extends ContactListener {
		@Override
		public boolean onContactAdded(int userValue0, int partId0, int index0, int userValue1, int partId1, int index1) {
			System.out.println("coll");
			instances.get(userValue0).moving = false;
			instances.get(userValue1).moving = false;
			return true;
		}

	}

	static class GameObject extends ModelInstance implements Disposable {

		public final btCollisionObject body;
		public boolean moving = true;

		public GameObject(Model model, btCollisionShape shape) {
			super(model);
			body = new btCollisionObject();
			body.setCollisionShape(shape);
		}

		public void position(Vector3 position) {
			transform.trn(position);
			body.setWorldTransform(transform);
		}

		@Override
		public void dispose() {
			body.dispose();
		}

		static class Constructor implements Disposable {

			public final Model model;
			public final btCollisionShape shape;

			public Constructor(Model model, btCollisionShape shape) {
				this.model = model;
				this.shape = shape;
			}

			public GameObject construct() {
				return new GameObject(model, shape);
			}

			@Override
			public void dispose() {
				shape.dispose();
			}
		}
	}

	public MainScreen(Game game) {

		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		Bullet.init();

		environment = new Environment();

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		modelBatch = new ModelBatch();

		camera = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.lookAt(Vector3.Z);

		camera.near = 1E-2f;
		camera.far = 1.5E3f;
		camera.update();

		Gdx.app.log(null, "created camera");

		assets = new AssetManager();
		assets.load("./temple.g3db", Model.class);
		assets.load("./ground.g3db", Model.class);
		assets.load("./player.g3db", Model.class);
		assets.finishLoading();

		Gdx.app.log(null, "loaded assets");

		Model modelTemple = assets.get("./temple.g3db", Model.class);
		Model modelGround = assets.get("./ground.g3db", Model.class);
		Model modelPlayer = assets.get("./player.g3db", Model.class);

		constructors = new ArrayMap<String, MainScreen.GameObject.Constructor>();

		constructors.put("temple", new GameObject.Constructor(modelTemple, Bullet.obtainStaticNodeShape(modelTemple.nodes)));
		constructors.put("ground", new GameObject.Constructor(modelGround, Bullet.obtainStaticNodeShape(modelGround.nodes)));

		constructors.put("player", new GameObject.Constructor(modelPlayer, new btCapsuleShape(0.25f, 1.8f)));

		Gdx.app.log(null, "constructed objects");

		instances = new Array<GameObject>();
		instances.add(constructors.get("ground").construct());
		instances.add(constructors.get("temple").construct());

		Gdx.app.log(null, "added instances");

		viewport = new FitViewport(1280, 720, camera);
		viewport.apply();

		GameObject player = constructors.get("player").construct();
		player.position(new Vector3(5, 2, 5));

		camController = new FPSInputProcessor(viewport, player, instances);
		Gdx.input.setInputProcessor(camController);

		Gdx.app.log(null, "created input processor");

		collisionConfig = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);
		broadphase = new btDbvtBroadphase();

		collisionWorld = new btCollisionWorld(dispatcher, broadphase, collisionConfig);

		debugDrawer = new DebugDrawer();
		collisionWorld.setDebugDrawer(debugDrawer);
		// debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);

		contactListener = new MyContactListener();
		contactListener.enable();

		player.body.setCollisionFlags(player.body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		collisionWorld.addCollisionObject(player.body, OBJECT_FLAG, GROUND_FLAG);

		for (GameObject obj : instances) {
			collisionWorld.addCollisionObject(obj.body, GROUND_FLAG, ALL_FLAG);
		}

		Gdx.app.log(null, "created collision world");
	}

	@Override
	public void dispose() {

		for (GameObject obj : instances)
			obj.dispose();
		instances.clear();

		for (GameObject.Constructor ctor : constructors.values())
			ctor.dispose();

		constructors.clear();
		collisionConfig.dispose();
		contactListener.dispose();
		dispatcher.dispose();

		collisionWorld.dispose();
		broadphase.dispose();

		modelBatch.dispose();
		assets.dispose();
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	Vector3 playerVelocity = new Vector3();

	@Override
	public void render(float dt) {
		camController.update();

		collisionWorld.performDiscreteCollisionDetection();

		Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
		int x = (Gdx.graphics.getWidth() - viewport.getScreenWidth()) / 2;
		int y = (Gdx.graphics.getHeight() - viewport.getScreenHeight()) / 2;
		Gdx.gl.glViewport(x, y, viewport.getScreenWidth(), viewport.getScreenHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(camera);
		modelBatch.render(instances, environment);
		modelBatch.end();

		debugDrawer.begin(viewport.getCamera());
		collisionWorld.debugDrawWorld();
		debugDrawer.end();

	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
		camera.update(true);
		camController.screenCenterX = width / 2;
		camController.screenCenterY = height / 2;

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub
	}

}
