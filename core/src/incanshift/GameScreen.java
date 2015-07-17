package incanshift;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

public class GameScreen extends AbstractScreen {

	final static String tag = "GameScreen";

	private GameWorld world;
	private ModelBatch modelBatch;

	// Lights and stuff
	private Environment environment;
	private ShapeRenderer shapeRenderer;

	// Crosshair coordinates
	Vector2 chHoriz1 = new Vector2();
	Vector2 chHoriz2 = new Vector2();
	Vector2 chVert1 = new Vector2();
	Vector2 chVert2 = new Vector2();

	Billboard sun;
	Array<Billboard> billboards = new Array<Billboard>();
	Ray bbTestRay = new Ray();
	Vector3 bbDirection = new Vector3();

	Color fogColor;
	float fogDistance;

	private Vector3 lastCameraDirection = new Vector3();

	public GameScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight);

		world = new GameWorld(game, viewport, screenCenter);

		// Various environment graphics stuff
		shapeRenderer = new ShapeRenderer();
		environment = new Environment();
		modelBatch = new ModelBatch();

		// Sun billboard
		Vector3 sunPosition = new Vector3(500, 800, 700);
		sun = new Billboard(sunPosition, 500f, 500f, "shader/common.vert",
				"shader/sun.frag");
		// Marker billboard
		billboards.add(new Billboard(new Vector3(-20, 2, 10), 1, 1,
				"shader/common.vert", "shader/test.frag"));
		billboards.add(new Billboard(new Vector3(5, 1, 20), 1, 1,
				"shader/common.vert", "shader/test.frag"));

		setEnvironment(Color.LIGHT_GRAY, 30, sunPosition);
	}

	@Override
	public void dispose() {
		super.dispose();

		world.dispose();

		modelBatch.dispose();

		for (Billboard b : billboards) {
			b.dispose();
		}
	}

	private BitmapFontCache getPlayerPositionTextCache() {
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
		return cache;
	}

	@Override
	public void hide() {
		lastCameraDirection.set(camera.direction);
		Gdx.input.setCursorCatched(false);
		world.music.pause();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	@Override
	public void render(float delta) {
		delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());
		world.update(delta);

		// Clear the screen
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		// Draw fog background color
		shapeRenderer.begin(ShapeType.Filled);
		shapeRenderer.setColor(fogColor);
		shapeRenderer.rect(0, 0, getViewportWidth(), getViewportHeight());
		shapeRenderer.end();

		// Render the skybox
		modelBatch.begin(camera);
		modelBatch.render(world.skybox);
		modelBatch.end();

		// Draw sun billboard
		spriteBatch.begin();
		spriteBatch.setProjectionMatrix(uiMatrix);
		sun.setProjection(viewport);
		spriteBatch.setShader(sun.shader);
		spriteBatch.draw(sun.texture, sun.screenPos.x - sun.screenWidth / 2,
				sun.screenPos.y - sun.screenHeight / 2, sun.screenWidth,
				sun.screenHeight);

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

		// Draw billboards
		spriteBatch.begin();
		spriteBatch.setProjectionMatrix(uiMatrix);
		for (Billboard b : billboards) {

			if (!camera.frustum.sphereInFrustum(b.worldPos, b.maxWorldRadius())) {
				continue;
			}

			bbDirection.set(b.worldPos).sub(world.player.position).nor();
			bbTestRay.set(world.player.position, bbDirection);

			short mask = (short) (CollisionHandler.GROUND_FLAG | CollisionHandler.OBJECT_FLAG);
			float dst = world.player.position.dst(b.worldPos);

			if (world.rayTest(bbTestRay, mask, dst) != null) {
				continue;
			}

			b.setProjection(viewport);
			spriteBatch.setShader(b.shader);
			spriteBatch.draw(b.texture, b.screenPos.x - b.screenWidth / 2,
					b.screenPos.y - b.screenHeight / 2, b.screenWidth,
					b.screenHeight);
		}
		spriteBatch.setShader(null);
		spriteBatch.end();

		// Draw collision debug wireframe
		// collisionHandler.debugDrawWorld(camera);

		// Draw crosshair
		shapeRenderer.setProjectionMatrix(uiMatrix);
		shapeRenderer.begin(ShapeType.Line);
		shapeRenderer.setColor(Color.WHITE);
		shapeRenderer.line(chHoriz1, chHoriz2);
		shapeRenderer.line(chVert1, chVert2);
		shapeRenderer.end();

		// Draw player coordinates
		spriteBatch.setShader(null);
		spriteBatch.setProjectionMatrix(uiMatrix);
		spriteBatch.begin();
		getPlayerPositionTextCache().draw(spriteBatch);
		spriteBatch.end();

	}

	@Override
	public void resize(int width, int height) {

		super.resize(width, height);

		camera = new PerspectiveCamera(GameSettings.CAMERA_FOV,
				getViewportWidth(), getViewportHeight());
		camera.near = 1E-2f;
		camera.far = 30f;
		camera.far = 50f;
		camera.lookAt(lastCameraDirection);

		camera.update(true);

		viewport.setCamera(camera);

		updateCrosshair();
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
	}

	void setEnvironment(Color fogColor, float fogDistance, Vector3 sunPosition) {
		this.fogColor = fogColor;
		environment.clear();
		environment.add(new DirectionalLight().set(Color.WHITE,
				sunPosition.scl(-1)));
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f,
				0.4f, 0.4f, 1.f));
		environment.set(new ColorAttribute(ColorAttribute.Fog, fogColor.r,
				fogColor.g, fogColor.b, fogColor.a));
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(world.player.controller);
		Gdx.input.setCursorCatched(true);

		world.music(true);

	}

	private void updateCrosshair() {
		float xc = getViewportWidth() / 2;
		float yc = getViewportHeight() / 2;
		chHoriz1.set(xc, yc - GameSettings.CROSSHAIR);
		chHoriz2.set(xc, yc + GameSettings.CROSSHAIR);
		chVert1.set(xc - GameSettings.CROSSHAIR, yc);
		chVert2.set(xc + GameSettings.CROSSHAIR, yc);
	}

}
