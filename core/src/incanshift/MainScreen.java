package incanshift;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MainScreen implements Screen {

	Viewport viewport;

	private Environment environment;

	private PerspectiveCamera camera;
	private FPSInputProcessor camController;

	private ModelBatch modelBatch;
	private AssetManager assets;

	private Array<ModelInstance> instances = new Array<ModelInstance>(10);

	public MainScreen(Game game) {
		environment = new Environment();

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f,
				0.3f, 0.3f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f,
				-0.8f, -0.2f));

		modelBatch = new ModelBatch();

		camera = new PerspectiveCamera(30, Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		float camd = 20f;
		camera.position.set(camd, camd, camd);
		camera.lookAt(0, 0, 0);
		camera.near = 1E-2f;
		camera.far = 1.5E3f;
		camera.update();

		assets = new AssetManager();
		assets.load("./temple.g3db", Model.class);
		assets.load("./ground.g3db", Model.class);
		assets.finishLoading();

		Model temple = assets.get("./temple.g3db", Model.class);
		Model ground = assets.get("./ground.g3db", Model.class);

		instances.add(new ModelInstance(temple, new Vector3()));
		instances.add(new ModelInstance(ground, new Vector3()));

		viewport = new FitViewport(1280, 720, camera);
		viewport.apply();

		camController = new FPSInputProcessor(viewport, instances);
		Gdx.input.setInputProcessor(camController);
	}

	@Override
	public void dispose() {
		modelBatch.dispose();
		assets.dispose();

	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stubz
	}

	@Override
	public void render(float dt) {
		camController.update();

		Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
		int x = (Gdx.graphics.getWidth() - viewport.getScreenWidth()) / 2;
		int y = (Gdx.graphics.getHeight() - viewport.getScreenHeight()) / 2;
		Gdx.gl.glViewport(x, y, viewport.getScreenWidth(),
				viewport.getScreenHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(camera);
		modelBatch.render(instances, environment);
		modelBatch.end();

	}
	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
		camera.update(true);
		camController.screenCenterX = width / 2;
		camController.screenCenterY = height / 2;

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
	}

	@Override
	public void show() {
		// TODO Auto-generated method stub
	}

}
