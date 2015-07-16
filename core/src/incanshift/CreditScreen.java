package incanshift;

public class CreditScreen extends AbstractMenuScreen {

	MenuItem back;

	public CreditScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight, "sound/music_credits.ogg");

		back = new MenuItem("Back", null, true);

		Menu menu = new Menu();
		menu.add(back);
		menu.add(new MenuItem("Anton Bjuhr", null, false));
		menu.add(new MenuItem("Joel Sahlin", null, false));
		menu.add(new MenuItem("Christoffer Lundberg", null, false));
		menu.add(new MenuItem("Johannes Sjölund", null, false));
		menu.add(new MenuItem("Created by:", null, false));

		setMenu(menu, back);
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
