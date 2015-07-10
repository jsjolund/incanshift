package incanshift;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Player implements Disposable {

	enum PlayerAction {
		STOP("stop"), WALK("walk"), RUN("run"), JUMP("jump"), CLIMB("climb"), SHOOT(
				"shoot");

		private String name;

		private PlayerAction(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private PlayerSound sound;

	IncanShift game;

	private Viewport viewport;
	Vector3 screenCenter;

	private GameObject object;

	private CollisionHandler collisionHandler;
	private PlayerContactListener contactListener;
	PlayerController controller;

	Vector3 direction;
	Vector3 position;
	Vector3 up;
	Vector3 moveDirection;
	Vector3 velocity;
	Vector3 xzVelocity;

	// private Array<GameObject> instances;

	public boolean captureMouse = true;
	public boolean canClimb = false;
	public boolean onGround = false;
	public boolean gravity = true;

	public boolean newCollision;
	private float cameraOffsetY = GameSettings.PLAYER_EYE_HEIGHT
			- GameSettings.PLAYER_HEIGHT / 2;

	public Player(IncanShift game, GameObject object, Vector3 screenCenter,
			Viewport viewport, CollisionHandler collisionHandler,
			Array<GameObject> instances, PlayerSound sound) {

		this.viewport = viewport;
		this.game = game;
		this.collisionHandler = collisionHandler;
		this.object = object;
		// this.instances = instances;
		this.sound = sound;
		this.screenCenter = screenCenter;

		velocity = new Vector3();
		up = new Vector3(Vector3.Y);
		position = new Vector3();
		object.transform.getTranslation(position);

		moveDirection = new Vector3();
		direction = new Vector3(Vector3.X);
		xzVelocity = new Vector3();

		contactListener = new PlayerContactListener(this);
		contactListener.enable();

		controller = new PlayerController(this);

	}

	@Override
	public void dispose() {
		contactListener.dispose();
	}

	Vector3 translation = new Vector3();

	PlayerAction moveMode = PlayerAction.STOP;

	public void update(float delta) {
		collisionHandler.performDiscreteCollisionDetection();

		moveDirection.set(controller.getMoveDirection());
		controller.update();

		if (controller.actionQueueContains(PlayerAction.STOP)
				&& moveMode != PlayerAction.STOP) {
			sound.halt();
			moveMode = PlayerAction.STOP;
		}
		if (controller.actionQueueContains(PlayerAction.WALK)
				&& moveMode != PlayerAction.WALK) {
			sound.move(false);
			moveMode = PlayerAction.WALK;
		}
		if (controller.actionQueueContains(PlayerAction.RUN)
				&& moveMode != PlayerAction.RUN) {
			sound.move(true);
			moveMode = PlayerAction.RUN;
		}

		// if (newAction && currentAction == PlayerAction.CLIMB) {
		// sound.climb();
		// }

		if (controller.actionQueueContains(PlayerAction.SHOOT)) {
			sound.shoot();
			Ray ray = viewport.getCamera().getPickRay(screenCenter.x,
					screenCenter.y);
			GameObject hitObject = collisionHandler.rayTest(ray, 100);
			if (hitObject != null && hitObject.removable && hitObject.visible) {
				hitObject.visible = false;
				sound.bump();
			}
		}
		if (controller.actionQueueContains(PlayerAction.JUMP) && onGround) {
			sound.jump();
			position.add(0, 0.1f, 0);
			velocity.y = GameSettings.PLAYER_JUMP_ACCELERATION;

		} else if (controller.actionQueueContains(PlayerAction.JUMP)) {
			velocity.y += GameSettings.PLAYER_JUMP_ACCELERATION;

		} else if (!onGround) {
			moveMode = PlayerAction.STOP;
		}

		// collisionHandler.performDiscreteCollisionDetection();

		float moveSpeed = velocity.len();

		if (moveMode == PlayerAction.WALK) {
			moveSpeed = GameSettings.PLAYER_WALK_SPEED;

		} else if (moveMode == PlayerAction.RUN) {
			moveSpeed = GameSettings.PLAYER_RUN_SPEED;

		} else if (moveMode == PlayerAction.CLIMB) {
			moveSpeed = GameSettings.PLAYER_CLIMB_SPEED;

		} else if (moveMode == PlayerAction.STOP) {
			moveSpeed = 0;
		}

		// Calculate movement velocity vector
		Vector3 moveVelocity = moveDirection.nor().scl(moveSpeed);

		if (onGround) {
			velocity.add(moveVelocity);
		}

		if (gravity) {
			velocity.y -= GameSettings.GRAVITY;
		}

		xzVelocity.set(velocity);
		xzVelocity.y = 0;
		if (moveMode == PlayerAction.STOP && onGround && xzVelocity.len() < 1) {
			velocity.setZero();
		}

		controller.actionQueueClear();

		// Translate player
		position.add(translation.set(velocity).scl(delta));

		// Update camera
		Camera cam = viewport.getCamera();
		cam.position.set(position);
		cam.position.add(0, cameraOffsetY, 0);
		cam.direction.set(direction);
		up.set(Vector3.Y);
		cam.up.set(up.nor());
		cam.update();

		object.position(position);
		// object.transform.rotate(Vector3.Y, direction);

	}
}
