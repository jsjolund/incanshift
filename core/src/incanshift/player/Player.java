package incanshift.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.Viewport;
import incanshift.IncanShift;
import incanshift.gameobjects.GameObject;
import incanshift.world.CollisionHandler;
import incanshift.world.GameSettings;
import incanshift.world.GameWorld;

public class Player extends GameObject {

	final static String tag = "Player";

//	public class PlayerContactListener extends ContactListener {
//		Vector3 equipPos = new Vector3();
//		Vector3 normal = new Vector3();
//
//		@Override
//		public boolean onContactAdded(btManifoldPoint cp,
//									  btCollisionObject colObj0, int partId0, int index0,
//									  btCollisionObject colObj1, int partId1, int index1) {
//			cp.getNormalWorldOnB(normal);
//			normal.scl(-cp.getDistance1());
//			return true;
//		}
//	}
//
//	private PlayerContactListener contactListener;

	public PlayerController controller;
	public IncanShift game;
	public Vector3 screenCenter;
	public Vector3 direction = new Vector3();
	public Vector3 velocity = new Vector3();
	public Vector3 position = new Vector3();
	public Vector3 moveDirection = new Vector3();
	public Vector3 up = new Vector3(Vector3.Y);
	public Vector3 positionCarried = new Vector3();
	GameWorld world;
	Vector3 teleportPosition = new Vector3();
	Array<GameObject> hookTrail;
	Vector3 tmp = new Vector3();
	PlayerAction soundMoveMode = PlayerAction.STOP;
	FovPosition gunNormal = new FovPosition(0.075f, 0.75f, 0.15f);
	FovPosition hookNormal = new FovPosition(0.01f, 0.1f, 0.4f);
	FovPosition blowpipeNormal = new FovPosition(0.075f, 0.1f, 0.3f);
	FovPosition blowpipeMove = new FovPosition(0.075f, 0.13f, 0.3f);
	FovPosition blowpipeFall = new FovPosition(0.075f, 0.06f, 0.3f);
	FovPosition blowpipeShoot = new FovPosition(-0.001f, 0.15f, 0.25f);
	FovPosition equipGoalPos = new FovPosition();
	FovPosition equipPos = new FovPosition();
	FovPosition equipMoveVel = new FovPosition();
	float walkAnimTimer = 0;
	float shootAnimTimer = 10;
	private PlayerSound sound;
	private Viewport viewport;
	private Vector3 velocityXZ = new Vector3();
	private Vector3 velocityNew = new Vector3();
	private ArrayMap<String, GameObject> inventory = new ArrayMap<String, GameObject>();
	private boolean isJumping = false;
	private boolean isFlying = false;
	private Ray ray = new Ray();
	private GameObject carried;
	private float cameraOffsetY = GameSettings.PLAYER_EYE_HEIGHT
			- GameSettings.PLAYER_HEIGHT / 2;
	private PlayerAction moveMode = PlayerAction.STOP;
	private GameObject currentEquip;
	private boolean isGrappling = false;
	private boolean gunHidden = false;
	private boolean grapplingBlocked = false;
	Task teleportTask = new Task() {
		@Override
		public void run() {
			position(teleportPosition);
			isGrappling = true;
			grapplingBlocked = false;
		}
	};
	Task unblockGrapplingTask = new Task() {
		@Override
		public void run() {
			grapplingBlocked = false;
		}
	};
	// Gun positioning
	private Matrix4 equipTransform = new Matrix4();
	private Vector3 equipFrontBackPosition = new Vector3();
	private Vector3 equipLeftRightPosition = new Vector3();
	private Vector3 equipUpDownPosition = new Vector3();

	public Player(Model model, btRigidBodyConstructionInfo constructionInfo,
				  IncanShift game, Vector3 screenCenter, Viewport viewport,
				  GameWorld world, PlayerSound sound) {

		super(model, constructionInfo);

		this.viewport = viewport;
		this.game = game;
		this.world = world;
		this.sound = sound;
		this.screenCenter = screenCenter;

		direction = new Vector3(GameSettings.PLAYER_START_DIR);

		transform.getTranslation(position);
		// contactListener = new PlayerContactListener();
		// contactListener.enable();

		controller = new PlayerController(this);
		hookTrail = new Array<GameObject>();
		hookTrail.ordered = true;
	}

	public void reset() {
		inventory.clear();
		body.setGravity(GameSettings.GRAVITY);
		isJumping = false;
		isGrappling = false;
	}

	public void addToInventory(GameObject item) {
		inventory.put(item.id, item);
	}


	public void equipFromInventory(String item) {
		if (!inventory.containsKey(item)) {
			return;
		}
		GameObject obj = inventory.get(item);
		currentEquip = obj;
		if (currentEquip.id.equals("blowpipe")) {
			equipGoalPos.set(blowpipeNormal);
		}
	}

	private void handleGrappling() {
		if (controller.actionQueueContains(PlayerAction.HOOK)
				// && inventory.containsKey("hook")
				&& !teleportTask.isScheduled() && !grapplingBlocked) {

			ray.set(viewport.getCamera().position,
					viewport.getCamera().direction);
			float distance = 1000;

			btRigidBody hitObject = world.rayTest(ray, teleportPosition,
					(short) (CollisionHandler.GROUND_FLAG
							| CollisionHandler.OBJECT_FLAG),
					distance);

			if (hitObject != null) {
				sound.grapple();
				if (!teleportTask.isScheduled()) {
					Timer.schedule(teleportTask,
							GameSettings.PLAYER_GRAPPLE_TELEPORT_TIME);
				}
			} else {
				grapplingBlocked = true;
				Timer.schedule(unblockGrapplingTask,
						GameSettings.PLAYER_GRAPPLE_MISS_TIME);
			}
		}
	}

	private void handleJumping(boolean isOnGround) {
		if (controller.actionQueueContains(PlayerAction.JUMP)) {

			if (!isJumping && isOnGround) {
				isJumping = true;
				sound.jump();

			} else if (isJumping) {
				body.applyCentralForce(
						new Vector3(up).scl(GameSettings.PLAYER_JUMP_FORCE));
			}
		} else {
			isJumping = false;
		}
	}

	private void handleMoving(boolean isOnGround) {

		if (controller.actionQueueContains(PlayerAction.STOP)) {
			moveMode = PlayerAction.STOP;
		}
		if (controller.actionQueueContains(PlayerAction.WALK)) {
			if (moveMode == PlayerAction.RUN) {
				body.setLinearVelocity(Vector3.Zero);
			}
			moveMode = PlayerAction.WALK;
		}
		if (controller.actionQueueContains(PlayerAction.RUN)) {
			if (moveMode == PlayerAction.WALK) {
				body.setLinearVelocity(Vector3.Zero);
			}
			moveMode = PlayerAction.RUN;
		}

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

		velocity.set(body.getLinearVelocity());
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
					velocity.set(velocityNew);
				}
			}
		}
	}

	private void handleShooting() {
		if (controller.actionQueueContains(PlayerAction.FIRE) && !gunHidden) {
			Gdx.app.debug(tag, "Shooting");
			sound.shoot();

			ray.set(viewport.getCamera().position,
					viewport.getCamera().direction);
			float distance = 100;

			btRigidBody hitObject = world.rayTest(ray, tmp,
					(short) (CollisionHandler.GROUND_FLAG
							| CollisionHandler.OBJECT_FLAG),
					distance);
			Gdx.app.debug(tag, "Hit object: " + hitObject);

			if (hitObject != null) {
				GameObject obj = world.getGameObject(hitObject);
				Gdx.app.debug(tag, "World object: " + obj);

				if (obj.id.equals("mask")) {
					world.destroy(obj);
					sound.maskHit();
				} else {
					sound.wallHit();
				}
			}
			Gdx.app.debug(tag, "Finished shooting");
		}
	}

	private void handleUsing() {
		if (controller.actionQueueContains(PlayerAction.USE)
				&& carried == null) {

			Ray ray = viewport.getCamera().getPickRay(screenCenter.x,
					screenCenter.y);
			btRigidBody hitObject = world.rayTest(ray, tmp,
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

			float forceGrab = positionCarried
					.dst(carried.body.getCenterOfMassPosition()) * 1000;
			Vector3 forceGrabVector = positionCarried.cpy()
					.sub(carried.body.getCenterOfMassPosition()).nor()
					.scl(forceGrab);
			carried.body.setLinearVelocity(
					carried.body.getLinearVelocity().scl(0.5f));
			carried.body.setAngularVelocity(
					carried.body.getAngularVelocity().scl(0.5f));
			carried.body.applyCentralForce(forceGrabVector);
		}
	}

	private boolean isOnGround() {
		ray.set(position, up.cpy().scl(-1));
		float distance = GameSettings.PLAYER_HEIGHT / 2 + 0.5f;
		return world
				.rayTest(ray, tmp,
						(short) (CollisionHandler.GROUND_FLAG
								| CollisionHandler.OBJECT_FLAG),
						distance) != null;
	}

	public void setMoveSound(boolean isOnGround) {

		if (controller.actionQueueContains(PlayerAction.STOP)
				&& soundMoveMode != PlayerAction.STOP) {
			sound.halt();
			soundMoveMode = PlayerAction.STOP;
			return;
		}
		if (!isOnGround) {
			sound.halt();
			soundMoveMode = PlayerAction.STOP;
			return;
		}
		if (controller.actionQueueContains(PlayerAction.WALK)
				&& soundMoveMode != PlayerAction.WALK) {
			sound.halt();
			sound.move(false);
			soundMoveMode = PlayerAction.WALK;
		}
		if (controller.actionQueueContains(PlayerAction.RUN)
				&& soundMoveMode != PlayerAction.RUN) {
			sound.halt();
			sound.move(true);
			soundMoveMode = PlayerAction.RUN;
		}
	}

	public void unequip() {
		if (currentEquip == null) {
			return;
		}
		currentEquip.position(position);
	}

	public void update(float delta) {

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
				body.setGravity(Vector3.Zero);
			} else {
				isFlying = false;
				body.setGravity(GameSettings.GRAVITY);
			}
		} else if (isGrappling) {
			body.setGravity(Vector3.Zero);
			body.setLinearVelocity(Vector3.Zero);
			if (!moveDirection.isZero()) {
				isGrappling = false;
				body.setGravity(GameSettings.GRAVITY);
			}
		}

		// React to input
		setMoveSound(isOnGround);
		handleGrappling();
		handleUsing();
		handleMoving(true);
		handleJumping(isOnGround);

		// Set the transforms
		body.setLinearVelocity(velocity);
		body.setAngularVelocity(Vector3.Zero);
		body.getWorldTransform(transform);
		transform.getTranslation(position);
		calculateTransforms();

		// Update camera
		Camera camera = viewport.getCamera();
		camera.position.set(position);
		camera.position.add(0, cameraOffsetY, 0);
		camera.direction.set(direction);
		camera.up.set(Vector3.Y);
		camera.update();

		updateEquip(delta, isOnGround);

		handleShooting();

		controller.actionQueueClear();
	}

	/**
	 * Update weapon position/rotation relative to camera
	 *
	 * @param delta
	 */
	private void updateEquip(float delta, boolean isOnGround) {

		for (Entry<String, GameObject> entry : inventory) {
			GameObject obj = entry.value;
			obj.position(position);
		}

		if (currentEquip == null) {
			return;
		}
		if (gunHidden) {
			equipTransform.set(viewport.getCamera().view);
		} else {
			equipTransform.set(viewport.getCamera().view).inv();
		}

		walkAnimTimer += delta;

		if (shootAnimTimer < 10) {
			// Prevent timer overflow
			shootAnimTimer += delta;
		}
		float speed = 0;

		if (controller.actionQueueContains(PlayerAction.FIRE) || shootAnimTimer < 0.5f) {
			speed = 25;
			equipGoalPos.set(blowpipeShoot);
			if (controller.actionQueueContains(PlayerAction.FIRE)) {
				shootAnimTimer = 0;
			}

		} else if (soundMoveMode == PlayerAction.WALK) {
			speed = 5;
			if (walkAnimTimer < 0.2f) {
				// After how long to move down
				equipGoalPos.set(blowpipeMove);
			} else if (walkAnimTimer < 0.6f) {
				// After how long to move up
				equipGoalPos.set(blowpipeNormal);
				speed = 3;
			} else if (walkAnimTimer < 11f) {
				// How long to hold in up pos
				walkAnimTimer = 0;
			}

		} else if (soundMoveMode == PlayerAction.RUN) {
			speed = 10;
			if (walkAnimTimer < 0.1f) {
				// After how long to move down
				equipGoalPos.set(blowpipeMove);
			} else if (walkAnimTimer < 0.3f) {
				// After how long to move up
				equipGoalPos.set(blowpipeNormal);
				speed = 6;
			} else if (walkAnimTimer < 0.8f) {
				// How long to hold in up pos
				walkAnimTimer = 0;
			}

		} else if (!isOnGround) {
			speed = 4;
			equipGoalPos.set(blowpipeFall);
			walkAnimTimer = 0;

		} else {
			speed = 3;
			walkAnimTimer = 0;
			equipGoalPos.set(blowpipeNormal);
		}

		if (!equipPos.epsilonEquals(equipGoalPos, 0.01f)) {
			float dst = equipPos.dst(equipGoalPos);
			equipMoveVel.set(equipGoalPos).sub(equipPos).nor().scl(speed).scl(dst);
			equipPos.add(equipMoveVel.scl(delta));
		}

		currentEquip.body.setWorldTransform(equipTransform);

		equipLeftRightPosition.set(direction).crs(Vector3.Y).nor()
				.scl(equipPos.leftRight());
		equipFrontBackPosition.set(direction).nor().scl(equipPos.frontBack());
		equipUpDownPosition.set(direction).nor().crs(equipLeftRightPosition)
				.nor().scl(equipPos.upDown());

		currentEquip.body.translate(equipLeftRightPosition);
		currentEquip.body.translate(equipFrontBackPosition);
		currentEquip.body.translate(equipUpDownPosition);
		currentEquip.body.setLinearVelocity(body.getLinearVelocity());
		currentEquip.body.getWorldTransform(currentEquip.transform);
	}

	enum PlayerAction {
		STOP("stop"), WALK("walk"), RUN("run"),

		JUMP("jump"), FIRE("shoot"), USE("use"),

		FLY("fly"), HOOK("hook");

		private String name;

		private PlayerAction(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private class FovPosition extends Vector3 {

		public FovPosition(float leftRight, float upDown, float frontBack) {
			super(leftRight, upDown, frontBack);
		}

		public FovPosition() {
			super();
		}

		public float leftRight() {
			return this.x;
		}

		public float upDown() {
			return this.y;
		}

		public float frontBack() {
			return this.z;
		}

	}
}
