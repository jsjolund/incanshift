package incanshift;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector3;

public class GameSettings {

	public static final int CROSSHAIR = 5;
	public static final float MOUSE_SENSITIVITY = 0.05f;
	public static final Vector3 GRAVITY = new Vector3(0, -9.81f * 2, 0);
	public static final float CAMERA_FOV = 60;

	public static final float PLAYER_CLIMB_SPEED = 1.5f;
	public static final float PLAYER_WALK_SPEED = 8f;
	public static final float PLAYER_RUN_SPEED = 12f;
	public static final float PLAYER_JUMP_FORCE = 6000f;
	public static final float PLAYER_MAX_JUMP_PRESS_TIME = 0.2f;
	public static final float PLAYER_STOP_DOWNSCALE = 0.9f;

	public static final float PLAYER_HEIGHT = 2f;
	public static final float PLAYER_RADIUS = 0.5f;
	public static final float PLAYER_EYE_HEIGHT = 1.8f;

	// TODO: Some values for direction makes the game crash on start...
	public static final Vector3 PLAYER_START_DIR = (new Vector3(1, 0, 1f))
			.nor();

	public static final int SHOOT = Buttons.LEFT;

	public static int FORWARD = Keys.W;
	public static int STRAFE_LEFT = Keys.A;
	public static int BACKWARD = Keys.S;
	public static int STRAFE_RIGHT = Keys.D;
	public static int JUMP = Keys.SPACE;
	public static int RUN = Keys.SHIFT_LEFT;
	public static int USE = Keys.E;
	public static int RESET = Keys.F5;
	
	public static int FLY = Keys.F1;

}
