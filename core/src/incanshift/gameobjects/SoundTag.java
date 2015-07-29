package incanshift.gameobjects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;

public class SoundTag {

	public static final String tag = "SoundTag";
	public float distance;
	public Vector3 position;
	public boolean finishedPlaying = false;
	public Sound sound;

	public SoundTag(String filePath, Vector3 position, float distance) {
		this.distance = distance;
		this.position = position;
		Gdx.app.debug(tag, "Loading sound " + filePath);
		FileHandle f = Gdx.files.internal(filePath);
		Gdx.app.debug(tag, "Sound exists? " + f.exists());
		sound = Gdx.audio.newSound(f);
		Gdx.app.debug(tag, "Sound is " + sound);

	}

	public void play() {
		sound.play();
		finishedPlaying = true;
	}
}
