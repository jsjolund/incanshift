package incanshift.player;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;
import incanshift.world.GameSettings;

import java.util.Random;

public class PlayerSound {

	private static Random rand = new Random();
	AssetManager assets;
	Array<Sound> soundJump = new Array<Sound>();
	Array<Sound> soundMaskHit = new Array<Sound>();
	Array<Sound> soundWallHit = new Array<Sound>();
	private Sound soundGrapple;
	private Sound soundShoot;
	private Sound soundRun;
	private Sound soundWalk;
	private Sound soundMove;
	private long soundMoveId = -1;
	private long soundGrappleId = -1;

	public PlayerSound() {
		assets = new AssetManager();

		assets.load("sound/jump.ogg", Sound.class);
		assets.load("sound/jump1.ogg", Sound.class);
		assets.load("sound/jump2.ogg", Sound.class);
		assets.load("sound/jump3.ogg", Sound.class);
		assets.load("sound/jump4.ogg", Sound.class);

		assets.load("sound/mask_hit1.ogg", Sound.class);
		assets.load("sound/mask_hit2.ogg", Sound.class);
		assets.load("sound/mask_hit3.ogg", Sound.class);
		assets.load("sound/mask_hit4.ogg", Sound.class);

		assets.load("sound/wall_hit1.ogg", Sound.class);
		assets.load("sound/wall_hit2.ogg", Sound.class);
		assets.load("sound/wall_hit3.ogg", Sound.class);
		assets.load("sound/wall_hit4.ogg", Sound.class);
		assets.load("sound/wall_hit5.ogg", Sound.class);
		assets.load("sound/wall_hit6.ogg", Sound.class);
		assets.load("sound/wall_hit7.ogg", Sound.class);

		assets.load("sound/mask_pickup.ogg", Sound.class);

		assets.load("sound/shoot.ogg", Sound.class);
		assets.load("sound/hook.ogg", Sound.class);
		assets.load("sound/run.ogg", Sound.class);
		assets.load("sound/walk.ogg", Sound.class);

		assets.finishLoading();

		soundJump.add(assets.get("sound/jump1.ogg", Sound.class));
		soundJump.add(assets.get("sound/jump2.ogg", Sound.class));
		soundJump.add(assets.get("sound/jump3.ogg", Sound.class));
		soundJump.add(assets.get("sound/jump4.ogg", Sound.class));

		soundMaskHit.add(assets.get("sound/mask_hit1.ogg", Sound.class));
		soundMaskHit.add(assets.get("sound/mask_hit2.ogg", Sound.class));
		soundMaskHit.add(assets.get("sound/mask_hit3.ogg", Sound.class));
		soundMaskHit.add(assets.get("sound/mask_hit4.ogg", Sound.class));

		soundWallHit.add(assets.get("sound/wall_hit1.ogg", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit2.ogg", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit3.ogg", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit4.ogg", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit5.ogg", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit6.ogg", Sound.class));
		soundWallHit.add(assets.get("sound/wall_hit7.ogg", Sound.class));

		soundShoot = assets.get("sound/shoot.ogg", Sound.class);
		soundGrapple = assets.get("sound/hook.ogg", Sound.class);
		soundRun = assets.get("sound/run.ogg", Sound.class);
		soundWalk = assets.get("sound/walk.ogg", Sound.class);
		soundMove = soundWalk;

	}

	private static int randInt(int min, int max) {
		return rand.nextInt((max - min) + 1) + min;
	}

	public void grapple() {
		soundGrappleId = soundGrapple.play(0.5f * GameSettings.SOUND_VOLUME);
		soundGrapple.setPitch(soundGrappleId, 2f);
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
		soundMoveId = -1;
	}

	public void move(boolean run) {
		if (soundMoveId == -1) {
			soundMove.stop(soundMoveId);
			soundMoveId = (run) ? soundRun.loop(4.0f * GameSettings.SOUND_VOLUME)
					: soundWalk.loop(4.0f * GameSettings.SOUND_VOLUME);
		}
	}

	public void shoot() {
		soundShoot.play(0.1f * GameSettings.SOUND_VOLUME);
	}

}
