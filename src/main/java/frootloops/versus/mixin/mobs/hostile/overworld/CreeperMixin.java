package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Creeper.class)
public class CreeperMixin extends Monster {
    protected CreeperMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if(super.hurtServer(world, source, amount)) {
            if (source.is(DamageTypes.WITHER) && this.getHealth() < 8.0f) {
                this.convertTo(ModEntities.PALE_CREEPER, ConversionParams.single(this, false, false), stray -> {});
            }
            return true;
        }
        else return false;
    }
}
