package incanshift.player;

/**
 * Created by user on 7/29/15.
 */
enum PlayerAction {
	STOP("stop"), WALK("walk"), RUN("run"),

	JUMP("jump"), FIRE("shoot"), USE("use"),

	FLY("fly"), HOOK("hook");

	private String name;

	PlayerAction(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}