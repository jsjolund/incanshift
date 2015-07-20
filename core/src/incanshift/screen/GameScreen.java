package incanshift.screen;

import incanshift.IncanShift;
import incanshift.gameobjects.Billboard;
import incanshift.gameobjects.BillboardOverlay;
import incanshift.gameobjects.EnvTag;
import incanshift.gameobjects.GameObject;
import incanshift.world.GameSettings;
import incanshift.world.GameWorld;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap.Entry;

public class GameScreen extends AbstractScreen {

	final static String tag = "GameScreen";

	private GameWorld world;
	private ModelBatch modelBatch;

	// Lights and stuff
	private Environment environment;
	private ShapeRenderer shapeRenderer;

	// Crosshair coordinates
	private Vector2 chHoriz1 = new Vector2();
	private Vector2 chHoriz2 = new Vector2();
	private Vector2 chVert1 = new Vector2();
	private Vector2 chVert2 = new Vector2();

	private float normalViewDistance = 1E3f;
	private float viewDistance;

	private BillboardOverlay sun;
	Vector3 sunPosition = new Vector3(500, 800, 700);

	private Color skyColor = new Color(0.28f, 0.56f, 0.83f, 1);

	private Color currentColor;

	private boolean overlayIsOn = false;
	private Texture overlay;
	private ShaderProgram overlayShader;
	private Color overlayColor = Color.BLACK;
	private float overlayRadius = 0.9f;
	private float overlaySoftness = 0.5f;

	private Vector3 lastCameraDirection = new Vector3();

	public GameScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight);

		world = new GameWorld(game, viewport, screenCenter, sansLarge);

		// Various environment graphics stuff
		shapeRenderer = new ShapeRenderer();
		environment = new Environment();
		modelBatch = new ModelBatch();

		// Sun billboard

		sun = new BillboardOverlay(sunPosition, 500f, 500f, 0,
				"shader/common.vert", "shader/sun.frag");

		setEnvironment(skyColor, normalViewDistance, sunPosition);

		overlayShader = loadShader("shader/common.vert", "shader/vignette.frag");
	}

	@Override
	public void dispose() {
		super.dispose();
		world.dispose();
		modelBatch.dispose();
		sun.dispose();
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
						world.player.position.y - GameSettings.PLAYER_HEIGHT
								/ 2), textX, textY * 2);
		cache.addText(
				String.format(
						"[WHITE]Game:[]    [RED]x=% .2f[]  [GREEN]y=% .2f[]  [BLUE]z=% .2f[]  [WHITE]v=% .2f",
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
	public void render(float delta) {
		delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());
		world.update(delta);

		// Clear the screen
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		// Fog background color
		shapeRenderer.begin(ShapeType.Filled);
		shapeRenderer.setColor(currentColor);
		shapeRenderer.rect(0, 0, getViewportWidth(), getViewportHeight());
		shapeRenderer.end();

		// Render the skybox
		modelBatch.begin(camera);
		// modelBatch.render(world.skybox);
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
		for (Billboard b : world.billboards) {
			if (b.modelInstance != null) {
				modelBatch.render(b.modelInstance);
			}
		}

		for (Entry<String, Array<GameObject>> entry : world.instances) {
			for (GameObject obj : entry.value) {
				modelBatch.render(obj, environment);
			}
		}

		modelBatch.end();

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

		// Draw player coordinates
		spriteBatch.setShader(null);
		spriteBatch.setProjectionMatrix(uiMatrix);
		spriteBatch.begin();
		getPlayerPositionTextCache().draw(spriteBatch);
		spriteBatch.end();

		// Crosshair
		shapeRenderer.setProjectionMatrix(uiMatrix);
		shapeRenderer.begin(ShapeType.Line);
		shapeRenderer.setColor(Color.WHITE);
		shapeRenderer.line(chHoriz1, chHoriz2);
		shapeRenderer.line(chVert1, chVert2);
		shapeRenderer.end();

		// Environment effect tags (fog, sunshine)
		viewDistance = normalViewDistance;
		Color colorDelta = new Color(skyColor);
		float dstNearest = Float.MAX_VALUE;
		EnvTag tagNearest = null;
		float fadeNearest = 0;
		for (EnvTag tag : world.envTags) {
			float fade = 0;
			float dst = tag.position.dst(world.player.position);

			if (dst > tag.fadeDistance) {
				continue;
			}
			if (dst <= tag.effectDistance) {
				fade = 1;
			} else {
				fade = 1 - (dst - tag.effectDistance)
						/ (tag.fadeDistance - tag.effectDistance);
				if (fade > 1) {
					fade = 1;
				}
			}
			if (dst < dstNearest) {
				dstNearest = dst;
				tagNearest = tag;
				fadeNearest = fade;
			}
			tag.color.a = fade;
			colorDelta = mixColors(colorDelta, tag.color);
			tag.color.a = 1;
		}
		if (tagNearest == null) {
			viewDistance = normalViewDistance;
			currentColor.set(skyColor);
			environment.set(new ColorAttribute(ColorAttribute.AmbientLight,
					0.4f, 0.4f, 0.4f, 1.f));
		} else {
			currentColor.set(colorDelta);
			viewDistance = tagNearest.minViewDistance + normalViewDistance
					* (1 - fadeNearest);
			float i = (currentColor.r + currentColor.g + currentColor.b) / 3 * 1f;
			i = (i > 1) ? 1 : i;
			System.out.println(i);
			float r = i;
			float g = i;
			float b = i;
			float a = 1;
			environment.set(new ColorAttribute(ColorAttribute.AmbientLight, r,
					g, b, a));
		}
		camera.far = viewDistance;
		environment.set(new ColorAttribute(ColorAttribute.Fog, currentColor.r,
				currentColor.g, currentColor.b, currentColor.a));

	}

	private static Color mixColors(Color bg, Color fg) {
		Color r = new Color();
		r.a = 1 - (1 - fg.a) * (1 - bg.a);
		r.r = fg.r * fg.a / r.a + bg.r * bg.a * (1 - fg.a) / r.a;
		r.g = fg.g * fg.a / r.a + bg.g * bg.a * (1 - fg.a) / r.a;
		r.b = fg.b * fg.a / r.a + bg.b * bg.a * (1 - fg.a) / r.a;
		return r;
	}

	@Override
	public void resize(int width, int height) {

		super.resize(width, height);

		camera = new PerspectiveCamera(GameSettings.CAMERA_FOV,
				getViewportWidth(), getViewportHeight());
		camera.lookAt(lastCameraDirection);
		camera.update(true);
		camera.far = viewDistance;
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

	void setEnvironment(Color color, float viewDistance, Vector3 sunPosition) {
		this.currentColor = color.cpy();
		this.viewDistance = viewDistance;

		// environment.clear();
		environment.add(new DirectionalLight().set(Color.WHITE,
				sunPosition.scl(-1)));
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f,
				0.4f, 0.4f, 1.f));
		environment.set(new ColorAttribute(ColorAttribute.Fog, color.r,
				color.g, color.b, color.a));
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
