package frootloops.versus.mixin.mobs.hostile.illager;

import frootloops.versus.mod.mobs.hostile.overworld.FleeAttackerAndHealGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Witch.class)
public abstract class WitchMixin extends Raider {

    protected WitchMixin(EntityType<? extends Raider> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "registerGoals", at = @At("HEAD"))
    private void witchesCanHeal(CallbackInfo ci) {
        this.goalSelector.addGoal(1, new FleeAttackerAndHealGoal<>(this, 1));
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, EntitySpawnReason spawnReason) {
        if(spawnReason != EntitySpawnReason.NATURAL) return true;

        BlockPos pos = this.blockPosition();
        int y = pos.getY();
        if (y < 32) return false;
        if (y < 56 && world.getBrightness(LightLayer.SKY, pos) < 4) return false;
        return true;
    }
}