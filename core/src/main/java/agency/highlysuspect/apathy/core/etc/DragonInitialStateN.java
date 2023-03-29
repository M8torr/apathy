package agency.highlysuspect.apathy.core.etc;

public enum DragonInitialStateN {
	DEFAULT,
	PASSIVE_DRAGON,
	CALM,
	;
	
	public boolean isPassive() {
		return this == PASSIVE_DRAGON;
	}
	
	public boolean isCalm() {
		return this == CALM;
	}
}