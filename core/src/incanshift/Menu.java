package incanshift;

import java.util.Iterator;

import com.badlogic.gdx.utils.Array;

public class Menu implements Iterable<MenuItem> {

	private Array<MenuItem> items = new Array<MenuItem>();

	static float textVerticalSpacing = 5;

	public void dispose() {
		items.get(0).texNameSelected.getTexture().dispose();
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
		for (int i = 0; i < size(); i++) {
			MenuItem item = items.get(i);
			if (item.value == null) {
				int xc = width / 2;
				int yc = height / 2;
				int tw = item.texNameUnselected.getRegionWidth();
				int th = item.texNameUnselected.getRegionHeight();
				float yOffset = yc - (textVerticalSpacing + th) * (size() - 1)
						/ 2;
				float x = xc - tw / 2;
				float y = yOffset + (th + textVerticalSpacing) * i;
				item.bounds.set(x, y, tw, th);
			} else {
				int xc = width / 2;
				int yc = height / 2;
				int tw = item.texNameUnselected.getRegionWidth();
				int th = item.texNameUnselected.getRegionHeight();
				float yOffset = yc - (textVerticalSpacing + th) * (size() - 1)
						/ 2;
				float x = xc - tw;
				float y = yOffset + (th + textVerticalSpacing) * i;
				item.bounds.set(x, y, tw, th);
			}
		}
	}

	@Override
	public Iterator<incanshift.MenuItem> iterator() {
		// TODO Auto-generated method stub
		return items.iterator();
	}

}
