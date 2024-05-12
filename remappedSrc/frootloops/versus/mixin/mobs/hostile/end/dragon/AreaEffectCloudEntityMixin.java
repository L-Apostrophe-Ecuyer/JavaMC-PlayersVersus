package frootloops.versus.mixin.mobs.hostile.end.dragon;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(AreaEffectCloudEntity.class)
public abstract class AreaEffectCloudEntityMixin extends Entity {


    public AreaEffectCloudEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow private static final TrackedData<Float> RADIUS = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.FLOAT);

    @Shadow
    public float getRadius() {
        return this.getDataTracker().get(RADIUS).floatValue();
    }

    @Override
    public void calculateDimensions() {
        super.calculateDimensions();
        double radius = (double)this.getDimensions(EntityPose.STANDING).width() / 2.0D;
        this.setBoundingBox(new Box(this.getX() - radius, this.getY() - radius, this.getZ() - radius, this.getX() + radius, this.getY() + radius, this.getZ() + radius));
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return EntityDimensions.changing(this.getRadius() * 2.0F, this.getRadius() * 2.0F);
    }
}
