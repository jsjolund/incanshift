package incanshift.player;

import java.util.Random;

import incanshift.world.GameSettings;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;

public class PlayerSound {

	Array<Sound> soundJump = new Array<Sound>();
	Array<Sound> soundMaskHit = new Array<Sound>();
	Array<Sound> soundWallHit = new Array<Sound>();
	Sound soundShoot;
	Sound soundRun;
	Sound soundWalk;
	Sound soundMove;
	Sound soundClimb;

	long soundMoveId = -1;
	long soundClimbId = -1;

	public PlayerSound(AssetManager assets) {

		assets.finishLoading();
		soundJump.add(assets.get("sound/jump1.wav", Sound.class));
		soundJump.add(assets.get("sound/jump2.wav", Sound.class));
		soundJump.add(assets.get("sound/jump3.wav", Sound.class));
		soundJump.add(assets.get("sound/jump4.wav", Sound.class));

		soundMaskHit.add(assets.get("sound/mask_hit1.wav", Sound.class));
		soundMaskHit.add(assets.get("sound/mask_hit2.wav", Sound.class));
		soundMaskHit.add(assets.get("sound/mask_hit3.wav", Sound.class));
		soundMaskHit.add(assets.get("sound/mask_hit4.wav", Sound.class));

		soundWallHit.add(assets.get("sound/wall_hit1.wav", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit2.wav", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit3.wav", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit4.wav", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit5.wav", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit6.wav", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit7.wav", Sound.class));

		soundShoot = assets.get("sound/shoot.wav", Sound.class);
		soundRun = assets.get("sound/run.wav", Sound.class);
		soundWalk = assets.get("sound/walk.wav", Sound.class);
		soundClimb = assets.get("sound/climb.wav", Sound.class);
		soundMove = soundWalk;

	}

	private static Random rand = new Random();

	public static int randInt(int min, int max) {
		return rand.nextInt((max - min) + 1) + min;
	}

	public void maskHit() {
		soundMaskHit.get(randInt(0, soundMaskHit.size - 1)).play(
				2.0f * GameSettings.SOUND_VOLUME);
	}

	public void wallHit() {
		soundWallHit.get(randInt(0, soundWallHit.size - 1)).play(
				2.0f * GameSettings.SOUND_VOLUME);
	}

	public void jump() {
		soundJump.get(randInt(0, soundJump.size - 1)).play(
				1.0f * GameSettings.SOUND_VOLUME);
	}

	public void halt() {
		soundMove.stop(soundMoveId);
		soundClimb.stop(soundClimbId);
		soundMoveId = -1;
		soundClimbId = -1;
	}

	public void move(boolean run) {
		soundMove.stop(soundMoveId);
		soundMoveId = (run) ? soundRun.loop(4.0f * GameSettings.SOUND_VOLUME)
				: soundWalk.loop(4.0f * GameSettings.SOUND_VOLUME);
	}

	public void shoot() {
		soundShoot.play(0.1f * GameSettings.SOUND_VOLUME);
	}

	public void climb() {
		soundMove.stop(soundMoveId);
		if (soundClimbId == -1) {
			soundClimbId = soundClimb.loop(1.0f * GameSettings.SOUND_VOLUME);
		}
	}
}
