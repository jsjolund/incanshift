package incanshift;

import incanshift.MainScreen.GameObject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btManifoldPoint;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.Viewport;

class FPSInputProcessor implements InputProcessor, Disposable {

	class MyContactListener extends ContactListener {

		Vector3 xzVelocity = new Vector3();

		@Override
		public boolean onContactAdded(btManifoldPoint cp, int userValue0,
				int partId0, int index0, int userValue1, int partId1, int index1) {

			// Translate player back along normal
			Vector3 normal = new Vector3(0, 0, 0);
			cp.getNormalWorldOnB(normal);
			player.trn(normal.cpy().scl(-cp.getDistance1()));

			player.onGround = true;

			player.velocity.y = 0;

			// Decrease player velocity. If walking stop immediately,
			// if running decrease until stopped.
			if (keys.containsKey(GameSettings.RUN)) {
				player.velocity.add(normal.scl(0.1f)).scl(0.9f);

				xzVelocity.set(player.velocity);
				xzVelocity.y = 0;
				if (xzVelocity.dst(Vector3.Zero) < 1f) {
					player.velocity.setZero();
				}
			} else {
				player.velocity.scl(0.01f);
			}

			return true;
		}

		@Override
		public void onContactEnded(btCollisionObject colObj0,
				btCollisionObject colObj1) {
			player.onGround = false;
		}

	}

	GameObject player;
	CollisionHandler collisionHandler;
	MyContactListener contactListener;
	Array<GameObject> instances;
	AssetManager assets;

	boolean jumpKeyReleased = true;
	volatile boolean keepJumping = true;

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

	boolean moving = false;

	Sound soundJump;
	Sound soundBump;
	Sound soundShoot;
	Sound soundRun;
	Sound soundWalk;
	Sound soundWind;
	Sound soundMove;

	long soundMoveId;
	long soundWindId;

	public FPSInputProcessor(Viewport viewport, GameObject player,
			CollisionHandler collisionHandler, Array<GameObject> instances,
			AssetManager assets) {
		this.collisionHandler = collisionHandler;
		this.viewport = viewport;
		this.player = player;
		this.instances = instances;
		this.assets = assets;

		centerMouseCursor();
		camera = viewport.getCamera();
		contactListener = new MyContactListener();
		contactListener.enable();

		assets.finishLoading();
		soundJump = assets.get("sound/jump.wav", Sound.class);
		soundBump = assets.get("sound/bump.wav", Sound.class);
		soundShoot = assets.get("sound/shoot.wav", Sound.class);
		soundRun = assets.get("sound/run.wav", Sound.class);
		soundWalk = assets.get("sound/walk.wav", Sound.class);
		soundWind = assets.get("sound/wind.wav", Sound.class);
		soundMove = soundWalk;

		soundWindId = soundWind.play(0.1f);
		soundWind.setLooping(soundWindId, true);

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
		camera.rotate(camera.direction.cpy().crs(Vector3.Y), -mouseSens
				* mouseDy);
		camera.up.set(Vector3.Y);

		camera.update();

		// Rotate player horizontally
		player.transform.rotate(Vector3.Y, -mouseSens * mouseDx);

		centerMouseCursor();
		return true;
	}

	public void update(float dt) {

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
		if (moveDirection.y > 0) {
			moveDirection.y = 0;
		}

		// Check if we should jump
		if ((keys.containsKey(GameSettings.JUMP) && player.onGround && jumpKeyReleased)) {

			jumpKeyReleased = false;
			player.velocity.y = GameSettings.PLAYER_JUMP_ACCELERATION * dt;
			keepJumping = true;
			moveDirection.y = 0;

			soundJump.play(1.0f);

			player.trn(Vector3.Y.cpy().scl(0.1f));

			Timer.schedule(new Task() {
				@Override
				public void run() {
					keepJumping = false;
				}
			}, GameSettings.PLAYER_JUMP_TIME);

		} else if (keys.containsKey(GameSettings.JUMP) && keepJumping) {
			player.velocity.y += GameSettings.PLAYER_JUMP_ACCELERATION * dt;
			moveDirection.y = 0;
		}

		// Calculate movement velocity vector
		float moveSpeed = keys.containsKey(GameSettings.RUN) ? GameSettings.PLAYER_RUN_SPEED
				: GameSettings.PLAYER_WALK_SPEED;
		Vector3 moveVelocity = moveDirection.nor().scl(moveSpeed);

		// Increase player velocity from movement unless already at max movement
		// speed
		tmp.set(player.velocity);
		tmp.y = 0;
		float currentSpeed = tmp.dst(Vector3.Zero);

		boolean prevMoving = moving;
		if (currentSpeed == 0 || !player.onGround) {
			moving = false;
			if (prevMoving) {
				soundRun.stop(soundMoveId);
			}
		} else {
			moving = true;
			if (!prevMoving) {

				soundMove.stop(soundMoveId);
				if (keys.containsKey(GameSettings.RUN)) {
					soundMoveId = soundRun.play(4.0f);
				} else {
					soundMoveId = soundWalk.play(4.0f);
				}
				soundMove.setLooping(soundMoveId, true);
			}
		}

		if (currentSpeed < moveSpeed) {
			player.velocity.add(moveVelocity.scl(moveSpeed));
		}

		player.velocity.y -= GameSettings.GRAVITY * dt;

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
		if (keycode == GameSettings.RUN) {
			if (player.onGround) {
				soundMove.stop(soundMoveId);
				soundMoveId = soundRun.play(4.0f);
				soundMove.setLooping(soundMoveId, true);
			}
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
			soundShoot.play(0.5f);

			if (hitObject != null && hitObject.removable && hitObject.visible) {
				hitObject.visible = false;
				soundBump.play(1.0f);
			}

		}
		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		keys.remove(keycode, 0);
		if (keycode == GameSettings.JUMP) {
			jumpKeyReleased = true;
		}
		if (keycode == GameSettings.RESET) {
			player.position(GameSettings.PLAYER_START_POS);
			for (GameObject obj : instances) {
				obj.visible = true;
			}
		}
		if (keycode == GameSettings.RUN) {
			if (player.onGround) {
				soundMove.stop(soundMoveId);
				soundMoveId = soundWalk.play(4.0f);
				soundMove.setLooping(soundMoveId, true);
			}
		}
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

	@Override
	public void dispose() {
		contactListener.dispose();
	}

}