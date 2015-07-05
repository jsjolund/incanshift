package incanshift;

import incanshift.MainScreen.GameObject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
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
	private final Vector3 moveDirection = new Vector3();
	private final Vector3 tmp = new Vector3();

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

		// Perform camera mouse look
		float mouseSens = GameSettings.MOUSE_SENSITIVITY;

		int mouseDx = screenX - screenCenterX;
		int mouseDy = screenY - screenCenterY;

		// Rotate camera horizontally and vertically
		camera.rotate(Vector3.Y, -mouseSens * mouseDx);
		camera.rotate(camera.direction.cpy().crs(Vector3.Y), -mouseSens * mouseDy);
		camera.up.set(Vector3.Y);
		camera.update();

		// Rotate player horizontally
		player.transform.rotate(Vector3.Y, -mouseSens * mouseDx);

		centerMouseCursor();
		return true;
	}

	public void update(float dt) {
		camera.update(true);

		// Calculate combined moved direction
		moveDirection.setZero();
		if (keys.containsKey(GameSettings.FORWARD) && player.onGround) {
			moveDirection.add(camera.direction);
		}
		if (keys.containsKey(GameSettings.BACKWARD) && player.onGround) {
			moveDirection.sub(camera.direction);
		}
		if (keys.containsKey(GameSettings.STRAFE_LEFT) && player.onGround) {
			tmp.setZero().sub(camera.direction).crs(camera.up);
			moveDirection.add(tmp);
		}
		if (keys.containsKey(GameSettings.STRAFE_RIGHT) && player.onGround) {
			tmp.setZero().add(camera.direction).crs(camera.up);
			moveDirection.add(tmp);
		}
		if (keys.containsKey(GameSettings.UP)) {
			moveDirection.add(camera.up);
		}
		if (keys.containsKey(GameSettings.DOWN)) {
			moveDirection.sub(camera.up);
		}
		// Prevent jumping/fighting gravity when looking up
		moveDirection.y = 0;

		// Check if we should jump
		if (keys.containsKey(GameSettings.JUMP) && player.onGround) {
			player.velocity.y += GameSettings.PLAYER_JUMP_SPEED;
		}

		// Calculate movement velocity vector
		Vector3 moveVelocity = tmp.set(moveDirection).nor();
		if (keys.containsKey(GameSettings.WALK)) {
			moveVelocity.scl(GameSettings.PLAYER_WALK_SPEED);
		} else {
			moveVelocity.scl(GameSettings.PLAYER_RUN_SPEED);
		}

		// Increase player velocity from movement and gravity
		player.velocity.add(moveVelocity);
		if (!player.onGround) {
			player.velocity.y -= 9.82 * dt;
		}

		// Translate player
		player.trn(player.velocity.cpy().scl(dt));

		// Update camera position
		player.transform.getTranslation(camera.position);
		camera.position.add(0, GameSettings.PLAYER_EYE_HEIGHT / 2, 0);

	}

	@Override
	public boolean keyDown(int keycode) {

		if (keycode == Input.Keys.ESCAPE) {
			// Check if we should capture mouse cursor or not
			captureMouse = !captureMouse;
			if (captureMouse) {
				Gdx.input.setCursorCatched(true);
				centerMouseCursor();
			} else {
				Gdx.input.setCursorCatched(false);
			}

		} else {
			// Player pressed a key, handler movement on update
			keys.put(keycode, keycode);
		}
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// Ignore dragging, interpret it as movement
		mouseMoved(screenX, screenY);
		return true;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {

		if (button == Input.Buttons.LEFT) {
			// Handle player click act
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