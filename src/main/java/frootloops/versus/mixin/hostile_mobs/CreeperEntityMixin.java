package frootloops.versus.mixin.hostile_mobs;

import frootloops.versus.util.hostile_mobs.creeper.XrayFollowTargetGoal;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public class CreeperEntityMixin extends HostileEntity {
    protected CreeperEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    private int fuseTime = 30;
    @Shadow
    private int explosionRadius = 3;

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityTag) {
        fuseTime = 34;
        explosionRadius = 5;
        this.experiencePoints = 16;
        return super.initialize(world, difficulty, spawnReason, entityData, entityTag);
    }

    @Override
    public boolean disablesShield(){
        return true;
    }

    @Redirect(at=@At(value = "NEW", target="net/minecraft/entity/ai/goal/ActiveTargetGoal"), method= "initGoals()V")
    private ActiveTargetGoal createFollowGoal(MobEntity creeper, Class targetClass, boolean checkVisibility) {
        return new XrayFollowTargetGoal(creeper, targetClass, false);
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(this.getBlockPos().getY() > 24) return false;
        return super.canSpawn(world, spawnReason);
    }

    // If a creeper isn't on the ground, it won't increase its ignition timer nor explode. Useful for ravines:
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo info) {
        if(!this.onGround && !((CreeperEntity)((Object)this)).isIgnited()) {
            super.tick();
            info.cancel();
        }
    }
}
