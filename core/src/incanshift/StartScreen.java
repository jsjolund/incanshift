package incanshift;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

public class StartScreen extends AbstractScreen {

	class MenuInputProcessor implements InputProcessor {

		Vector3 tmp = new Vector3();
		int lastKeycode;

		@Override
		public boolean keyDown(int keycode) {
			if (keycode == GameSettings.FORWARD || keycode == Input.Keys.UP
					&& keycode != lastKeycode) {
				selectedItem = selectedItem.getUp(selectedItem);
				soundClick.play();
			}
			if (keycode == GameSettings.BACKWARD || keycode == Input.Keys.DOWN
					&& keycode != lastKeycode) {
				selectedItem = selectedItem.getDown(selectedItem);
				soundClick.play();
			}
			if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) {
				soundEnter.play();
				enterSelected();
			}
			if (keycode == Input.Keys.ESCAPE && canResume) {
				selectedItem = MenuItem.START;
				enterSelected();
			}
			lastKeycode = keycode;
			return true;
		}

		@Override
		public boolean keyTyped(char character) {
			// TODO Auto-generated method stub
			return false;

		}

		@Override
		public boolean keyUp(int keycode) {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean mouseMoved(int screenX, int screenY) {

			float vx = screenX - viewport.getRightGutterWidth();
			float vy = (viewport.getWorldHeight() - screenY)
					- viewport.getBottomGutterHeight();

			for (MenuItem item : MenuItem.values()) {
				if (item.getBounds().contains(vx, vy)) {
					if (item != selectedItem) {
						soundClick.play();
					}
					selectedItem = item;
					break;
				}
			}
			return true;
		}

		@Override
		public boolean scrolled(int amount) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer,
				int button) {

			float vx = screenX - viewport.getRightGutterWidth();
			float vy = (viewport.getWorldHeight() - screenY)
					- viewport.getBottomGutterHeight();

			for (MenuItem item : MenuItem.values()) {
				if (item.getBounds().contains(vx, vy)) {
					soundEnter.play();
					enterSelected();
					break;
				}
			}

			return true;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			// TODO Auto-generated method stub
			return false;
		}
	}

	private enum MenuItem {
		EXIT("Exit"), CREDITS("Credits"), OPTIONS("Options"), START("Start");

		public static void dispose() {
			get(0).texTrue.getTexture().dispose();
		}

		public static MenuItem get(int i) {
			return MenuItem.values()[i];
		}

		public static int size() {
			return MenuItem.values().length;
		}

		private String name;

		private TextureRegion texFalse;

		private TextureRegion texTrue;

		private Rectangle bounds = new Rectangle();

		private MenuItem(String name) {
			this.name = name;
		}

		public Rectangle getBounds() {
			return bounds;
		}

		public MenuItem getDown(MenuItem current) {
			int i = current.ordinal();
			return (i == 0) ? get(size() - 1) : get(i - 1);
		}

		public TextureRegion getTex(boolean selected) {
			return (selected) ? texTrue : texFalse;
		}

		public MenuItem getUp(MenuItem current) {
			int i = current.ordinal();
			return (i == size() - 1) ? get(0) : get(i + 1);
		}

		public void resize(int width, int height) {
			int xc = width / 2;
			int yc = height / 2;
			int tw = texFalse.getRegionWidth();
			int th = texFalse.getRegionHeight();
			float yOffset = yc - (textVerticalSpacing + th)
					* (MenuItem.size() - 1) / 2;
			float x = xc - tw / 2;
			float y = yOffset + (th + textVerticalSpacing) * ordinal();
			bounds.set(x, y, tw, th);
		}

		public void setTex(TextureRegion tex, boolean selected) {
			if (selected) {
				this.texTrue = tex;
			} else {
				this.texFalse = tex;
			}
		}

		@Override
		public String toString() {
			return name;
		}

	}

	static float textVerticalSpacing = 5;

	MenuItem selectedItem = MenuItem.START;
	boolean canResume = false;

	Matrix4 uiMatrix;
	FrameBuffer fbo = null;

	MenuInputProcessor input;

	AssetManager assets;

	Music music;
	Sound soundClick;
	Sound soundEnter;

	public StartScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight);

		assets = new AssetManager();
		assets.load("sound/music_menu.ogg", Music.class);
		assets.load("sound/click.wav", Sound.class);
		assets.load("sound/enter.wav", Sound.class);

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, viewport.getScreenWidth(),
				viewport.getScreenHeight());

		createMenuTextures();
		input = new MenuInputProcessor();

		assets.finishLoading();
		soundClick = assets.get("sound/click.wav", Sound.class);
		soundEnter = assets.get("sound/enter.wav", Sound.class);

	}

	/**
	 * Draw menu text
	 */
	private void createMenuTextures() {
		camera.update();

		int w = 1024;
		int h = 1024;

		try {
			fbo = new FrameBuffer(Format.RGBA8888, w, h, false);
		} catch (Exception e) {
			System.out.println("Failed to create fbo.");
			e.printStackTrace();
		}

		fbo.begin();
		Gdx.graphics.getGL20().glClearColor(0.5f, 0.5f, 0.5f, 0);

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
		spriteBatch.begin();

		for (int i = 0; i < MenuItem.size(); i++) {
			MenuItem item = MenuItem.get(i);

			int yspace = 50;
			int xspace = 256;

			int x = 10;
			int y = h - 2 * yspace * (i + 1);

			sansLarge.setColor(Color.GRAY);
			GlyphLayout text = sansLarge.draw(spriteBatch, item.toString(), x,
					y + yspace);
			sansLarge.setColor(Color.WHITE);
			sansLarge
					.draw(spriteBatch, item.toString(), x + xspace, y + yspace);

			int tw = (int) text.width;
			int th = (int) text.height * 2;

			TextureRegion texFalse = new TextureRegion(
					fbo.getColorBufferTexture(), x, y, tw, th);
			TextureRegion texTrue = new TextureRegion(
					fbo.getColorBufferTexture(), x + xspace, y, tw, th);

			texFalse.flip(false, true);
			texTrue.flip(false, true);
			item.setTex(texFalse, false);
			item.setTex(texTrue, true);

		}
		spriteBatch.end();
		fbo.end();

	}

	@Override
	public void dispose() {
		super.dispose();
		MenuItem.dispose();
		fbo.dispose();
		assets.dispose();
	}

	private void enterSelected() {

		switch (selectedItem) {
		case START:
			canResume = true;
			MenuItem.dispose();
			MenuItem.START.name = "Resume";
			createMenuTextures();
			game.showGameScreen();
			break;
		case CREDITS:
			game.showCreditScreen();
			break;
		case EXIT:
			Gdx.app.exit();
			break;

		default:
			break;
		}
	}

	@Override
	public void hide() {
		music.stop();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	@Override
	public void render(float delta) {
		super.render(delta);

		spriteBatch.setShader(null);
		spriteBatch.setProjectionMatrix(uiMatrix);

		spriteBatch.begin();

		for (int i = 0; i < MenuItem.size(); i++) {
			MenuItem item = MenuItem.get(i);
			Rectangle b = item.getBounds();
			TextureRegion tex = item.getTex(item == selectedItem);
			spriteBatch.draw(tex, b.x, b.y);
		}
		spriteBatch.end();

	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);

		float vw = viewport.getScreenWidth();
		float vh = viewport.getScreenHeight();

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, vw, vh);

		for (MenuItem item : MenuItem.values()) {
			item.resize((int) vw, (int) vh);
		}

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(input);
		music = assets.get("sound/music_menu.ogg", Music.class);
		music.play();
		music.setVolume(1f);
		music.setLooping(true);

	}
}
