package incanshift.player;


import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import incanshift.gameobjects.GameObject;


class FovObjectHandler {

	public final static String tag = "FovObjectHandler";
	private float walkAnimTimer = 0;
	private float shootAnimTimer = 10;

	private Matrix4 objTransform = new Matrix4();
	private Vector3 objFrontBackPos = new Vector3();
	private Vector3 objLeftRightPos = new Vector3();
	private Vector3 objUpDownPos = new Vector3();

	private ArrayMap<GameObject, FovObjectState> fovPosMap = new ArrayMap<GameObject, FovObjectState>();
	private GameObject activeObj;

	/**
	 * Add object to fov tracking
	 *
	 * @param obj
	 */
	public void add(GameObject obj) {
		if (obj == null || fovPosMap.containsKey(obj)) {
			return;
		}
		FovObjectState state = new FovObjectState();
		if (obj.id.equals("blowpipe")) {
			state.constants.normal.set(0.075f, 0.1f, 0.3f);
			state.constants.hide.set(0.075f, 1f, 0.3f);
			state.constants.move.set(0.075f, 0.13f, 0.3f);
			state.constants.fall.set(0.075f, 0.06f, 0.3f);
			state.constants.shoot.set(-0.001f, 0.15f, 0.25f);
			state.goalPos.set(state.constants.hide);
			state.pos.set(state.constants.hide);
		}
		if (obj.id.equals("hook")) {
			state.constants.normal.set(0, 0, 0.4f);
			state.constants.hide.set(0, 0, -1);
			state.goalPos.set(state.constants.hide);
			state.pos.set(state.constants.hide);
		}
		fovPosMap.put(obj, state);
	}

	/**
	 * Sets the object to active/currently held item
	 *
	 * @param newActiveObj
	 */
	public void setCurrentObj(GameObject newActiveObj) {
		activeObj = newActiveObj;
		if (newActiveObj == null) {
			return;
		}
		if (!fovPosMap.containsKey(newActiveObj)) {
			add(newActiveObj);
		}
		FovObjectState state = fovPosMap.get(newActiveObj);
		state.goalPos.set(state.constants.normal);
		if (newActiveObj.id.equals("hook")) {
			state.pos.set(state.constants.normal);
		}
		for (ObjectMap.Entry<GameObject, FovObjectState> entry : fovPosMap) {
			if (entry.key.equals(newActiveObj)) {
				continue;
			}
			entry.value.goalPos.set(state.constants.hide);
		}
	}

	/**
	 * Calculates where the object should be positioned depending on player state.
	 *
	 * @param controller
	 * @param moveMode
	 * @param isOnGround
	 * @param delta
	 * @return
	 */
	private float calculateFovState(PlayerController controller, PlayerAction moveMode, boolean isOnGround, float delta) {
		FovObjectState state = fovPosMap.get(activeObj);
		float speed;
		if (controller.actionQueueContains(PlayerAction.FIRE) || shootAnimTimer < 0.5f) {
			speed = 25;
			state.goalPos.set(state.constants.shoot);
			if (controller.actionQueueContains(PlayerAction.FIRE)) {
				shootAnimTimer = 0;
			}

		} else if (moveMode == PlayerAction.WALK) {
			speed = 5;
			if (walkAnimTimer < 0.2f) {
				// After how long to move down
				state.goalPos.set(state.constants.move);
			} else if (walkAnimTimer < 0.6f) {
				// After how long to move up
				state.goalPos.set(state.constants.normal);
				speed = 3;
			} else if (walkAnimTimer < 11f) {
				// How long to hold in up pos
				walkAnimTimer = 0;
			}

		} else if (moveMode == PlayerAction.RUN) {
			speed = 10;
			if (walkAnimTimer < 0.1f) {
				// After how long to move down
				state.goalPos.set(state.constants.move);
			} else if (walkAnimTimer < 0.3f) {
				// After how long to move up
				state.goalPos.set(state.constants.normal);
				speed = 6;
			} else if (walkAnimTimer < 0.8f) {
				// How long to hold in up pos
				walkAnimTimer = 0;
			}

		} else if (!isOnGround) {
			speed = 4;
			state.goalPos.set(state.constants.fall);
			walkAnimTimer = 0;

		} else {
			speed = 3;
			walkAnimTimer = 0;
			state.goalPos.set(state.constants.normal);
		}
		return speed;
	}

	/**
	 * Update the objects held by the player.
	 *
	 * @param viewport
	 * @param controller
	 * @param moveMode
	 * @param playerPosition
	 * @param isOnGround
	 * @param delta
	 */
	void update(Viewport viewport, PlayerController controller, PlayerAction moveMode, Vector3 playerPosition, boolean isOnGround, float delta) {
		walkAnimTimer = (walkAnimTimer < 60) ? walkAnimTimer + delta : 0;
		shootAnimTimer = (shootAnimTimer < 60) ? shootAnimTimer + delta : 0;

		for (ObjectMap.Entry<GameObject, FovObjectState> entry : fovPosMap) {
			GameObject obj = entry.key;
			FovObjectState state = entry.value;
			float speed = 4;
			if (obj.equals(activeObj)) {
				speed = calculateFovState(controller, moveMode, isOnGround, delta);
			} else {
				state.goalPos.set(state.constants.hide);
			}
			Camera cam = viewport.getCamera();
			objTransform.set(cam.view).inv();
			obj.position(playerPosition);
			obj.body.setWorldTransform(objTransform);

			if (!state.pos.epsilonEquals(state.goalPos, 0.01f)) {
				float dst = state.pos.dst(state.goalPos);
				state.moveVel.set(state.goalPos).sub(state.pos).nor().scl(speed).scl(dst);
				state.pos.add(state.moveVel.scl(delta));
			}

			objLeftRightPos.set(cam.direction).crs(Vector3.Y).nor()
					.scl(state.pos.leftRight());
			objFrontBackPos.set(cam.direction).nor().scl(state.pos.frontBack());
			objUpDownPos.set(cam.direction).nor().crs(objLeftRightPos)
					.nor().scl(state.pos.upDown());

			obj.body.translate(objLeftRightPos);
			obj.body.translate(objFrontBackPos);
			obj.body.translate(objUpDownPos);
			obj.body.getWorldTransform(obj.transform);
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

	private class FovObjectState {
		FovObjectStateConstants constants = new FovObjectStateConstants();
		FovPosition goalPos = new FovPosition();
		FovPosition pos = new FovPosition();
		FovPosition moveVel = new FovPosition();
	}

	private class FovObjectStateConstants {
		FovPosition normal = new FovPosition();
		FovPosition hide = new FovPosition();
		FovPosition move = new FovPosition();
		FovPosition fall = new FovPosition();
		FovPosition shoot = new FovPosition();
	}
}
