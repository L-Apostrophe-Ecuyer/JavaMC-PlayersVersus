package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CreeperEntity.class)
public class CreeperMixin extends HostileEntity {
    protected CreeperMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if(super.damage(world, source, amount)) {
            if (source.isOf(DamageTypes.WITHER) && this.getHealth() < 8.0f) {
                this.convertTo(ModEntities.DEEPER_CREEPER, EntityConversionContext.create(this, false, false), stray -> {});
            }
            return true;
        }
        else return false;
    }
}
