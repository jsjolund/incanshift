package incanshift;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.math.Vector3;

public class GameSettings {

	public static final int CROSSHAIR = 5;
	public static final float MOUSE_SENSITIVITY = 0.05f;
	public static final Vector3 GRAVITY = new Vector3(0, -9.81f, 0);
	public static final float CAMERA_FOV = 60;

	public static final float PLAYER_CLIMB_SPEED = 1.5f;
	public static final float PLAYER_WALK_SPEED = 8f;
	public static final float PLAYER_RUN_SPEED = 12f;
	public static final float PLAYER_JUMP_FORCE = 3000f;
	public static final float PLAYER_MAX_JUMP_PRESS_TIME = 0.2f;
	public static final float PLAYER_STOP_DOWNSCALE = 0.9f;

	public static final float PLAYER_HEIGHT = 2f;
	public static final float PLAYER_RADIUS = 0.5f;
	public static final float PLAYER_EYE_HEIGHT = 1.8f;
	public static final Vector3 PLAYER_START_POS = new Vector3(0, 5, 0);

	// TODO: Some values for direction makes the game crash on start...
	public static final Vector3 PLAYER_START_DIR = new Vector3(1E-5f, 0, 1E-5f);

	public static final int FORWARD = Keys.W;
	public static final int STRAFE_LEFT = Keys.A;
	public static final int BACKWARD = Keys.S;
	public static final int STRAFE_RIGHT = Keys.D;
	public static final int JUMP = Keys.SPACE;
	public static final int RUN = Keys.SHIFT_LEFT;
	public static final int USE = Keys.E;
	public static final int RESET = Keys.F5;

}
