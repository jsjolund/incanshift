package incanshift.world;

import incanshift.gameobjects.BillboardOverlay;
import incanshift.gameobjects.EnvTag;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class WorldEnvironment extends Environment implements Disposable {

	// private Environment environment;

	public float normalViewDistance = 1E3f;
	public float viewDistance;

	public Color currentColor;

	public BillboardOverlay sun;
	public Vector3 sunPosition = new Vector3(500, 800, 700);
	public Color skyColor = new Color(0.28f, 0.56f, 0.83f, 1);

	public WorldEnvironment() {
		sun = new BillboardOverlay(sunPosition, 500f, 500f, 0,
				"shader/common.vert", "shader/sun.frag");
		setEnvironment(skyColor, normalViewDistance, sunPosition);
		this.currentColor = skyColor.cpy();

		add(new DirectionalLight().set(Color.WHITE, sunPosition.scl(-1)));
		set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f,
				1.f));
		set(new ColorAttribute(ColorAttribute.Fog, currentColor.r,
				currentColor.g, currentColor.b, currentColor.a));
	}

	void setEnvironment(Color color, float viewDistance, Vector3 sunPosition) {

	}

	private static Color mixColors(Color bg, Color fg) {
		Color r = new Color();
		r.a = 1 - (1 - fg.a) * (1 - bg.a);
		r.r = fg.r * fg.a / r.a + bg.r * bg.a * (1 - fg.a) / r.a;
		r.g = fg.g * fg.a / r.a + bg.g * bg.a * (1 - fg.a) / r.a;
		r.b = fg.b * fg.a / r.a + bg.b * bg.a * (1 - fg.a) / r.a;
		return r;
	}

	/**
	 * Updates the environment, view distance, fog color, sky color based on how
	 * close the player is to tags in the map.
	 * 
	 * Can produce a fog like effect or a bright light effect.
	 * 
	 * @param envTags
	 * @param playerPosition
	 */
	public void update(Array<EnvTag> envTags, Vector3 playerPosition) {
		// Environment effect tags (fog, sunshine)
		viewDistance = normalViewDistance;
		Color colorDelta = new Color(skyColor);
		float dstNearest = Float.MAX_VALUE;
		EnvTag tagNearest = null;
		float fadeNearest = 0;
		for (EnvTag tag : envTags) {
			float fade = 0;
			float dst = tag.position.dst(playerPosition);

			if (dst > tag.fadeDistance) {
				continue;
			}
			if (dst <= tag.effectDistance) {
				fade = 1;
			} else {
				fade = 1 - (dst - tag.effectDistance)
						/ (tag.fadeDistance - tag.effectDistance);
				if (fade > 1) {
					fade = 1;
				}
			}
			if (dst < dstNearest) {
				dstNearest = dst;
				tagNearest = tag;
				fadeNearest = fade;
			}
			tag.color.a = fade;
			colorDelta = mixColors(colorDelta, tag.color);
			tag.color.a = 1;
		}
		if (tagNearest == null) {
			viewDistance = normalViewDistance;
			currentColor.set(skyColor);
			set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f,
					0.4f, 1.f));
		} else {
			currentColor.set(colorDelta);
			viewDistance = tagNearest.minViewDistance + normalViewDistance
					* (1 - fadeNearest);
			float i = (currentColor.r + currentColor.g + currentColor.b) / 3;
			i = (i > 1) ? 1 : i;
			set(new ColorAttribute(ColorAttribute.AmbientLight, i, i, i, 1));
			// overlayColor.set(r, g, b, a/2);
		}

		set(new ColorAttribute(ColorAttribute.Fog, currentColor.r,
				currentColor.g, currentColor.b, currentColor.a));
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		sun.dispose();
	}

}
