package incanshift.screen;

import incanshift.IncanShift;
import incanshift.screen.menu.Menu;
import incanshift.screen.menu.MenuItem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class StartScreen extends AbstractMenuScreen {

	boolean canResume = false;

	MenuItem back;
	MenuItem credits;
	MenuItem options;
	MenuItem start;
	
	BitmapFont menuFont;
	
	public StartScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight, "sound/music_menu.ogg");
		
		menuFont = sansLarge;
		
		back = new MenuItem("Exit", null, true);
		credits = new MenuItem("Credits", null, true);
		options = new MenuItem("Options", null, true);
		start = new MenuItem("Start", null, true);
		Menu menu = new Menu();
		menu.add(back);
		menu.add(credits);
		menu.add(options);
		menu.add(start);

		setMenu(menu, back, menuFont);
		selectedItem = start;

	}

	public void enterSelected() {
		if (selectedItem == start) {
			if (!canResume) {
				canResume = true;
				menu.dispose();
				start.key = "Resume";
				createMenuTextures(menuFont);
			}
			game.showGameScreen();
		}
		if (selectedItem == credits) {
			game.showCreditScreen();
		}
		if (selectedItem == options) {
			game.showSettingsScreen();
		}
		if (selectedItem == back) {
			Gdx.app.exit();
		}

	}

	@Override
	boolean keyDownCapture(int keycode) {
		if (canResume && keycode == Input.Keys.ESCAPE) {
			selectedItem = start;
			enterSelected();
			return true;
		}
		return false;
	}

	@Override
	boolean keyTypedCapture(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	boolean mouseMovedCapture(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	boolean touchDownCapture(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}

}
