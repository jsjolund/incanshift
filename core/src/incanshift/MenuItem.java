package incanshift;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class MenuItem {

	public String key;
	public String value;
	public boolean selectable;

	TextureRegion texKeyUnselected;
	TextureRegion texKeySelected;
	TextureRegion texValueUnselected;
	TextureRegion texValueSelected;

	Rectangle bounds = new Rectangle();

	public MenuItem(String name, String value, boolean selectable) {
		this.key = name;
		this.value = value;
		this.selectable = selectable;
	}

	public Rectangle getBounds() {
		return bounds;
	}

	public TextureRegion getKeyTex(boolean selected) {
		return (selected) ? texKeySelected : texKeyUnselected;
	}

	public TextureRegion getValueTex(boolean selected) {
		return (selected) ? texValueSelected : texValueUnselected;
	}

	public void setKeyTex(TextureRegion tex, boolean selected) {
		if (selected) {
			this.texKeySelected = tex;
		} else {
			this.texKeyUnselected = tex;
		}
	}

	public void setValueTex(TextureRegion tex, boolean selected) {
		if (selected) {
			this.texValueSelected = tex;
		} else {
			this.texValueUnselected = tex;
		}
	}

	@Override
	public String toString() {
		if (value != null) {
			return key;
		}
		return key;
	}

}