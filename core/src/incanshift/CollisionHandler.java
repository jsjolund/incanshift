package incanshift;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class CollisionHandler implements Disposable {

	// Collision flags
	final static short NONE_FLAG = 0;
	final static short GROUND_FLAG = 1 << 8;
	final static short OBJECT_FLAG = 1 << 9;
	final static short ALL_FLAG = -1;

	private Array<GameObject> instances;
	private btCollisionConfiguration collisionConfig;
	private btDispatcher dispatcher;

	btDynamicsWorld dynamicsWorld;
	private btConstraintSolver constraintSolver;
	private btDbvtBroadphase broadphase;
	private DebugDrawer debugDrawer;

	public CollisionHandler(Array<GameObject> instances) {
		this.instances = instances;

		collisionConfig = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);
		broadphase = new btDbvtBroadphase();
		constraintSolver = new btSequentialImpulseConstraintSolver();
		dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase,
				constraintSolver, collisionConfig);
		dynamicsWorld.setGravity(GameSettings.GRAVITY);

		debugDrawer = new DebugDrawer();
		dynamicsWorld.setDebugDrawer(debugDrawer);
		debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);

	}

	public void add(GameObject obj, short flag0, short flag1) {
		dynamicsWorld.addRigidBody(obj.body, flag0, flag1);
	}

	public void debugDrawWorld(Camera camera) {
		debugDrawer.begin(camera);
		dynamicsWorld.debugDrawWorld();
		debugDrawer.end();
	}

	@Override
	public void dispose() {
		collisionConfig.dispose();
		dispatcher.dispose();
		dynamicsWorld.dispose();
		broadphase.dispose();
		constraintSolver.dispose();
	}

	public GameObject rayTest(Ray ray, float maxDistance) {
		Vector3 rayFrom = new Vector3(ray.origin);
		Vector3 rayTo = new Vector3(ray.direction).scl(maxDistance)
				.add(rayFrom);

		ClosestRayResultCallback callback = new ClosestRayResultCallback(
				rayFrom, rayTo);
		dynamicsWorld.rayTest(rayFrom, rayTo, callback);

		btCollisionObject co = callback.getCollisionObject();
		boolean hasHit = callback.hasHit();

		callback.dispose();

		if (hasHit) {
			for (GameObject obj : instances) {
				if (obj.body.equals(co)) {
					return obj;
				}
			}
		}

		return null;
	}
}
