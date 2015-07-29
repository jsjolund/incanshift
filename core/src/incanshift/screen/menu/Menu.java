package incanshift.screen.menu;

import com.badlogic.gdx.utils.Array;
import incanshift.IncanShift;

import java.util.Iterator;

public class Menu implements Iterable<MenuItem> {

	static float textVerticalSpacing = 5;
	private Array<MenuItem> items = new Array<MenuItem>();

	public void dispose() {
		items.get(0).texKeySelected.getTexture().dispose();
	}

	public MenuItem get(int i) {
		return items.get(i);
	}

	public int size() {
		return items.size;
	}

	public void add(MenuItem item) {
		items.add(item);
	}

	public MenuItem getDown(MenuItem current) {
		int i = items.indexOf(current, true);
		MenuItem next = (i == 0) ? get(size() - 1) : get(i - 1);
		return (next.selectable) ? next : getDown(next);
	}

	public MenuItem getUp(MenuItem current) {
		int i = items.indexOf(current, true);
		MenuItem next = (i == size() - 1) ? get(0) : get(i + 1);
		return (next.selectable) ? next : getUp(next);
	}

	public void resize(int width, int height) {
		float wScl = (float) width / IncanShift.reqWidth;
		float hScl = (float) height / IncanShift.reqHeight;
		int xc = width / 2;
		int yc = height / 2;
		for (int i = 0; i < size(); i++) {
			MenuItem item = items.get(i);
			float kw = item.texKeyUnselected.getRegionWidth() * wScl;
			float kh = item.texKeyUnselected.getRegionHeight() * hScl;
			float yOffset = yc - (textVerticalSpacing + kh) * (size() - 1)
					/ 2;
			float ky = yOffset + (kh + textVerticalSpacing) * i;
			float kx = xc - kw;
			if (item.value == null) {
				kx = xc - kw / 2;
			}
			item.keyBounds.set(kx, ky, kw, kh);

			if (item.value == null) {
				continue;
			}
			float vw = item.texValueUnselected.getRegionWidth() * wScl;
			float vh = item.texValueUnselected.getRegionHeight() * hScl;
			float vx = kx + 20 + kw;
			float vy = ky;
			item.valBounds.set(vx, vy, vw, vh);
		}
	}

	@Override
	public Iterator<incanshift.screen.menu.MenuItem> iterator() {
		// TODO Auto-generated method stub
		return items.iterator();
	}

}
