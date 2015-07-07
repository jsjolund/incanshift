package incanshift;

import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.utils.Disposable;

/**
 * A game object consisting of a model, collision body and some additional
 * data
 */
class GameObject extends ModelInstance implements Disposable {

	public final btCollisionObject body;

	public boolean onGround = false;
	public boolean removable = false;
	public boolean visible = true;
	public Vector3 velocity = new Vector3();

	public GameObject(Model model, btCollisionShape shape) {
		super(model);
		body = new btCollisionObject();
		body.setCollisionShape(shape);
	}

	public void trn(Vector3 translation) {
		transform.trn(translation);
		body.setWorldTransform(transform);
		calculateTransforms();
	}

	public void position(Vector3 position) {
		transform.setTranslation(position);
		body.setWorldTransform(transform);
		calculateTransforms();
	}

	@Override
	public void dispose() {
		body.dispose();
	}

	/**
	 * Constructor class for game objects
	 */
	static class Constructor implements Disposable {

		public final Model model;
		public final btCollisionShape shape;

		public Constructor(Model model, btCollisionShape shape) {
			this.shape = shape;
			if (model == null) {
				model = new ModelBuilder().createXYZCoordinates(1,
						new Material(), Usage.Position | Usage.Normal);
			}
			this.model = model;
		}

		public GameObject construct() {
			return new GameObject(model, shape);
		}

		@Override
		public void dispose() {
			shape.dispose();
		}
	}
}