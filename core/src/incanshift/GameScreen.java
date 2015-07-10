package incanshift;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
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

public class GameScreen extends AbstractScreen implements Screen {

	private Player playerController;

	private ModelBatch modelBatch;
	private AssetManager assets;

	// Lights and stuff
	private Environment environment;
	private ShaderProgram shaderSun;
	private Texture sunTexture;
	private Vector3 sunPosition;
	private float sunRadius;

	private ShapeRenderer shapeRenderer;

	private Matrix4 uiMatrix;
	private Vector3 tmp = new Vector3();

	// Game objects
	private Array<GameObject> instances;
	private ArrayMap<String, GameObject.Constructor> gameObjectFactory;

	// Collision
	private CollisionHandler collisionHandler;

	private GameObject playerObject;
	private GameObject gunInstance;
	private ModelInstance skybox;
	private Music music;

	private String msg = new String();

	public GameScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight);

		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		assets = new AssetManager();
		assets.load("model/temple.g3db", Model.class);
		assets.load("model/ground.g3db", Model.class);
		assets.load("model/skybox.g3db", Model.class);
		assets.load("model/level.g3db", Model.class);
		assets.load("model/gun.g3db", Model.class);
		assets.load("model/sphere.g3db", Model.class);

		assets.load("sound/jump.wav", Sound.class);
		assets.load("sound/bump.wav", Sound.class);
		assets.load("sound/shoot.wav", Sound.class);
		assets.load("sound/run.wav", Sound.class);
		assets.load("sound/walk.wav", Sound.class);
		assets.load("sound/climb.wav", Sound.class);
		assets.load("sound/music_game.ogg", Music.class);

		shapeRenderer = new ShapeRenderer();

		Bullet.init();

		environment = new Environment();

		sunTexture = new Texture(512, 512, Format.RGBA8888);
		sunPosition = new Vector3(500, 1200, 700);
		sunRadius = 500000f;

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f,
				0.3f, 0.3f, 1f));
		environment.add(new DirectionalLight().set(Color.WHITE,
				sunPosition.scl(-1)));

		loadShaders();

		modelBatch = new ModelBatch();

		ModelBuilder modelBuilder = new ModelBuilder();

		Model modelPlayer = modelBuilder.createCapsule(
				GameSettings.PLAYER_RADIUS, GameSettings.PLAYER_HEIGHT - 2
						* GameSettings.PLAYER_RADIUS, 4, GL20.GL_TRIANGLES,
				new Material(), Usage.Position | Usage.Normal);

		assets.finishLoading();

		Model modelTemple = assets.get("model/temple.g3db", Model.class);
		Model modelGround = assets.get("model/ground.g3db", Model.class);
		Model modelLevel = assets.get("model/level.g3db", Model.class);
		Model modelSphere = assets.get("model/sphere.g3db", Model.class);
		Model modelGun = assets.get("model/gun.g3db", Model.class);

		gameObjectFactory = new ArrayMap<String, GameObject.Constructor>();
		gameObjectFactory.put("temple", new GameObject.Constructor(modelTemple,
				Bullet.obtainStaticNodeShape(modelTemple.nodes)));
		gameObjectFactory.put("ground", new GameObject.Constructor(modelGround,
				Bullet.obtainStaticNodeShape(modelGround.nodes)));
		gameObjectFactory.put("level", new GameObject.Constructor(modelLevel,
				Bullet.obtainStaticNodeShape(modelLevel.nodes)));
		gameObjectFactory.put("sphere", new GameObject.Constructor(modelSphere,
				Bullet.obtainStaticNodeShape(modelSphere.nodes)));
		gameObjectFactory.put("gun", new GameObject.Constructor(modelGun,
				new btCapsuleShape(0, 0)));
		gameObjectFactory.put("player", new GameObject.Constructor(modelPlayer,
				new btCapsuleShape(GameSettings.PLAYER_RADIUS,
						GameSettings.PLAYER_HEIGHT - 2
								* GameSettings.PLAYER_RADIUS)));

		instances = new Array<GameObject>();
		instances.add(gameObjectFactory.get("ground").construct());
		// instances.add(gameObjectFactory.get("temple").construct());
		instances.add(gameObjectFactory.get("level").construct());

		// Blender sphere coordinates
		Vector3[] spherePos = { new Vector3(-2, 5, 7), new Vector3(-4, 1, 0),
				new Vector3(2, 1, 0), new Vector3(7, -3, 7),
				new Vector3(-2, -8, 7), new Vector3(0, -8, 7), };

		for (Vector3 pos : spherePos) {
			pos.set(pos.x, pos.z, -pos.y);
			GameObject sphere = gameObjectFactory.get("sphere").construct();
			sphere.position(pos);
			sphere.removable = true;
			instances.add(sphere);
		}

		gunInstance = gameObjectFactory.get("gun").construct();
		// instances.add(gunInstance);

		playerObject = gameObjectFactory.get("player").construct();
		playerObject.position(GameSettings.PLAYER_START_POS);
		playerObject.visible = false;

		collisionHandler = new CollisionHandler(playerObject, instances);

		Model modelSkybox = assets.get("model/skybox.g3db", Model.class);
		skybox = new ModelInstance(modelSkybox);

		PlayerSound sound = new PlayerSound(assets);

		playerController = new Player(game, playerObject, screenCenter,
				viewport, collisionHandler, instances, sound);

		Model arrow = modelBuilder.createArrow(Vector3.Zero, Vector3.Y.cpy()
				.scl(4), null, Usage.Position | Usage.Normal);

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

		modelBuilder = new ModelBuilder();
		gameObjectFactory
				.put("compass",
						new GameObject.Constructor(modelCompass, Bullet
								.obtainStaticNodeShape(modelCompass.nodes)));

		GameObject compass = gameObjectFactory.get("compass").construct();
		compass.position(10, 0.5f, 10);
		instances.add(compass);
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

	Vector3 lastCameraDirection = new Vector3();

	@Override
	public void hide() {
		lastCameraDirection.set(camera.direction);
		Gdx.input.setCursorCatched(false);
		music.stop();
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
	public void render(float delta) {
		super.render(delta);

		playerController.update(delta);
		playerObject.transform.getTranslation(tmp);
		msg = String.format("x=%.2f, y=%.2f, z=%.2f", tmp.x, -tmp.z, tmp.y
				- GameSettings.PLAYER_HEIGHT / 2);

		// camera.update();

		gunInstance.transform.setToTranslation(camera.position);
		// gunInstance.transform.rotate(camera.direction,
		// gunInstance.transform.);

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

		spriteBatch.setShader(null);
		spriteBatch.setProjectionMatrix(uiMatrix);

		spriteBatch.begin();
		fontTiny.draw(spriteBatch, msg, 10, 15);
		spriteBatch.end();

		// Draw crosshair
		shapeRenderer.setProjectionMatrix(uiMatrix);
		shapeRenderer.begin(ShapeType.Line);
		int vWidth = viewport.getScreenWidth();
		int vHeight = viewport.getScreenHeight();
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

	public Vector3 screenCenter = new Vector3();

	@Override
	public void resize(int width, int height) {

		super.resize(width, height);

		float vw = viewport.getScreenWidth();
		float vh = viewport.getScreenHeight();

		screenCenter.set(width / 2, height / 2, 1);

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, vw, vh);

		camera = new PerspectiveCamera(GameSettings.CAMERA_FOV,
				viewport.getScreenWidth(), viewport.getScreenHeight());
		camera.near = 1E-2f;
		camera.far = 1.5E3f;
		camera.lookAt(lastCameraDirection);

		camera.update(true);

		viewport.setCamera(camera);

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(playerController.controller);
		Gdx.input.setCursorCatched(true);

		// Play some music
		music = assets.get("sound/music_game.ogg", Music.class);
		music.play();
		music.setVolume(0.3f);
		music.setLooping(true);

	}

}
