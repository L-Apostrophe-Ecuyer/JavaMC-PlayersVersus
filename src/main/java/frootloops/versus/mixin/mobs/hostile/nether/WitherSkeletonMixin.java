package frootloops.versus.mixin.mobs.hostile.nether;

import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.environment.WorldTime;
import frootloops.versus.mod.mobs.hostile.nether.WildfireEntity;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherSkeleton.class)
public class WitherSkeletonMixin extends Monster {
    protected WitherSkeletonMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if(target instanceof Monster) return;
        else super.setTarget(target);
    }

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void decreaseHealth(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, SpawnGroupData entityData, CallbackInfoReturnable<SpawnGroupData> cir) {
        this.getAttributes().getInstance(Attributes.MAX_HEALTH).setBaseValue(32.0D);
        this.setHealth(this.getMaxHealth());

        AttributeInstance instanceKnockbackRes = this.getAttributes().getInstance(Attributes.KNOCKBACK_RESISTANCE);
        if (instanceKnockbackRes != null) instanceKnockbackRes.setBaseValue(0.5D);

        this.getAttributes().getInstance(Attributes.ATTACK_DAMAGE).setBaseValue(0.5D);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, EntitySpawnReason spawnReason) {
        boolean result = this.getWalkTargetValue(this.blockPosition(), world) >= 0.0F;
        if(result && spawnReason != EntitySpawnReason.MOB_SUMMONED && WorldTime.ultraWarm(world) && world.getBlockState(this.blockPosition().below()).getSoundType() == SoundType.NETHER_BRICKS) {

            // Rarely spawn a Wildfire:
            // By key: a Holder never equalled a ResourceKey, so this was always false before 26.3.
            boolean isInSoulSandValley = this.level().getBiome(this.blockPosition()).is(Biomes.SOUL_SAND_VALLEY);
            if(this.getRandom().nextInt(isInSoulSandValley ? 6 : 12) == 0) {
                WildfireEntity wildfireEntity = new WildfireEntity(ModEntities.WILDFIRE, this.level());
                wildfireEntity.setPos(this.position());
                world.addFreshEntity(wildfireEntity);
            }
        }
        return result;
    }
}
