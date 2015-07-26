package incanshift;

import incanshift.screen.AbstractScreen;
import incanshift.screen.CreditScreen;
import incanshift.screen.GameScreen;
import incanshift.screen.SettingsScreen;
import incanshift.screen.StartScreen;
import incanshift.world.GameSettings;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

public class IncanShift extends Game {

	public static final String tag = "IncanShift";

	private IncanShift game;

	private AbstractScreen currentScreen;

	private StartScreen startScreen;
	private GameScreen gameScreen;
	private CreditScreen creditScreen;
	private SettingsScreen settingsScreen;

	Pixmap scrot;

	public static int reqWidth = 1280;
	public static int reqHeight = 720;

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

		GameSettings.MUSIC_VOLUME = 0;
		GameSettings.SOUND_VOLUME = 1;

		startScreen = new StartScreen(game, reqWidth, reqHeight);
		currentScreen = startScreen;
		setScreen(startScreen);

	}

	public void getScreenshot() {
		scrot = AbstractScreen.getScreenshot(currentScreen.getLeftGutterWidth(), currentScreen.getBottomGutterWidth(),
				currentScreen.getViewportWidth(), currentScreen.getViewportHeight(), true);
	}

	public void showStartScreen() {
		currentScreen = startScreen;
		if (scrot != null) {
			startScreen.setBackgroundImage(scrot);
		}
		setScreen(startScreen);
	}

	public void showStartScreen(Pixmap bkg) {
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
			creditScreen = new CreditScreen(game, reqWidth, reqHeight);
		}
		if (scrot != null) {
			creditScreen.setBackgroundImage(scrot);
		}
		currentScreen = creditScreen;
		setScreen(creditScreen);
	}

	public void showSettingsScreen() {
		if (settingsScreen == null) {
			settingsScreen = new SettingsScreen(game, reqWidth, reqHeight);
		}
		if (scrot != null) {
			settingsScreen.setBackgroundImage(scrot);
		}
		currentScreen = settingsScreen;
		setScreen(settingsScreen);
	}
}
