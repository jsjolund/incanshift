package incanshift;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector3;

public class GameSettings {

	public static final int CROSSHAIR = 7;
	public static final float MOUSE_SENSITIVITY = 0.05f;
	public static final float GRAVITY = 25f;
	public static final float CAMERA_FOV = 90;

	public static final float PLAYER_CLIMB_SPEED = 2f;
	public static final float PLAYER_WALK_SPEED = 3f;
	public static final float PLAYER_RUN_SPEED = 5f;
	public static final float PLAYER_JUMP_ACCELERATION = 60f;
	public static final float PLAYER_JUMP_TIME = 0.2f;

	public static final float PLAYER_HEIGHT = 2f;
	public static final float PLAYER_RADIUS = 0.5f;
	public static final float PLAYER_EYE_HEIGHT = 1.8f;
	public static final Vector3 PLAYER_START_POS = new Vector3(-10,30, 10);

	public static final int STRAFE_LEFT = Keys.A;
	public static final int STRAFE_RIGHT = Keys.D;
	public static final int FORWARD = Keys.W;
	public static final int BACKWARD = Keys.S;
	public static final int UP = Keys.Q;
	public static final int DOWN = Keys.E;
	public static final int JUMP = Keys.SPACE;
	public static final int RUN = Keys.SHIFT_LEFT;
	public static final int RESET = Keys.F5;

}
