package incanshift.player;


import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import incanshift.gameobjects.GameObject;

class FovObject {

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

	FovPosition gunNormal = new FovPosition(0.075f, 0.75f, 0.15f);
	FovPosition hookNormal = new FovPosition(0.01f, 0.1f, 0.4f);
	FovPosition blowpipeNormal = new FovPosition(0.075f, 0.1f, 0.3f);
	FovPosition blowpipeMove = new FovPosition(0.075f, 0.13f, 0.3f);
	FovPosition blowpipeFall = new FovPosition(0.075f, 0.06f, 0.3f);
	FovPosition blowpipeShoot = new FovPosition(-0.001f, 0.15f, 0.25f);
	FovPosition equipGoalPos = new FovPosition();
	FovPosition equipPos = new FovPosition();
	FovPosition equipMoveVel = new FovPosition();

	private float walkAnimTimer = 0;
	private float shootAnimTimer = 10;

	private Matrix4 objTransform = new Matrix4();
	private Vector3 objFrontBackPos = new Vector3();
	private Vector3 objLeftRightPos = new Vector3();
	private Vector3 objUpDownPos = new Vector3();

	private GameObject obj;

	public void setCurrentObj(GameObject obj) {
		this.obj = obj;
		if (obj.id.equals("blowpipe")) {
			equipGoalPos.set(blowpipeNormal);
		}
	}

	/**
	 * Update weapon position/rotation relative to camera
	 *
	 * @param delta
	 */
	void updateEquip(Viewport viewport, PlayerController controller, PlayerAction moveMode, Vector3 playerPosition, boolean isOnGround, float delta) {

		if (obj == null) {
			return;
		}

		Camera cam = viewport.getCamera();
		obj.position(playerPosition);
		objTransform.set(cam.view).inv();
		walkAnimTimer += delta;

		if (shootAnimTimer < 10) {
			// Prevent timer overflow
			shootAnimTimer += delta;
		}
		float speed;

		if (controller.actionQueueContains(PlayerAction.FIRE) || shootAnimTimer < 0.5f) {
			speed = 25;
			equipGoalPos.set(blowpipeShoot);
			if (controller.actionQueueContains(PlayerAction.FIRE)) {
				shootAnimTimer = 0;
			}

		} else if (moveMode == PlayerAction.WALK) {
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

		} else if (moveMode == PlayerAction.RUN) {
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

		obj.body.setWorldTransform(objTransform);

		objLeftRightPos.set(cam.direction).crs(Vector3.Y).nor()
				.scl(equipPos.leftRight());
		objFrontBackPos.set(cam.direction).nor().scl(equipPos.frontBack());
		objUpDownPos.set(cam.direction).nor().crs(objLeftRightPos)
				.nor().scl(equipPos.upDown());

		obj.body.translate(objLeftRightPos);
		obj.body.translate(objFrontBackPos);
		obj.body.translate(objUpDownPos);
//		obj.body.setLinearVelocity(playerLinearVelocity);
		obj.body.getWorldTransform(obj.transform);
	}
}
