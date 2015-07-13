package incanshift;

import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Disposable;

/**
 * A game object consisting of a model, collision body and some additional data
 */
class GameObject extends ModelInstance implements Disposable {

	/**
	 * Constructor class for game objects
	 */
	static class Constructor implements Disposable {

		public final Model model;
		public final btCollisionShape shape;
		public final btRigidBody.btRigidBodyConstructionInfo constructionInfo;
		private static Vector3 localInertia = new Vector3();

		public Constructor(Model model, btCollisionShape shape, float mass) {
			if (model == null) {
				this.model = new ModelBuilder().createXYZCoordinates(1,
						new Material(), Usage.Position | Usage.Normal);
			} else {
				this.model = model;
			}
			this.shape = shape;
			if (mass > 0f)
				shape.calculateLocalInertia(mass, localInertia);
			else
				localInertia.set(0, 0, 0);
			this.constructionInfo = new btRigidBody.btRigidBodyConstructionInfo(
					mass, null, shape, localInertia);
		}

		public GameObject construct() {
			return new GameObject(model, constructionInfo);
		}

		@Override
		public void dispose() {
			shape.dispose();
			constructionInfo.dispose();
		}

	}

	public final btRigidBody body;
	public boolean removable = false;
	public boolean visible = true;

	public GameObject(Model model,
			btRigidBody.btRigidBodyConstructionInfo constructionInfo) {
		super(model);
		body = new btRigidBody(constructionInfo);
	}

	@Override
	public void dispose() {
		body.dispose();
	}

	public void position(float x, float y, float z) {
		transform.setTranslation(x, y, z);
		body.setWorldTransform(transform);
		calculateTransforms();
	}

	public void position(Vector3 position) {
		transform.setTranslation(position);
		body.setWorldTransform(transform);
		calculateTransforms();
	}

	public void trn(float x, float y, float z) {
		transform.trn(x, y, z);
		body.setWorldTransform(transform);
		calculateTransforms();
	}

	public void trn(Vector3 translation) {
		transform.trn(translation);
		body.setWorldTransform(transform);
		calculateTransforms();
	}
}