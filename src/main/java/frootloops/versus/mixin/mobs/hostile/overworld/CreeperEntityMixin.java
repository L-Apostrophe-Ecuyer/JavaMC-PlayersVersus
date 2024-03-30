package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason == SpawnReason.NATURAL) {
            if (this.getBlockPos().getY() > 96 || this.getBlockPos().getY() < 24)
                return false;
            if (this.getBlockPos().getY() > 60 && (world.getLightLevel(LightType.SKY, this.getBlockPos()) > 4 || world.getBiome(this.getBlockPos()).isIn(BiomeTags.SPAWNS_COLD_VARIANT_FROGS)))
                return false;
        }
        return super.canSpawn(world, spawnReason);
    }
}
