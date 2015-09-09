package incanshift.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import incanshift.IncanShift;

public class CreditScreen extends AbstractScreen {


	class CreditsInputProcessor implements InputProcessor {

		Vector3 tmp = new Vector3();
		boolean released = true;

		@Override
		public boolean keyDown(int keycode) {
			if (keycode == Input.Keys.ESCAPE) {
				game.showStartScreen();
			} else {
				loadNextImage();
			}
			return true;
		}

		@Override
		public boolean keyTyped(char character) {
			return false;
		}

		@Override
		public boolean keyUp(int keycode) {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean mouseMoved(int screenX, int screenY) {

			return true;
		}

		@Override
		public boolean scrolled(int amount) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			if (released) {
				loadNextImage();
				released = false;
			}
			return true;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			// TODO Auto-generated method stub
			released = true;
			return false;
		}
	}


	private void loadNextImage() {
		if (currentImage == paths.length) {
			currentImage = 0;
			game.showStartScreen();
		} else {
			setBackgroundImage(textures.get(currentImage));
		}
		currentImage++;
	}

	private CreditsInputProcessor input = new CreditsInputProcessor();
	TextureRegion bkgTex;

	int currentImage = 0;
	IncanShift game;
	String[] paths = {"images/credits01.png",
			"images/credits02.png",
			"images/credits03.png",
			"images/credits04.png",
			"images/credits05.png",
			"images/credits06.png",
			"images/credits07.png",
			"images/credits08.png",
			"images/credits09.png",	
	};
	Array<Texture> textures = new Array<Texture>();

	public CreditScreen(IncanShift game, int reqWidth, int reqHeight) {
		super(game, reqWidth, reqHeight);
		this.game = game;

		for (String path : paths) {
			Texture pTex = new Texture(Gdx.files.internal(path));
			textures.add(pTex);
		}
		input = new CreditsInputProcessor();
		loadNextImage();
	}

	public void setBackgroundImage(Texture texture) {
		bkgTex = new TextureRegion(texture, texture.getWidth(), texture.getHeight());
	}

	public void setBackgroundImage(Pixmap pixmap) {
		Texture tex = new Texture(pixmap);
		bkgTex = new TextureRegion(tex, pixmap.getWidth(), pixmap.getHeight());
	}

	@Override
	public void show() {
		currentImage = 0;
		Gdx.input.setCursorCatched(false);
		Gdx.input.setInputProcessor(input);
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void hide() {

	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		spriteBatch.setProjectionMatrix(uiMatrix);
		spriteBatch.begin();
		if (bkgTex != null) {
			spriteBatch.draw(bkgTex, 0, 0, getViewportWidth(), getViewportHeight());
		}
		spriteBatch.end();
	}
}
