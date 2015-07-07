package incanshift;

import com.badlogic.gdx.Game;

public class IncanShift extends Game {

	@Override
	public void create() {
		this.setScreen(new GameScreen(this, 1280, 720));
	}

}
