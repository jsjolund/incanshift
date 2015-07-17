package incanshift;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;

public class SettingsScreen extends AbstractMenuScreen {

	private MenuItem backItem;
	private MenuItem keyFireItem;
	private MenuItem keyUseItem;
	private MenuItem keyRunItem;
	private MenuItem keyJumpItem;
	private MenuItem keyStrafeLeftItem;
	private MenuItem keyStrafeRightItem;
	private MenuItem keyBackwardItem;
	private MenuItem keyForwardItem;

	boolean capturing = false;

	ArrayMap<Integer, MenuItem> keycodeUses = new ArrayMap<Integer, MenuItem>();

	public SettingsScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight, "sound/music_menu.ogg");

		backItem = new MenuItem("Back", null, true);
		keyFireItem = new MenuItem("Fire/Throw", "Left Mouse", false);

		int key;

		key = GameSettings.USE;
		keyUseItem = new MenuItem("Use/Pick Up", Keys.toString(key), true);
		keycodeUses.put(key, keyUseItem);

		key = GameSettings.RUN;
		keyRunItem = new MenuItem("Run", Keys.toString(key), true);
		keycodeUses.put(key, keyRunItem);

		key = GameSettings.JUMP;
		keyJumpItem = new MenuItem("Jump", Keys.toString(key), true);
		keycodeUses.put(key, keyJumpItem);

		key = GameSettings.STRAFE_LEFT;
		keyStrafeLeftItem = new MenuItem("Strafe Left", Keys.toString(key),
				true);
		keycodeUses.put(key, keyStrafeLeftItem);

		key = GameSettings.STRAFE_RIGHT;
		keyStrafeRightItem = new MenuItem("Strafe Right", Keys.toString(key),
				true);
		keycodeUses.put(key, keyStrafeRightItem);

		key = GameSettings.BACKWARD;
		keyBackwardItem = new MenuItem("Move Backward", Keys.toString(key),
				true);
		keycodeUses.put(key, keyBackwardItem);

		key = GameSettings.FORWARD;
		keyForwardItem = new MenuItem("Move Forward", Keys.toString(key), true);
		keycodeUses.put(key, keyForwardItem);

		Menu menu = new Menu();
		menu.add(backItem);
		menu.add(keyFireItem);
		for (Entry<Integer, MenuItem> entry : keycodeUses) {
			menu.add(entry.value);
		}
		setMenu(menu, backItem);
	}

	public void enterSelected() {

		if (selectedItem == backItem) {
			game.showStartScreen();

		} else if (selectedItem.selectable) {
			itemValueSelected = true;
			capturing = true;
			if (msg == null) {
				msg = "Press a key for " + selectedItem.key
						+ ", or Esc to cancel...";
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
				msg = "Key used by " + occupying.key
						+ ", select new or Esc to cancel...";
			} else {
				keyDownCapture(Keys.ESCAPE);
			}
			return true;
		}

		if (selectedItem == keyUseItem) {
			GameSettings.USE = keycode;
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
		createMenuTextures();
		game.showSettingsScreen();

		return true;
	}

	float msgXpos = 0;
	float msgYpos = 30;
	String msg = null;

	@Override
	public void render(float delta) {
		super.render(delta);

		if (msg == null) {
			return;
		}
		spriteBatch.setShader(null);
		spriteBatch.setProjectionMatrix(uiMatrix);
		spriteBatch.begin();

		if (msgXpos == 0) {
			Color transparent = Color.YELLOW.cpy();
			transparent.a = 0;
			sansNormal.setColor(transparent);
		} else {
			sansNormal.setColor(keySelectedColor);
		}
		GlyphLayout msgGlyph = sansNormal.draw(spriteBatch, msg, msgXpos,
				msgYpos);

		msgXpos = screenCenter.x - msgGlyph.width / 2;

		spriteBatch.end();

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
