package agency.highlysuspect.apathy.rule.predicate;

import agency.highlysuspect.apathy.Apathy119;
import agency.highlysuspect.apathy.hell.rule.CoolGsonHelper;
import agency.highlysuspect.apathy.hell.rule.PartialSerializer;
import agency.highlysuspect.apathy.rule.CodecUtil;
import agency.highlysuspect.apathy.rule.Specs;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public record PartialSpecAny(Set<PartialSpec<?>> others) implements PartialSpec<PartialSpecAny> {
	@Override
	public PartialSpec<?> optimize() {
		Set<PartialSpec<?>> loweredSpecs = others.stream().map(PartialSpec::optimize).collect(Collectors.toSet());
		
		//If an always-true partial is present, surely this partial is also always true.
		if(loweredSpecs.stream().anyMatch(pred -> pred == PartialSpecAlways.TRUE)) return PartialSpecAlways.TRUE;
		
		//Always-false partial can be ignored.
		loweredSpecs.removeIf(pred -> pred == PartialSpecAlways.FALSE);
		
		//If there are no partials left, uhh
		if(loweredSpecs.size() == 0) return PartialSpecAlways.FALSE;
		
		//If there is one partial left, we don't need the wrapping
		if(loweredSpecs.size() == 1) return loweredSpecs.iterator().next();
		
		return new PartialSpecAny(loweredSpecs);
	}
	
	@Override
	public Partial build() {
		Partial[] arrayParts = others.stream().map(PartialSpec::build).toArray(Partial[]::new);
		return (attacker, defender) -> {
			for(Partial p : arrayParts) {
				if(p.test(attacker, defender)) return true;
			}
			return false;
		};
	}
	
	@Override
	public PartialSerializer<PartialSpecAny> getSerializer() {
		return Serializer.INSTANCE;
	}
	
	public static class Serializer implements PartialSerializer<PartialSpecAny> {
		public static final Serializer INSTANCE = new Serializer();
		
		@Override
		public JsonObject write(PartialSpecAny pred, JsonObject json) {
			json.add("predicates", pred.others.stream().map(Apathy119.instance119::writePartial).collect(CoolGsonHelper.toJsonArray()));
			return json;
		}
		
		@Override
		public PartialSpecAny read(JsonObject json) {
			Set<PartialSpec<?>> partials = new HashSet<>();
			JsonArray partialsArray = json.getAsJsonArray("predicates");
			for(JsonElement e : partialsArray) partials.add(Apathy119.instance119.readPartial(e));
			return new PartialSpecAny(partials);
		}
	}
	
	///CODEC HELL///
	
	@Deprecated(forRemoval = true)
	public static final Codec<PartialSpecAny> CODEC = RecordCodecBuilder.create(i -> i.group(
		CodecUtil.setOf(Specs.PREDICATE_SPEC_CODEC).fieldOf("predicates").forGetter(x -> x.others)
	).apply(i, PartialSpecAny::new));
	
	@Deprecated(forRemoval = true)
	@Override
	public Codec<? extends PartialSpec<?>> codec() {
		return CODEC;
	}
}
