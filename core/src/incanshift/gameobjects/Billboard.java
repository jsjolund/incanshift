package incanshift.gameobjects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/**
 * A billboard is a graphical component consisting of a plane which is always
 * facing the camera. The aspect ratio is always maintained, but the size scales
 * with viewing distance. Textures and shaders can be used on the plane, and
 * text can be drawn on the surface.
 */
public class Billboard implements Disposable {

	public final static String tag = "Billboard";
	public ModelInstance modelInstance;
	public ShaderProgram shader = null;
	BlendingAttribute blendAttrib;
	Vector3 worldPos = new Vector3();
	float worldWidth;
	float worldHeight;
	int texWidth = 1024;
	int texHeight = 1024;
	float viewDistance = 0;
	private TextureRegion texture;

	/**
	 * Draw text on the billboard.
	 *
	 * @param worldPos
	 * @param worldWidth
	 * @param worldHeight
	 * @param viewDistance
	 * @param msg
	 * @param textColor
	 * @param bkgColor
	 * @param font
	 */
	public Billboard(Vector3 worldPos, float worldWidth, float worldHeight,
					 float viewDistance, String msg, Color textColor, Color bkgColor,
					 BitmapFont font) {

		this.worldPos.set(worldPos);
		this.worldWidth = worldWidth;
		this.worldHeight = worldHeight;
		this.viewDistance = viewDistance;

		SpriteBatch spriteBatch = new SpriteBatch();
		FrameBuffer fbo = null;

		// Draw string on texture
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

		Material material = new Material();

		material.set(new TextureAttribute(TextureAttribute.Diffuse, texture));
		blendAttrib = new BlendingAttribute(1);
		material.set(blendAttrib);
		// material.set( new FloatAttribute(FloatAttribute.AlphaTest, 0.5f));
		Model plane = createPlaneModel(worldWidth, worldHeight, material, 0, 0,
				1, 1);

		modelInstance = new ModelInstance(plane);
		modelInstance.transform.setTranslation(worldPos);
		modelInstance.calculateTransforms();
	}

	private static Model createPlaneModel(final float width,
										  final float height, final Material material, final float u1,
										  final float v1, final float u2, final float v2) {

		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();
		MeshPartBuilder bPartBuilder = modelBuilder.part("rect",
				GL20.GL_TRIANGLES, Usage.Position | Usage.Normal
						| Usage.TextureCoordinates, material);
		// NOTE ON TEXTURE REGION, MAY FILL OTHER REGIONS, USE GET region.getU()
		// and so on
		bPartBuilder.setUVRange(u1, v1, u2, v2);
		bPartBuilder.rect(-(width * 0.5f), -(height * 0.5f), 0, (width * 0.5f),
				-(height * 0.5f), 0, (width * 0.5f), (height * 0.5f), 0,
				-(width * 0.5f), (height * 0.5f), 0, 0, 0, -1);

		return (modelBuilder.end());
	}

	@Override
	public void dispose() {
		texture.getTexture().dispose();
		if (shader != null) {
			shader.dispose();
		}
	}

	public void update(Camera camera) {
		try {
			modelInstance.transform.set(camera.view).inv();
		} catch (RuntimeException e) {
			return;
		}
		modelInstance.transform.setTranslation(worldPos);
		modelInstance.calculateTransforms();

		float dst = worldPos.dst(camera.position);
		float distanceFade = (viewDistance == 0) ? 1 : 1 - dst / viewDistance;

		blendAttrib.opacity = distanceFade;
		modelInstance.materials.get(0).set(blendAttrib);
	}
}
