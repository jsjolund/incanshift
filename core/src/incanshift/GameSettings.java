package incanshift;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector3;

public class GameSettings {
	public static final float PLAYER_WALK_SPEED = 3f;
	public static final float PLAYER_RUN_SPEED = 6f;
	public static final float PLAYER_JUMP_SPEED = 4f;

	public static final float PLAYER_HEIGHT = 1.8f;
	public static final float PLAYER_RADIUS = 0.5f;
	public static final float PLAYER_EYE_HEIGHT = 1.5f;
	public static final Vector3 PLAYER_START_POS = new Vector3(50, 5, 20);

	public static final int STRAFE_LEFT = Keys.A;
	public static final int STRAFE_RIGHT = Keys.D;
	public static final int FORWARD = Keys.W;
	public static final int BACKWARD = Keys.S;
	public static final int UP = Keys.Q;
	public static final int DOWN = Keys.E;
	public static final int JUMP = Keys.SPACE;
	public static final int WALK = Keys.SHIFT_LEFT;
	
	public static final float MOUSE_SENSITIVITY = 0.05f;
}
