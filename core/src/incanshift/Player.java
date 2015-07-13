package incanshift;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btManifoldPoint;
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

	public class PlayerContactListener extends ContactListener {

		@Override
		public boolean onContactAdded(btManifoldPoint cp, int userValue0,
				int partId0, int index0, int userValue1, int partId1, int index1) {
			// Check if collision normal is horizontal, if so, climb
			return true;
		}

		@Override
		public void onContactEnded(btCollisionObject colObj0,
				btCollisionObject colObj1) {
		}
	}

	private PlayerSound sound;

	IncanShift game;

	private Viewport viewport;
	Vector3 screenCenter;

	GameObject object;

	private CollisionHandler collisionHandler;
	private PlayerContactListener contactListener;
	PlayerController controller;

	public Vector3 direction = new Vector3();
	public Vector3 velocity = new Vector3();
	public Vector3 velocityXZ = new Vector3();
	public Vector3 velocityNew = new Vector3();
	public Vector3 position = new Vector3();
	public Vector3 moveDirection = new Vector3();
	public Vector3 up = new Vector3(Vector3.Y);

	boolean canClimb = false;
	boolean isJumping = false;
	private Ray ray = new Ray();

	public boolean newCollision;
	private float cameraOffsetY = GameSettings.PLAYER_EYE_HEIGHT
			- GameSettings.PLAYER_HEIGHT / 2;

	PlayerAction moveMode = PlayerAction.STOP;

	public Player(IncanShift game, GameObject playerObject,
			Vector3 screenCenter, Viewport viewport,
			CollisionHandler collisionHandler, PlayerSound sound) {
		this.viewport = viewport;
		this.game = game;
		this.collisionHandler = collisionHandler;
		this.object = playerObject;
		this.sound = sound;
		this.screenCenter = screenCenter;

		position = new Vector3();
		playerObject.transform.getTranslation(position);

		moveDirection = new Vector3();
		direction = new Vector3(Vector3.X);

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

		controller.update();
		moveDirection.set(controller.getMoveDirection());

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

		float moveSpeed = 0;

		if (moveMode == PlayerAction.WALK) {
			moveSpeed = GameSettings.PLAYER_WALK_SPEED;

		} else if (moveMode == PlayerAction.RUN) {
			moveSpeed = GameSettings.PLAYER_RUN_SPEED;

		} else if (moveMode == PlayerAction.CLIMB) {
			moveSpeed = GameSettings.PLAYER_CLIMB_SPEED;

		} else if (moveMode == PlayerAction.STOP) {
			moveSpeed = 0;
		}

		if (moveDirection.y > 0) {
			moveDirection.y = 0;
		}

		velocity.set(object.body.getLinearVelocity());
		velocityXZ.set(velocity.x, 0, velocity.z);
		float currentSpeed = velocityXZ.len();

		object.body.setActivationState(Collision.DISABLE_DEACTIVATION);

		if (isOnGround) {
			if (moveSpeed == 0) {
				velocity.x *= 0.5f;
				velocity.z *= 0.5f;
			}
			if (currentSpeed < moveSpeed) {
				velocityNew.set(moveDirection).nor().scl(moveSpeed);

				velocity.set(velocityNew.x, velocity.y, velocityNew.z);
			}
		}

		if (controller.actionQueueContains(PlayerAction.JUMP)) {
			if (!isJumping && isOnGround) {
				isJumping = true;
				sound.jump();
			} else if (isJumping) {
				object.body.applyCentralForce(new Vector3(Vector3.Y)
						.scl(GameSettings.PLAYER_JUMP_ACCELERATION));
			}
		} else {
			isJumping = false;
		}

		object.body.setLinearVelocity(velocity);

		controller.actionQueueClear();

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

	}
}
