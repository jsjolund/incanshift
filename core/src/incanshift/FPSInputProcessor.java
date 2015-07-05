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
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.viewport.Viewport;

class FPSInputProcessor implements InputProcessor {

	GameObject player;
	CollisionHandler collisionHandler;

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
	private int JUMP = Keys.SPACE;
	private int WALK = Keys.SHIFT_LEFT;

	public FPSInputProcessor(Viewport viewport, GameObject player, CollisionHandler collisionHandler) {
		this.collisionHandler = collisionHandler;
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

	private final Vector3 moveDirection = new Vector3();
	private final Vector3 tmp = new Vector3();

	private float walkMoveSpeed = 3f;
	private float runMoveSpeed = 6f;

	public void update(float dt) {
		moveDirection.setZero();

		if (keys.containsKey(FORWARD) && player.onGround) {
			moveDirection.add(camera.direction);
		}
		if (keys.containsKey(BACKWARD) && player.onGround) {
			moveDirection.sub(camera.direction);
		}
		if (keys.containsKey(STRAFE_LEFT) && player.onGround) {
			tmp.setZero().sub(camera.direction).crs(camera.up);
			moveDirection.add(tmp);
		}
		if (keys.containsKey(STRAFE_RIGHT) && player.onGround) {
			tmp.setZero().add(camera.direction).crs(camera.up);
			moveDirection.add(tmp);
		}
		if (keys.containsKey(UP)) {
			moveDirection.add(camera.up);
		}
		if (keys.containsKey(DOWN)) {
			moveDirection.sub(camera.up);
		}
		moveDirection.y = 0;
		if (keys.containsKey(JUMP) && player.onGround) {
			player.velocity.y += 6;
		}

		Vector3 moveVelocity = tmp.set(moveDirection).nor();
		float moveSpeed = moveVelocity.dst(Vector3.Zero);

		if (moveSpeed > 0) {
			if (keys.containsKey(WALK)) {
				moveVelocity.scl(walkMoveSpeed / moveSpeed);
			} else {
				moveVelocity.scl(runMoveSpeed / moveSpeed);
			}
		}

		player.velocity.add(moveVelocity);
		if (!player.onGround) {
			player.velocity.y -= 9.82 * dt;
		}

		player.trn(player.velocity.cpy().scl(dt));

		// Update camera position
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
			GameObject hitObject = collisionHandler.rayTest(ray, 100);
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