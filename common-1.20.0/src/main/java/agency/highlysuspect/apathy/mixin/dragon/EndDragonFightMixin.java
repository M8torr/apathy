package agency.highlysuspect.apathy.mixin.dragon;

import agency.highlysuspect.apathy.EndDragonFightExt;
import agency.highlysuspect.apathy.core.Apathy;
import agency.highlysuspect.apathy.core.CoreBossOptions;
import agency.highlysuspect.apathy.core.etc.PortalInitialState;
import agency.highlysuspect.apathy.core.wrapper.DragonDuck;
import agency.highlysuspect.apathy.coreplusminecraft.ApathyPlusMinecraft;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("SameParameterValue")
@Mixin(EndDragonFight.class)
public abstract class EndDragonFightMixin {
	//a zillion shadows
	@Shadow @Final private Predicate<Entity> validPlayer;
	@Shadow @Final private ServerLevel level;
	@Shadow @Final private BlockPos origin;
	@Shadow @Final private ObjectArrayList<Integer> gateways;
	@Shadow private boolean dragonKilled;
	@Shadow private boolean previouslyKilled;
	@Shadow private BlockPos portalLocation;
	@Shadow private List<EndCrystal> respawnCrystals;
	
	@Shadow protected abstract boolean isArenaLoaded();
	@Shadow protected abstract void spawnNewGateway();
	@Shadow protected abstract void spawnExitPortal(boolean previouslyKilled);
	
	//my additions
	
	@Unique EndDragonFightExt ext;
	
	@Unique private boolean apathyIsManagingTheInitialPortalVanillaDontLookPlease = false;
	
	@Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;JLnet/minecraft/world/level/dimension/end/EndDragonFight$Data;Lnet/minecraft/core/BlockPos;)V", at = @At("TAIL"))
	void apathy$onInit(ServerLevel slevel, long $$1, EndDragonFight.Data $$2, BlockPos $$3, CallbackInfo ci) {
		ext = EndDragonFightExt.get(slevel);
	}
	
	//runs BEFORE vanilla tick().
	@Inject(method = "tick", at = @At("HEAD"))
	void apathy$dontTick(CallbackInfo ci) {
		//Vanilla tick() adds a chunk ticket that loads a region around the main End Island if there's anyone standing nearby.
		if(!isArenaLoaded()) return;
		
		//First-run tasks.
		if(!ext.hasCompletedInitialSetup()) {
			PortalInitialState portalInitialState = Apathy.instance.bossCfg.get(CoreBossOptions.portalInitialState);
			
			//1. If the End Portal was requested to be open by default, honor that.
			if(portalInitialState.isOpenByDefault()) {
				//boolean prop is "whether it's open or not".
				//this has computeIfAbsent semantics regarding the position of the portal - if the portal position is not already known,
				//it is computed from the heightmap (which is totally busted if !isArenaLoaded(), btw)
				spawnExitPortal(true);
				
				//styled after setDragonKilled
				if(portalInitialState.hasEgg()) {
					level.setBlockAndUpdate(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, EndPodiumFeature.getLocation(origin)), Blocks.DRAGON_EGG.defaultBlockState());
				}
			}
			
			//2. If any End Gateways were requested to be open by default, generate those too.
			int initialEndGatewayCount = Apathy.instance.bossCfg.get(CoreBossOptions.initialEndGatewayCount);
			for(int i = 0; i < initialEndGatewayCount; i++) {
				spawnNewGateway();
			}
			
			ext.markInitialSetupCompleted();
		}
		
		//3. Handle the ticker for the ResummonSequence.SPAWN_GATEWAY mechanic.
		if(portalLocation != null && ext.tickTimer()) doGatewaySpawn();
		
		//4. Handle simulacra advancements.
		boolean simulacra = Apathy.instance.bossCfg.get(CoreBossOptions.simulacraDragonAdvancements);
		boolean startCalm = Apathy.instance.bossCfg.get(CoreBossOptions.dragonInitialState).isCalm();
		if(simulacra && startCalm) {
			//this grants the "Free the End" advancement, in a kind of clunky way
			EnderDragon rarrrh = EntityType.ENDER_DRAGON.create(level);
			for(ServerPlayer player : level.getPlayers(validPlayer)) {
				CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(player, rarrrh, ApathyPlusMinecraft.instanceMinecraft.comicalAnvilSound(rarrrh));
			}
		}
	}
	
	//wait wait gimme a sec, i can explain
	@Inject(method = "scanState", at = @At("HEAD"))
	void apathy$startScanningState(CallbackInfo ci) {
		apathyIsManagingTheInitialPortalVanillaDontLookPlease = Apathy.instance.bossCfg.get(CoreBossOptions.portalInitialState) != PortalInitialState.CLOSED;
	}
	
	@Inject(method = "scanState", at = @At("RETURN"))
	void apathy$finishScanningState(CallbackInfo ci) {
		apathyIsManagingTheInitialPortalVanillaDontLookPlease = false;
		
		//scanState is called ONCE, EVER, the very first time any player loads the End. It is never called again.
		//(see: the needsStateScanning variable.)
		//It is also called before vanilla code spawns the initial Ender Dragon.
		//This is the perfect time to set the magic "do not automatically spawn an enderdragon" variable if the
		//player has requested for the initial dragon to be removed.
		if(Apathy.instance.bossCfg.get(CoreBossOptions.dragonInitialState).isCalm()) {
			dragonKilled = true; //This is the magic variable.
			previouslyKilled = true;
		}
	}
	
	//the SUPER AWESOME ULTRA TURBO MEGA HACK:
	//so if Apathy creates an already-opened End portal, it tends to confuse the shit out of the vanilla scanState logic
	//it takes the existence of any End Portal block entities at all to mean "the dragon was already killed" and it does not
	//spawn a dragon on first login. Because "already opened end portal" + "dragon" is a valid setup in apathy, i need to bop
	//this shit on the head, the solution is to prevent endportals from being discovered in scanState.
	@Inject(method = "hasActiveExitPortal", at = @At("HEAD"), cancellable = true)
	void apathy$bopActiveExitPortal(CallbackInfoReturnable<Boolean> cir) {
		if(apathyIsManagingTheInitialPortalVanillaDontLookPlease) cir.setReturnValue(false);
	}
	
	@Inject(method = "createNewDragon", at = @At("RETURN"))
	void apathy$whenCreatingDragon(CallbackInfoReturnable<EnderDragon> cir) {
		if(!previouslyKilled && Apathy.instance.bossCfg.get(CoreBossOptions.dragonInitialState).isPassive()) {
			((DragonDuck) cir.getReturnValue()).apathy$disallowAttackingPlayers();
		}
	}
	
	//tryRespawn handles detecting the 4 end crystals by the exit portal.
	//If there are four, respawnDragon gets called with the list of end crystals and actually summons the boss.
	@Inject(method = "respawnDragon", at = @At("HEAD"), cancellable = true)
	void apathy$whenBeginningRespawnSequence(List<EndCrystal> crystals, CallbackInfo ci) {
		//Nothing to do.
		switch(Apathy.instance.bossCfg.get(CoreBossOptions.resummonSequence)) {
			case DEFAULT: break;
			case SPAWN_GATEWAY:
				tryEnderCrystalGateway(crystals);
				//fall through
			case DISABLED: 
				ci.cancel();
		}
	}
	
	@Unique private void tryEnderCrystalGateway(List<EndCrystal> crystalsAroundEndPortal) {
		if(!ext.gatewayTimerRunning()) {
			BlockPos pos = gatewayDryRun();
			if(pos != null) {
				BlockPos downABit = pos.below(2); //where the actual gateway block will be
				for(EndCrystal crystal : crystalsAroundEndPortal) {
					crystal.setBeamTarget(downABit);
				}
				
				this.respawnCrystals = crystalsAroundEndPortal;
				ext.setGatewayTimer(100); //5 seconds
			}
		}
	}
	
	//The end of the "spawn gateway" cutscene
	@Unique private void doGatewaySpawn() {
		spawnNewGateway(); //Actually generate it now
		
		//Blow up the crystals located on the end portal.
		//(Yes, this means you can smuggle them away with a piston, just like vanilla lol.)
		BlockPos exitPos = portalLocation;
		BlockPos oneAboveThat = exitPos.above();
		for(Direction d : Direction.Plane.HORIZONTAL) {
			for(EndCrystal crystal : this.level.getEntitiesOfClass(EndCrystal.class, new AABB(oneAboveThat.relative(d, 2)))) {
				crystal.setBeamTarget(null);
				ApathyPlusMinecraft.instanceMinecraft.explodeNoBlockInteraction(level, crystal, crystal.getX(), crystal.getY(), crystal.getZ(), 6f);
				crystal.discard();
			}
		}
		
		//Grant the advancement for resummoning the Ender Dragon (close enough)
		if(Apathy.instance.bossCfg.get(CoreBossOptions.simulacraDragonAdvancements)) {
			EnderDragon dummy = EntityType.ENDER_DRAGON.create(level);
			for(ServerPlayer player : level.getPlayers(validPlayer)) {
				CriteriaTriggers.SUMMONED_ENTITY.trigger(player, dummy);
			}
		}
	}
	
	//Copypaste of "createNewEndGateway", but simply returns the BlockPos instead of actually creating a gateway there.
	//Also peeks the gateway list with "get" instead of popping with "remove".
	@Unique private @Nullable BlockPos gatewayDryRun() {
		if(this.gateways.isEmpty()) return null;
		else {
			int i = this.gateways.get(this.gateways.size() - 1);
			int j = Mth.floor(96.0D * Math.cos(2.0D * (-3.141592653589793D + 0.15707963267948966D * (double)i)));
			int k = Mth.floor(96.0D * Math.sin(2.0D * (-3.141592653589793D + 0.15707963267948966D * (double)i)));
			return new BlockPos(j, 75, k);
		}
	}
}
