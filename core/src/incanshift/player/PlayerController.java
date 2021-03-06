package incanshift.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import incanshift.world.GameSettings;

class PlayerController implements InputProcessor {

	private final IntIntMap keys = new IntIntMap();
	private final IntIntMap actionQueue = new IntIntMap();
	private final Vector3 moveDirection = new Vector3();
	private final Vector3 tmp = new Vector3();
	private final Vector3 xzMouseRotation = new Vector3();
	Vector3 directionOld = new Vector3();
	float epsilonY = 0.008f;
	private Player player;
	private boolean jumpKeyReleased = true;
	private boolean flyKeyReleased = true;
	private boolean jumpTimerRunning = true;
	private PlayerAction move = PlayerAction.WALK;

	// private static final String tag = "PlayerController";

	public PlayerController(Player player) {
		this.player = player;
	}

	private void actionQueueAdd(PlayerAction action) {
		actionQueue.put(action.ordinal(), 1);
	}

	public void actionQueueClear() {
		actionQueue.clear();
	}

	public boolean actionQueueContains(PlayerAction action) {
		return actionQueue.containsKey(action.ordinal());
	}

	public void centerMouseCursor() {
		Gdx.input.setCursorPosition((int) player.screenCenter.x,
				(int) player.screenCenter.y);
	}

	public Vector3 getMoveDirection() {
		return moveDirection;
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Input.Keys.ESCAPE) {
			actionQueueClear();
			actionQueueAdd(PlayerAction.STOP);
			player.game.getScreenshot();
			player.game.showStartScreen();
		} else {
			keys.put(keycode, keycode);
		}

		if (keycode == GameSettings.RUN) {
			move = PlayerAction.RUN;
		}
		if (keycode == GameSettings.USE) {
			actionQueueAdd(PlayerAction.USE);
		}
		if (keycode == Keys.F1 && flyKeyReleased) {
			flyKeyReleased = false;
			actionQueueAdd(PlayerAction.FLY);
		}
		if (keycode == Keys.F4) {
			player.world.loadPrevLevel();
		}
		if (keycode == Keys.F5) {
			player.world.reloadLevel();
		}
		if (keycode == Keys.F6) {
			player.world.loadNextLevel();
		}
		if (keycode == GameSettings.MASK) {
			player.world.xRayMask = true;
		}
		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		keys.remove(keycode, 0);

		if (keycode == GameSettings.JUMP) {
			jumpKeyReleased = true;
		}
		if (keycode == GameSettings.RUN) {
			move = PlayerAction.WALK;
		}
		if (keycode == Keys.F1) {
			flyKeyReleased = true;
		}
		if (keycode == GameSettings.MASK) {
			player.world.xRayMask = false;
		}
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// Perform camera mouse look
		float mouseSens = GameSettings.MOUSE_SENSITIVITY;

		directionOld.set(player.direction);

		float mouseDx = screenX - player.screenCenter.x;
		float mouseDy = screenY - player.screenCenter.y;

		player.direction.rotate(
				xzMouseRotation.set(player.direction).crs(Vector3.Y),
				-mouseSens * mouseDy);
		player.direction.rotate(Vector3.Y, -mouseSens * mouseDx);

		if ((Math.signum(player.direction.x) != Math.signum(directionOld.x))
				&& Math.signum(player.direction.z) != Math.signum(directionOld.z)) {
			player.direction.set(directionOld);
		}

		player.direction.nor();
		centerMouseCursor();
		return true;
	}

	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (button == GameSettings.SHOOT) {
			actionQueueAdd(PlayerAction.FIRE);
		}
		if (button == GameSettings.HOOK) {
			actionQueueAdd(PlayerAction.HOOK);
		}
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		mouseMoved(screenX, screenY);
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	public void update() {
		// Calculate combined move direction
		moveDirection.setZero();
		PlayerAction action = PlayerAction.STOP;

		if (keys.containsKey(GameSettings.FORWARD)) {
			moveDirection.add(player.direction);
			action = move;
		}
		if (keys.containsKey(GameSettings.BACKWARD)) {
			moveDirection.sub(player.direction);
			action = move;
		}
		if (keys.containsKey(GameSettings.STRAFE_LEFT)) {
			tmp.setZero().sub(player.direction).crs(player.up);
			moveDirection.add(tmp);
			action = move;
		}
		if (keys.containsKey(GameSettings.STRAFE_RIGHT)) {
			tmp.setZero().add(player.direction).crs(player.up);
			moveDirection.add(tmp);
			action = move;
		}

		actionQueueAdd(action);

		if ((keys.containsKey(GameSettings.JUMP) && jumpKeyReleased)) {
			jumpKeyReleased = false;
			jumpTimerRunning = true;
			actionQueueAdd(PlayerAction.JUMP);

			Timer.schedule(new Task() {
				@Override
				public void run() {
					jumpTimerRunning = false;
				}
			}, GameSettings.PLAYER_MAX_JUMP_PRESS_TIME);

		} else if (keys.containsKey(GameSettings.JUMP) && jumpTimerRunning) {
			actionQueueAdd(PlayerAction.JUMP);
		}
	}
}