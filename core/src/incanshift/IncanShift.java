package incanshift;

import incanshift.screen.CreditScreen;
import incanshift.screen.GameScreen;
import incanshift.screen.SettingsScreen;
import incanshift.screen.StartScreen;
import incanshift.world.GameSettings;

import com.badlogic.gdx.Game;

public class IncanShift extends Game {

	int reqWidth = 1280;
	int reqHeight = 720;

	private IncanShift game;

	private StartScreen startScreen;
	private GameScreen gameScreen;
	private CreditScreen creditScreen;
	private SettingsScreen settingsScreen;
	

	@Override
	public void create() {
		game = this;

		GameSettings.MUSIC_VOLUME = 1;
		GameSettings.SOUND_VOLUME = 1;
		
		startScreen = new StartScreen(game, reqWidth, reqHeight);
		setScreen(startScreen);
	}

	public void showStartScreen() {
		setScreen(startScreen);
	}

	public void showGameScreen() {
		if (gameScreen == null) {
			gameScreen = new GameScreen(game, reqWidth, reqHeight);
		}
		setScreen(gameScreen);
	}
	

	public void restartGameScreen() {
		gameScreen = new GameScreen(game, reqWidth, reqHeight);
		setScreen(gameScreen);
	}

	public void showCreditScreen() {
		if (creditScreen == null) {
			creditScreen = new CreditScreen(game, reqWidth, reqHeight);
		}
		setScreen(creditScreen);
	}
	
	public void showSettingsScreen() {
		if (settingsScreen == null) {
			settingsScreen = new SettingsScreen(game, reqWidth, reqHeight);
		}
		setScreen(settingsScreen);
	}
}
