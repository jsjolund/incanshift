package incanshift.gameobjects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

public class EnvTag {

	public float fadeDistance;
	public float effectDistance;
	public float minViewDistance;

	public Color color;
	public Vector3 position;

	public EnvTag(Vector3 position, float tagFadeDistance,
			float tagEffectDistance, Color color, float tagMinViewDistance) {
		this.position = position;
		this.fadeDistance = tagFadeDistance;
		this.effectDistance = tagEffectDistance;
		this.minViewDistance = tagMinViewDistance;
		this.color = color;
	}

}
