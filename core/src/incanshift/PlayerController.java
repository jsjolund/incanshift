package incanshift;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
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

	boolean canClimb = false;

	class MyContactListener extends ContactListener {

		Vector3 xzVelocity = new Vector3();

		long groundCollisions = 0;
		long climbCollisions = 0;
		long collisions = 0;

		@Override
		public boolean onContactAdded(btManifoldPoint cp, int userValue0,
				int partId0, int index0, int userValue1, int partId1, int index1) {

			// Translate player back along normal
			Vector3 normal = new Vector3(0, 0, 0);
			cp.getNormalWorldOnB(normal);

			collisions++;
			if (normal.epsilonEquals(Vector3.Y, 0.25f)) {
				groundCollisions++;
			} else {
				climbCollisions++;
			}
			if (collisions > 10) {
				if (climbCollisions / collisions > 0.5) {
					canClimb = true;
				} else {
					canClimb = false;
				}
				groundCollisions = 0;
				climbCollisions = 0;
				collisions = 0;
			}

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

	boolean jumpKeyReleased = true;
	volatile boolean keepJumping = true;

	Viewport viewport;

	Ray ray;
	BoundingBox box = new BoundingBox();

	public int screenCenterX;
	public int screenCenterY;

	public boolean captureMouse = true;

	private final IntIntMap keys = new IntIntMap();
	private final Vector3 moveDirection = new Vector3();
	private final Vector3 tmp = new Vector3();

	boolean moving = false;

	PlayerSound sound;
	IncanShift game;

	public FPSInputProcessor(IncanShift game, Viewport viewport,
			GameObject player, CollisionHandler collisionHandler,
			Array<GameObject> instances, PlayerSound sound) {
		this.game = game;
		this.collisionHandler = collisionHandler;
		this.viewport = viewport;
		this.player = player;
		this.instances = instances;
		this.sound = sound;

		centerMouseCursor();

		contactListener = new MyContactListener();
		contactListener.enable();

	}

	public void centerMouseCursor() {
		Gdx.input.setCursorPosition(screenCenterX, screenCenterY);
	}

	@Override
	public void dispose() {
		contactListener.dispose();
	}

	@Override
	public boolean keyDown(int keycode) {

		if (keycode == Input.Keys.ESCAPE) {

			sound.halt();
			game.showStartScreen();

		} else {
			// Player pressed a key, handler movement on update
			keys.put(keycode, keycode);
		}
		if (keycode == GameSettings.RUN) {
			if (player.onGround && player.velocity.len() > 0) {
				sound.move(true);
			}
		}

		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
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
			if (player.onGround && player.velocity.len() > 0) {
				sound.move(false);
			}
		}
		return false;
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
		viewport.getCamera().rotate(Vector3.Y, -mouseSens * mouseDx);
		viewport.getCamera().rotate(
				viewport.getCamera().direction.cpy().crs(Vector3.Y),
				-mouseSens * mouseDy);
		viewport.getCamera().up.set(Vector3.Y);

		viewport.getCamera().update();

		// Rotate player horizontally
		player.transform.rotate(Vector3.Y, -mouseSens * mouseDx);

		centerMouseCursor();
		return true;
	}

	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {

		if (button == Input.Buttons.LEFT) {

			// Handle player click act
			ray = viewport.getPickRay(screenX, screenY);
			GameObject hitObject = collisionHandler.rayTest(ray, 100);
			sound.shoot();

			if (hitObject != null && hitObject.removable && hitObject.visible) {
				hitObject.visible = false;
				sound.bump();
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
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	public void update(float dt) {

		// Calculate combined moved direction
		moveDirection.setZero();
		float moveSpeed = keys.containsKey(GameSettings.RUN) ? GameSettings.PLAYER_RUN_SPEED
				: GameSettings.PLAYER_WALK_SPEED;

		if (keys.containsKey(GameSettings.FORWARD) && player.onGround) {
			moveDirection.add(viewport.getCamera().direction);
		}
		if (keys.containsKey(GameSettings.BACKWARD) && player.onGround) {
			moveDirection.sub(viewport.getCamera().direction);
		}
		if (keys.containsKey(GameSettings.STRAFE_LEFT) && player.onGround) {
			tmp.setZero().sub(viewport.getCamera().direction)
					.crs(viewport.getCamera().up);
			moveDirection.add(tmp);
		}
		if (keys.containsKey(GameSettings.STRAFE_RIGHT) && player.onGround) {
			tmp.setZero().add(viewport.getCamera().direction)
					.crs(viewport.getCamera().up);
			moveDirection.add(tmp);
		}
		if (keys.containsKey(GameSettings.UP)) {
			moveDirection.add(viewport.getCamera().up);
		}
		if (keys.containsKey(GameSettings.DOWN)) {
			moveDirection.sub(viewport.getCamera().up);
		}
		// Prevent jumping/fighting gravity when looking up
		if (moveDirection.y > 0) {
			moveDirection.y = 0;
		}

		// Check if we should jump or climb
		if (keys.containsKey(GameSettings.JUMP) && player.onGround && canClimb) {
			moveDirection.y = 1f;
			moveSpeed = GameSettings.PLAYER_CLIMB_SPEED;
			jumpKeyReleased = false;
			keepJumping = false;
			sound.climb();

		} else if ((keys.containsKey(GameSettings.JUMP) && player.onGround && jumpKeyReleased)) {
			sound.halt();
			sound.jump();

			jumpKeyReleased = false;
			player.velocity.y = GameSettings.PLAYER_JUMP_ACCELERATION * dt;
			keepJumping = true;
			moveDirection.y = 0;

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
				sound.halt();
			}
		} else {
			moving = true;
			if (!prevMoving) {

				if (keys.containsKey(GameSettings.RUN)) {
					sound.move(true);
				} else {
					sound.move(false);
				}

			}
		}

		if (currentSpeed < moveSpeed) {
			player.velocity.add(moveVelocity.scl(moveSpeed));
		}

		player.velocity.y -= GameSettings.GRAVITY * dt;

		// Translate player
		player.trn(player.velocity.cpy().scl(dt));

		// Update camera position
		player.transform.getTranslation(viewport.getCamera().position);
		viewport.getCamera().position.add(0, GameSettings.PLAYER_EYE_HEIGHT
				- GameSettings.PLAYER_HEIGHT / 2, 0);

	}
}