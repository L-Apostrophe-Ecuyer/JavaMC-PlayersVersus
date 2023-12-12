package frootloops.versus.mixin.mobs.hostile.dragon;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
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
    @Shadow
    private int duration = 600;
    @Shadow
    private int durationOnUse;
    @Shadow
    private final int waitTime = 20;
    @Shadow
    private float radiusGrowth;
    @Shadow
    private float radiusOnUse;
    @Shadow
    private final Map<Entity, Integer> affectedEntities = Maps.newHashMap();
    @Shadow
    private final Potion potion = Potions.EMPTY;
    @Shadow @Nullable
    private LivingEntity owner;
    @Shadow @Nullable
    private UUID ownerUuid;
    @Shadow
    private static final TrackedData<Float> RADIUS = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.FLOAT);
    @Shadow
    private static final TrackedData<Integer> COLOR = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Shadow
    private static final TrackedData<Boolean> WAITING = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Shadow
    private static final TrackedData<ParticleEffect> PARTICLE_ID = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.PARTICLE);
    @Shadow
    private final List<StatusEffectInstance> effects = Lists.newArrayList();
    @Shadow
    public float getRadius() {
        return this.getDataTracker().get(RADIUS).floatValue();
    }
    @Shadow
    public int getColor() {
        return this.getDataTracker().get(COLOR);
    }
    @Shadow
    public boolean isWaiting() {
        return this.getDataTracker().get(WAITING);
    }
    @Shadow
    public ParticleEffect getParticleType() {
        return this.getDataTracker().get(PARTICLE_ID);
    }
    @Shadow
    public void setRadius(float radius) {
        if (!this.getWorld().isClient) {
            this.getDataTracker().set(RADIUS, Float.valueOf(MathHelper.clamp(radius, 0.0f, 32.0f)));
        }
    }
    @Shadow @Nullable
    public LivingEntity getOwner() {
        Entity entity;
        if (this.owner == null && this.ownerUuid != null && this.getWorld() instanceof ServerWorld && (entity = ((ServerWorld)this.getWorld()).getEntity(this.ownerUuid)) instanceof LivingEntity) {
            this.owner = (LivingEntity)entity;
        }
        return this.owner;
    }

    @Shadow
    protected void setWaiting(boolean waiting) {
        this.getDataTracker().set(WAITING, waiting);
    }

    @Override
    public void calculateDimensions() {
        super.calculateDimensions();
        double radius = (double)this.getDimensions(EntityPose.STANDING).width / 2.0D;
        this.setBoundingBox(new Box(this.getX() - radius, this.getY() - radius, this.getZ() - radius, this.getX() + radius, this.getY() + radius, this.getZ() + radius));
    }

    @Override
    public void tick() {
        boolean isWaiting = this.isWaiting();
        float radius = this.getRadius();
        if (this.getWorld().isClient) {
            ParticleEffect particleOptions = this.getParticleType();
            if (isWaiting) {
                if (this.random.nextBoolean()) {
                    for (int i = 0; i < radius; ++i) {
                        float f1 = this.random.nextFloat() * ((float)Math.PI * 2F);
                        float f2 = MathHelper.sqrt(this.random.nextFloat()) * 0.2F;
                        float x = MathHelper.cos(f1) * f2;
                        float z = MathHelper.sin(f1) * f2;
                        if (particleOptions.getType() == ParticleTypes.ENTITY_EFFECT) {
                            int j = this.random.nextBoolean() ? 16777215 : this.getColor();
                            int k = j >> 16 & 255;
                            int l = j >> 8 & 255;
                            int i1 = j & 255;
                            this.getWorld().addParticle(particleOptions, this.getX() + (double)x, this.getY(), this.getZ() + (double)z, (float)k / 255.0F, (float)l / 255.0F, (float)i1 / 255.0F);
                        }
                        else {
                            this.getWorld().addParticle(particleOptions, this.getX() + (double)x, this.getY(), this.getZ() + (double)z, 0.0D, 0.0D, 0.0D);
                        }
                    }
                }
            }
            else {
                int particleAmount = (int) (Math.PI * radius * radius);

                for (int k1 = 0; k1 < particleAmount; ++k1) {
                    float f6 = this.random.nextFloat() * ((float)Math.PI * 2F);
                    float f7 = MathHelper.sqrt(this.random.nextFloat()) * radius;

                    float x = random.nextFloat() * (radius + radius) - radius;
                    float y = random.nextFloat() * (radius + radius) - radius;
                    float z = random.nextFloat() * (radius + radius) - radius;

                    if ((x*x) + (y*y) + (z*z) > (radius*radius))
                        continue;

                    if (particleOptions.getType() == ParticleTypes.ENTITY_EFFECT) {
                        int l1 = this.getColor();
                        int i2 = l1 >> 16 & 255;
                        int j2 = l1 >> 8 & 255;
                        int j1 = l1 & 255;
                        this.getWorld().addParticle(particleOptions, this.getX() + (double)x, this.getY() + (double)y, this.getZ() + (double)z, (float)i2 / 255.0F, (float)j2 / 255.0F, (float)j1 / 255.0F);
                    } else {
                        this.getWorld().addParticle(particleOptions, this.getX() + (double)x, this.getY() + (double)y, this.getZ() + (double)z, (0.5D - this.random.nextDouble()) * 0.15D, 0.01F, (0.5D - this.random.nextDouble()) * 0.15D);
                    }
                }
            }
        }
        else {
            if (this.age >= this.waitTime + this.duration) {
                this.discard();
                return;
            }

            boolean flag1 = this.age < this.waitTime;
            if (isWaiting != flag1) {
                this.setWaiting(flag1);
            }

            if (flag1) {
                return;
            }

            if (this.radiusGrowth != 0.0F) {
                radius += this.radiusGrowth;
                if (radius < 0.5F) {
                    this.discard();
                    return;
                }

                this.setRadius(radius);
            }

            if (this.age % 5 == 0) {
                this.affectedEntities.entrySet().removeIf(entry -> this.age >= entry.getValue());

                List<StatusEffectInstance> list = Lists.newArrayList();

                for(StatusEffectInstance effectinstance1 : this.potion.getEffects()) {
                    list.add(new StatusEffectInstance(effectinstance1.getEffectType(), effectinstance1.getDuration() / 4, effectinstance1.getAmplifier(), effectinstance1.isAmbient(), effectinstance1.shouldShowParticles()));
                }

                list.addAll(this.effects);
                if (list.isEmpty()) {
                    this.affectedEntities.clear();
                } else {
                    List<LivingEntity> list1 = this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox());
                    if (!list1.isEmpty()) {
                        for(LivingEntity livingentity : list1) {
                            if (!this.affectedEntities.containsKey(livingentity) && livingentity.isAffectedBySplashPotions()) {
                                this.affectedEntities.put(livingentity, this.age + 20);
                                double x = livingentity.getX() - this.getX();
                                double y = livingentity.getY() + (livingentity.getDimensions(livingentity.getPose()).height / 2) - (this.getY());
                                double z = livingentity.getZ() - this.getZ();
                                double d2 = x * x + y * y + z * z;
                                if (d2 <= (double)(radius * radius)) {
                                    for (StatusEffectInstance effectinstance : list) {
                                        if (effectinstance.getEffectType().isInstant()) {
                                            effectinstance.getEffectType().applyInstantEffect(this, this.getOwner(), livingentity, effectinstance.getAmplifier(), 0.5D);
                                        }
                                        else {
                                            livingentity.addStatusEffect(new StatusEffectInstance(effectinstance));
                                        }
                                    }
                                    if (this.radiusOnUse != 0.0F) {
                                        radius += this.radiusOnUse;
                                        if (radius < 0.5F) {
                                            this.discard();
                                            return;
                                        }
                                        this.setRadius(radius);
                                    }
                                    if (this.durationOnUse != 0) {
                                        this.duration += this.durationOnUse;
                                        if (this.duration <= 0) {
                                            this.discard();
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        return EntityDimensions.changing(this.getRadius() * 2.0F, this.getRadius() * 2.0F);
    }
}
