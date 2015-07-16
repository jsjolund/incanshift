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
		int lastKeycode;

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
			float vx = screenX - viewport.getRightGutterWidth();
			float vy = (viewport.getWorldHeight() - screenY)
					- viewport.getBottomGutterHeight();
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
			float vx = screenX - viewport.getRightGutterWidth();
			float vy = (viewport.getWorldHeight() - screenY)
					- viewport.getBottomGutterHeight();

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

	Menu menu;
	MenuItem selectedItem;
	boolean itemValueSelected = false;
	MenuItem backItem;

	Sound soundClick;
	Sound soundEnter;

	Color valueUnselectedColor = Color.LIGHT_GRAY;
	Color valueSelectedColor = Color.YELLOW;
	Color nameUnselectedColor = Color.GRAY;
	Color nameSelectedColor = Color.WHITE;

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

			if (item.value == null) {
				int yspace = maxCharHeight;
				int xspace = texWidth / 2;

				int x = 10;
				int y = texHeight - 2 * yspace * (i + 1);

				sansLarge.setColor(Color.GRAY);
				GlyphLayout text = sansLarge.draw(spriteBatch, item.name, x, y
						+ yspace);

				sansLarge.setColor(Color.WHITE);
				sansLarge.draw(spriteBatch, item.name, x + xspace, y + yspace);

				int tw = (int) text.width;
				int th = (int) text.height * 2;

				TextureRegion texFalse = new TextureRegion(
						fbo.getColorBufferTexture(), x, y, tw, th);
				TextureRegion texTrue = new TextureRegion(
						fbo.getColorBufferTexture(), x + xspace, y, tw, th);

				texFalse.flip(false, true);
				texTrue.flip(false, true);
				item.setNameTex(texFalse, false);
				item.setNameTex(texTrue, true);

			} else {

				int yspace = maxCharHeight;
				int xspace = texWidth / 2;

				int x = 10;
				int y = texHeight - 2 * yspace * (i + 1);

				// Value unselected
				sansLarge.setColor(valueUnselectedColor);
				GlyphLayout nameValueText = sansLarge.draw(spriteBatch,
						item.name + item.value, x, y + yspace);
				int nvw = (int) nameValueText.width;
				// Value selected
				sansLarge.setColor(valueSelectedColor);
				sansLarge.draw(spriteBatch, item.name + item.value, x + xspace,
						y + yspace);

				// Name unselected
				sansLarge.setColor(nameUnselectedColor);
				GlyphLayout nameText = sansLarge.draw(spriteBatch, item.name,
						x, y + yspace);
				int nw = (int) nameText.width;
				int nh = (int) nameText.height * 2;
				// Name selected
				sansLarge.setColor(nameSelectedColor);
				sansLarge.draw(spriteBatch, item.name, x + xspace, y + yspace);
				int vw = nvw - nw;
				int vh = nh;

				TextureRegion texValueUnselected = new TextureRegion(
						fbo.getColorBufferTexture(), x + nw, y, vw, vh);
				TextureRegion texValueSelected = new TextureRegion(
						fbo.getColorBufferTexture(), x + xspace + nw, y, vw, vh);

				TextureRegion texNameUnselected = new TextureRegion(
						fbo.getColorBufferTexture(), x, y, nw, nh);
				TextureRegion texNameSelected = new TextureRegion(
						fbo.getColorBufferTexture(), x + xspace, y, nw, nh);

				texNameUnselected.flip(false, true);
				texNameSelected.flip(false, true);
				texValueSelected.flip(false, true);
				texValueUnselected.flip(false, true);
				item.setNameTex(texNameUnselected, false);
				item.setNameTex(texNameSelected, true);
				item.setValueTex(texValueUnselected, false);
				item.setValueTex(texValueSelected, true);

			}

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
				TextureRegion texName = item.getNameTex(item == selectedItem);
				spriteBatch.draw(texName, b.x, b.y);

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

		float vw = viewport.getScreenWidth();
		float vh = viewport.getScreenHeight();

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
