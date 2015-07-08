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

public class CreditScreen extends AbstractScreen {

	class MenuInputProcessor implements InputProcessor {

		Vector3 tmp = new Vector3();
		int lastKeycode;

		@Override
		public boolean keyDown(int keycode) {
			selectedItem = MenuItem.EXIT;
			if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) {
				soundEnter.play();
				enterSelected();
			}
			if (keycode == Input.Keys.ESCAPE) {
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
					if (item == MenuItem.EXIT && item != selectedItem) {
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
				if (item.getBounds().contains(vx, vy) && item == MenuItem.EXIT) {
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
		EXIT("Back"), N4("Anton Bjuhr"), N3("Joel Sahlin"), N2(
				"Christoffer Lundberg"), N1("Johannes Sjölund"), N0(
				"Created by:");

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

		// public MenuItem getDown(MenuItem current) {
		// int i = current.ordinal();
		// return (i == 0) ? get(size() - 1) : get(i - 1);
		// }

		public TextureRegion getTex(boolean selected) {
			return (selected) ? texTrue : texFalse;
		}

		// public MenuItem getUp(MenuItem current) {
		// int i = current.ordinal();
		// return (i == size() - 1) ? get(0) : get(i + 1);
		// }

		public void resize(int width, int height) {
			float spacing = 50;
			int xc = width / 2;
			int yc = height / 2;
			int tw = texFalse.getRegionWidth();
			int th = texFalse.getRegionHeight();
			float yOffset = yc - (spacing + th) * (MenuItem.size() - 1) / 2;
			float x = xc - tw / 2;
			float y = yOffset + (th + spacing) * ordinal();
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

	MenuItem selectedItem = MenuItem.EXIT;

	Matrix4 uiMatrix;
	FrameBuffer fbo = null;

	MenuInputProcessor input;

	AssetManager assets;

	Music music;
	Sound soundClick;
	Sound soundEnter;

	public CreditScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight);

		assets = new AssetManager();
		assets.load("sound/click.wav", Sound.class);
		assets.load("sound/music_credits.wav", Music.class);
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
			int xspace = h / 2;

			int x = 10;
			int y = h - 2 * yspace * (i + 1);

			font42.setColor(Color.WHITE);
			GlyphLayout text = font42.draw(spriteBatch, item.toString(), x, y
					+ yspace);
			font42.setColor(Color.YELLOW);
			font42.draw(spriteBatch, item.toString(), x + xspace, y + yspace);

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
		case EXIT:
			game.showStartScreen();
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
			TextureRegion tex = item.getTex(false);
			if (item == MenuItem.EXIT) {
				tex = item.getTex(item == selectedItem);
			}
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
		music = assets.get("sound/music_credits.wav", Music.class);
		music.play();
		music.setVolume(0.3f);
		music.setLooping(true);

	}

}
