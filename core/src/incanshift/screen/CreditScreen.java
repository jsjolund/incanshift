package incanshift.screen;

import com.badlogic.gdx.graphics.g2d.BitmapFont;

import incanshift.IncanShift;
import incanshift.screen.menu.Menu;
import incanshift.screen.menu.MenuItem;

public class CreditScreen extends AbstractMenuScreen {

	MenuItem back;
	BitmapFont menuFont;
	
	public CreditScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight, "sound/music_credits.ogg");
		
		menuFont = sansLarge;
		
		back = new MenuItem("Back", null, true);

		Menu menu = new Menu();
		menu.add(back);
		menu.add(new MenuItem("Oscar Lundberg", null, false));
		menu.add(new MenuItem("Anton Bjuhr", null, false));
		menu.add(new MenuItem("Joel Sahlin", null, false));
		menu.add(new MenuItem("Christoffer Lundberg", null, false));
		menu.add(new MenuItem("Johannes Sjölund", null, false));
		menu.add(new MenuItem("Created by:", null, false));

		setMenu(menu, back, menuFont);
	}

	@Override
	public void enterSelected() {
		if (selectedItem == back) {
			game.showStartScreen();
		}
	}

	@Override
	boolean keyDownCapture(int keycode) {
		// TODO Auto-generated method stub
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
