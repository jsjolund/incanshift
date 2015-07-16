package incanshift;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Billboard implements Disposable {

	private final static String tag = "Billboard";

	Vector3 worldPos = new Vector3();
	float worldWidth;
	float worldHeight;

	Vector3 screenPos = new Vector3();
	float screenWidth;
	float screenHeight;

	ShaderProgram shader;

	Texture texture;

	public Billboard(Vector3 worldPos, float worldWidth, float worldHeight,
			String vertPath, String fragPath) {
		this.worldPos.set(worldPos);
		this.worldWidth = worldWidth;
		this.worldHeight = worldHeight;

		texture = new Texture(512, 512, Format.RGBA8888);

		String vert = Gdx.files.local(vertPath).readString();
		String fragSunShader = Gdx.files.local(fragPath).readString();
		shader = new ShaderProgram(vert, fragSunShader);

		ShaderProgram.pedantic = false;

		if (!shader.isCompiled()) {
			Gdx.app.debug(tag, shader.getLog());
			Gdx.app.exit();
		}
		if (shader.getLog().length() != 0) {
			Gdx.app.debug(tag, shader.getLog());
		}

	}

	public float maxWorldRadius() {
		return Math.max(worldWidth / 2, worldHeight / 2);
	}

	public Vector3 setProjection(Viewport viewport) {
		screenPos.set(viewport.project(worldPos.cpy()));

		screenPos.x = screenPos.x - viewport.getRightGutterWidth();
		screenPos.y = screenPos.y - viewport.getBottomGutterHeight();
		
		float dst = viewport.getCamera().position.dst(worldPos)/viewport.getScreenHeight();

		screenWidth = worldWidth / dst;
		screenHeight = worldHeight / dst;

		return screenPos;
	}

	@Override
	public void dispose() {
		texture.dispose();
		shader.dispose();
	}
}
