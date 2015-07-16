package incanshift;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

public class SettingsScreen extends AbstractMenuScreen {

	private MenuItem back;
	private MenuItem keyUse;
	private MenuItem keyFire;
	private MenuItem keyRun;
	private MenuItem keyJump;
	private MenuItem keyStrafeLeft;
	private MenuItem keyStrafeRight;
	private MenuItem keyBackward;
	private MenuItem keyForward;

	boolean capturing = false;

	public SettingsScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight, "sound/music_menu.ogg");

		back = new MenuItem("Back", null, true);
		keyUse = new MenuItem("Use/Pick Up", Keys.toString(GameSettings.USE),
				true);
		keyFire = new MenuItem("Fire/Throw", "Left Mouse", false);
		keyRun = new MenuItem("Run", Keys.toString(GameSettings.RUN), true);
		keyJump = new MenuItem("Jump", Keys.toString(GameSettings.JUMP), true);
		keyStrafeLeft = new MenuItem("Strafe Left",
				Keys.toString(GameSettings.STRAFE_LEFT), true);
		keyStrafeRight = new MenuItem("Strafe Right",
				Keys.toString(GameSettings.STRAFE_RIGHT), true);
		keyBackward = new MenuItem("Move Backward",
				Keys.toString(GameSettings.BACKWARD), true);
		keyForward = new MenuItem("Move Forward",
				Keys.toString(GameSettings.FORWARD), true);

		Menu menu = new Menu();
		menu.add(back);
		menu.add(keyUse);
		menu.add(keyRun);
		menu.add(keyJump);
		menu.add(keyStrafeLeft);
		menu.add(keyStrafeRight);
		menu.add(keyBackward);
		menu.add(keyForward);
		menu.add(keyFire);

		setMenu(menu, back);
	}

	public void enterSelected() {

		if (selectedItem == back) {
			game.showStartScreen();

		} else if (selectedItem.selectable) {
			itemValueSelected = true;
			capturing = true;
			msg = "Press a key for " + selectedItem.name
					+ ", or Esc to cancel...";
		}

	}

	@Override
	boolean keyDownCapture(int keycode) {
		if (!capturing) {
			return false;
		}

		itemValueSelected = false;
		capturing = false;

		if (keycode == Keys.ESCAPE) {
			return true;
		}

		if (selectedItem == keyUse) {
			GameSettings.USE = keycode;
		}
		if (selectedItem == keyRun) {
			GameSettings.RUN = keycode;
		}
		if (selectedItem == keyJump) {
			GameSettings.JUMP = keycode;
		}
		if (selectedItem == keyStrafeLeft) {
			GameSettings.STRAFE_LEFT = keycode;
		}
		if (selectedItem == keyStrafeRight) {
			GameSettings.STRAFE_RIGHT = keycode;
		}
		if (selectedItem == keyBackward) {
			GameSettings.BACKWARD = keycode;
		}
		if (selectedItem == keyForward) {
			GameSettings.FORWARD = keycode;
		}

		msg = null;

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
			sansNormal.setColor(nameSelectedColor);
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
