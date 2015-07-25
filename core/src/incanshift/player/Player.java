package incanshift.player;

import incanshift.IncanShift;
import incanshift.gameobjects.GameObject;
import incanshift.world.CollisionHandler;
import incanshift.world.GameSettings;
import incanshift.world.GameWorld;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btManifoldPoint;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Player implements Disposable {

	enum PlayerAction {
		STOP("stop"), WALK("walk"), RUN("run"),

		JUMP("jump"), FIRE("shoot"), USE("use"),

		RESET("reset"), FLY("fly"), HOOK("hook");

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

		Vector3 pos = new Vector3();
		Vector3 normal = new Vector3();

		@Override
		public boolean onContactAdded(btManifoldPoint cp,
				btCollisionObject colObj0, int partId0, int index0,
				btCollisionObject colObj1, int partId1, int index1) {

			if (hook != null && colObj0.equals(hook.body)) {

				hook.transform.getTranslation(pos);
				cp.getNormalWorldOnB(normal);
				normal.scl(-cp.getDistance1());
				pos.add(normal);
				playerObject.position(pos);
				isGrappling = true;

				if (resetHook.isScheduled()) {
					resetHook.cancel();
					resetHook.run();
				}

			}
			return true;
		}

		// @Override
		// public boolean onContactAdded(btManifoldPoint cp, int userValue0,
		// int partId0, int index0, int userValue1, int partId1, int index1) {
		//
		// cp.getNormalWorldOnB(collisionNormal);
		// horizontalDirection.set(direction).scl(1, 0, 1).nor();
		//
		// /*
		// * Climbing, check if the surface is approximately vertical and if
		// * player is facing the surface. If so store the normal and handle
		// * it in player update. If player stops facing the vertical surface,
		// * disable climbing using a timeout.
		// */
		// if (collisionNormal.isPerpendicular(Vector3.Y,
		// climbNormalEpsilonVertical)
		// && collisionNormal.isCollinearOpposite(horizontalDirection,
		// climbNormalEpsilonDirection)) {
		// climbSurfaceNormal.set(collisionNormal).nor();
		// if (!climbResetTimer.isScheduled()) {
		// climbResetTimer = Timer.schedule(climbResetTimer,
		// canClimbTimeout);
		// } else {
		// climbResetTimer.cancel();
		// climbResetTimer = Timer.schedule(climbResetTimer,
		// canClimbTimeout);
		// }
		// }
		// return true;
		// }

	}

	final static String tag = "Player";
	private PlayerSound sound;

	public IncanShift game;

	private Viewport viewport;
	public Vector3 screenCenter;

	public GameObject playerObject;

	private PlayerContactListener contactListener;
	public PlayerController controller;

	private Vector3 velocityXZ = new Vector3();
	private Vector3 velocityNew = new Vector3();

	public Vector3 direction = new Vector3();
	public Vector3 velocity = new Vector3();
	public Vector3 position = new Vector3();
	public Vector3 moveDirection = new Vector3();
	public Vector3 up = new Vector3(Vector3.Y);

	public Vector3 positionCarried = new Vector3();

	private ArrayMap<String, GameObject> inventory = new ArrayMap<String, GameObject>();

	public boolean isClimbing = false;
	private boolean isJumping = false;
	private boolean isFlying = false;

	private Ray ray = new Ray();

	private GameObject carried;

	public boolean newCollision;
	private float cameraOffsetY = GameSettings.PLAYER_EYE_HEIGHT
			- GameSettings.PLAYER_HEIGHT / 2;

	private PlayerAction moveMode = PlayerAction.STOP;

	private GameObject currentEquip;
	private GameObject hook;
	private boolean isGrappling = false;

	private boolean gunHidden = false;

	private float yOffset = 0;
	private boolean yIncrease = true;

	// Gun positioning
	private Matrix4 equipTransform = new Matrix4();
	private Vector3 equipFrontBackPosition = new Vector3();
	private Vector3 equipLeftRightPosition = new Vector3();
	private Vector3 equipUpDownPosition = new Vector3();

	GameWorld world;

	Task resetHook = new Task() {
		@Override
		public void run() {
			if (hook != null) {
				inventory.put("hook", hook);
				hook = null;
				updateEquip(0);
			}
		}
	};

	Array<GameObject> hookTrail;

	public Player(IncanShift game, GameObject playerObject,
			Vector3 screenCenter, Viewport viewport, GameWorld world,
			PlayerSound sound) {
		this.viewport = viewport;
		this.game = game;
		this.world = world;
		this.playerObject = playerObject;
		this.sound = sound;
		this.screenCenter = screenCenter;

		direction = new Vector3(GameSettings.PLAYER_START_DIR);

		playerObject.transform.getTranslation(position);
		contactListener = new PlayerContactListener();
		contactListener.enable();

		controller = new PlayerController(this);
		hookTrail = new Array<GameObject>();
		hookTrail.ordered = true;
	}

	public void addToInventory(GameObject item) {
		inventory.put(item.id, item);
	}

	public void clearInventory() {
		if (resetHook.isScheduled()) {
			resetHook.run();
		}
		inventory.clear();
	}

	@Override
	public void dispose() {
		contactListener.dispose();
	}

	public void equipFromInventory(String item) {
		if (!inventory.containsKey(item)) {
			return;
		}
		GameObject obj = inventory.get(item);
		currentEquip = obj;
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

	private void handleGrappling() {
		if (controller.actionQueueContains(PlayerAction.HOOK)
				&& inventory.containsKey("hook")) {
			GameObject weapon = currentEquip;
			hook = inventory.get("hook");
			equipFromInventory("hook");
			updateEquip(0);
			hook.body.setAngularVelocity(Vector3.Zero);
			Vector3 hookVelocity = direction.cpy().add(0, 0.1f, 0).nor()
					.scl(40);
			hook.body.setLinearVelocity(hookVelocity);
			inventory.removeKey("hook");
			equipFromInventory(weapon.id);
			updateEquip(0);

			Timer.schedule(resetHook, 1);

		} else if (hook != null) {

			GameObject trail = world.spawn("hook_trail", position,
					new Vector3(), false, false, false, false,
					CollisionHandler.OBJECT_FLAG, CollisionHandler.GROUND_FLAG);
			trail.body.setWorldTransform(hook.body.getWorldTransform());
			trail.body.setGravity(Vector3.Zero);
			hookTrail.add(trail);

		} else if (hookTrail.size != 0) {
			for (GameObject obj : hookTrail) {
				world.destroy(obj);
			}
			hookTrail.clear();
		}
	}

	private void handleJumping(boolean isOnGround) {
		if (controller.actionQueueContains(PlayerAction.JUMP) && !isClimbing) {

			if (!isJumping && isOnGround) {
				isJumping = true;
				sound.jump();

			} else if (isJumping) {
				playerObject.body.applyCentralForce(new Vector3(up)
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

		if (moveDirection.y > 0 && !isFlying) {
			moveDirection.y = 0;
		}

		velocity.set(playerObject.body.getLinearVelocity());
		velocityXZ.set(velocity.x, 0, velocity.z);
		float currentSpeed = velocityXZ.len();

		// Increase/decrease velocity
		if (isOnGround) {
			if (moveSpeed == 0) {
				velocity.x *= GameSettings.PLAYER_STOP_DOWNSCALE;
				velocity.z *= GameSettings.PLAYER_STOP_DOWNSCALE;
				if (isFlying) {
					velocity.y *= GameSettings.PLAYER_STOP_DOWNSCALE;
				}
			}
			if (currentSpeed < moveSpeed) {
				velocityNew.set(moveDirection).nor().scl(moveSpeed);

				velocity.set(velocityNew.x, velocity.y, velocityNew.z);
				if (isFlying) {
					velocity.set(velocityNew.x, velocityNew.y, velocityNew.z);
				}
			}
		}
	}

	private void handleShooting() {
		if (controller.actionQueueContains(PlayerAction.FIRE) && !gunHidden) {

			sound.shoot();

			ray.set(viewport.getCamera().position,
					viewport.getCamera().direction);
			float distance = 100;

			btRigidBody hitObject = world
					.rayTest(
							ray,
							(short) (CollisionHandler.GROUND_FLAG | CollisionHandler.OBJECT_FLAG),
							distance);

			if (hitObject != null) {
				GameObject obj = world.getGameObject(hitObject);
				if (obj.removable) {
					world.destroy(obj);
					sound.shatter();
				}
			}
		}
	}

	private void handleUsing() {
		if (controller.actionQueueContains(PlayerAction.USE) && carried == null) {

			Ray ray = viewport.getCamera().getPickRay(screenCenter.x,
					screenCenter.y);
			btRigidBody hitObject = world.rayTest(ray,
					CollisionHandler.OBJECT_FLAG, 5);

			if (hitObject != null) {
				GameObject obj = world.getGameObject(hitObject);

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
		return world
				.rayTest(
						ray,
						(short) (CollisionHandler.GROUND_FLAG | CollisionHandler.OBJECT_FLAG),
						distance) != null;
	}

	public void setMoveMode(boolean isOnGround) {

		if (controller.actionQueueContains(PlayerAction.STOP)
				&& moveMode != PlayerAction.STOP) {
			sound.halt();
			moveMode = PlayerAction.STOP;
		}
		if (controller.actionQueueContains(PlayerAction.WALK)
				&& moveMode != PlayerAction.WALK) {
			if (isOnGround) {
				sound.move(false);
			}
			moveMode = PlayerAction.WALK;
		}
		if (controller.actionQueueContains(PlayerAction.RUN)
				&& moveMode != PlayerAction.RUN) {
			if (isOnGround) {
				sound.move(true);
			}
			moveMode = PlayerAction.RUN;
		}
	}

	public void unequip() {
		if (currentEquip == null) {
			return;
		}
		currentEquip.position(position);
	}

	public void update(float delta) {

		if (controller.actionQueueContains(PlayerAction.RESET)) {
			game.restartGameScreen();
		}

		boolean isOnGround = isOnGround();

		if (isFlying) {
			isOnGround = true;
		}

		// Get user input
		controller.update();
		moveDirection.set(controller.getMoveDirection());

		if (controller.actionQueueContains(PlayerAction.FLY)) {
			// Toggle gravity each time fly button is pressed
			if (!isFlying) {
				isFlying = true;
				playerObject.body.setGravity(Vector3.Zero);
			} else {
				isFlying = false;
				playerObject.body.setGravity(GameSettings.GRAVITY);
			}
		} else if (isGrappling) {
			playerObject.body.setGravity(Vector3.Zero);
			playerObject.body.setLinearVelocity(Vector3.Zero);
			if (!moveDirection.isZero()) {
				isGrappling = false;
				playerObject.body.setGravity(GameSettings.GRAVITY);
			}
		}

		// if (controller.actionQueueContains(PlayerAction.JUMP) || !isOnGround)
		// {
		// object.body.setGravity(GameSettings.GRAVITY.cpy().scl(1));
		// } else {
		// object.body.setGravity(GameSettings.GRAVITY.cpy().scl(4));
		// }

		// React to input
		setMoveMode(isOnGround);
		handleShooting();
		handleGrappling();
		handleUsing();
		handleMoving(true);
		handleJumping(isOnGround);
		handleClimbing();

		// Set the transforms
		playerObject.body.setLinearVelocity(velocity);
		playerObject.body.setAngularVelocity(Vector3.Zero);
		playerObject.body.getWorldTransform(playerObject.transform);
		playerObject.transform.getTranslation(position);
		playerObject.calculateTransforms();

		// Update camera
		Camera camera = viewport.getCamera();
		camera.position.set(position);
		camera.position.add(0, cameraOffsetY, 0);
		camera.direction.set(direction);
		camera.up.set(Vector3.Y);
		camera.update();

		updateEquip(delta);

		controller.actionQueueClear();
	}

	private void updateEquip(float delta) {

		for (Entry<String, GameObject> entry : inventory) {
			GameObject obj = entry.value;
			obj.position(Vector3.Zero);
		}

		if (currentEquip == null) {
			return;
		}
		if (gunHidden) {
			equipTransform.set(viewport.getCamera().view);
		} else {
			equipTransform.set(viewport.getCamera().view).inv();
		}
		// Update gun position/rotation relative to camera
		if (currentEquip.id == "blowpipe") {
			currentEquip.body.setWorldTransform(equipTransform);
			equipLeftRightPosition.set(direction).crs(Vector3.Y).nor()
					.scl(0.075f);
			equipFrontBackPosition.set(direction).nor().scl(0.3f);
			equipUpDownPosition.set(direction).nor()
					.crs(equipLeftRightPosition).nor().scl(0.080f);

		} else if (currentEquip.id == "hook") {
			// gunBaseTransform.rotate(Vector3.X, 90);
			currentEquip.body.setWorldTransform(equipTransform);
			equipLeftRightPosition.set(direction).crs(Vector3.Y).nor()
					.scl(0.01f);
			equipFrontBackPosition.set(direction).nor().scl(0.4f);
			equipUpDownPosition.set(direction).nor()
					.crs(equipLeftRightPosition).nor().scl(0.1f);
		} else if (currentEquip.id == "gun") {
			// gun.body.setWorldTransform(gunBaseTransform);
			// gunLeftRightPosition.set(direction).crs(Vector3.Y).nor().scl(0.075f);
			// gunFrontBackPosition.set(direction).nor().scl(0.15f);
			// gunUpDownPosition.set(direction).nor().crs(gunLeftRightPosition)
			// .scl(0.75f);
		}

		// Move the gun around if walking or running
		if (moveMode == PlayerAction.STOP) {
			yOffset = 0;

		} else {
			float moveSpeed = (moveMode == PlayerAction.RUN) ? GameSettings.PLAYER_RUN_SPEED
					: GameSettings.PLAYER_WALK_SPEED;
			moveSpeed *= 0.0015;
			double moveLimitY = 0.005;

			if (yOffset > 0) {
				yIncrease = false;

			} else if (yOffset < -moveLimitY) {
				yIncrease = true;
			}
			if (yIncrease) {
				yOffset += delta * moveSpeed;
			} else {
				yOffset -= delta * moveSpeed * 3;
			}
		}
		equipUpDownPosition.y += yOffset;

		currentEquip.body.translate(equipLeftRightPosition);
		currentEquip.body.translate(equipFrontBackPosition);
		currentEquip.body.translate(equipUpDownPosition);
		currentEquip.body.setLinearVelocity(playerObject.body
				.getLinearVelocity());
		currentEquip.body.getWorldTransform(currentEquip.transform);
	}
}
