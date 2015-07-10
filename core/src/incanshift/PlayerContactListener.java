package incanshift;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btManifoldPoint;
import com.badlogic.gdx.utils.Timer.Task;

public class PlayerContactListener extends ContactListener {

	private Player player;
	private Vector3 collisionNormal = new Vector3();
	private float collisionDistance = 0;

	// private Vector3 xzVelocity = new Vector3();
	private Vector3 collisionRemoval = new Vector3();

	private Vector3 velocityNormal = new Vector3();
	private Vector3 orthagonal = new Vector3();
	private Vector3 projection = new Vector3();
	private Vector3 tmp = new Vector3();

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

		// Change velocity vector to the projection on the surface
		velocityNormal.set(player.velocity).nor();

		orthagonal.set(velocityNormal).crs(collisionNormal).nor()
				.rotate(collisionNormal, 90);

		projection
				.set(orthagonal)
				.sub(collisionNormal.scl(tmp.set(orthagonal)
						.sub(velocityNormal).dot(collisionNormal))).nor();

		player.velocity.set(projection).scl(player.velocity.len());

		// Slow down the player
		// player.velocity.y *= collisionNormal.dst2(Vector3.Y) / 2;
		player.velocity.y = 0;

		player.gravity = false;
		player.onGround = true;
		return true;
	}

	@Override
	public void onContactEnded(btCollisionObject colObj0,
			btCollisionObject colObj1) {
		if (colObj0.getContactCallbackFlag() == CollisionHandler.OBJECT_FLAG) {

		}
		if (colObj0.getContactCallbackFlag() == CollisionHandler.GROUND_FLAG) {

		}
		// if (!timerIsOn) {
		// timerIsOn = true;
		// setOnGround = Timer.schedule(new Task() {
		// @Override
		// public void run() {
		player.onGround = false;
		// timerIsOn = false;
		// }
		// }, 0.1f);
		// }
		player.gravity = true;

	}
}
