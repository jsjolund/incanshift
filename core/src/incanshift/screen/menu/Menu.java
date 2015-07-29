package incanshift.screen.menu;

import com.badlogic.gdx.utils.Array;

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
		int xc = width / 2;
		int yc = height / 2;
		for (int i = 0; i < size(); i++) {
			MenuItem item = items.get(i);
			int tw = item.texKeyUnselected.getRegionWidth();
			int th = item.texKeyUnselected.getRegionHeight();
			float yOffset = yc - (textVerticalSpacing + th) * (size() - 1)
					/ 2;
			float y = yOffset + (th + textVerticalSpacing) * i;
			float x = xc - tw;
			if (item.value == null) {
				x = xc - tw / 2;
			}
			item.bounds.set(x, y, tw, th);
		}
	}

	@Override
	public Iterator<incanshift.screen.menu.MenuItem> iterator() {
		// TODO Auto-generated method stub
		return items.iterator();
	}

}
