package incanshift;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class MenuItem {

	public String name;
	public String value;
	public boolean selectable;
	
	TextureRegion texNameUnselected;
	TextureRegion texNameSelected;
	TextureRegion texValueUnselected;
	TextureRegion texValueSelected;

	Rectangle bounds = new Rectangle();

	public MenuItem(String name, String value, boolean selectable) {
		this.name = name;
		this.value = value;
		this.selectable = selectable;
	}

	public Rectangle getBounds() {
		return bounds;
	}

	public TextureRegion getNameTex(boolean selected) {
		return (selected) ? texNameSelected : texNameUnselected;
	}

	public void setNameTex(TextureRegion tex, boolean selected) {
		if (selected) {
			this.texNameSelected = tex;
		} else {
			this.texNameUnselected = tex;
		}
	}
	
	public TextureRegion getValueTex(boolean selected) {
		return (selected) ? texValueSelected : texValueUnselected;
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
			return name;
		}
		return name;
	}

}