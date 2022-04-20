package agency.highlysuspect.apathy.config;

import agency.highlysuspect.apathy.config.annotation.AtLeast;
import agency.highlysuspect.apathy.config.annotation.AtMost;
import agency.highlysuspect.apathy.config.annotation.Comment;
import agency.highlysuspect.apathy.config.annotation.NoDefault;
import agency.highlysuspect.apathy.config.annotation.Note;
import agency.highlysuspect.apathy.config.annotation.Section;
import agency.highlysuspect.apathy.rule.CodecUtil;
import com.mojang.serialization.Codec;

import java.util.Locale;

@SuppressWarnings("CanBeFinal")
public class BossConfig extends Config {
	protected static final int CURRENT_CONFIG_VERSION = 1;
	@NoDefault protected int configVersion = CURRENT_CONFIG_VERSION;
	
	////////////////////////
	@Section("Ender Dragon")
	////////////////////////
	
	@Comment({
		"What is the initial state of the Ender Dragon in the End?",
		"If 'default', she will be present and attack players, just like the vanilla game.",
		"If 'passive_dragon', she will be present, but fly in a circle until a player provokes her first.",
		"If 'calm', the End will not contain an Ender Dragon by default."
	})
	@Note({
		"If you choose 'calm', you should also change the 'portalInitialState' setting, so it is possible to leave the End."
	})
	public DragonInitialState dragonInitialState = DragonInitialState.DEFAULT;
	
	@Comment({
		"What is the initial state of the End Portal in the center of the main End Island?",
		"If 'closed', it will not be usable until the first Ender Dragon dies, just like in vanilla.",
		"If 'open', it will already be open.",
		"If 'open_with_egg', it will already be open and a Dragon Egg will be present."
	})
	public PortalInitialState portalInitialState = PortalInitialState.CLOSED;
	
	@Comment("How many End Gateways will be available when first entering the End, without any Dragons having to die?")
	@AtLeast(minInt = 0)
	@AtMost(maxInt = 20) //EndDragonFight.GATEWAY_COUNT
	public int initialEndGatewayCount = 0;
	
	@Comment({
		"What happens when a player places four End Crystals onto the exit End Portal?",
		"If 'default', a new Ender Dragon will be summoned and she will attack the player, just like in vanilla.",
		"If 'spawn_gateway', the mechanic will be replaced with one that directly creates an End Gateway, with no fighting required.",
		"If 'disabled', nothing will happen.",
	})
	public ResummonSequence resummonSequence = ResummonSequence.DEFAULT;
	
	@Comment({
		"If 'true', and 'dragonInitialState' is 'calm', players automatically earn the Free the End advancement when visiting the End.",
		"If 'true', and 'resummonSequence' is 'spawn_gateway', players earn the advancement for resummoning the Dragon when using the spawn_gateway mechanic.",
		"Has no effects otherwise. Probably should be left as 'true'."
	})
	public boolean simulacraDragonAdvancements = true;
	
	@Comment({
		"Can the Dragon perform the 'strafe_player' or 'charging_player' actions?",
		"strafe_player is the one where she shoots a fireball.",
		"charge_player is the one where she tries to fly into you.",
		"If 'false', she will perform the 'landing_approach' action instead, which will cause her to perch on the portal."
	})
	public boolean dragonFlies = true;
	
	@Comment({
		"Can the Dragon perform the 'sitting_flaming' or 'sitting_attacking' actions?",
		"sitting_flaming is the one where she perches on the End portal and pours out a puddle of dragon's breath.",
		"sitting_attacking is when she roars at you.",
		"If 'false', she will perform the 'sitting_scanning' action instead, which will soon lead to her leaving her perch."
	})
	public boolean dragonSits = true;
	
	@Comment("Does the Dragon deal contact damage?")
	public boolean dragonDamage = true;
	
	@Comment("Does the Dragon knock back nearby entities, and damage them while she's sitting?")
	public boolean dragonKnockback = true;
	
	//////////////////
	@Section("Wither")
	//////////////////
	
	@Comment({
		"Set to 'true' to remove the Wither fight sequence.",
		"Building the Wither formation will spawn a Nether Star item and give you the advancement for killing the Wither."
	})
	public boolean noWither;
	
	@Comment("Is the Wither allowed to intentionally target players?")
	public boolean witherTargetsPlayers = true;
	
	@Comment("Is the Wither allowed to intentionally target non-players?")
	public boolean witherTargetsMobs = true;
	
	@Comment("Can the Wither fire black wither skulls?")
	public boolean blackWitherSkulls = true;
	
	@Comment("Can the Wither fire blue ('charged') wither skulls on Normal and Hard difficulty?")
	public boolean blueWitherSkulls = true;
	
	@Comment("Does the Wither break nearby blocks after it gets damaged?")
	public boolean witherBreaksBlocks = true;
	
	@Override
	protected Config upgrade() {
		if(configVersion == 0) {
			//noDragon option was replaced with a couple separate options for the Bliss modpack
			if(unknownKeys != null && unknownKeys.getOrDefault("noDragon", "false").trim().toLowerCase(Locale.ROOT).equals("true")) {
				dragonInitialState = DragonInitialState.CALM;
				portalInitialState = PortalInitialState.OPEN_WITH_EGG;
				resummonSequence = ResummonSequence.SPAWN_GATEWAY;
			}
		}
		
		return this;
	}
	
	public enum DragonInitialState {
		DEFAULT,
		PASSIVE_DRAGON,
		CALM,
		;
	}
	
	public enum PortalInitialState {
		CLOSED,
		OPEN,
		OPEN_WITH_EGG,
		;
		
		public boolean isOpenByDefault() {
			return this != CLOSED;
		}
		
		public boolean hasEgg() {
			return this == OPEN_WITH_EGG;
		}
	}
	
	public enum ResummonSequence {
		DEFAULT,
		SPAWN_GATEWAY,
		DISABLED,
		;
	}
}
