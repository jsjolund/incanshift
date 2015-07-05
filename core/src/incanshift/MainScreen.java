package incanshift;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
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
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MainScreen implements Screen {

	Viewport viewport;
	private PerspectiveCamera camera;
	private FPSInputProcessor playerController;

	private ModelBatch modelBatch;
	private AssetManager assets;

	// Lights and stuff
	private Environment environment;

	// Game objects
	Array<GameObject> instances;
	ArrayMap<String, GameObject.Constructor> gameObjectFactory;

	// Collision

	private CollisionHandler collisionHandler;

	GameObject player;

	ModelInstance skybox;

	SpriteBatch spriteBatch;
	BitmapFont font12;
	boolean showStartMsg = true;
	String startMsg = "Press ESC to exit";

	/**
	 * A game object consisting of a model, collision body and some additional
	 * data
	 */
	static class GameObject extends ModelInstance implements Disposable {

		public final btCollisionObject body;

		public boolean onGround = false;
		public Vector3 velocity = new Vector3();

		public GameObject(Model model, btCollisionShape shape) {
			super(model);
			body = new btCollisionObject();
			body.setCollisionShape(shape);
		}

		public void trn(Vector3 translation) {
			transform.trn(translation);
			body.setWorldTransform(transform);
		}

		public void position(Vector3 position) {
			transform.setTranslation(position);
			body.setWorldTransform(transform);
		}

		@Override
		public void dispose() {
			body.dispose();
		}

		/**
		 * Constructor class for game objects
		 */
		static class Constructor implements Disposable {

			public final Model model;
			public final btCollisionShape shape;

			public Constructor(Model model, btCollisionShape shape) {
				this.shape = shape;
				if (model == null) {
					model = new ModelBuilder().createXYZCoordinates(1, new Material(), Usage.Position | Usage.Normal);
				}
				this.model = model;
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

	private void createGameObjects() {
		assets.finishLoading();

		Model modelTemple = assets.get("./temple.g3db", Model.class);
		Model modelGround = assets.get("./ground.g3db", Model.class);

		gameObjectFactory = new ArrayMap<String, MainScreen.GameObject.Constructor>();
		gameObjectFactory.put("temple", new GameObject.Constructor(modelTemple, Bullet.obtainStaticNodeShape(modelTemple.nodes)));
		gameObjectFactory.put("ground", new GameObject.Constructor(modelGround, Bullet.obtainStaticNodeShape(modelGround.nodes)));
		gameObjectFactory.put("player", new GameObject.Constructor(null, new btCapsuleShape(GameSettings.PLAYER_RADIUS, GameSettings.PLAYER_HEIGHT / 2)));

		instances = new Array<GameObject>();
		instances.add(gameObjectFactory.get("ground").construct());
		instances.add(gameObjectFactory.get("temple").construct());

		player = gameObjectFactory.get("player").construct();
		player.position(GameSettings.PLAYER_START_POS);

		collisionHandler = new CollisionHandler(player, instances);

		Model modelSkybox = assets.get("./skybox.g3db", Model.class);
		skybox = new ModelInstance(modelSkybox);
	}

	public MainScreen(Game game) {

		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		assets = new AssetManager();
		assets.load("./temple.g3db", Model.class);
		assets.load("./ground.g3db", Model.class);
		assets.load("./skybox.g3db", Model.class);

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 14;
		font12 = generator.generateFont(parameter);
		generator.dispose();

		Bullet.init();

		environment = new Environment();

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		modelBatch = new ModelBatch();
		spriteBatch = new SpriteBatch();

		camera = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.near = 1E-2f;
		camera.far = 1.5E3f;

		viewport = new FitViewport(1280, 720, camera);
		viewport.apply();

		createGameObjects();

		playerController = new FPSInputProcessor(viewport, player, collisionHandler);
		Gdx.input.setInputProcessor(playerController);
		playerController.centerMouseCursor();


		Gdx.input.setCursorCatched(true);
		camera.update();
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
		playerController.dispose();

		modelBatch.dispose();
		spriteBatch.dispose();
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

	@Override
	public void render(float dt) {

		collisionHandler.performDiscreteCollisionDetection();
		playerController.update(dt);

		Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
		int x = (Gdx.graphics.getWidth() - viewport.getScreenWidth()) / 2;
		int y = (Gdx.graphics.getHeight() - viewport.getScreenHeight()) / 2;
		Gdx.gl.glViewport(x, y, viewport.getScreenWidth(), viewport.getScreenHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(camera);
		modelBatch.render(skybox);
		modelBatch.render(instances, environment);
		modelBatch.end();

		collisionHandler.debugDrawWorld(camera);

		// Show a message which disappears after 5 sec
		if (showStartMsg) {
			Timer.schedule(new Task() {
				@Override
				public void run() {
					showStartMsg = false;
				}
			}, 5);
			spriteBatch.begin();
			font12.draw(spriteBatch, "Press ESC to pause...", 10, 15);
			spriteBatch.end();

		}

	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
		camera.update(true);
		playerController.screenCenterX = width / 2;
		playerController.screenCenterY = height / 2;
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
