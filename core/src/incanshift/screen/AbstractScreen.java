package incanshift.screen;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import incanshift.IncanShift;

import java.nio.ByteBuffer;

public abstract class AbstractScreen implements Screen {

	public static final String tag = "AbstractScreen";
	public static BitmapFont sansTiny;
	public static BitmapFont sansNormal;
	public static BitmapFont sansLarge;
	public static BitmapFont sansHuge;
	public static BitmapFont monoTiny;
	public static BitmapFont monoNormal;
	public static BitmapFont monoLarge;
	protected Viewport viewport;
	Camera camera;
	SpriteBatch spriteBatch;
	Matrix4 uiMatrix;
	Vector3 screenCenter = new Vector3();
	IncanShift game;
	int reqHeight;
	int reqWidth;

	public AbstractScreen(IncanShift game, int reqWidth, int reqHeight) {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		this.game = game;
		this.reqHeight = reqHeight;
		this.reqWidth = reqWidth;

		spriteBatch = new SpriteBatch();

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font/sans.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();

		parameter.size = 12;
		sansTiny = generator.generateFont(parameter);

		parameter.size = 20;
		sansNormal = generator.generateFont(parameter);

		parameter.size = 34;
		sansLarge = generator.generateFont(parameter);

		parameter.size = 62;
		sansHuge = generator.generateFont(parameter);
		generator.dispose();

		generator = new FreeTypeFontGenerator(Gdx.files.internal("font/mono.ttf"));
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

	public static Pixmap getScreenshot(int x, int y, int w, int h, boolean yDown) {
		final Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(x, y, w, h);

		if (yDown) {
			// Flip the pixmap upside down
			ByteBuffer pixels = pixmap.getPixels();
			int numBytes = w * h * 4;
			byte[] lines = new byte[numBytes];
			int numBytesPerLine = w * 4;
			for (int i = 0; i < h; i++) {
				pixels.position((h - i - 1) * numBytesPerLine);
				pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
			}
			pixels.clear();
			pixels.put(lines);
			pixels.clear();
		}

		return pixmap;
	}

	@Override
	public void dispose() {
		sansTiny.dispose();
		sansNormal.dispose();
		sansLarge.dispose();
		sansHuge.dispose();
		monoTiny.dispose();
		monoNormal.dispose();
		monoLarge.dispose();
		spriteBatch.dispose();
		game.dispose();
	}

	public int getLeftGutterWidth() {
		return viewport.getLeftGutterWidth();
	}

	public int getBottomGutterWidth() {
		return viewport.getBottomGutterHeight();
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

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, viewportWidth, viewportHeight);

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
		return viewport.getWorldHeight() - viewport.getBottomGutterHeight() - screenY;
	}
}
