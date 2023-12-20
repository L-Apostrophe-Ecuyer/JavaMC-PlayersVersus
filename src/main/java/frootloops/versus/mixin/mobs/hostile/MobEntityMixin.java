package frootloops.versus.mixin.mobs.hostile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.MobVisibilityCache;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity {

    @Shadow @Nullable private LivingEntity target;
    @Shadow private MobVisibilityCache visibilityCache;

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean startRiding(Entity entity) {
        if(this.hurtTime > 0) return false;
        if(target instanceof PlayerEntity && visibilityCache.canSee(target)) return super.startRiding(entity, false);
        else return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.hasVehicle() && !(this.getVehicle() instanceof LivingEntity)) this.stopRiding();
        return super.damage(source, amount);
    }

    @Redirect(at=@At(value = "INVOKE", target="Lnet/minecraft/world/LocalDifficulty;getClampedLocalDifficulty()F"), method= "Lnet/minecraft/entity/mob/MobEntity;initEquipment(Lnet/minecraft/util/math/random/Random;Lnet/minecraft/world/LocalDifficulty;)V")
    private float harderFartherAndDeeper(LocalDifficulty localDifficulty) {
        float distanceMultiplier = ((float)(this.getBlockPos().getX() - this.getWorld().getSpawnPos().getX()))/512f + ((float)(this.getBlockPos().getZ() - this.getWorld().getSpawnPos().getZ()))/512f;
        float depthBonus = 160.0f/Math.abs((float)this.getPos().y - 64f);
        return (localDifficulty.getClampedLocalDifficulty() + depthBonus) * distanceMultiplier;
    }

}
