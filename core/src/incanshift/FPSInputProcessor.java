package incanshift;

import incanshift.MainScreen.GameObject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.viewport.Viewport;

class FPSInputProcessor implements InputProcessor {

	GameObject player;
	btCollisionWorld world;

	Viewport viewport;
	Camera camera;

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
	private int SPACE = Keys.SPACE;
	private float velocity = 5;
	private final Vector3 tmp = new Vector3();

	public FPSInputProcessor(Viewport viewport, GameObject player, btCollisionWorld world) {
		this.world = world;
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

		camera.up.set(Vector3.Y);
		camera.update();

		centerMouseCursor();

		return true;
	}

	public void update() {
		update(Gdx.graphics.getDeltaTime());
	}

	public void update(float dt) {
		if (keys.containsKey(FORWARD)) {
			tmp.set(camera.direction).nor().scl(dt * velocity);
			player.position(tmp);
		}
		if (keys.containsKey(BACKWARD)) {
			tmp.set(camera.direction).nor().scl(-dt * velocity);
			player.position(tmp);
		}
		if (keys.containsKey(STRAFE_LEFT)) {
			tmp.set(camera.direction).crs(camera.up).nor().scl(-dt * velocity);
			player.position(tmp);
		}
		if (keys.containsKey(STRAFE_RIGHT)) {
			tmp.set(camera.direction).crs(camera.up).nor().scl(dt * velocity);
			player.position(tmp);
		}
		if (keys.containsKey(UP)) {
			tmp.set(camera.up).nor().scl(dt * velocity);
			player.position(tmp);
		}
		if (keys.containsKey(DOWN)) {
			tmp.set(camera.up).nor().scl(-dt * velocity);
			player.position(tmp);
		}
		if (keys.containsKey(SPACE) && player.onGround) {
			player.velocity.y = 5;
			player.onGround = false;
		}
		player.transform.getTranslation(camera.position);
		camera.position.add(0, MainScreen.PLAYER_EYE_HEIGHT / 2, 0);

		camera.update(true);
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Input.Keys.ESCAPE) {
			captureMouse = !captureMouse;

			if (captureMouse) {
				Gdx.input.setCursorCatched(true);
				centerMouseCursor();
			} else {
				Gdx.input.setCursorCatched(false);
			}

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
			btCollisionObject hitObject = MainScreen.rayTest(world, ray, 100);
			if (hitObject != null) {
				System.out.println("Hit " + hitObject);
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