package incanshift;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

public class MainScreen extends AbstractScreen implements Screen {

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

	private ShapeRenderer shapeRenderer;

	Matrix4 uiMatrix;

	// Game objects
	Array<GameObject> instances;
	ArrayMap<String, GameObject.Constructor> gameObjectFactory;

	// Collision

	private CollisionHandler collisionHandler;

	GameObject player;
	GameObject compass;

	ModelInstance skybox;

	boolean showStartMsg = true;
	String startMsg = "Press ESC to exit";

	public MainScreen(Game game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight);

		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		assets = new AssetManager();
		assets.load("model/temple.g3db", Model.class);
		assets.load("model/ground.g3db", Model.class);
		assets.load("model/skybox.g3db", Model.class);
		assets.load("model/level.g3db", Model.class);
		assets.load("model/sphere.g3db", Model.class);

		assets.load("sound/jump.wav", Sound.class);
		assets.load("sound/bump.wav", Sound.class);
		assets.load("sound/shoot.wav", Sound.class);
		assets.load("sound/run.wav", Sound.class);
		assets.load("sound/walk.wav", Sound.class);
		assets.load("sound/wind.wav", Sound.class);

		shapeRenderer = new ShapeRenderer();

		Bullet.init();

		camera = new PerspectiveCamera(55, Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		camera.near = 1E-2f;
		camera.far = 1.5E3f;
		viewport.setCamera(camera);

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, viewport.getScreenWidth(),
				viewport.getScreenHeight());

		environment = new Environment();

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f,
				0.3f, 0.3f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f,
				-0.8f, -0.2f));

		sunTexture = new Texture(512, 512, Format.RGBA8888);
		sunPosition = new Vector3(0, 1000, -1000);
		sunRadius = 500000f;

		loadShaders();

		modelBatch = new ModelBatch();

		ModelBuilder modelBuilder = new ModelBuilder();
		Model arrow = modelBuilder.createArrow(Vector3.Zero, Vector3.Y.cpy()
				.scl(1), null, Usage.Position | Usage.Normal);

		modelBuilder.begin();

		Mesh xArrow = arrow.meshes.first().copy(false);
		xArrow.transform(new Matrix4().rotate(Vector3.X, 90));
		modelBuilder.part("part1", xArrow, GL20.GL_TRIANGLES, new Material(
				ColorAttribute.createDiffuse(Color.RED)));

		modelBuilder.node();
		Mesh yArrow = arrow.meshes.first().copy(false);
		modelBuilder.part("part2", yArrow, GL20.GL_TRIANGLES, new Material(
				ColorAttribute.createDiffuse(Color.GREEN)));

		modelBuilder.node();
		Mesh zArrow = arrow.meshes.first().copy(false);
		zArrow.transform(new Matrix4().rotate(Vector3.Z, 90));
		modelBuilder.part("part3", zArrow, GL20.GL_TRIANGLES, new Material(
				ColorAttribute.createDiffuse(Color.BLUE)));

		arrow.dispose();
		Model modelCompass = modelBuilder.end();

		assets.finishLoading();

		Model modelTemple = assets.get("model/temple.g3db", Model.class);
		Model modelGround = assets.get("model/ground.g3db", Model.class);
		Model modelLevel = assets.get("model/level.g3db", Model.class);
		Model modelSphere = assets.get("model/sphere.g3db", Model.class);

		gameObjectFactory = new ArrayMap<String, GameObject.Constructor>();
		gameObjectFactory.put("temple", new GameObject.Constructor(modelTemple,
				Bullet.obtainStaticNodeShape(modelTemple.nodes)));
		gameObjectFactory.put("ground", new GameObject.Constructor(modelGround,
				Bullet.obtainStaticNodeShape(modelGround.nodes)));
		gameObjectFactory.put("level", new GameObject.Constructor(modelLevel,
				Bullet.obtainStaticNodeShape(modelLevel.nodes)));
		gameObjectFactory.put("sphere", new GameObject.Constructor(modelSphere,
				Bullet.obtainStaticNodeShape(modelSphere.nodes)));
		gameObjectFactory
				.put("compass",
						new GameObject.Constructor(modelCompass, Bullet
								.obtainStaticNodeShape(modelCompass.nodes)));
		gameObjectFactory.put("player", new GameObject.Constructor(null,
				new btCapsuleShape(GameSettings.PLAYER_RADIUS,
						GameSettings.PLAYER_HEIGHT / 2)));

		instances = new Array<GameObject>();
		instances.add(gameObjectFactory.get("ground").construct());
		// instances.add(gameObjectFactory.get("temple").construct());
		instances.add(gameObjectFactory.get("level").construct());

		compass = gameObjectFactory.get("compass").construct();

		Vector3[] spherePos = { new Vector3(10, 10, 10),
				new Vector3(10, 0, 20), new Vector3(20, 10, 10),
				new Vector3(10, 30, 20) };

		for (Vector3 pos : spherePos) {
			GameObject sphere = gameObjectFactory.get("sphere").construct();
			sphere.position(pos);
			sphere.removable = true;
			instances.add(sphere);
		}

		player = gameObjectFactory.get("player").construct();
		player.position(GameSettings.PLAYER_START_POS);

		collisionHandler = new CollisionHandler(player, instances);

		Model modelSkybox = assets.get("model/skybox.g3db", Model.class);
		skybox = new ModelInstance(modelSkybox);

		playerController = new FPSInputProcessor(viewport, player,
				collisionHandler, instances, assets);
		Gdx.input.setInputProcessor(playerController);
		playerController.centerMouseCursor();

		Gdx.input.setCursorCatched(true);
		camera.update();

	}

	@Override
	public void dispose() {
		super.dispose();

		for (GameObject obj : instances)
			obj.dispose();
		instances.clear();

		for (GameObject.Constructor ctor : gameObjectFactory.values())
			ctor.dispose();
		gameObjectFactory.clear();

		collisionHandler.dispose();
		playerController.dispose();

		modelBatch.dispose();
		assets.dispose();

		shaderSun.dispose();
		sunTexture.dispose();

	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
	}

	private void loadShaders() {
		String vert = Gdx.files.local("shader/sun.vert").readString();
		String frag = Gdx.files.local("shader/sun.frag").readString();
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

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	@Override
	public void render(float dt) {
		int vWidth = viewport.getScreenWidth();
		int vHeight = viewport.getScreenHeight();

		collisionHandler.performDiscreteCollisionDetection();
		camera.update(true);
		playerController.update(dt);

		Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		// Render the models
		modelBatch.begin(camera);
		modelBatch.render(skybox);
		modelBatch.end();

		// Draw the sun
		spriteBatch.begin();
		spriteBatch.setProjectionMatrix(uiMatrix);
		shaderSun.begin();

		Vector3 s_pos_sun = viewport.project(sunPosition.cpy());
		s_pos_sun.y = s_pos_sun.y - viewport.getScreenHeight() / 2;
		shaderSun.setUniformf("pos_sun", s_pos_sun);
		shaderSun.setUniformf("resolution", viewport.getScreenWidth(),
				viewport.getScreenHeight());

		shaderSun.end();

		float dst = camera.position.dst(sunPosition);
		spriteBatch.setShader(shaderSun);
		float sw = sunRadius / dst;
		float sh = sw;
		spriteBatch.draw(sunTexture, s_pos_sun.x - sw / 2,
				s_pos_sun.y - sh / 2, sw, sh);

		spriteBatch.setShader(null);
		spriteBatch.end();

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
			}, 5);
			spriteBatch.begin();
			font12.draw(spriteBatch, "Press ESC to pause...", 10, 15);
			spriteBatch.end();

		}

		// Draw crosshair
		shapeRenderer.setProjectionMatrix(uiMatrix);
		shapeRenderer.begin(ShapeType.Line);
		float xc = vWidth / 2;
		float yc = vHeight / 2;

		shapeRenderer.setColor(Color.GRAY);
		shapeRenderer.line(xc + 1, yc - GameSettings.CROSSHAIR, xc + 1, yc
				+ GameSettings.CROSSHAIR);
		shapeRenderer.line(xc - GameSettings.CROSSHAIR, yc - 1, xc
				+ GameSettings.CROSSHAIR, yc - 1);

		shapeRenderer.setColor(Color.WHITE);
		shapeRenderer.line(xc, yc - GameSettings.CROSSHAIR, xc, yc
				+ GameSettings.CROSSHAIR);
		shapeRenderer.line(xc - GameSettings.CROSSHAIR, yc, xc
				+ GameSettings.CROSSHAIR, yc);

		shapeRenderer.end();

	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);

		playerController.screenCenterX = width / 2;
		playerController.screenCenterY = height / 2;

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, viewport.getScreenWidth(),
				viewport.getScreenHeight());

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
