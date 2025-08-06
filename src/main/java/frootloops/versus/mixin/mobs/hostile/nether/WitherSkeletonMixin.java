package frootloops.versus.mixin.mobs.hostile.nether;

import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.hostile.nether.WildfireEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherSkeletonEntity.class)
public class WitherSkeletonMixin extends HostileEntity {
    protected WitherSkeletonMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initialize", at = @At("TAIL"))
    private void decreaseHealth(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        this.getAttributes().getCustomInstance(EntityAttributes.MAX_HEALTH).setBaseValue(32.0D);
        this.setHealth(this.getMaxHealth());

        EntityAttributeInstance instanceKnockbackRes = this.getAttributes().getCustomInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
        if (instanceKnockbackRes != null) instanceKnockbackRes.setBaseValue(0.5D);

        this.getAttributes().getCustomInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(0.5D);
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        boolean result = this.getPathfindingFavor(this.getBlockPos(), world) >= 0.0F;
        if(result && spawnReason == SpawnReason.NATURAL && this.getWorld().getDimension().ultrawarm()) {

            // Rarely spawn a Wildfire:
            boolean isInSoulSandValley = this.getWorld().getBiome(this.getBlockPos()) == BiomeKeys.SOUL_SAND_VALLEY;
            if(this.getRandom().nextInt(isInSoulSandValley ? 8 : 16) == 0) {
                WildfireEntity wildfireEntity = new WildfireEntity(ModEntities.WILDFIRE, this.getWorld());
                wildfireEntity.setPosition(this.getPos());
                world.spawnEntity(wildfireEntity);
            }
        }
        return result;
    }
}
