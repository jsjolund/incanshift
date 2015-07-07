package incanshift;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public abstract class AbstractScreen implements Screen {

	protected Viewport viewport;
	Camera camera;

	SpriteBatch spriteBatch;
	BitmapFont font12;

	Game game;
	int reqHeight;
	int reqWidth;

	public AbstractScreen(Game game, int reqWidth, int reqHeight) {
		this.game = game;
		this.reqHeight = reqHeight;
		this.reqWidth = reqWidth;

		spriteBatch = new SpriteBatch();

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("font/font.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 14;
		font12 = generator.generateFont(parameter);
		generator.dispose();

		camera = new OrthographicCamera(reqWidth, reqHeight);
		camera.position.set(reqWidth / 2, reqHeight / 2, 0);
		camera.update();

		viewport = new FitViewport(reqWidth, reqHeight, camera);
		viewport.apply();

	}

	@Override
	public void dispose() {
		spriteBatch.dispose();
		font12.dispose();
		game.dispose();

	}

	@Override
	public void resize(int width, int height) {

		viewport.update(width, height);
		camera.update(true);
		Gdx.gl.glViewport(0, 0, viewport.getScreenWidth(),
				viewport.getScreenHeight());

		Vector2 size = Scaling.fit.apply(reqWidth, reqHeight, width, height);
		int viewportX = (int) (width - size.x) / 2;
		int viewportY = (int) (height - size.y) / 2;
		int viewportWidth = (int) size.x;
		int viewportHeight = (int) size.y;

		Gdx.gl.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
		viewport.setWorldSize(width, height);
		viewport.setScreenSize(viewportWidth, viewportHeight);
		viewport.setScreenPosition(viewportX, viewportY);

	}
}
