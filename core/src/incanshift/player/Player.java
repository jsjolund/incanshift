package incanshift.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.Viewport;
import incanshift.IncanShift;
import incanshift.gameobjects.GameObject;
import incanshift.world.CollisionHandler;
import incanshift.world.GameSettings;
import incanshift.world.GameWorld;

public class Player extends GameObject {

	public class PlayerContactListener extends ContactListener {

	}
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

	final static String tag = "Player";

	public IncanShift game;
	private Viewport viewport;
	public GameWorld world;

	// Listeners, controllers, handlers
	private PlayerContactListener contactListener;
	public PlayerController controller;
	private FovObjectHandler fovObjhandler = new FovObjectHandler();
	private PlayerSound sound;

	// For determining type of action performed
	private PlayerAction moveMode = PlayerAction.STOP;
	private boolean isJumping = false;
	private boolean isFlying = false;
	private boolean grapplingBlocked = false;
	private boolean isGrappling = false;

	// Used when calculating movement and camera positioning
	public Vector3 screenCenter;
	public Vector3 direction = new Vector3();
	public Vector3 velocity = new Vector3();
	public Vector3 position = new Vector3();
	public Vector3 moveDirection = new Vector3();
	public Vector3 up = new Vector3(Vector3.Y);
	private Vector3 velocityXZ = new Vector3();
	private Vector3 velocityNew = new Vector3();

	// TODO: Refactor to FovObjectHandler
	public Vector3 positionCarried = new Vector3();
	private GameObject carried;
	// TODO: Unused, probably refactor to FovObjectHandler
	private boolean gunHidden = false;

	// Used to toggle which sound to play
	private PlayerAction soundMoveMode = PlayerAction.STOP;

	private float cameraOffsetY = GameSettings.PLAYER_EYE_HEIGHT
			- GameSettings.PLAYER_HEIGHT / 2;


	public ArrayMap<String, GameObject> inventory = new ArrayMap<String, GameObject>();

	Vector3 teleportPosition = new Vector3();
	Task teleportTask = new Task() {
		@Override
		public void run() {
			position(teleportPosition);
			isGrappling = true;
			grapplingBlocked = false;
			equipFromInventory("blowpipe");
		}
	};
	Task unblockGrapplingTask = new Task() {
		@Override
		public void run() {
			grapplingBlocked = false;
		}
	};

	private Ray ray = new Ray();
	private Vector3 tmp = new Vector3();

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
		contactListener = new PlayerContactListener();
		contactListener.enable();

		controller = new PlayerController(this);
	}

	public void resetActions() {
//		inventory.clear();
//		fovObjhandler.setCurrentObj(null);
		body.setGravity(GameSettings.GRAVITY);
		isJumping = false;
		isGrappling = false;
	}

	public void addToInventory(GameObject item) {
		inventory.put(item.id, item);
		fovObjhandler.add(item);
	}


	public void equipFromInventory(String item) {
		if (inventory.containsKey(item)) {
			fovObjhandler.setCurrentObj(inventory.get(item));
		} else {
			fovObjhandler.setCurrentObj(null);
		}
	}

	private void handleGrappling() {
		if (controller.actionQueueContains(PlayerAction.HOOK)
				&& inventory.containsKey("hook")
				&& !teleportTask.isScheduled() && !grapplingBlocked) {

			ray.set(viewport.getCamera().position,
					viewport.getCamera().direction);
			float distance = 1000;

			btRigidBody hitObject = world.rayTest(ray, teleportPosition,
					(short) (CollisionHandler.GROUND_FLAG
							| CollisionHandler.OBJECT_FLAG),
					distance);

			if (hitObject != null) {
				equipFromInventory("hook");
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
		if (controller.actionQueueContains(PlayerAction.FIRE) && !gunHidden && !grapplingBlocked) {
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

		handleShooting();

		fovObjhandler.update(viewport, controller, soundMoveMode, position, isOnGround, delta);

		controller.actionQueueClear();
	}


}
