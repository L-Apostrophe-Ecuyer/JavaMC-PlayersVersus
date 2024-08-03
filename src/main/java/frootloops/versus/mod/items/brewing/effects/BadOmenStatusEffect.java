package frootloops.versus.mod.items.brewing.effects;

import frootloops.versus.VersusMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.Difficulty;
import net.minecraft.world.poi.PointOfInterestStorage;

public class BadOmenStatusEffect extends StatusEffect  {

    private static final StatusEffectCategory category = StatusEffectCategory.HARMFUL;
    private static final int color = 745784;

    public BadOmenStatusEffect(String id) {
        super(category, color, ParticleTypes.EFFECT);
        this.applySound(SoundEvents.EVENT_MOB_EFFECT_BAD_OMEN);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        VersusMod.MOD_LOGGER.warn("Checking for raids.");
        if (entity instanceof ServerPlayerEntity serverPlayer) {
            if(serverPlayer.isSpectator()) return false;
            ServerWorld serverWorld;
            if((serverWorld = serverPlayer.getServerWorld()).getDifficulty() == Difficulty.PEACEFUL || !serverPlayer.getWorld().getDimension().hasRaids()) return true;

            boolean isPlayerInsideVillage = serverWorld.isNearOccupiedPointOfInterest(serverPlayer.getBlockPos());
            boolean isPlayerInsideTheirBase = !isPlayerInsideVillage && (serverPlayer.getSpawnPointPosition().isWithinDistance(serverPlayer.getBlockPos(), 48)) && !serverWorld.getPointOfInterestStorage().getInCircle(poiType -> poiType.isIn(PointOfInterestTypeTags.ACQUIRABLE_JOB_SITE), serverPlayer.getBlockPos(), 48, PointOfInterestStorage.OccupationStatus.ANY).toList().isEmpty();

            VersusMod.MOD_LOGGER.warn("Raid attempt. Is player inside their base? " + isPlayerInsideTheirBase);

            if ((isPlayerInsideVillage || isPlayerInsideTheirBase) && serverWorld.getRaidAt(serverPlayer.getBlockPos()) == null) {
                if(isPlayerInsideVillage) serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RAID_OMEN, 600, amplifier));
                else serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RAID_OMEN, 3000, 0));
                serverPlayer.setStartRaidPos(serverPlayer.getBlockPos());
                return false;
            }
        }
        return true;
    }
}
