package incanshift.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import incanshift.IncanShift;
import incanshift.screen.menu.Menu;
import incanshift.screen.menu.MenuItem;
import incanshift.world.GameSettings;

public class SettingsScreen extends AbstractMenuScreen {

	boolean capturing = false;
	BitmapFont menuFont;
	ArrayMap<Integer, MenuItem> keycodeUses = new ArrayMap<Integer, MenuItem>();
	float msgXpos = 0;
	float msgYpos = 30;
	String msg = null;
	private MenuItem backItem;
	private MenuItem fullscreenItem;
	private MenuItem keyFireItem;
	private MenuItem keyGrappleItem;
	private MenuItem keyMaskItem;
	// private MenuItem keyUseItem;
	private MenuItem keyRunItem;
	private MenuItem keyJumpItem;
	private MenuItem keyStrafeLeftItem;
	private MenuItem keyStrafeRightItem;
	private MenuItem keyBackwardItem;
	private MenuItem keyForwardItem;

	public SettingsScreen(IncanShift game, AbstractMenuScreen parentMenu, int reqWidth, int reqHeight) {
		super(game, parentMenu, reqWidth, reqHeight, null);

		menuFont = sansNormal;

		backItem = new MenuItem("Back", null, true);
		keyFireItem = new MenuItem("Fire/Throw", "Left Mouse", false);
		keyGrappleItem = new MenuItem("Grappling Hook", "Right Mouse", false);
		fullscreenItem = new MenuItem("Fullscreen", Gdx.graphics.isFullscreen() ? "On" : "Off", true);

		int key;

		key = GameSettings.MASK;
		keyMaskItem = new MenuItem("Mask Vision", Keys.toString(key), true);
		keycodeUses.put(key, keyMaskItem);

		key = GameSettings.RUN;
		keyRunItem = new MenuItem("Run", Keys.toString(key), true);
		keycodeUses.put(key, keyRunItem);

		key = GameSettings.JUMP;
		keyJumpItem = new MenuItem("Jump", Keys.toString(key), true);
		keycodeUses.put(key, keyJumpItem);

		key = GameSettings.STRAFE_LEFT;
		keyStrafeLeftItem = new MenuItem("Strafe Left", Keys.toString(key), true);
		keycodeUses.put(key, keyStrafeLeftItem);

		key = GameSettings.STRAFE_RIGHT;
		keyStrafeRightItem = new MenuItem("Strafe Right", Keys.toString(key), true);
		keycodeUses.put(key, keyStrafeRightItem);

		key = GameSettings.BACKWARD;
		keyBackwardItem = new MenuItem("Move Backward", Keys.toString(key), true);
		keycodeUses.put(key, keyBackwardItem);

		key = GameSettings.FORWARD;
		keyForwardItem = new MenuItem("Move Forward", Keys.toString(key), true);
		keycodeUses.put(key, keyForwardItem);

		Menu menu = new Menu();
		menu.add(backItem);

		for (Entry<Integer, MenuItem> entry : keycodeUses) {
			menu.add(entry.value);
		}
		menu.add(keyGrappleItem);
		menu.add(keyFireItem);
		menu.add(fullscreenItem);
		setMenu(menu, backItem, menuFont);
	}

	public void enterSelected() {

		if (selectedItem == backItem) {
			game.showStartScreen();

		} else if (selectedItem.selectable) {

			if (selectedItem == fullscreenItem) {
				game.toggleFullscreen();
				selectedItem.value = Gdx.graphics.isFullscreen() ? "On" : "Off";
				menu.dispose();
				createMenuTextures(menuFont);
				selectedItem = backItem;

			} else {
				itemValueSelected = true;
				capturing = true;
				if (msg == null) {
					msg = "Press a key for " + selectedItem.key + ", or Esc to cancel...";
				}
			}
		}
	}

	@Override
	boolean keyDownCapture(int keycode) {

		if (!capturing) {
			return false;
		}

		if (keycode == lastKeycode) {
			return true;
		}

		if (keycode == Keys.ESCAPE || keycode == Keys.ENTER) {
			msg = null;
			itemValueSelected = false;
			capturing = false;
			lastKeycode = 0;
			return true;
		}

		if (keycodeUses.containsKey(keycode)) {
			MenuItem occupying = keycodeUses.get(keycode);
			if (selectedItem != occupying) {
				msg = "Key used by " + occupying.key + ", select new or Esc to cancel...";
			} else {
				keyDownCapture(Keys.ESCAPE);
			}
			return true;
		}

		if (selectedItem == keyMaskItem) {
			GameSettings.MASK = keycode;
		}
		if (selectedItem == keyRunItem) {
			GameSettings.RUN = keycode;
		}
		if (selectedItem == keyJumpItem) {
			GameSettings.JUMP = keycode;
		}
		if (selectedItem == keyStrafeLeftItem) {
			GameSettings.STRAFE_LEFT = keycode;
		}
		if (selectedItem == keyStrafeRightItem) {
			GameSettings.STRAFE_RIGHT = keycode;
		}
		if (selectedItem == keyBackwardItem) {
			GameSettings.BACKWARD = keycode;
		}
		if (selectedItem == keyForwardItem) {
			GameSettings.FORWARD = keycode;
		}

		msg = null;

		itemValueSelected = false;
		capturing = false;

		keycodeUses.removeKey(keycode);
		keycodeUses.removeValue(selectedItem, true);
		keycodeUses.put(keycode, selectedItem);

		// Redraw settings menu
		selectedItem.value = Keys.toString(keycode);
		menu.dispose();
		createMenuTextures(menuFont);
		game.showSettingsScreen();

		return true;
	}

	@Override
	public void render(float delta) {
		super.render(delta);

		spriteBatch.setShader(null);
		spriteBatch.setProjectionMatrix(uiMatrix);
		spriteBatch.begin();

		if (msgXpos == 0) {
			Color transparent = Color.YELLOW.cpy();
			transparent.a = 0;
			menuFont.setColor(transparent);
		} else {
			menuFont.setColor(keySelectedColor);
		}
		spriteBatch.end();

		if (msg != null) {
			spriteBatch.begin();
			GlyphLayout msgGlyph = menuFont.draw(spriteBatch, msg, msgXpos, msgYpos);
			msgXpos = screenCenter.x - msgGlyph.width / 2;
			spriteBatch.end();
		}


	}

	@Override
	boolean keyTypedCapture(char character) {
		return capturing;
	}

	@Override
	boolean mouseMovedCapture(int screenX, int screenY) {
		return capturing;
	}

	@Override
	boolean touchDownCapture(int screenX, int screenY) {
		return capturing;
	}

}
