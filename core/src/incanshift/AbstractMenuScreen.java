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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

public abstract class AbstractMenuScreen extends AbstractScreen implements
		Disposable {

	class MenuInputProcessor implements InputProcessor {

		Vector3 tmp = new Vector3();

		@Override
		public boolean keyDown(int keycode) {
			if (keyDownCapture(keycode)) {
				return true;
			}
			if (keycode == GameSettings.FORWARD || keycode == Input.Keys.UP
					&& keycode != lastKeycode) {
				selectedItem = menu.getUp(selectedItem);
				soundClick.play();
			}
			if (keycode == GameSettings.BACKWARD || keycode == Input.Keys.DOWN
					&& keycode != lastKeycode) {
				selectedItem = menu.getDown(selectedItem);
				soundClick.play();
			}
			if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) {
				soundEnter.play();
				enterSelected();
			}
			if (keycode == Input.Keys.ESCAPE) {
				selectedItem = backItem;
				enterSelected();
			}
			lastKeycode = keycode;
			return true;
		}

		@Override
		public boolean keyTyped(char character) {
			if (keyTypedCapture(character)) {
				return true;
			}
			return false;

		}

		@Override
		public boolean keyUp(int keycode) {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean mouseMoved(int screenX, int screenY) {
			if (mouseMovedCapture(screenX, screenY)) {
				return true;
			}
			float vx = screenXtoViewportX(screenX);
			float vy = screenYtoViewportY(screenY);

			for (MenuItem item : menu) {
				if (item.selectable && item.getBounds().contains(vx, vy)) {
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
			if (touchDownCapture(screenX, screenY)) {
				return true;
			}
			float vx = screenXtoViewportX(screenX);
			float vy = screenYtoViewportY(screenY);

			for (MenuItem item : menu) {
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

	public static final String tag = "AbstractMenuScreen";

	AssetManager assets;
	Music music;
	String musicFile;

	MenuInputProcessor input;
	int lastKeycode;

	Menu menu;
	MenuItem selectedItem;
	boolean itemValueSelected = false;
	MenuItem backItem;

	Sound soundClick;
	Sound soundEnter;

	Color valueUnselectedColor = Color.LIGHT_GRAY;
	Color valueSelectedColor = Color.YELLOW;
	Color keyUnselectedColor = Color.GRAY;
	Color keySelectedColor = Color.WHITE;

	FrameBuffer fbo = null;

	public AbstractMenuScreen(IncanShift game, int reqWidth, int reqHeight,
			String musicFile) {
		super(game, reqWidth, reqHeight);

		this.musicFile = musicFile;

		input = new MenuInputProcessor();

		assets = new AssetManager();
		assets.load(musicFile, Music.class);
		assets.load("sound/click.wav", Sound.class);
		assets.load("sound/enter.wav", Sound.class);

		assets.finishLoading();
		soundClick = assets.get("sound/click.wav", Sound.class);
		soundEnter = assets.get("sound/enter.wav", Sound.class);
	}

	/**
	 * Draw menu text onto framebuffer in order to create textures with rendered
	 * menu strings
	 */
	void createMenuTextures() {
		camera.update();

		int texWidth = 1024;
		int texHeight = 1024;
		int maxCharHeight = 30;

		try {
			fbo = new FrameBuffer(Format.RGBA8888, texWidth, texHeight, false);
		} catch (Exception e) {
			System.out.println("Failed to create framebuffer.");
			e.printStackTrace();
		}

		fbo.begin();
		Gdx.graphics.getGL20().glClearColor(1, 1, 1, 0);

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, texWidth,
				texHeight);
		spriteBatch.begin();

		for (int i = 0; i < menu.size(); i++) {

			MenuItem item = menu.get(i);

			int yspace = maxCharHeight;
			int xspace = texWidth / 2;
			int x = 10;
			int y = texHeight - 2 * yspace * (i + 1);

			TextureRegion texValueUnselected;
			TextureRegion texValueSelected;

			TextureRegion texKeyUnselected;
			TextureRegion texKeySelected;

			// Value unselected
			sansLarge.setColor(valueUnselectedColor);
			String string = item.key + ((item.value == null) ? "" : item.value);
			GlyphLayout keyValueText = sansLarge.draw(spriteBatch, string, x, y
					+ yspace);
			// Value selected
			sansLarge.setColor(valueSelectedColor);
			sansLarge.draw(spriteBatch, string, x + xspace, y + yspace);

			int kvw = (int) keyValueText.width;

			// Key unselected
			sansLarge.setColor(keyUnselectedColor);
			GlyphLayout keyText = sansLarge.draw(spriteBatch, item.key, x, y
					+ yspace);

			// Key selected
			sansLarge.setColor(keySelectedColor);
			sansLarge.draw(spriteBatch, item.key, x + xspace, y + yspace);

			int kw = (int) keyText.width;
			int kh = (int) keyText.height * 2;

			// Set texture region for value if it exists
			if (item.value != null) {
				int vw = kvw - kw;

				texValueUnselected = new TextureRegion(
						fbo.getColorBufferTexture(), x + kw, y, vw, kh);
				texValueSelected = new TextureRegion(
						fbo.getColorBufferTexture(), x + xspace + kw, y, vw, kh);

				texValueSelected.flip(false, true);
				texValueUnselected.flip(false, true);

				item.setValueTex(texValueUnselected, false);
				item.setValueTex(texValueSelected, true);
			}

			// Set texture region for key
			texKeyUnselected = new TextureRegion(fbo.getColorBufferTexture(),
					x, y, kw, kh);
			texKeySelected = new TextureRegion(fbo.getColorBufferTexture(), x
					+ xspace, y, kw, kh);
			texKeyUnselected.flip(false, true);
			texKeySelected.flip(false, true);
			item.setKeyTex(texKeyUnselected, false);
			item.setKeyTex(texKeySelected, true);

		}
		spriteBatch.end();
		fbo.end();
	}

	@Override
	public void dispose() {
		super.dispose();
		menu.dispose();
		fbo.dispose();
		assets.dispose();
	}

	public abstract void enterSelected();

	@Override
	public void hide() {
		music.stop();
	}

	abstract boolean keyDownCapture(int keycode);

	abstract boolean keyTypedCapture(char character);

	abstract boolean mouseMovedCapture(int screenX, int screenY);

	abstract boolean touchDownCapture(int screenX, int screenY);

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

		if (menu != null) {
			for (int i = 0; i < menu.size(); i++) {
				MenuItem item = menu.get(i);

				Rectangle b = item.getBounds();
				TextureRegion texKey = item.getKeyTex(item == selectedItem);
				spriteBatch.draw(texKey, b.x, b.y);

				if (item.value != null) {
					TextureRegion texValue = item
							.getValueTex(item == selectedItem
									&& itemValueSelected);
					spriteBatch.draw(texValue, b.x + b.width + 20, b.y);
				}

			}
		}
		spriteBatch.end();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);

		float vw = getViewportWidth();
		float vh = getViewportHeight();

		if (menu != null) {
			menu.resize((int) vw, (int) vh);
		}

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	public void setMenu(Menu menu, MenuItem backItem) {
		this.menu = menu;
		this.backItem = backItem;
		selectedItem = menu.get(0);
		createMenuTextures();
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(input);
		music = assets.get(musicFile, Music.class);
		music.play();
		music.setVolume(1f);
		music.setLooping(true);

	}

}
