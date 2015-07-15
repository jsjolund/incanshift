package incanshift;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;

public class GameScreen extends AbstractScreen implements Screen {

	final static String tag = "GameScreen";

	private GameWorld world;

	private ModelBatch modelBatch;

	// private Music music;

	// Lights and stuff
	private Environment environment;
	private ShaderProgram shaderSun;
	private Texture sunTexture;
	private Vector3 sunPosition;
	private Vector3 sunPositionProj;
	private float sunRadius;
	private ShapeRenderer shapeRenderer;

	// Game objects
	// private Array<GameObject> instances;
	// private ArrayMap<String, GameObject.Constructor> gameObjectFactory;

	private Vector3 lastCameraDirection = new Vector3();

	public GameScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight);

		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		world = new GameWorld(game, viewport, screenCenter);
		// Blender sphere coordinates
		Vector3[] spherePos = { new Vector3(-2, 5, 7), new Vector3(-4, 1, 0),
				new Vector3(2, 1, 0), new Vector3(7, -3, 7),
				new Vector3(-2, -8, 7), new Vector3(0, -8, 7), };
		for (Vector3 pos : spherePos) {
			pos.set(pos.x, pos.z, -pos.y);
			world.spawnEnemyMask(pos);
		}

		world.spawnCrate(new Vector3(15, 10, 15));
		world.spawnCrate(new Vector3(-15, 20, 15));
		world.spawnCrate(new Vector3(5, 2, 5));

		// Various environment graphics stuff
		shapeRenderer = new ShapeRenderer();
		environment = new Environment();
		sunTexture = new Texture(512, 512, Format.RGBA8888);
		sunPosition = new Vector3(500, 1200, 700);
		sunPositionProj = sunPosition.cpy();
		sunRadius = 500000f;
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f,
				0.3f, 0.3f, 1f));
		environment.add(new DirectionalLight().set(Color.WHITE,
				sunPosition.scl(-1)));
		loadShaders();

		// Create game instances
		modelBatch = new ModelBatch();

		Gdx.app.debug(tag, "Loaded player");
	}

	@Override
	public void dispose() {
		super.dispose();

		world.dispose();

		modelBatch.dispose();

		shaderSun.dispose();
		sunTexture.dispose();

	}

	@Override
	public void hide() {
		lastCameraDirection.set(camera.direction);
		Gdx.input.setCursorCatched(false);
		world.music(false);
	}

	private void loadShaders() {
		String vert = Gdx.files.local("shader/sun.vert").readString();
		String frag = Gdx.files.local("shader/sun.frag").readString();
		shaderSun = new ShaderProgram(vert, frag);
		ShaderProgram.pedantic = false;
		if (!shaderSun.isCompiled()) {
			Gdx.app.debug("Shader:", shaderSun.getLog());
			Gdx.app.exit();
		}
		if (shaderSun.getLog().length() != 0) {
			Gdx.app.debug("Shader:", shaderSun.getLog());
		}
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	@Override
	public void render(float delta) {
		delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());
		super.render(delta);

		world.update(delta);

		// Render the skybox
		modelBatch.begin(camera);
		modelBatch.render(world.skybox);
		modelBatch.end();

		// Draw the sun
		spriteBatch.begin();
		spriteBatch.setProjectionMatrix(uiMatrix);
		shaderSun.begin();
		Vector3 s_pos_sun = viewport.project(sunPositionProj.set(sunPosition));
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

		// Render the game level models and player gun
		modelBatch.begin(camera);
		for (GameObject obj : world.instances) {
			if (obj.visible) {
				modelBatch.render(obj, environment);
			}
		}
		modelBatch.end();

		// Draw collision debug wireframe
		// collisionHandler.debugDrawWorld(camera);

		// Draw crosshair
		shapeRenderer.setProjectionMatrix(uiMatrix);
		shapeRenderer.begin(ShapeType.Line);
		float xc = viewport.getScreenWidth() / 2;
		float yc = viewport.getScreenHeight() / 2;
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

		// Draw player coordinates
		BitmapFontCache cache = monoTiny.getCache();
		cache.clear();
		cache.setColor(Color.BLACK);
		float textX = 10;
		float textY = 20;
		monoTiny.getData().markupEnabled = true;
		cache.addText(String.format(
				"Blender: [RED]x=% .2f[]  [GREEN]y=% .2f[]  [BLUE]z=% .2f[]",
				world.player.position.x, -world.player.position.z,
				world.player.position.y - GameSettings.PLAYER_HEIGHT / 2),
				textX, textY * 2);
		cache.addText(
				String.format(
						"Game:    [RED]x=% .2f[]  [GREEN]y=% .2f[]  [BLUE]z=% .2f[]  [WHITE]v=% .2f",
						world.player.position.x, world.player.position.y
								- GameSettings.PLAYER_HEIGHT / 2,
						world.player.position.z, world.player.object.body
								.getLinearVelocity().len()), textX, (textY));
		monoTiny.getData().markupEnabled = false;
		spriteBatch.setShader(null);
		spriteBatch.setProjectionMatrix(uiMatrix);
		spriteBatch.begin();
		cache.draw(spriteBatch);
		spriteBatch.end();
	}

	@Override
	public void resize(int width, int height) {

		super.resize(width, height);

		camera = new PerspectiveCamera(GameSettings.CAMERA_FOV,
				viewport.getScreenWidth(), viewport.getScreenHeight());
		camera.near = 1E-2f;
		camera.far = 1.5E3f;
		// camera.far = 10f;
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
		Gdx.input.setInputProcessor(world.player.controller);
		Gdx.input.setCursorCatched(true);

		world.music(true);

	}

}
