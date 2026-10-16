package frootloops.versus.mixin.mobs.hostile.illager;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Vex.class)
public abstract class VexMixin extends Monster {
    protected VexMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void decreaseHealth(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, SpawnGroupData entityData, CallbackInfoReturnable<SpawnGroupData> cir) {
        AttributeInstance instanceHP = this.getAttributes().getInstance(Attributes.MAX_HEALTH);
        if (instanceHP != null) {
            instanceHP.setBaseValue(8.0D);
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    public float getPickRadius() {
        return 0.5F;
    }
}