package incanshift;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btManifoldPoint;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Player implements Disposable {

	enum PlayerAction {
		STOP("stop"), WALK("walk"), RUN("run"), JUMP("jump"), SHOOT("shoot");

		private String name;

		private PlayerAction(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public class PlayerContactListener extends ContactListener {

		Task climbResetTimer = new Task() {
			@Override
			public void run() {
				climbSurfaceNormal.setZero();
			}
		};
		Vector3 collisionNormal = new Vector3();
		Vector3 horizontalDirection = new Vector3();

		@Override
		public boolean onContactAdded(btManifoldPoint cp, int userValue0,
				int partId0, int index0, int userValue1, int partId1, int index1) {

			cp.getNormalWorldOnB(collisionNormal);
			horizontalDirection.set(direction).scl(1, 0, 1).nor();

			/*
			 * Climbing, check if the surface is approximately vertical and if
			 * player is facing the surface. If so store the normal and handle
			 * it in player update. If player stops facing the vertical surface,
			 * disable climbing using a timeout.
			 */
			if (collisionNormal.isPerpendicular(Vector3.Y,
					climbNormalEpsilonVertical)
					&& collisionNormal.isCollinearOpposite(horizontalDirection,
							climbNormalEpsilonDirection)) {
				climbSurfaceNormal.set(collisionNormal).nor();
				if (!climbResetTimer.isScheduled()) {
					climbResetTimer = Timer.schedule(climbResetTimer,
							canClimbTimeout);
				} else {
					climbResetTimer.cancel();
					climbResetTimer = Timer.schedule(climbResetTimer,
							canClimbTimeout);
				}
			}
			return true;
		}

	}

	private PlayerSound sound;

	public IncanShift game;

	private Viewport viewport;
	public Vector3 screenCenter;

	public GameObject object;

	private CollisionHandler collisionHandler;
	private PlayerContactListener contactListener;
	public PlayerController controller;

	private Vector3 climbSurfaceNormal = new Vector3();
	private float climbNormalEpsilonDirection = 0.1f;
	private float climbNormalEpsilonVertical = 0.5f;
	float canClimbTimeout = 0.1f;

	private Vector3 moveDirectionXZ = new Vector3();
	private Vector3 velocityXZ = new Vector3();
	private Vector3 velocityNew = new Vector3();

	public Vector3 direction = new Vector3();
	public Vector3 velocity = new Vector3();
	public Vector3 position = new Vector3();
	public Vector3 moveDirection = new Vector3();
	public Vector3 up = new Vector3(Vector3.Y);

	private boolean isClimbing = false;
	private boolean isJumping = false;
	private Ray ray = new Ray();

	public boolean newCollision;
	private float cameraOffsetY = GameSettings.PLAYER_EYE_HEIGHT
			- GameSettings.PLAYER_HEIGHT / 2;

	private PlayerAction moveMode = PlayerAction.STOP;

	public Player(IncanShift game, GameObject playerObject,
			Vector3 screenCenter, Viewport viewport,
			CollisionHandler collisionHandler, PlayerSound sound) {
		this.viewport = viewport;
		this.game = game;
		this.collisionHandler = collisionHandler;
		this.object = playerObject;
		this.sound = sound;
		this.screenCenter = screenCenter;

		direction = new Vector3(GameSettings.PLAYER_START_DIR);

		playerObject.transform.getTranslation(position);
		contactListener = new PlayerContactListener();
		contactListener.enable();

		controller = new PlayerController(this);
	}

	@Override
	public void dispose() {
		contactListener.dispose();
	}

	private boolean isOnGround() {
		ray.set(position, up.cpy().scl(-1));
		float distance = GameSettings.PLAYER_HEIGHT / 2 + 0.5f;
		return collisionHandler.rayTest(ray, distance) != null;
	}

	public void update(float delta) {
		boolean isOnGround = isOnGround();

		// Get user input
		controller.update();
		moveDirection.set(controller.getMoveDirection());

		// React to new movement mode, play sounds
		if (!isOnGround || controller.actionQueueContains(PlayerAction.STOP)
				&& moveMode != PlayerAction.STOP) {
			sound.halt();
			moveMode = PlayerAction.STOP;
		}
		if (isOnGround && controller.actionQueueContains(PlayerAction.WALK)
				&& moveMode != PlayerAction.WALK) {
			sound.move(false);
			moveMode = PlayerAction.WALK;
		}
		if (isOnGround && controller.actionQueueContains(PlayerAction.RUN)
				&& moveMode != PlayerAction.RUN) {
			sound.move(true);
			moveMode = PlayerAction.RUN;
		}

		// Handle shooting
		if (controller.actionQueueContains(PlayerAction.SHOOT)) {
			sound.shoot();
			Ray ray = viewport.getCamera().getPickRay(screenCenter.x,
					screenCenter.y);
			GameObject hitObject = collisionHandler.rayTest(ray, 100);
			if (hitObject != null && hitObject.removable && hitObject.visible) {
				hitObject.visible = false;
				sound.shatter();
			}
		}

		// Set move speed
		float moveSpeed = 0;
		if (moveMode == PlayerAction.WALK) {
			moveSpeed = GameSettings.PLAYER_WALK_SPEED;

		} else if (moveMode == PlayerAction.RUN) {
			moveSpeed = GameSettings.PLAYER_RUN_SPEED;

		} else if (moveMode == PlayerAction.STOP) {
			moveSpeed = 0;
		}

		if (moveDirection.y > 0) {
			moveDirection.y = 0;
		}

		velocity.set(object.body.getLinearVelocity());
		velocityXZ.set(velocity.x, 0, velocity.z);
		float currentSpeed = velocityXZ.len();

		// Increase/decrease velocity
		if (isOnGround) {
			if (moveSpeed == 0) {
				velocity.x *= GameSettings.PLAYER_STOP_DOWNSCALE;
				velocity.z *= GameSettings.PLAYER_STOP_DOWNSCALE;
			}
			if (currentSpeed < moveSpeed) {
				velocityNew.set(moveDirection).nor().scl(moveSpeed);

				velocity.set(velocityNew.x, velocity.y, velocityNew.z);
			}
		}

		// Jumping logic
		if (controller.actionQueueContains(PlayerAction.JUMP) && !isClimbing) {

			if (!isJumping && isOnGround) {
				isJumping = true;
				sound.jump();

			} else if (isJumping) {
				object.body.applyCentralForce(new Vector3(Vector3.Y)
						.scl(GameSettings.PLAYER_JUMP_FORCE));
			}
		} else {
			isJumping = false;
		}

		// Climbing logic
		moveDirectionXZ.set(moveDirection.x, 0, moveDirection.z);
		if (!climbSurfaceNormal.isZero() && !isJumping) {

			if (controller.actionQueueContains(PlayerAction.WALK)
					|| controller.actionQueueContains(PlayerAction.RUN)) {

				if (moveDirectionXZ.isCollinearOpposite(climbSurfaceNormal,
						climbNormalEpsilonDirection)) {
					// Climb upwards
					isClimbing = true;

					velocity.set(moveDirectionXZ).nor().scl(1f);
					velocity.y = GameSettings.PLAYER_CLIMB_SPEED;
					object.body.setGravity(Vector3.Zero);

				} else if (moveDirectionXZ.isCollinear(climbSurfaceNormal,
						climbNormalEpsilonDirection)) {
					// Climb downwards
					isClimbing = true;

					velocity.setZero();
					velocity.y = -GameSettings.PLAYER_CLIMB_SPEED;
					object.body.setGravity(Vector3.Zero);
				}
			} else {
				velocity.setZero();
			}

		} else {
			isClimbing = false;
		}

		if (!isClimbing) {
			object.body.setGravity(GameSettings.GRAVITY);
		}

		// Set the transforms
		object.body.setLinearVelocity(velocity);
		object.body.setAngularVelocity(Vector3.Zero);
		object.body.getWorldTransform(object.transform);
		object.transform.getTranslation(position);
		object.calculateTransforms();

		// Update camera
		Camera camera = viewport.getCamera();
		camera.position.set(position);
		camera.position.add(0, cameraOffsetY, 0);
		camera.direction.set(direction);
		camera.up.set(Vector3.Y);
		camera.update();

		controller.actionQueueClear();
	}
}
