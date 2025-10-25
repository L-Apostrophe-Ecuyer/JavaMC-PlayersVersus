package frootloops.versus.mixin.mobs.hostile.illager;

import frootloops.versus.mod.mobs.hostile.overworld.FleeAttackerAndHealGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitchEntity.class)
public abstract class WitchMixin extends RaiderEntity {

    protected WitchMixin(EntityType<? extends RaiderEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("HEAD"))
    private void witchesCanHeal(CallbackInfo ci) {
        this.goalSelector.add(1, new FleeAttackerAndHealGoal<>(this, 1));
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason != SpawnReason.NATURAL) return true;

        BlockPos pos = this.getBlockPos();
        int y = pos.getY();
        if (y < 32) return false;
        if (y < 56 && world.getLightLevel(LightType.SKY, pos) < 4) return false;
        return true;
    }
}