package incanshift.gameobjects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Billboard which is not blocked by world objects.
 */
public class BillboardOverlay implements Disposable {

	public final static String tag = "BillboardOverlay";

	public TextureRegion texture;
	public ShaderProgram shader = null;

	public Vector3 worldPos = new Vector3();
	public float worldWidth;
	public float worldHeight;

	int texWidth = 1024;
	int texHeight = 1024;

	public Vector3 screenPos = new Vector3();
	public float screenWidth;
	public float screenHeight;

	float viewDistance = 0;

	/**
	 * Draw a shader on the billboard.
	 * 
	 * @param worldPos
	 * @param worldWidth
	 * @param worldHeight
	 * @param viewDistance
	 * @param vertPath
	 * @param fragPath
	 */
	public BillboardOverlay(Vector3 worldPos, float worldWidth,
			float worldHeight, float viewDistance, String vertPath,
			String fragPath) {
		this.worldPos.set(worldPos);
		this.worldWidth = worldWidth;
		this.worldHeight = worldHeight;
		this.viewDistance = viewDistance;

		texture = new TextureRegion(new Texture(texWidth, texHeight,
				Format.RGBA8888));

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

	@Override
	public void dispose() {
		texture.getTexture().dispose();
		if (shader != null) {
			shader.dispose();
		}
	}

	public float maxWorldRadius() {
		return Math.max(worldWidth / 2, worldHeight / 2);
	}

	public Vector3 setProjection(Viewport viewport) {
		screenPos.set(viewport.project(worldPos.cpy()));

		screenPos.x = screenPos.x - viewport.getRightGutterWidth();
		screenPos.y = screenPos.y - viewport.getBottomGutterHeight();

		float dst = viewport.getCamera().position.dst(worldPos)
				/ viewport.getScreenHeight();

		screenWidth = worldWidth / dst;
		screenHeight = worldHeight / dst;

		return screenPos;
	}
}
