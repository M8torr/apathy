package agency.highlysuspect.apathy.platform.neoforge;

import agency.highlysuspect.apathy.core.Apathy;
import agency.highlysuspect.apathy.core.config.ConfigProperty;
import agency.highlysuspect.apathy.core.config.ConfigSchema;
import agency.highlysuspect.apathy.core.config.CookedConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.*;
import java.util.function.Supplier;

public class ForgeBackedConfig implements CookedConfig {
	public ForgeBackedConfig(Map<ConfigProperty<?>, Supplier<?>> liveConfig) {
		this.liveConfig = liveConfig;
	}
	
	//Behind these Suppliers are live ForgeConfigSpec.Builder objects. "Live" in the sense that Forge will keep them up to date.
	private final Map<ConfigProperty<?>, Supplier<?>> liveConfig;
	
	//NightConfig can't handle all the object types I want, so I map weirder types to string config options (in the bakery below.)
	//This means to realize the object I need to parse the string. I'd like to not have to do that every time I read an option.
	private final Map<ConfigProperty<?>, Object> cache = new IdentityHashMap<>();
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(ConfigProperty<T> key) {
		return (T) cache.computeIfAbsent(key, this::getUncached);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T getUncached(ConfigProperty<T> key) {
		Supplier<T> s = (Supplier<T>) liveConfig.get(key);
		if(s == null) return key.defaultValue();
		else {
			try {
				return s.get();
			} catch (Exception e) {
				Apathy.instance.log.error("Failed to parse option '" + key.name() + "': " + e.getMessage() + ". Using default value.", e);
				return key.defaultValue();
			}
		}
	}
	
	@Override
	public boolean refresh() {
		cache.clear();
		return true;
	}
	
	public static class Bakery implements ConfigSchema.Bakery {
		public Bakery(ModConfigSpec.Builder spec) {
			this.spec = spec;
		}
		
		public final ModConfigSpec.Builder spec;
		
		@Override
		public CookedConfig cook(ConfigSchema schema) {
			Map<ConfigProperty<?>, Supplier<?>> configGetters = new IdentityHashMap<>();
			
			spec.push("Uncategorized");
			
			schema.accept(new ConfigSchema.Visitor() {
				@Override
				public void visitSection(String section) {
					spec.pop();
					spec.push(section);
				}
				
				@SuppressWarnings("unchecked")
				@Override
				public <T> void visitOption(ConfigProperty<T> option) {
					List<String> comment = option.comment();
					if(comment.isEmpty()) spec.comment(" "); //forge will complain otherwise
					else {
						List<String> commentWithDefault = new ArrayList<>(comment.size() + 1);
						commentWithDefault.addAll(comment);
						//not part of forge's stock format for some weird reason!
						commentWithDefault.add("Default: " + option.write(option.defaultValue()));
						spec.comment(commentWithDefault.toArray(String[]::new));
					}
					
					//annoying part:
					T hmm = option.defaultValue();
					if(hmm instanceof Boolean && !option.name().equals("fallthrough") /* dumb hack for apathy-boss.toml */) {
						//Forge has weirdshit around booleans and its bad lmao, nightconfig can't do bools so forge has a wrapper function
						ModConfigSpec.BooleanValue forge = spec.define(option.name(), (boolean) hmm);
						configGetters.put(option, () -> {
							//Surprise!!!! Forge actually gives you a string option when you use this helper
							//I genuinely have no idea what is going on. Writing this any shorter still gave me classcastexceptions
							Object what = forge.get();
							String stringified = what.toString();
							boolean real = Boolean.parseBoolean(stringified);
							option.validate(option, (T) (Object) real);
							return real;
						});
					} else if(hmm instanceof Long) {
						//You'd think that if you passed Long.class into nightconfig, you'd get a config property that deserialized... longs.
						//Nope! You get one that deserializes integers. And there's another forge wrapper to correct for this deficiency
						ModConfigSpec.LongValue forge = spec.defineInRange(option.name(), (long) hmm, Long.MIN_VALUE, Long.MAX_VALUE);
						configGetters.put(option, () -> {
							Object what = forge.get();
							String stringified = what.toString();
							long real = Long.parseLong(stringified);
							option.validate(option, (T) (Object) real);
							return real;
						});
					} else if(hmm instanceof Integer) {
						//I just dont trust forge configs anymore honestly.
						//I hate this defineInRange function, it's such a shit kludge and it seems to always add a comment to the config file
						ModConfigSpec.IntValue forge = spec.defineInRange(option.name(), (int) hmm, Integer.MIN_VALUE, Integer.MAX_VALUE);
						configGetters.put(option, () -> {
							Object what = forge.get();
							String stringified = what.toString();
							int real = Integer.parseInt(stringified);
							option.validate(option, (T) (Object) real);
							return real;
						});
					} else if(hmm instanceof String) {
						ModConfigSpec.ConfigValue<String> forge = spec.define(option.name(), (String) hmm);
						configGetters.put(option, () -> {
							String s = forge.get();
							option.validate(option, (T) s);
							return s;
						});
					} else {
						//Forge config weirdstuff is definitely not able to handle this type by default.
						//Fall back to a string option, and do the stringifying/parsing ourselves.
						//This is not the best fallback for most options because the TOML format will quote strings
						//but users don't expect to have to quote options that are e.g. numbers.
						//so thats what all the crap above is about :sweat_smile:
						ModConfigSpec.ConfigValue<String> forge = spec.define(
							Collections.singletonList(option.name()),
							() -> option.write(option.defaultValue()), //<- stringify on the way in
							(Object thing) -> {
								try {
									T thingParsed = option.parse(thing.toString()); //<- parse on the way out
									option.validate(option, thingParsed);
									return true;
								} catch (Exception e) {
									return false;
								}
							},
							String.class
						);
						
						configGetters.put(option, () -> option.parse(forge.get())); //<- parse on the way out
					}
				}
			});
			
			return new ForgeBackedConfig(configGetters);
		}
	}
}
