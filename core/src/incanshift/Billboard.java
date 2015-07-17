package incanshift;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
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

	ShaderProgram shader = null;

	TextureRegion texture;

	int texWidth = 1024;
	int texHeight = 1024;

	float viewDistance = 0;

	public Billboard(Vector3 worldPos, float worldWidth, float worldHeight,
			float viewDistance, String vertPath, String fragPath) {
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

	public Billboard(Vector3 worldPos, float worldWidth, float worldHeight,
			float viewDistance, String msg, Color textColor, Color bkgColor,
			BitmapFont font) {

		this.worldPos.set(worldPos);
		this.worldWidth = worldWidth;
		this.worldHeight = worldHeight;
		this.viewDistance = viewDistance;

		SpriteBatch spriteBatch = new SpriteBatch();
		FrameBuffer fbo = null;

		try {
			fbo = new FrameBuffer(Format.RGBA8888, texWidth, texHeight, false);
		} catch (Exception e) {
			System.out.println("Failed to create framebuffer.");
			e.printStackTrace();
		}

		fbo.begin();
		Gdx.graphics.getGL20().glClearColor(bkgColor.r, bkgColor.g, bkgColor.b,
				bkgColor.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, texWidth,
				texHeight);
		spriteBatch.begin();
		font.setColor(textColor);
		font.draw(spriteBatch, msg, 10, texHeight);
		texture = new TextureRegion(fbo.getColorBufferTexture(), 0, 0,
				texWidth, texHeight);
		texture.flip(false, true);
		spriteBatch.end();
		fbo.end();
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

	@Override
	public void dispose() {
		texture.getTexture().dispose();
		shader.dispose();
	}
}
