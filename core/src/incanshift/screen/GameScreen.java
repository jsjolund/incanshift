package incanshift.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import incanshift.IncanShift;
import incanshift.gameobjects.Billboard;
import incanshift.gameobjects.BillboardOverlay;
import incanshift.gameobjects.GameObject;
import incanshift.world.GameSettings;
import incanshift.world.GameWorld;
import incanshift.world.WorldEnvironment;

public class GameScreen extends AbstractScreen {

	final static String tag = "GameScreen";
	ModelBatch shadowBatch;
	private GameWorld world;
	private WorldEnvironment env;

	// Lights and stuff
	private ModelBatch modelBatch;
	private ShapeRenderer shapeRenderer;
	// Crosshair coordinates
	private Vector2 chHoriz1 = new Vector2();
	private Vector2 chHoriz2 = new Vector2();
	private Vector2 chVert1 = new Vector2();
	private Vector2 chVert2 = new Vector2();
	private boolean overlayIsOn = false;
	private Texture overlay;
	private ShaderProgram overlayShader;
	private Color overlayColor = Color.BLACK;
	private float overlayRadius = 0.9f;
	private float overlaySoftness = 0.5f;
	private Vector3 lastCameraDirection = new Vector3();
	private Vector3 objPos = new Vector3();

	public GameScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight);

		world = new GameWorld(game, viewport, screenCenter, sansHuge);

		shapeRenderer = new ShapeRenderer();
		env = new WorldEnvironment();
		modelBatch = new ModelBatch();

		overlayShader = loadShader("shader/common.vert",
				"shader/vignette.frag");

		shadowBatch = new ModelBatch(new DepthShaderProvider());
	}

	@Override
	public void dispose() {
		super.dispose();
		world.dispose();
		modelBatch.dispose();
		env.dispose();
		overlay.dispose();
		overlayShader.dispose();
	}

	private BitmapFontCache getPlayerPositionTextCache() {
		BitmapFontCache cache = monoTiny.getCache();
		cache.clear();
		cache.setColor(Color.BLACK);
		float textX = 10;
		float textY = 20;
		monoTiny.getData().markupEnabled = true;
		cache.addText(
				String.format(
						"[WHITE]Blender:[] [RED]x=% .2f[]  [GREEN]y=% .2f[]  [BLUE]z=% .2f[]",
						world.player.position.x, -world.player.position.z,
						world.player.position.y
								- GameSettings.PLAYER_HEIGHT / 2),
				textX, textY * 2);
		cache.addText(
				String.format(
						"[WHITE]Game:[]    [RED]x=% .2f[]  [GREEN]y=% .2f[]  [BLUE]z=% .2f[]  [WHITE]v=% .2f",
						world.player.position.x,
						world.player.position.y
								- GameSettings.PLAYER_HEIGHT / 2,
						world.player.position.z,
						world.player.body.getLinearVelocity().len()),
				textX, (textY));
		monoTiny.getData().markupEnabled = false;
		return cache;
	}

	@Override
	public void hide() {
		lastCameraDirection.set(camera.direction);
		Gdx.input.setCursorCatched(false);
		world.currentMusic.pause();
	}

	protected boolean isVisible(final Camera cam, final GameObject instance) {
		instance.transform.getTranslation(objPos);
		objPos.add(instance.center);
		return cam.frustum.sphereInFrustum(objPos, instance.radius);
	}

	private ShaderProgram loadShader(String vertPath, String fragPath) {
		String vert = Gdx.files.local(vertPath).readString();
		String frag = Gdx.files.local(fragPath).readString();
		ShaderProgram shader = new ShaderProgram(vert, frag);
		ShaderProgram.pedantic = false;
		if (!shader.isCompiled()) {
			Gdx.app.debug("Shader", shader.getLog());
			Gdx.app.exit();
		}
		if (shader.getLog().length() != 0) {
			Gdx.app.debug("Shader", shader.getLog());
		}
		return shader;
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	// @SuppressWarnings("deprecation")
	@Override
	public void render(float delta) {
		delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());
		world.update(delta);

		// Clear the screen
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		// env.shadowLight.begin(Vector3.Zero, camera.direction);
		//
		// env.shadowLight.update(world.player.position.cpy(), Vector3.Z);
		//
		// shadowBatch.begin(env.shadowLight.getCamera());
		// for (Entry<String, Array<GameObject>> entry : world.instances) {
		// for (GameObject obj : entry.value) {
		// shadowBatch.render(obj);
		// }
		// }
		// shadowBatch.end();
		//
		// env.shadowLight.end();

		// Fog background color
		shapeRenderer.begin(ShapeType.Filled);
		shapeRenderer.setColor(env.currentColor);
		shapeRenderer.rect(0, 0, getViewportWidth(), getViewportHeight());
		shapeRenderer.end();

		// Draw sun billboard
		spriteBatch.begin();
		spriteBatch.setProjectionMatrix(uiMatrix);
		env.sun.setProjection(viewport);
		spriteBatch.setShader(env.sun.shader);
		spriteBatch.draw(env.sun.texture,
				env.sun.screenPos.x - env.sun.screenWidth / 2,
				env.sun.screenPos.y - env.sun.screenHeight / 2,
				env.sun.screenWidth, env.sun.screenHeight);

		spriteBatch.setShader(null);
		spriteBatch.end();

		// Render the game level models
		modelBatch.begin(camera);
		for (Entry<String, Array<GameObject>> entry : world.getInstances()) {
			for (GameObject obj : entry.value) {
				if (obj.id.equals("outside_level_mountains")) {
					modelBatch.render(obj);
				} else {
					if (isVisible(viewport.getCamera(), obj)) {
						modelBatch.render(obj, env);
					}
				}
			}
		}
		modelBatch.flush();
		for (Billboard b : world.getBillboards()) {
			if (b.modelInstance != null) {
				modelBatch.render(b.modelInstance);
			}
		}
		for (Entry<String, GameObject> entry : world.player.inventory) {
			modelBatch.render(entry.value, env);
		}
		modelBatch.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);

		if (world.xRayMask) {
			// Draw black overlay
			Gdx.gl.glEnable(GL20.GL_BLEND);
			shapeRenderer.begin(ShapeType.Filled);
			Color c = Color.BLACK;
			shapeRenderer.setColor(c.r, c.g, c.b, 0.5f);
			shapeRenderer.rect(0, 0, getViewportWidth(), getViewportHeight());
			shapeRenderer.end();
			Gdx.gl.glDisable(GL20.GL_BLEND);

			// Draw markers where masks are
			spriteBatch.begin();
			spriteBatch.setProjectionMatrix(uiMatrix);
			for (Entry<GameObject, BillboardOverlay> entry : world
					.getBillboardOverlays()) {
				BillboardOverlay o = entry.value;

				if (camera.frustum.pointInFrustum(o.worldPos)) {
					o.setProjection(viewport);
					spriteBatch.setShader(o.shader);
					spriteBatch.draw(o.texture,
							o.screenPos.x - o.screenWidth / 2,
							o.screenPos.y - o.screenHeight / 2, o.screenWidth,
							o.screenHeight);
					spriteBatch.setShader(null);
				}
			}
			spriteBatch.end();

		}

		// Draw collision debug wireframe
		// world.collisionHandler.debugDrawWorld(camera);

		// Overlay shading
		if (overlayIsOn) {
			spriteBatch.begin();
			overlayShader.begin();
			overlayShader.setUniformf("resolution", getViewportWidth(),
					getViewportHeight());
			overlayShader.setUniformf("radius", overlayRadius);
			overlayShader.setUniformf("softness", overlaySoftness);
			overlayShader.setUniformf("color", overlayColor);
			overlayShader.end();
			spriteBatch.setShader(overlayShader);
			spriteBatch.draw(overlay, 0, 0);
			spriteBatch.end();

		}

//		// Draw player coordinates
//		spriteBatch.setShader(null);
//		spriteBatch.setProjectionMatrix(uiMatrix);
//		spriteBatch.begin();
//		getPlayerPositionTextCache().draw(spriteBatch);
//		spriteBatch.end();

		// Crosshair
		shapeRenderer.setProjectionMatrix(uiMatrix);
		shapeRenderer.begin(ShapeType.Line);
		shapeRenderer.setColor(Color.WHITE);
		shapeRenderer.line(chHoriz1, chHoriz2);
		shapeRenderer.line(chVert1, chVert2);
		shapeRenderer.end();

		// Update environment from tags
		env.update(world.getEnvTags(), world.player.position);
		camera.far = env.viewDistance;
	}

	@Override
	public void resize(int width, int height) {

		super.resize(width, height);

		camera = new PerspectiveCamera(GameSettings.CAMERA_FOV,
				getViewportWidth(), getViewportHeight());
		camera.lookAt(lastCameraDirection);
		camera.update(true);
		camera.far = env.viewDistance;
		camera.near = 1E-2f;

		viewport.setCamera(camera);

		updateCrosshair();

		overlay = new Texture(getViewportWidth(), getViewportHeight(),
				Format.RGBA8888);
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
	}

	@Override
	public void show() {
		Gdx.input.setCursorCatched(true);
		Gdx.input.setInputProcessor(world.player.controller);
		world.playMusic();
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
