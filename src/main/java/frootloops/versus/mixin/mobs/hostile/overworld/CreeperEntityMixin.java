package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CreeperEntity.class)
public class  CreeperEntityMixin extends HostileEntity {
    protected CreeperEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean disablesShield(){
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if(super.damage(source,amount)) {
            if (source.isOf(DamageTypes.WITHER) && this.getHealth() < 8.0f) {
                this.convertTo(ModEntities.DEEPER_CREEPER, false);
            }
            return true;
        }
        else return false;
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason != SpawnReason.NATURAL) return true;
        BlockPos pos = this.getBlockPos();
        int y = pos.getY();
        if (y > 96 || y < 24) return false;
        if (y > 60 && world.getLightLevel(LightType.SKY, pos) > 4) return false;
        return world.getBlockState(pos.down()).isIn(BlockTags.OVERWORLD_CARVER_REPLACEABLES);
    }
}
