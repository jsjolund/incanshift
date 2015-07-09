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

	private PlayerAction currentAction = PlayerAction.STOP;
	private PlayerAction previousAction = PlayerAction.STOP;
	private Array<PlayerAction> previousActions;

	private GameObject object;

	private CollisionHandler collisionHandler;
	private PlayerContactListener contactListener;
	PlayerController controller;
	Vector3 direction;

	Vector3 position;
	Vector3 up;
	Vector3 moveDirection;
	Vector3 velocity;

	// private Array<GameObject> instances;
	Vector3 screenCenter;

	private Vector3 xzVelocity = new Vector3();

	public boolean captureMouse = true;

	public boolean canClimb = false;

	public boolean onGround = false;
	private PlayerSound sound;

	IncanShift game;

	public boolean gravity = true;

	Viewport viewport;

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

		contactListener = new PlayerContactListener(this);
		contactListener.enable();

		controller = new PlayerController(this);

		previousActions = new Array<Player.PlayerAction>(10);
		previousActions.ordered = true;
		previousActions.add(currentAction);
	}

	@Override
	public void dispose() {
		contactListener.dispose();
	}

	public void setCurrentAction(PlayerAction action) {
		previousAction = currentAction;
		if (previousActions.size == 10) {
			previousActions.removeIndex(0);
		}
		previousActions.add(previousAction);
		currentAction = action;
	}

	public void update(float delta) {
		collisionHandler.performDiscreteCollisionDetection();
		boolean newAction = currentAction != previousAction;

		if (newAction && currentAction == PlayerAction.STOP) {
			sound.halt();

		} else if (newAction && currentAction == PlayerAction.WALK) {
			sound.move(false);

		} else if (newAction && currentAction == PlayerAction.RUN) {
			sound.move(true);

		} else if (newAction && currentAction == PlayerAction.CLIMB) {
			sound.climb();

		} else if (newAction && currentAction == PlayerAction.SHOOT) {
			sound.shoot();
			Ray ray = viewport.getCamera().getPickRay(screenCenter.x,
					screenCenter.y);
			GameObject hitObject = collisionHandler.rayTest(ray, 100);
			if (hitObject != null && hitObject.removable && hitObject.visible) {
				hitObject.visible = false;
				sound.bump();
			}

		}

		// collisionHandler.performDiscreteCollisionDetection();

		float moveSpeed = 0;

		if (currentAction == PlayerAction.RUN) {
			moveSpeed = GameSettings.PLAYER_RUN_SPEED;
		} else if (currentAction == PlayerAction.WALK) {
			moveSpeed = GameSettings.PLAYER_WALK_SPEED;
		} else if (currentAction == PlayerAction.CLIMB) {
			moveSpeed = GameSettings.PLAYER_CLIMB_SPEED;
		} else if (currentAction == PlayerAction.STOP) {
			moveSpeed = 0;
		}

		// System.out.println(currentAction.toString());

		// Calculate movement velocity vector
		moveDirection.set(controller.getMoveDirection());
		Vector3 moveVelocity = moveDirection.nor().scl(moveSpeed);

		if (onGround && currentAction != PlayerAction.JUMP) {
			velocity.add(moveVelocity);
		}

		if (gravity) {
			velocity.y -= GameSettings.GRAVITY * delta;
		}

		// Translate player

		if (newAction && currentAction == PlayerAction.JUMP && onGround) {
			sound.jump();

			position.add(0, 0.1f, 0);
			velocity.y += GameSettings.PLAYER_JUMP_ACCELERATION* delta;

		} else if (currentAction == PlayerAction.JUMP) {
			velocity.y += GameSettings.PLAYER_JUMP_ACCELERATION * delta;
		} else if (!onGround) {
			currentAction = PlayerAction.STOP;
		}

		if (currentAction != previousAction) {
			System.out.println(currentAction);
		}

		position.add(velocity.cpy().scl(delta));



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
