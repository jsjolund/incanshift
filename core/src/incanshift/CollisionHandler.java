package incanshift;

import incanshift.MainScreen.GameObject;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class CollisionHandler implements Disposable {

	Array<GameObject> instances;

	btCollisionConfiguration collisionConfig;
	btDispatcher dispatcher;
	private btDbvtBroadphase broadphase;
	private btCollisionWorld collisionWorld;
	private DebugDrawer debugDrawer;

	// Collision flags
	final static short GROUND_FLAG = 1 << 8;
	final static short OBJECT_FLAG = 1 << 9;
	final static short ALL_FLAG = -1;

	public CollisionHandler(GameObject player, Array<GameObject> instances) {
		this.instances = instances;

		collisionConfig = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);
		broadphase = new btDbvtBroadphase();

		collisionWorld = new btCollisionWorld(dispatcher, broadphase, collisionConfig);

		debugDrawer = new DebugDrawer();
		collisionWorld.setDebugDrawer(debugDrawer);
		// debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);

		player.body.setCollisionFlags(player.body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		collisionWorld.addCollisionObject(player.body, OBJECT_FLAG, GROUND_FLAG);

		for (GameObject obj : instances) {
			collisionWorld.addCollisionObject(obj.body, GROUND_FLAG, ALL_FLAG);
		}

	}

	public GameObject rayTest(Ray ray, float maxDistance) {
		Vector3 rayFrom = new Vector3(ray.origin);
		Vector3 rayTo = new Vector3(ray.direction).scl(maxDistance).add(rayFrom);

		ClosestRayResultCallback callback = new ClosestRayResultCallback(rayFrom, rayTo);
		collisionWorld.rayTest(rayFrom, rayTo, callback);

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

	@Override
	public void dispose() {
		collisionConfig.dispose();
		dispatcher.dispose();
		collisionWorld.dispose();
		broadphase.dispose();
	}

	public void performDiscreteCollisionDetection() {
		collisionWorld.performDiscreteCollisionDetection();
	}

	public void debugDrawWorld(Camera camera) {
		debugDrawer.begin(camera);
		collisionWorld.debugDrawWorld();
		debugDrawer.end();
	}
}
