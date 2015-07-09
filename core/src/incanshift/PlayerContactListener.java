package incanshift;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btManifoldPoint;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

public class PlayerContactListener extends ContactListener {

	private Player player;
	Vector3 collisionNormal = new Vector3();
	float collisionDistance = 0;

	Vector3 collisionRemoval = new Vector3();

	Vector3 velocityNormal = new Vector3();
	Vector3 velocityCombined = new Vector3();

	Vector3 velocityCrs1 = new Vector3();
	Vector3 velocityCrs2 = new Vector3();

	Vector3 xzVelocity = new Vector3();

	boolean timerIsOn = false;
	Task setOnGround;

	public PlayerContactListener(Player player) {
		this.player = player;
	}

	@Override
	public boolean onContactAdded(btManifoldPoint cp, int userValue0,
			int partId0, int index0, int userValue1, int partId1, int index1) {

		cp.getNormalWorldOnB(collisionNormal);
		collisionDistance = cp.getDistance1();

		collisionNormal.nor();

		// Translate player back along normal
		collisionRemoval.set(collisionNormal).scl(-collisionDistance);
		player.position.add(collisionRemoval);

		velocityNormal.set(player.velocity.cpy().scl(1, 0, 1)).nor();
		velocityCombined.set(velocityNormal).add(collisionNormal).nor()
				.scl(player.velocity.len());

		player.velocity.set(velocityCombined);

		player.velocity.y *= collisionNormal.dst2(Vector3.Y) / 2;

		if (collisionNormal.epsilonEquals(Vector3.Y, 0.1f)) {

			if (setOnGround != null && setOnGround.isScheduled()) {
				setOnGround.cancel();
			}
			player.gravity = false;
			player.onGround = true;

		} else if (collisionNormal.isPerpendicular(Vector3.Y, 0.1f)) {
			// wall
		}

		player.velocity.x *= 1;
		// player.velocity.y *= 1f;
		player.velocity.z *= 1;

		return true;
	}

	@Override
	public void onContactEnded(btCollisionObject colObj0,
			btCollisionObject colObj1) {
		if (colObj0.getContactCallbackFlag() == CollisionHandler.OBJECT_FLAG) {

		}
		if (colObj0.getContactCallbackFlag() == CollisionHandler.GROUND_FLAG) {

		}
		System.out.println("fly");
		if (!timerIsOn) {
			timerIsOn = true;
			setOnGround = Timer.schedule(new Task() {
				@Override
				public void run() {
					player.onGround = false;
				}
			}, 0.1f);
		}
		player.gravity = true;

	}
}
