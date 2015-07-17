package incanshift;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btManifoldPoint;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Player implements Disposable {

	enum PlayerAction {
		STOP("stop"), WALK("walk"), RUN("run"),

		JUMP("jump"), FIRE("shoot"), USE("use"),

		RESET("reset");

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

	private Vector3 velocityXZ = new Vector3();
	private Vector3 velocityNew = new Vector3();

	public Vector3 direction = new Vector3();
	public Vector3 velocity = new Vector3();
	public Vector3 position = new Vector3();
	public Vector3 moveDirection = new Vector3();
	public Vector3 up = new Vector3(Vector3.Y);

	public Vector3 positionCarried = new Vector3();

	private boolean isClimbing = false;
	private boolean isJumping = false;
	private Ray ray = new Ray();

	private GameObject carried;

	public boolean newCollision;
	private float cameraOffsetY = GameSettings.PLAYER_EYE_HEIGHT
			- GameSettings.PLAYER_HEIGHT / 2;

	private PlayerAction moveMode = PlayerAction.STOP;

	GameObject currentGun;
	private boolean gunHidden = false;

	private float gunYoffset = 0;
	private boolean gunYIncrease = true;

	// Gun positioning
	private Matrix4 gunBaseTransform = new Matrix4();
	private Vector3 gunFrontBackPosition = new Vector3();
	private Vector3 gunLeftRightPosition = new Vector3();
	private Vector3 gunUpDownPosition = new Vector3();

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

	public GameObject getGun(GameObject gun) {
		return gun;
	}

	public void handleClimbing() {
		// // Climbing logic
		// moveDirectionXZ.set(moveDirection.x, 0, moveDirection.z);
		// if (!climbSurfaceNormal.isZero() && !isJumping) {
		//
		// if (controller.actionQueueContains(PlayerAction.WALK)
		// || controller.actionQueueContains(PlayerAction.RUN)) {
		//
		// if (moveDirectionXZ.isCollinearOpposite(climbSurfaceNormal,
		// climbNormalEpsilonDirection)) {
		// // Climb upwards
		// isClimbing = true;
		//
		// System.out.println("climb up");
		// velocity.set(moveDirectionXZ).nor()
		// .scl(GameSettings.PLAYER_CLIMB_SPEED);
		// velocity.y = GameSettings.PLAYER_CLIMB_SPEED;
		// // object.body.setGravity(Vector3.Zero);
		//
		// } else if (moveDirectionXZ.isCollinear(climbSurfaceNormal,
		// climbNormalEpsilonDirection)) {
		// System.out.println("climb down");
		// // Climb downwards
		// isClimbing = true;
		//
		// velocity.setZero();
		// velocity.y = -GameSettings.PLAYER_CLIMB_SPEED;
		// // object.body.setGravity(Vector3.Zero);
		// }
		// } else {
		// velocity.set(direction.cpy().nor().scl(1));
		// }
		//
		// } else if (isClimbing) {
		// System.out.println("not climbing");
		// isClimbing = false;
		// // if (!isOnGround) {
		// //
		// // Vector3 stopClimbImpulse = direction.cpy().nor().scl(2);
		// // stopClimbImpulse.y = 4;
		// // object.body.applyCentralImpulse(stopClimbImpulse);
		// // }
		// object.body.setGravity(GameSettings.GRAVITY);
		// }
	}

	private void handleJumping(boolean isOnGround) {
		if (controller.actionQueueContains(PlayerAction.JUMP) && !isClimbing) {

			if (!isJumping && isOnGround) {
				isJumping = true;
				sound.jump();

			} else if (isJumping) {
				object.body.applyCentralForce(new Vector3(up)
						.scl(GameSettings.PLAYER_JUMP_FORCE));
			}
		} else {
			isJumping = false;
		}
	}

	private void handleMoving(boolean isOnGround) {
		float moveSpeed = 0;
		if (moveMode == PlayerAction.WALK) {
			moveSpeed = isOnGround ? GameSettings.PLAYER_WALK_SPEED
					: GameSettings.PLAYER_WALK_SPEED * 0.5f;

		} else if (moveMode == PlayerAction.RUN) {
			moveSpeed = isOnGround ? GameSettings.PLAYER_RUN_SPEED
					: GameSettings.PLAYER_RUN_SPEED * 0.5f;

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
	}

	private void handleShooting() {
		if (controller.actionQueueContains(PlayerAction.FIRE) && !gunHidden) {
			sound.shoot();
			Ray ray = viewport.getCamera().getPickRay(screenCenter.x,
					screenCenter.y);
			btRigidBody hitObject = collisionHandler
					.rayTest(
							ray,
							(short) (CollisionHandler.GROUND_FLAG | CollisionHandler.OBJECT_FLAG),
							100);
			if (hitObject != null) {
				GameObject obj = collisionHandler.getGameObject(hitObject);
				if (obj.removable) {
					collisionHandler.removeGameObject(obj);
					sound.shatter();
				}
			}
		}
	}

	private void handleUsing() {
		if (controller.actionQueueContains(PlayerAction.USE) && carried == null) {

			Ray ray = viewport.getCamera().getPickRay(screenCenter.x,
					screenCenter.y);
			btRigidBody hitObject = collisionHandler.rayTest(ray,
					CollisionHandler.OBJECT_FLAG, 5);

			if (hitObject != null) {
				GameObject obj = collisionHandler.getGameObject(hitObject);

				if (obj.movable) {
					gunHidden = true;
					carried = obj;
					carried.body.setGravity(Vector3.Zero);
				}
			}
		} else if (controller.actionQueueContains(PlayerAction.USE)
				&& carried != null) {
			carried.body.setGravity(GameSettings.GRAVITY);
			gunHidden = false;
			carried = null;

		} else if (controller.actionQueueContains(PlayerAction.FIRE)
				&& carried != null) {
			carried.body.setGravity(GameSettings.GRAVITY);
			Vector3 forcePushVector = direction.cpy().nor().scl(20);
			carried.body.applyCentralImpulse(forcePushVector);
			carried = null;
			gunHidden = false;

		} else if (carried != null) {
			positionCarried.set(direction).nor().scl(2f).add(position);
			positionCarried.y += 1.5f;

			float forceGrab = positionCarried.dst(carried.body
					.getCenterOfMassPosition()) * 1000;
			Vector3 forceGrabVector = positionCarried.cpy()
					.sub(carried.body.getCenterOfMassPosition()).nor()
					.scl(forceGrab);
			carried.body.setLinearVelocity(carried.body.getLinearVelocity()
					.scl(0.5f));
			carried.body.setAngularVelocity(carried.body.getAngularVelocity()
					.scl(0.5f));
			carried.body.applyCentralForce(forceGrabVector);
		}
	}

	private boolean isOnGround() {
		ray.set(position, up.cpy().scl(-1));
		float distance = GameSettings.PLAYER_HEIGHT / 2 + 0.5f;
		return collisionHandler
				.rayTest(
						ray,
						(short) (CollisionHandler.GROUND_FLAG | CollisionHandler.OBJECT_FLAG),
						distance) != null;
	}

	public void setGun(GameObject gun) {
		if (gun != null) {
			gun.position(position);
		}
		this.currentGun = gun;
	}

	public void setMoveMode(boolean isOnGround) {
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
	}

	public void update(float delta) {

		if (controller.actionQueueContains(PlayerAction.RESET)) {
			game.restartGameScreen();
		}

		boolean isOnGround = isOnGround();

		// Get user input
		controller.update();
		moveDirection.set(controller.getMoveDirection());

		// React to input
		setMoveMode(isOnGround);
		handleShooting();
		handleUsing();
		handleMoving(isOnGround);
		handleJumping(isOnGround);
		handleClimbing();

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

		updateGun(delta);

		controller.actionQueueClear();
	}

	private void updateGun(float delta) {
		GameObject gun = currentGun;
		if (gun == null) {
			return;
		}
		if (gunHidden) {
			gunBaseTransform.set(viewport.getCamera().view);
		} else {
			gunBaseTransform.set(viewport.getCamera().view).inv();
		}
		// Update gun position/rotation relative to camera
		gun.body.setWorldTransform(gunBaseTransform);

		// // For gun
		// gunLeftRightPosition.set(direction).crs(Vector3.Y).nor().scl(0.075f);
		// gunFrontBackPosition.set(direction).nor().scl(0.15f);
		// gunUpDownPosition.set(direction).nor().crs(gunLeftRightPosition).scl(0.75f);

		// For blowgun
		gunLeftRightPosition.set(direction).crs(Vector3.Y).nor().scl(0.075f);
		gunFrontBackPosition.set(direction).nor().scl(0.25f);
		gunUpDownPosition.set(direction).nor().crs(gunLeftRightPosition)
				.scl(0.80f);

		// Move the gun around if walking or running
		if (moveMode == PlayerAction.STOP) {
			gunYoffset = 0;

		} else {
			float gunMoveSpeed = (moveMode == PlayerAction.RUN) ? GameSettings.PLAYER_RUN_SPEED
					: GameSettings.PLAYER_WALK_SPEED;
			gunMoveSpeed *= 0.0015;
			double gunMoveLimitY = 0.005;

			if (gunYoffset > 0) {
				gunYIncrease = false;

			} else if (gunYoffset < -gunMoveLimitY) {
				gunYIncrease = true;
			}
			if (gunYIncrease) {
				gunYoffset += delta * gunMoveSpeed;
			} else {
				gunYoffset -= delta * gunMoveSpeed * 3;
			}
		}
		gunUpDownPosition.y += gunYoffset;

		gun.body.translate(gunLeftRightPosition);
		gun.body.translate(gunFrontBackPosition);
		gun.body.translate(gunUpDownPosition);
		gun.body.setLinearVelocity(object.body.getLinearVelocity());
		gun.body.getWorldTransform(gun.transform);
	}
}
