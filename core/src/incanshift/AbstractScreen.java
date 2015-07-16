package incanshift;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public abstract class AbstractScreen implements Screen {

	protected Viewport viewport;
	Camera camera;

	SpriteBatch spriteBatch;

	Matrix4 uiMatrix;
	Vector3 screenCenter = new Vector3();

	BitmapFont sansTiny;
	BitmapFont sansNormal;
	BitmapFont sansLarge;
	BitmapFont monoTiny;
	BitmapFont monoNormal;
	BitmapFont monoLarge;

	IncanShift game;
	int reqHeight;
	int reqWidth;

	public AbstractScreen(IncanShift game, int reqWidth, int reqHeight) {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		this.game = game;
		this.reqHeight = reqHeight;
		this.reqWidth = reqWidth;

		spriteBatch = new SpriteBatch();

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("font/sans.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 12;
		sansTiny = generator.generateFont(parameter);
		parameter.size = 24;
		sansNormal = generator.generateFont(parameter);
		parameter.size = 35;
		sansLarge = generator.generateFont(parameter);
		generator.dispose();

		generator = new FreeTypeFontGenerator(
				Gdx.files.internal("font/mono.ttf"));
		parameter = new FreeTypeFontParameter();
		parameter.size = 12;
		monoTiny = generator.generateFont(parameter);
		parameter.size = 24;
		monoNormal = generator.generateFont(parameter);
		parameter.size = 35;
		monoLarge = generator.generateFont(parameter);
		generator.dispose();

		camera = new OrthographicCamera(reqWidth, reqHeight);
		camera.position.set(reqWidth / 2, reqHeight / 2, 0);
		camera.update();

		viewport = new FitViewport(reqWidth, reqHeight, camera);
		viewport.apply();

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, getViewportWidth(), getViewportHeight());

	}

	@Override
	public void dispose() {
		spriteBatch.dispose();
		sansTiny.dispose();
		game.dispose();

	}

	public int getViewportHeight() {
		return viewport.getScreenHeight();
	}

	public int getViewportWidth() {
		return viewport.getScreenWidth();
	}

	@Override
	public void render(float delta) {
		Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public void resize(int width, int height) {

		Vector2 size = Scaling.fit.apply(reqWidth, reqHeight, width, height);
		int viewportX = (int) (width - size.x) / 2;
		int viewportY = (int) (height - size.y) / 2;
		int viewportWidth = (int) size.x;
		int viewportHeight = (int) size.y;

		Gdx.gl.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
		viewport.setWorldSize(width, height);
		viewport.setScreenSize(viewportWidth, viewportHeight);
		viewport.setScreenPosition(viewportX, viewportY);

		viewport.apply();

		screenCenter.set(width / 2, height / 2, 1);

		float vw = getViewportWidth();
		float vh = getViewportHeight();

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, vw, vh);
	}

	public Vector3 screenPointToViewport(Vector3 screen) {
		screen.x = screenXtoViewportX(screen.x);
		screen.y = screenYtoViewportY(screen.y);
		return screen;
	}

	public float screenXtoViewportX(float screenX) {
		return screenX - viewport.getRightGutterWidth();
	}

	public float screenYtoViewportY(float screenY) {
		return viewport.getWorldHeight() - viewport.getBottomGutterHeight()
				- screenY;
	}
}
