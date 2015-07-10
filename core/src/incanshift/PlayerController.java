package incanshift;

import incanshift.Player.PlayerAction;

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
	private boolean jumpKeyReleased = true;
	private boolean keepJumping = true;
	private final Vector3 moveDirection = new Vector3();
	private final Vector3 tmp = new Vector3();
	private IntIntMap actionQueue = new IntIntMap();

	PlayerAction move = PlayerAction.WALK;

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
		if (keycode == GameSettings.RUN) {
			if (player.onGround && player.velocity.len() > 0) {
				move = PlayerAction.RUN;
			}
		}

		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		keys.remove(keycode, 0);
		if (keycode == GameSettings.JUMP) {
			jumpKeyReleased = true;
		}
		if (keycode == GameSettings.RESET) {
			player.position.set(GameSettings.PLAYER_START_POS);
			player.velocity.setZero();
		}
		if (keycode == GameSettings.RUN) {
			if (player.onGround && player.velocity.len() > 0) {
				move = PlayerAction.WALK;
			}
		}
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		if (!player.captureMouse) {
			return true;
		}
		// Perform camera mouse look
		float mouseSens = GameSettings.MOUSE_SENSITIVITY;

		float mouseDx = screenX - player.screenCenter.x;
		float mouseDy = screenY - player.screenCenter.y;

		player.direction.rotate(Vector3.Y, -mouseSens * mouseDx);
		player.direction.rotate(player.direction.cpy().crs(Vector3.Y),
				-mouseSens * mouseDy);
		player.up.rotate(Vector3.Y, -mouseSens * mouseDx);
		player.up.rotate(player.direction.cpy().crs(Vector3.Y), -mouseSens
				* mouseDy);

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
		if (button == Input.Buttons.LEFT) {
			actionQueueAdd(PlayerAction.SHOOT);
		}
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// Ignore dragging, interpret it as movement
		mouseMoved(screenX, screenY);
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	public void update() {
		// Calculate combined moved direction
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
		if (keys.containsKey(GameSettings.UP)) {
			moveDirection.add(player.up);
			action = move;
		}
		if (keys.containsKey(GameSettings.DOWN)) {
			moveDirection.sub(player.up);
			action = move;
		}

		actionQueueAdd(action);

		// Prevent jumping/fighting gravity when looking up
		if (moveDirection.y > 0) {
			moveDirection.y = 0;
		}
		// Check if we should jump or climb
		if (keys.containsKey(GameSettings.JUMP) && player.canClimb) {
			moveDirection.y = 1f;
			jumpKeyReleased = false;
			keepJumping = false;
			actionQueueAdd(PlayerAction.CLIMB);

		} else if ((keys.containsKey(GameSettings.JUMP) && jumpKeyReleased)) {
			jumpKeyReleased = false;
			keepJumping = true;
			moveDirection.y = 0;
			actionQueueAdd(PlayerAction.JUMP);

			Timer.schedule(new Task() {
				@Override
				public void run() {
					keepJumping = false;
				}
			}, GameSettings.PLAYER_JUMP_TIME);

		} else if (keys.containsKey(GameSettings.JUMP) && keepJumping) {
			actionQueueAdd(PlayerAction.JUMP);
			moveDirection.y = 0;
		}
	}
}