package incanshift.screen;

import com.badlogic.gdx.utils.Array;
import incanshift.IncanShift;
import incanshift.screen.menu.Menu;
import incanshift.screen.menu.MenuItem;
import incanshift.world.GameSettings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import java.util.Random;

public abstract class AbstractMenuScreen extends AbstractScreen implements Disposable {

	class MenuInputProcessor implements InputProcessor {

		Vector3 tmp = new Vector3();

		@Override
		public boolean keyDown(int keycode) {
			if (keyDownCapture(keycode)) {
				return true;
			}
			if ((keycode == GameSettings.FORWARD || keycode == Keys.UP)) {
				selectedItem = menu.getUp(selectedItem);
				soundClick();
			}
			if ((keycode == GameSettings.BACKWARD || keycode == Keys.DOWN)) {
				selectedItem = menu.getDown(selectedItem);
				soundClick();
			}

			if (keycode == Keys.ENTER
					&& (Gdx.input.isKeyPressed(Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Keys.ALT_RIGHT))) {
				game.toggleFullscreen();

			} else if (keycode == Keys.SPACE || keycode == Keys.ENTER) {
				soundEnter();
				enterSelected();
			}
			if (keycode == Keys.ESCAPE) {
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
						soundClick();
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
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			if (touchDownCapture(screenX, screenY)) {
				return true;
			}
			float vx = screenXtoViewportX(screenX);
			float vy = screenYtoViewportY(screenY);

			for (MenuItem item : menu) {
				if (item.getBounds().contains(vx, vy)) {
					soundEnter();
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
	Texture bkgTex;

	MenuInputProcessor input;
	int lastKeycode;

	Menu menu;
	MenuItem selectedItem;
	boolean itemValueSelected = false;
	MenuItem backItem;

	protected void soundClick() {
		soundClick.get(randInt(0, soundClick.size - 1)).play(
				1.0f * GameSettings.SOUND_VOLUME);
	}

	protected void soundEnter() {
		soundEnter.get(randInt(0, soundEnter.size - 1)).play(
				1.0f * GameSettings.SOUND_VOLUME);
	}

	private Array<Sound> soundClick = new Array<Sound>();
	private Array<Sound> soundEnter = new Array<Sound>();

	Color valueUnselectedColor = Color.LIGHT_GRAY;
	Color valueSelectedColor = Color.YELLOW;
	Color keyUnselectedColor = Color.LIGHT_GRAY;
	Color keySelectedColor = Color.WHITE;

	FrameBuffer fbo = null;

	AbstractMenuScreen parentMenu;

	private static Random rand = new Random();

	private static int randInt(int min, int max) {
		return rand.nextInt((max - min) + 1) + min;
	}

	public AbstractMenuScreen(IncanShift game, AbstractMenuScreen parentMenu, int reqWidth, int reqHeight, String musicFile) {
		super(game, reqWidth, reqHeight);
		this.musicFile = musicFile;
		this.parentMenu = parentMenu;

		input = new MenuInputProcessor();
		try {
			Gdx.input.setCursorImage(new Pixmap(Gdx.files.local("model/cursor.png")), 0, 0);
		} catch (Exception e) {
			Gdx.app.debug(tag, "Cannot set cursor pixmap..", e);
		}
		assets = new AssetManager();

		assets.load("sound/menu1.ogg", Sound.class);
		assets.load("sound/menu2.ogg", Sound.class);
		assets.load("sound/menu3.ogg", Sound.class);
		assets.load("sound/menu4.ogg", Sound.class);
		assets.load("sound/menu5.ogg", Sound.class);
		assets.load("sound/menu6.ogg", Sound.class);
		assets.load("sound/menu_long.ogg", Sound.class);

		if (musicFile != null) {
			assets.load(musicFile, Music.class);
		}
		assets.finishLoading();
		soundClick.add(assets.get("sound/menu1.ogg", Sound.class));
		soundClick.add(assets.get("sound/menu2.ogg", Sound.class));
		soundClick.add(assets.get("sound/menu3.ogg", Sound.class));
		soundClick.add(assets.get("sound/menu4.ogg", Sound.class));
		soundClick.add(assets.get("sound/menu5.ogg", Sound.class));

		soundEnter.add(assets.get("sound/menu6.ogg", Sound.class));
//        soundEnter.add(assets.get("sound/menu_long.ogg", Sound.class));
		if (musicFile != null) {
			music = assets.get(musicFile, Music.class);
		}
	}

	public void setBackgroundImage(Pixmap pixmap) {
		bkgTex = new Texture(pixmap);
	}

	/**
	 * Draw menu text onto framebuffer in order to create textures with rendered
	 * menu strings
	 */
	void createMenuTextures(BitmapFont font) {
		camera.update();

		int texWidth = 1024;
		int texHeight = 1024;
		int maxCharHeight = (int) font.getLineHeight();

		try {
			fbo = new FrameBuffer(Format.RGBA8888, texWidth, texHeight, false);
		} catch (Exception e) {
			System.out.println("Failed to create framebuffer.");
			e.printStackTrace();
		}

		fbo.begin();
		Gdx.graphics.getGL20().glClearColor(1, 1, 1, 0);

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, texWidth, texHeight);
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
			font.setColor(valueUnselectedColor);
			String string = item.key + ((item.value == null) ? "" : item.value);
			GlyphLayout keyValueText = font.draw(spriteBatch, string, x, y + yspace);
			// Value selected
			font.setColor(valueSelectedColor);
			font.draw(spriteBatch, string, x + xspace, y + yspace);

			int kvw = (int) keyValueText.width;

			// Key unselected
			font.setColor(keyUnselectedColor);
			GlyphLayout keyText = font.draw(spriteBatch, item.key, x, y + yspace);

			// Key selected
			font.setColor(keySelectedColor);
			font.draw(spriteBatch, item.key, x + xspace, y + yspace);

			int kw = (int) keyText.width;
			int kh = (int) keyText.height * 2;

			// Set texture region for value if it exists
			if (item.value != null) {
				int vw = kvw - kw;

				texValueUnselected = new TextureRegion(fbo.getColorBufferTexture(), x + kw, y, vw, kh);
				texValueSelected = new TextureRegion(fbo.getColorBufferTexture(), x + xspace + kw, y, vw, kh);

				texValueSelected.flip(false, true);
				texValueUnselected.flip(false, true);

				item.setValueTex(texValueUnselected, false);
				item.setValueTex(texValueSelected, true);
			}

			// Set texture region for key
			texKeyUnselected = new TextureRegion(fbo.getColorBufferTexture(), x, y, kw, kh);
			texKeySelected = new TextureRegion(fbo.getColorBufferTexture(), x + xspace, y, kw, kh);
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
		if (music != null) {
			music.pause();
		}
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

		if (bkgTex != null) {
			spriteBatch.draw(bkgTex, 0, 0);
		}

		if (menu != null) {
			for (int i = 0; i < menu.size(); i++) {
				MenuItem item = menu.get(i);

				Rectangle b = item.getBounds();
				TextureRegion texKey = item.getKeyTex(item == selectedItem);
				spriteBatch.draw(texKey, b.x, b.y);

				if (item.value != null) {
					TextureRegion texValue = item.getValueTex(item == selectedItem && itemValueSelected);
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

	public void setMenu(Menu menu, MenuItem backItem, BitmapFont font) {
		this.menu = menu;
		this.backItem = backItem;
		selectedItem = menu.get(0);
		createMenuTextures(font);
	}

	@Override
	public void show() {
		Gdx.input.setCursorCatched(false);
		Gdx.input.setInputProcessor(input);

		Music m = (music != null) ? music : parentMenu.music;
		if (m != null) {
			m.play();
			m.setVolume(1f * GameSettings.MUSIC_VOLUME);
			m.setLooping(true);
		}
	}

}
