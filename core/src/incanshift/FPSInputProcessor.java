package incanshift;

import incanshift.MainScreen.GameObject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.viewport.Viewport;

class FPSInputProcessor implements InputProcessor {

	GameObject player;

	Viewport viewport;
	Camera camera;

	Array<GameObject> instances;
	Ray ray;
	BoundingBox box = new BoundingBox();

	public int screenCenterX;
	public int screenCenterY;

	public boolean captureMouse = true;

	private final IntIntMap keys = new IntIntMap();
	private int STRAFE_LEFT = Keys.A;
	private int STRAFE_RIGHT = Keys.D;
	private int FORWARD = Keys.W;
	private int BACKWARD = Keys.S;
	private int UP = Keys.Q;
	private int DOWN = Keys.E;
	private float velocity = 5;
	private final Vector3 tmp = new Vector3();

	public FPSInputProcessor(Viewport viewport, GameObject player, Array<GameObject> instances) {
		// super(viewport.getCamera());
		this.instances = instances;
		this.viewport = viewport;
		this.player = player;
		centerMouseCursor();
		camera = viewport.getCamera();
	}

	public void centerMouseCursor() {
		Gdx.input.setCursorPosition(screenCenterX, screenCenterY);
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		if (!captureMouse) {
			return true;
		}

		float sens = 0.05f;

		int mouseDx = screenX - screenCenterX;
		int mouseDy = screenY - screenCenterY;

		camera.rotate(Vector3.Y, -sens * mouseDx);

		camera.rotate(camera.direction.cpy().crs(Vector3.Y), -sens * mouseDy);

		player.transform.rotate(Vector3.Y, -sens * mouseDx);
		// player.transform.rotate(camera.direction.cpy().crs(Vector3.Y), -sens
		// * mouseDy);

		camera.up.set(Vector3.Y);
		camera.update();

		centerMouseCursor();

		return true;
	}

	public void update() {
		update(Gdx.graphics.getDeltaTime());
	}

	public void update(float deltaTime) {
		if (keys.containsKey(FORWARD)) {
			tmp.set(camera.direction).nor().scl(deltaTime * velocity);
			player.position(tmp);

			// player.transform.translate(Vector3.Z);
		}
		if (keys.containsKey(BACKWARD)) {
			tmp.set(camera.direction).nor().scl(-deltaTime * velocity);
			player.position(tmp);
		}
		if (keys.containsKey(STRAFE_LEFT)) {
			tmp.set(camera.direction).crs(camera.up).nor().scl(-deltaTime * velocity);
			player.position(tmp);
		}
		if (keys.containsKey(STRAFE_RIGHT)) {
			tmp.set(camera.direction).crs(camera.up).nor().scl(deltaTime * velocity);
			player.position(tmp);
		}
		if (keys.containsKey(UP)) {
			tmp.set(camera.up).nor().scl(deltaTime * velocity);
			player.position(tmp);
		}
		if (keys.containsKey(DOWN)) {
			tmp.set(camera.up).nor().scl(-deltaTime * velocity);
			player.position(tmp);
		}
		player.transform.getTranslation(camera.position);
		camera.position.add(0, 0.25f, 0);

		camera.update(true);
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Input.Keys.ESCAPE) {
			captureMouse = !captureMouse;
			centerMouseCursor();
		} else {
			keys.put(keycode, keycode);
		}
		return true;
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

			for (GameObject instance : instances) {

				instance.calculateBoundingBox(box).mul(instance.transform);

				if (Intersector.intersectRayBoundsFast(ray, box)) {
					System.out.println("Click " + instance.toString());
					break;
				}

			}
		}
		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		// TODO Auto-generated method stub
		keys.remove(keycode, 0);
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}

}