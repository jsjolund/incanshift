package incanshift.player;

import incanshift.player.Player.PlayerAction;
import incanshift.world.GameSettings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

class PlayerController implements InputProcessor {

	private Player player;
	private final IntIntMap keys = new IntIntMap();
	private final IntIntMap actionQueue = new IntIntMap();
	private boolean jumpKeyReleased = true;
	private boolean flyKeyReleased = true;
	private boolean jumpTimerRunning = true;
	private final Vector3 moveDirection = new Vector3();
	private final Vector3 tmp = new Vector3();
	private final Vector3 xzMouseRotation = new Vector3();
	Vector3 directionOld = new Vector3();
	float epsilonY = 0.01f;

	private PlayerAction move = PlayerAction.WALK;

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
			player.game.showStartScreen();
		} else {
			keys.put(keycode, keycode);
		}
		if (keycode == GameSettings.RESET) {
			actionQueueAdd(PlayerAction.RESET);
		}
		if (keycode == GameSettings.RUN) {
			move = PlayerAction.RUN;
		}
		if (keycode == GameSettings.USE) {
			actionQueueAdd(PlayerAction.USE);
		}
		if (keycode == GameSettings.FLY && flyKeyReleased) {
			flyKeyReleased = false;
			actionQueueAdd(PlayerAction.FLY);
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
		if (keycode == GameSettings.FLY) {
			flyKeyReleased = true;
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

		if (player.direction.isCollinear(Vector3.Y, epsilonY)
				|| player.direction.isCollinearOpposite(Vector3.Y, epsilonY)) {
			player.direction.set(directionOld);
		}
		player.direction.rotate(Vector3.Y, -mouseSens * mouseDx);
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