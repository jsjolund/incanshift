package incanshift;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

class FPSInputProcessor extends FirstPersonCameraController
		implements
			InputProcessor {

	Viewport viewport;
	Array<ModelInstance> instances;
	Ray ray;
	BoundingBox box = new BoundingBox();

	public int screenCenterX;
	public int screenCenterY;

	public boolean captureMouse = true;

	public FPSInputProcessor(Viewport viewport, Array<ModelInstance> instances) {
		super(viewport.getCamera());
		this.instances = instances;
		this.viewport = viewport;
		centerMouseCursor();
	}

	public void centerMouseCursor() {
		Gdx.input.setCursorPosition(screenCenterX, screenCenterY);
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		if (!captureMouse) {
			return true;
		}

		Camera cam = viewport.getCamera();

		float sens = 0.05f;

		int mouseDx = screenX - screenCenterX;
		int mouseDy = screenY - screenCenterY;

		cam.rotate(Vector3.Y, -sens * mouseDx);

		cam.rotate(cam.direction.cpy().crs(Vector3.Y), -sens * mouseDy);

		cam.up.set(Vector3.Y);
		cam.update();

		centerMouseCursor();

		return true;
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Input.Keys.ESCAPE) {
			captureMouse = !captureMouse;
			centerMouseCursor();
		}
		return super.keyDown(keycode);
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		mouseMoved(screenX, screenY);
		return true;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {

		if (button == Input.Buttons.LEFT) {

			ray = viewport.getPickRay(screenX, screenY);

			for (ModelInstance instance : instances) {

				instance.calculateBoundingBox(box).mul(instance.transform);

				if (Intersector.intersectRayBoundsFast(ray, box)) {
					System.out.println("Click " + instance.toString());
					break;
				}

			}
		}
		return true;
	}

}