package agency.highlysuspect.apathy.config;

import agency.highlysuspect.apathy.Apathy119;
import agency.highlysuspect.apathy.hell.TriState;
import agency.highlysuspect.apathy.config.annotation.AtLeast;
import agency.highlysuspect.apathy.config.annotation.Comment;
import agency.highlysuspect.apathy.config.annotation.Example;
import agency.highlysuspect.apathy.config.annotation.NoDefault;
import agency.highlysuspect.apathy.config.annotation.Note;
import agency.highlysuspect.apathy.config.annotation.Section;
import agency.highlysuspect.apathy.config.annotation.Use;
import agency.highlysuspect.apathy.hell.ApathyHell;
import agency.highlysuspect.apathy.rule.Rule;
import agency.highlysuspect.apathy.rule.RuleSpecAlways;
import agency.highlysuspect.apathy.rule.RuleSpecChain;
import agency.highlysuspect.apathy.rule.RuleSpecJson;
import agency.highlysuspect.apathy.rule.RuleSpecPredicated;
import agency.highlysuspect.apathy.rule.RuleSpec;
import agency.highlysuspect.apathy.rule.predicate.AttackerIsBossPredicateSpec;
import agency.highlysuspect.apathy.rule.predicate.AttackerIsPredicateSpec;
import agency.highlysuspect.apathy.rule.predicate.AttackerTaggedWithPredicateSpec;
import agency.highlysuspect.apathy.rule.predicate.DefenderInPlayerSetPredicateSpec;
import agency.highlysuspect.apathy.rule.predicate.DifficultyIsPredicateSpec;
import agency.highlysuspect.apathy.rule.predicate.RevengeTimerPredicateSpec;
import com.google.common.collect.ImmutableList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "CanBeFinal"})
public class MobConfig extends Config {
	protected static final int CURRENT_CONFIG_VERSION = 0;
	
	protected transient RuleSpec ruleSpec;
	protected transient Rule rule;
	
	@SuppressWarnings("BooleanMethodIsAlwaysInverted") //But it makes more sense that way!
	public boolean allowedToTargetPlayer(Mob attacker, ServerPlayer player) {
		if(attacker.level.isClientSide) throw new IllegalStateException("Do not call on the client, please");
		
		TriState result = rule.apply(attacker, player);
		if(result != TriState.DEFAULT) return result.get();
		else return fallthrough;
	}
	
	// values below //
	
	@NoDefault protected int configVersion = CURRENT_CONFIG_VERSION;
	
	//////////////////////////
	@Section("Nuclear Option")
	//////////////////////////
	
	@Comment({
		"If set to 'true', no mob will ever attack anyone.",
		"Use this option if you don't want to deal with the rest of the config file."
	})
	public boolean nuclearOption = false;
	
	///////////////////////////////
	@Section("Built In Rule Order")
	///////////////////////////////
	
	@Comment({
		"Which order should the rules in this config file be evaluated in?",
		"Comma-separated list built out of any or all of the following keywords, in any order:",
		"json, difficulty, boss, mobSet, tagSet, playerSet, revenge"
	})
	@Note("If a rule is not listed in the rule order, it will not be checked.")
	@Example("difficulty, revenge, playerSet")
	@Use("stringList")
	public List<String> ruleOrder = ImmutableList.of("json", "difficulty", "boss", "mobSet", "tagSet", "playerSet", "revenge");
	
	///////////////////////////
	@Section("Difficulty Rule")
	///////////////////////////
	
	@Comment({
		"Comma-separated list of difficulties.",
	})
	@Example("easy, normal")
	@Use("difficultySet")
	public Set<Difficulty> difficultySet = Collections.emptySet();
	
	@Comment({
		"What happens when the current world difficulty appears in difficultySet?",
		"May be one of:",
		"allow - Every mob is always allowed to attack everyone.",
		"deny  - No mob is ever allowed to attack anyone.",
		"pass  - Defer to the next rule.",
	})
	@Use("triStateAllowDenyPass")
	public TriState difficultySetIncluded = TriState.DEFAULT;
	
	@Comment({
		"What happens when the current world difficulty does *not* appear in difficultySet?",
		"May be one of:",
		"allow - Every mob is always allowed to attack everyone.",
		"deny  - No mob is ever allowed to attack anyone.",
		"pass  - Defer to the next rule.",
	})
	@Use("triStateAllowDenyPass")
	public TriState difficultySetExcluded = TriState.DEFAULT;
	
	/////////////////////
	@Section("Boss Rule")
	/////////////////////
	
	@Comment({
		"What happens when the attacker is a boss?",
		"'Bossness' is defined by inclusion in the 'apathy:bosses' tag.",
		"May be one of:",
		"allow - Every boss is allowed to attack everyone.",
		"deny  - No boss is allowed to attack anyone.",
		"pass  - Defer to the next rule."
	})
	@Note("If the current attacker is *not* a boss, always passes to the next rule.")
	@Use("triStateAllowDenyPass")
	public TriState boss = TriState.TRUE;
	
	////////////////////////
	@Section("Mob Set Rule")
	////////////////////////
	
	@Comment("A comma-separated set of mob IDs.")
	@Example("minecraft:creeper, minecraft:spider")
	@Use("entityTypeSet")
	public Set<EntityType<?>> mobSet = Collections.emptySet();
	
	@Comment({
		"What happens when the attacker's entity ID appears in mobSet?",
		"May be one of:",
		"allow - The mob will be allowed to attack the player.",
		"deny  - The mob will not be allowed to attack the player.",
		"pass  - Defer to the next rule."
	})
	@Use("triStateAllowDenyPass")
	public TriState mobSetIncluded = TriState.DEFAULT;
	
	@Comment({
		"What happens when the attacker's entity ID does *not* appear in mobSet?",
		"May be one of:",
		"allow - The mob will be allowed to attack the player.",
		"deny  - The mob will not be allowed to attack the player.",
		"pass  - Defer to the next rule."
	})
	@Use("triStateAllowDenyPass")
	public TriState mobSetExcluded = TriState.DEFAULT;
	
	////////////////////////////
	@Section("Tag Set Rule")
	////////////////////////////
	
	@Comment("A comma-separated set of entity type tags.")
	@Example("minecraft:raiders, some_datapack:some_tag")
	@Use("entityTypeTagSet")
	public Set<TagKey<EntityType<?>>> tagSet = Collections.emptySet();
	
	@Comment({
		"What happens when the attacker is tagged with one of the tags in mobTagSet?",
		"May be one of:",
		"allow - The mob will be allowed to attack the player.",
		"deny  - The mob will not be allowed to attack the player.",
		"pass  - Defer to the next rule."
	})
	@Use("triStateAllowDenyPass")
	public TriState tagSetIncluded = TriState.DEFAULT;
	
	@Comment({
		"What happens when the attacker is *not* tagged with one of the tags in mobTagSet?",
		"May be one of:",
		"allow - The mob will be allowed to attack the player.",
		"deny  - The mob will not be allowed to attack the player.",
		"pass  - Defer to the next rule."
	})
	@Use("triStateAllowDenyPass")
	public TriState tagSetExcluded = TriState.DEFAULT;
	
	///////////////////////////
	@Section("Player Set Rule")
	///////////////////////////
	
	@Comment({
		"The name of a set of players.",
		"If this option is not provided, a player set is not created, and this whole rule always passes.",
	})
	@Use("optionalString")
	public Optional<String> playerSetName = Optional.of("no-mobs");
	
	@Comment({
		"If 'true', players can add themselves to the set, using '/apathy set join <playerListName>'.",
		"If 'false', only an operator can add them to the set, using '/apathy set-admin join <selector> <playerListName>'."
	})
	public boolean playerSetSelfSelect = true;
	
	@Comment({
		"What happens when a mob tries to attack someone who appears in the playerSet?",
		"May be one of:",
		"allow - The mob is allowed to attack the player.",
		"deny  - The mob is not allowed to attack the player.",
		"pass  - Defer to the next rule."
	})
	@Use("triStateAllowDenyPass")
	public TriState playerSetIncluded = TriState.FALSE;
	
	@Comment({
		"What happens when a mob tries to attack someone who does *not* appear in the playerSet?",
		"May be one of:",
		"allow - The mob is allowed to attack the player.",
		"deny  - The mob is not allowed to attack the player.",
		"pass  - Defer to the next rule."
	})
	@Use("triStateAllowDenyPass")
	public TriState playerSetExcluded = TriState.DEFAULT;
	
	////////////////////////
	@Section("Revenge Rule")
	////////////////////////
	
	@Comment({
		"For how many ticks is a mob allowed to retaliate after being attacked?",
		"Set to -1 to disable this 'revenge' mechanic.",
		"When the timer expires, defers to the next rule."
	})
	@Note({
		"The exact duration of the attack may be up to (<revengeTimer> + <recheckInterval>) ticks.",
		"Btw, the original mod had an option for 'eternal revenge', with an uncapped timer.",
		"I didn't port that, but the maximum value of the timer is " + Long.MAX_VALUE + " ticks.",
		"Make of that information what you will ;)"
	})
	@AtLeast(minLong = -1)
	public long revengeTimer = -1;
	
	////////////////////////////
	@Section("Last Resort Rule")
	////////////////////////////
	
	@Comment({
		"If absolutely none of the previous rules applied, what happens?",
		"May be one of:",
		"allow - By default, mobs are allowed to attack players.",
		"deny  - By default, mobs are not allowed to attack players.",
		"May *not* be set to 'pass'."
	})
	@Use("boolAllowDeny")
	public boolean fallthrough = true;
	
	@Override
	public Config upgrade() {
		super.upgrade();
		
		if(unknownKeys != null) {
			//There haven't been any breaking changes to the config yet, so all unknown keys are probably a mistake.
			unknownKeys.forEach((key, value) -> ApathyHell.instance.log.warn("Unknown config field: " + key));
			//We don't need to keep track of them anymore.
			unknownKeys = null;
		}
		
		configVersion = CURRENT_CONFIG_VERSION;
		return this;
	}
	
	@Override
	public Config finish() {
		super.finish();
		
		if(nuclearOption) {
			ApathyHell.instance.log.info("Nuclear option enabled - Ignoring ALL rules in the config file");
			ruleSpec = RuleSpecAlways.ALWAYS_DENY;
		} else {
			ArrayList<RuleSpec<?>> ruleSpecList = new ArrayList<>();
			for(String ruleName : ruleOrder) {
				switch(ruleName.trim().toLowerCase(Locale.ROOT)) {
					case "json"       -> ruleSpecList.add(new RuleSpecJson());
					case "difficulty" -> ruleSpecList.add(new RuleSpecPredicated(difficultySetIncluded, difficultySetExcluded, new DifficultyIsPredicateSpec(difficultySet)));
					case "boss"       -> ruleSpecList.add(new RuleSpecPredicated(boss, TriState.DEFAULT, new AttackerIsBossPredicateSpec()));
					case "mobset"     -> ruleSpecList.add(new RuleSpecPredicated(mobSetIncluded, mobSetExcluded, new AttackerIsPredicateSpec(mobSet)));
					case "tagset"     -> ruleSpecList.add(new RuleSpecPredicated(tagSetIncluded, tagSetExcluded, new AttackerTaggedWithPredicateSpec(tagSet)));
					case "playerset"  -> playerSetName.ifPresent(s -> ruleSpecList.add(new RuleSpecPredicated(playerSetIncluded, playerSetExcluded, new DefenderInPlayerSetPredicateSpec(Collections.singleton(s)))));
					case "revenge"    -> ruleSpecList.add(RuleSpecPredicated.allowIf(new RevengeTimerPredicateSpec(revengeTimer)));
					default -> ApathyHell.instance.log.warn("Unknown rule " + ruleName + " listed in the ruleOrder config option.");
				}
			}
			
			ruleSpec = new RuleSpecChain(ruleSpecList);
		}
		
		if(Apathy119.instance119.generalConfig.debugBuiltinRule) ruleSpec.dump(ApathyHell.instance.configPath, "builtin-rule");
		if(Apathy119.instance119.generalConfig.runRuleOptimizer) {
			ruleSpec = ruleSpec.optimize();
			if(Apathy119.instance119.generalConfig.debugBuiltinRule) ruleSpec.dump(ApathyHell.instance.configPath, "builtin-rule-opt");
		}
		
		rule = ruleSpec.build();
		
		return this;
	}
}
