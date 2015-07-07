package incanshift;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
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
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
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
	private ShaderProgram shaderSun;
	private Texture sunTexture;
	private Vector3 sunPosition;
	private float sunRadius;
	private Vector3 sunDimensions;

	private ShapeRenderer shapeRenderer;

	Matrix4 uiMatrix;

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
		public boolean removable = false;
		public boolean visible = true;
		public Vector3 velocity = new Vector3();

		public GameObject(Model model, btCollisionShape shape) {
			super(model);
			body = new btCollisionObject();
			body.setCollisionShape(shape);
		}

		public void trn(Vector3 translation) {
			transform.trn(translation);
			body.setWorldTransform(transform);
			calculateTransforms();
		}

		public void position(Vector3 position) {
			transform.setTranslation(position);
			body.setWorldTransform(transform);
			calculateTransforms();
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

	private void loadShaders() {
		String vert = Gdx.files.local("shaders/sun.vert").readString();
		String frag = Gdx.files.local("shaders/sun.frag").readString();
		shaderSun = new ShaderProgram(vert, frag);
		ShaderProgram.pedantic = false;
		if (!shaderSun.isCompiled()) {
			Gdx.app.debug("Shader", shaderSun.getLog());
			Gdx.app.exit();
		}
		if (shaderSun.getLog().length() != 0) {
			Gdx.app.debug("Shader", shaderSun.getLog());
		}
	}

	private void createGameObjects() {
		assets.finishLoading();

		Model modelTemple = assets.get("./temple.g3db", Model.class);
		Model modelGround = assets.get("./ground.g3db", Model.class);
		Model modelLevel = assets.get("./level.g3db", Model.class);
		Model modelSphere = assets.get("./sphere.g3db", Model.class);

		gameObjectFactory = new ArrayMap<String, MainScreen.GameObject.Constructor>();
		gameObjectFactory.put("temple", new GameObject.Constructor(modelTemple, Bullet.obtainStaticNodeShape(modelTemple.nodes)));
		gameObjectFactory.put("ground", new GameObject.Constructor(modelGround, Bullet.obtainStaticNodeShape(modelGround.nodes)));
		gameObjectFactory.put("level", new GameObject.Constructor(modelLevel, Bullet.obtainStaticNodeShape(modelLevel.nodes)));
		gameObjectFactory.put("sphere", new GameObject.Constructor(modelSphere, Bullet.obtainStaticNodeShape(modelSphere.nodes)));
		gameObjectFactory.put("player", new GameObject.Constructor(null, new btCapsuleShape(GameSettings.PLAYER_RADIUS, GameSettings.PLAYER_HEIGHT / 2)));

		instances = new Array<GameObject>();
		instances.add(gameObjectFactory.get("ground").construct());
		// instances.add(gameObjectFactory.get("temple").construct());
		instances.add(gameObjectFactory.get("level").construct());

		Vector3[] spherePos = { new Vector3(10, 10, 10), new Vector3(10, 0, 20), new Vector3(20, 10, 10), new Vector3(10, 30, 20) };

		for (Vector3 pos : spherePos) {
			GameObject sphere = gameObjectFactory.get("sphere").construct();
			sphere.position(pos);
			sphere.removable = true;
			instances.add(sphere);
		}

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
		assets.load("./level.g3db", Model.class);
		assets.load("./sphere.g3db", Model.class);

		shapeRenderer = new ShapeRenderer();

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 14;
		font12 = generator.generateFont(parameter);
		generator.dispose();

		Bullet.init();

		camera = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.near = 1E-2f;
		camera.far = 1.5E3f;

		viewport = new FitViewport(1280, 720, camera);
		viewport.apply();

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());

		environment = new Environment();

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		// sunTexture = new Texture(viewport.getScreenWidth(),
		// viewport.getScreenHeight(), Format.RGBA8888);
		sunTexture = new Texture(10, 10, Format.RGBA8888);
		sunPosition = new Vector3(0, 0, 0);
		sunRadius = 10f;
		sunDimensions = new Vector3(sunRadius, sunRadius, sunRadius);

		spriteBatch = new SpriteBatch();
		loadShaders();

		modelBatch = new ModelBatch();
		spriteBatch = new SpriteBatch();

		createGameObjects();

		playerController = new FPSInputProcessor(viewport, player, collisionHandler, instances);
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

		shaderSun.dispose();
		sunTexture.dispose();
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
		camera.update(true);
		playerController.update(dt);

		Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
		int x = (Gdx.graphics.getWidth() - viewport.getScreenWidth()) / 2;
		int y = (Gdx.graphics.getHeight() - viewport.getScreenHeight()) / 2;
		Gdx.gl.glViewport(x, y, viewport.getScreenWidth(), viewport.getScreenHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(camera);
		modelBatch.render(skybox);
		modelBatch.end();

		modelBatch.begin(camera);
		for (GameObject obj : instances) {
			if (obj.visible) {
				modelBatch.render(obj, environment);
			}
		}
		modelBatch.end();

		collisionHandler.debugDrawWorld(camera);

		// Show a message which disappears after 5 sec
		if (showStartMsg) {

			spriteBatch.setShader(null);
			spriteBatch.setProjectionMatrix(uiMatrix);

			Timer.schedule(new Task() {
				@Override
				public void run() {
					showStartMsg = false;
				}
			}, 500);
			spriteBatch.begin();
			font12.draw(spriteBatch, "Press ESC to pause...", 10, 15);
			spriteBatch.end();

		}

//		shapeRenderer.setProjectionMatrix(uiMatrix);
//
//		shapeRenderer.begin(ShapeType.Line);
//
//		shapeRenderer.rect(0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());
//
//		float xc = viewport.getScreenWidth() / 2;
//		float yc = viewport.getScreenHeight() / 2;
//		float x1 = xc;
//		float y1 = yc - 10;
//		float x2 = xc;
//		float y2 = yc + 10;
//		shapeRenderer.line(x1, y1, x2, y2);
//		x1 = xc - 10;
//		y1 = yc;
//		x2 = xc + 10;
//		y2 = yc;
//		shapeRenderer.line(x1, y1, x2, y2);
//		shapeRenderer.end();

		// if (camera.frustum.boundsInFrustum(sunPosition, sunDimensions)) {
		// spriteBatch.setProjectionMatrix(viewport.getCamera().combined.cpy().setToLookAt(new
		// Vector3(10, 10, 10), Vector3.Y));

		// spriteBatch.begin();
		// viewport.getCamera().update();
		// spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
		// spriteBatch.getProjectionMatrix().trn(0, 0, 0);
		// spriteBatch.getProjectionMatrix().rotate(Vector3.Y,
		// camera.position.cpy().crs(Vector3.X).nor());
		//
		//
		//
		// // modelInstance1.direction(from matrix) =
		// (modelInstance2.position).sub(modelInstance1.position).nor();
		// // modelInstance1.setRotation(modelInstance1.direction, Vector3.Y);
		//
		// shaderSun.begin();
		//
		// Vector3 s_pos_sun = viewport.project(sunPosition.cpy());
		// // sunTexture.setP
		// // s_pos_sun.y = viewport.getScreenHeight() - s_pos_sun.y;
		// // s_pos_sun.x = viewport.getScreenWidth() - s_pos_sun.x;
		// shaderSun.setUniformf("pos_sun", s_pos_sun);
		// // Vector3 s_pos_sun_ort_bound =
		// // viewport.project(sunPosition.cpy().sub(sunDimensions));
		// // s_pos_sun_ort_bound.y = viewport.getScreenHeight() -
		// // s_pos_sun_ort_bound.y;
		// // s_pos_sun_ort_bound.x = viewport.getScreenWidth() -
		// // s_pos_sun_ort_bound.x;
		//
		// // System.out.println();
		// // System.out.println(s_pos_sun);
		// // // System.out.println(s_pos_sun_ort_bound);
		// //
		// // shaderSun.setUniformf("radius_sun", 10);
		// // shaderSun.setUniformf("time", Gdx.graphics.getDeltaTime());
		// shaderSun.setUniformf("resolution", viewport.getScreenWidth(),
		// viewport.getScreenHeight());
		//
		// // shaderSun.end();
		//
		// spriteBatch.setShader(shaderSun);
		// spriteBatch.draw(sunTexture, 0,0);
		// spriteBatch.setShader(null);
		// spriteBatch.end();
		// }

		// spriteBatch.begin();
		//
		// spriteBatch.setShader(shaderSun);
		// // draw some sprites... they will all be affected by our shaders
		// spriteBatch.draw(sunTexture, 0, 0, 32, 32);
		//
		// // end our batch
		// spriteBatch.end();
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
		camera.update(true);
		playerController.screenCenterX = width / 2;
		playerController.screenCenterY = height / 2;
		// sunTexture.dispose();
		// sunTexture = new Texture(width, height, Format.RGBA8888);
		uiMatrix = camera.combined.cpy();

//		System.out.println();
//		System.out.println(width);
//		System.out.println(height);
//		System.out.println(viewport.getScreenWidth());
//		System.out.println(viewport.getScreenHeight());
//		System.out.println(viewport.getWorldWidth());
//		System.out.println(viewport.getWorldHeight());
//		System.out.println(Gdx.graphics.getWidth());
//		System.out.println(Gdx.graphics.getHeight());
		float x = 0;
		float y = -(Gdx.graphics.getHeight()*2 - viewport.getScreenHeight());
		uiMatrix.setToOrtho2D(x, y, viewport.getScreenWidth(), viewport.getScreenHeight());
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
