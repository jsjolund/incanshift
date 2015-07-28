package incanshift.world;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Disposable;
import incanshift.gameobjects.GameObject;

public class CollisionHandler implements Disposable {

	// Collision flags
	public final static short NONE_FLAG = 0;
	public final static short HOOK_FLAG = 1 << 6;
	public final static short PLAYER_FLAG = 1 << 7;
	public final static short GROUND_FLAG = 1 << 8;
	public final static short OBJECT_FLAG = 1 << 9;
	public final static short ALL_FLAG = -1;
	Vector3 tmp = new Vector3();
	private btCollisionConfiguration collisionConfig;
	private btDispatcher dispatcher;
	private btDynamicsWorld dynamicsWorld;
	private btConstraintSolver constraintSolver;
	private btDbvtBroadphase broadphase;
	private DebugDrawer debugDrawer;

	public CollisionHandler() {

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

	public void add(GameObject obj) {
		dynamicsWorld.addRigidBody(obj.body, obj.belongsToFlag, obj.collidesWithFlag);
	}

	public void remove(GameObject obj) {
		dynamicsWorld.removeCollisionObject(obj.body);
	}

	public void stepSimulation(float delta) {
		dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);
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

	public btRigidBody rayTest(Ray ray, Vector3 point, short mask,
							   float maxDistance) {
		btRigidBody rb = null;

		Vector3 rayFrom = new Vector3(ray.origin);
		Vector3 rayTo = new Vector3(ray.direction).scl(maxDistance)
				.add(rayFrom);

		ClosestRayResultCallback callback = new ClosestRayResultCallback(
				rayFrom, rayTo);
		callback.setCollisionFilterMask(mask);
		dynamicsWorld.rayTest(rayFrom, rayTo, callback);
		if (callback.hasHit()) {
			rb = (btRigidBody) callback.getCollisionObject();
			callback.getHitPointWorld(point);
			callback.getHitNormalWorld(tmp);
			point.add(tmp.nor());
		}
		callback.dispose();
		return rb;
	}

}
