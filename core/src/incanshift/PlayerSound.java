package incanshift;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;

public class PlayerSound {

	Sound soundJump;
	Sound soundBump;
	Sound soundShoot;
	Sound soundRun;
	Sound soundWalk;
	Sound soundMove;
	Sound soundClimb;

	long soundMoveId = -1;
	long soundClimbId = -1;

	public PlayerSound(AssetManager assets) {

		assets.finishLoading();
		soundJump = assets.get("sound/jump.wav", Sound.class);
		soundBump = assets.get("sound/bump.wav", Sound.class);
		soundShoot = assets.get("sound/shoot.wav", Sound.class);
		soundRun = assets.get("sound/run.wav", Sound.class);
		soundWalk = assets.get("sound/walk.wav", Sound.class);
		soundClimb = assets.get("sound/climb.wav", Sound.class);
		soundMove = soundWalk;

	}

	public void bump() {
		soundBump.play(1.0f);
	}

	public void halt() {
		soundMove.stop(soundMoveId);
		soundClimb.stop(soundClimbId);
		soundMoveId = -1;
		soundClimbId = -1;
	}

	public void jump() {
		soundJump.play(0.5f);
	}

	public void move(boolean run) {
		soundMove.stop(soundMoveId);
		soundMoveId = (run) ? soundRun.loop(4.0f) : soundWalk.loop(4.0f);
	}

	public void shoot() {
		soundShoot.play(0.5f);
	}

	public void climb() {
		soundMove.stop(soundMoveId);
		if (soundClimbId == -1) {
			soundClimbId = soundClimb.loop(1.0f);
		}
	}
}
