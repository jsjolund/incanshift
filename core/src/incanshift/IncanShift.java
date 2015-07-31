package incanshift;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import incanshift.screen.*;
import incanshift.world.GameSettings;

public class IncanShift extends Game {

	public static final String tag = "IncanShift";
	public static int reqWidth = 1280;
	public static int reqHeight = 720;
	Pixmap menuBackground;
	private IncanShift game;
	private AbstractScreen currentScreen;
	private StartScreen startScreen;
	private GameScreen gameScreen;
	private CreditScreen creditScreen;
	private SettingsScreen settingsScreen;

	public void toggleFullscreen() {
		if (Gdx.graphics.isFullscreen()) {
			Gdx.app.debug(tag, String.format("Disabling fullscreen w=%s, h=%s", reqWidth, reqHeight));
			Gdx.graphics.setDisplayMode(reqWidth, reqHeight, false);
		} else {
			Gdx.app.debug(tag, String.format("Enabling fullscreen w=%s, h=%s",
					Gdx.graphics.getDesktopDisplayMode().width, Gdx.graphics.getDesktopDisplayMode().height));
			Gdx.graphics.setDisplayMode(Gdx.graphics.getDesktopDisplayMode().width,
					Gdx.graphics.getDesktopDisplayMode().height, true);
		}
	}

	@Override
	public void create() {
		game = this;
		Gdx.graphics.setDisplayMode(reqWidth, reqHeight, false);
		try {
			Gdx.input.setCursorImage(new Pixmap(Gdx.files.local("images/cursor.png")), 0, 0);
		} catch (Exception e) {
			Gdx.app.debug(tag, "Cannot set cursor pixmap..", e);
		}

		menuBackground = new Pixmap(Gdx.files.local("images/start_screen.jpg"));

		GameSettings.MUSIC_VOLUME = 0;
		GameSettings.SOUND_VOLUME = 1;

//		showGameScreen();
		showStartScreen(menuBackground);
	}

	public void getScreenshot() {
		menuBackground = AbstractScreen.getScreenshot(currentScreen.getLeftGutterWidth(), currentScreen.getBottomGutterWidth(),
				currentScreen.getViewportWidth(), currentScreen.getViewportHeight(), true);
	}

	public void showStartScreen() {
		currentScreen = startScreen;
		if (menuBackground != null) {
			startScreen.setBackgroundImage(menuBackground);
		}
		setScreen(startScreen);
	}

	public void showMenu(AbstractMenuScreen menu) {
		currentScreen = menu;
		if (menuBackground != null) {
			menu.setBackgroundImage(menuBackground);
		}
		setScreen(menu);
	}

	public void showStartScreen(Pixmap bkg) {
		if (startScreen == null) {
			startScreen = new StartScreen(game, null, reqWidth, reqHeight);
		}
		currentScreen = startScreen;
		startScreen.setBackgroundImage(bkg);
		setScreen(startScreen);
	}

	public void showGameScreen() {
		if (gameScreen == null) {
			gameScreen = new GameScreen(game, reqWidth, reqHeight);
		}
		currentScreen = gameScreen;
		setScreen(gameScreen);
	}

	public void showCreditScreen() {
		if (creditScreen == null) {
			creditScreen = new CreditScreen(game, startScreen, reqWidth, reqHeight);
		}
		if (menuBackground != null) {
			creditScreen.setBackgroundImage(menuBackground);
		}
		currentScreen = creditScreen;
		setScreen(creditScreen);
	}

	public void showSettingsScreen() {
		if (settingsScreen == null) {
			settingsScreen = new SettingsScreen(game, startScreen, reqWidth, reqHeight);
		}
		if (menuBackground != null) {
			settingsScreen.setBackgroundImage(menuBackground);
		}
		currentScreen = settingsScreen;
		setScreen(settingsScreen);
	}
}
