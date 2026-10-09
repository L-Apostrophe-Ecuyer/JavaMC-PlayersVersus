package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.network.syncher.EntityDataAccessor;
import frootloops.versus.mod.environment.WorldTime;
import frootloops.versus.mod.mobs.hostile.overworld.crawling.GripState;
import frootloops.versus.mod.mobs.hostile.overworld.crawling.CrawlingMoveControl;
import frootloops.versus.mod.mobs.hostile.overworld.crawling.SurfaceCrawler;
import frootloops.versus.mod.mobs.hostile.overworld.crawling.SurfaceGrip;
import frootloops.versus.mod.mobs.hostile.overworld.crawling.SurfaceNavigation;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Spider.class)
public class SpiderMixin extends Monster implements SurfaceCrawler {
    private static final EntityDataAccessor<Boolean> BABY = SynchedEntityData.defineId(Spider.class, EntityDataSerializers.BOOLEAN);
    /** The face a spider clings by, and the way it faces along a wall or ceiling (SurfaceGrip). */
    @Unique
    private static final EntityDataAccessor<Direction> GRIP_FACE = SynchedEntityData.defineId(Spider.class, EntityDataSerializers.DIRECTION);
    @Unique
    private static final EntityDataAccessor<Vector3fc> CRAWL_HEADING = SynchedEntityData.defineId(Spider.class, EntityDataSerializers.VECTOR3);

    @Unique
    private final GripState playersVersus$grip = new GripState();

    private static final Identifier BABY_SCALE_MODIFIER_ID = Identifier.withDefaultNamespace("baby");
    private static final AttributeModifier BABY_SCALE_MODIFIER  = new AttributeModifier(BABY_SCALE_MODIFIER_ID, -0.5, AttributeModifier.Operation.ADD_VALUE);

    protected SpiderMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public void playAmbientSound() {
        // Skip! Spiders should be quieter. Also, their ambient noise is pretty grating
    }

    @Override
    public float getVoicePitch() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.3F + 1.5F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 0.6F;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, EntitySpawnReason spawnReason) {
        if(spawnReason != EntitySpawnReason.NATURAL) return super.checkSpawnRules(world, spawnReason);

        int y = this.blockPosition().getY();
        if(y < -8) return false;

        int ySpawnBonus = world.getBiome(this.blockPosition()).is(BiomeTags.SPAWNS_WARM_VARIANT_FROGS) ? 32 : 0;
        if(y > 96 + ySpawnBonus) return false;
        if(y > 64 + ySpawnBonus && world.getBrightness(LightLayer.SKY, this.blockPosition()) > 2) return false;
        return super.checkSpawnRules(world, spawnReason);
    }

    @Override
    public int calculateFallDamage(double fallDistance, float damageMultiplier) {
        return super.calculateFallDamage(fallDistance, damageMultiplier) - 10;
    }

    @Override
    public void dropFromLootTable(ServerLevel world, DamageSource source, boolean causedByPlayer) {
        super.dropFromLootTable(world, source, causedByPlayer);
        if(!this.isBaby()) {
            super.dropFromLootTable(world, source, causedByPlayer); // Triple loot for the big boys!
            super.dropFromLootTable(world, source, causedByPlayer);
        }
    }

    @Override
    public int getBaseExperienceReward(ServerLevel world) {
        if (!this.isBaby()) this.xpReward = 17;
        return super.getBaseExperienceReward(world);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData) {
        entityData = super.finalizeSpawn(world, difficulty, spawnReason, entityData);
        if (entityData == null) {
            entityData = new Spider.SpiderEffectsGroupData();
            if ((world.getDifficulty() == Difficulty.HARD || WorldTime.moonPhase(world) == 7 || this.getY() < 32.0) && random.nextFloat() < 0.3f * difficulty.getSpecialMultiplier()) {
                ((Spider.SpiderEffectsGroupData)entityData).setRandomEffect(random);
            }
        }
        if (entityData instanceof Spider.SpiderEffectsGroupData) {
            Spider.SpiderEffectsGroupData spiderData = (Spider.SpiderEffectsGroupData)entityData;
            if (spiderData.effect != null && spiderData.effect.isBound()) {
                this.addEffect(new MobEffectInstance(spiderData.effect, -1));
            }
        }

        AttributeInstance instanceMvt = this.getAttributes().getInstance(Attributes.MOVEMENT_SPEED);
        AttributeInstance instanceDmg = this.getAttributes().getInstance(Attributes.ATTACK_DAMAGE);
        AttributeInstance instanceHP = this.getAttributes().getInstance(Attributes.MAX_HEALTH);
        AttributeInstance instanceScale = this.getAttributes().getInstance(Attributes.SCALE);

        if(random.nextFloat() < 0.85F) {
            this.setBaby(true);
            if (instanceMvt != null) instanceMvt.setBaseValue(0.36D);
            if (instanceDmg != null) instanceDmg.setBaseValue(3.0D);
            if (instanceScale != null) instanceScale.setBaseValue(0.7D);
            if (instanceHP != null) {
                instanceHP.setBaseValue(12.0f);
                this.setHealth(12.0f);
            }
        }
        else {
            if (instanceMvt != null) instanceMvt.setBaseValue(0.32D);
            if (instanceDmg != null) instanceDmg.setBaseValue(6.0D);
            if (instanceHP != null) {
                instanceHP.setBaseValue(24.0f);
                this.setHealth(24.0f);
            }
            Skeleton skeletonEntity;
            if (random.nextInt(60) == 0 && (skeletonEntity = EntityTypes.SKELETON.create(this.level(), EntitySpawnReason.JOCKEY)) != null) {
                skeletonEntity.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0f);
                skeletonEntity.finalizeSpawn(world, difficulty, spawnReason, null);
                skeletonEntity.startRiding(this);
            }
        }

        this.getNavigation().setCanFloat(true);
        return entityData;
    }


    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void addBabyData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(BABY, false);
    }

    /**
     *  - SPIDERS CRAWL ON ANY SURFACE
     * They path along floors, walls and ceilings, crawl along them without falling, and tilt their body onto the
     * surface they grip (SurfaceGrip).
     */
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void playersVersus$defineGripData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(GRIP_FACE, Direction.DOWN);
        builder.define(CRAWL_HEADING, new Vector3f(0.0F, 0.0F, 1.0F));
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void playersVersus$crawl(EntityType<? extends Spider> type, Level level, CallbackInfo ci) {
        this.moveControl = new CrawlingMoveControl(this);
    }

    @Inject(method = "createNavigation", at = @At("HEAD"), cancellable = true)
    private void playersVersus$pathAlongSurfaces(Level level, CallbackInfoReturnable<PathNavigation> cir) {
        cir.setReturnValue(new SurfaceNavigation(this, level));
    }

    /** After moving: the server picks the face the spider clings by and its heading; clients turn the model toward them. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void playersVersus$keepGrip(CallbackInfo ci) {
        Direction face = this.getEntityData().get(GRIP_FACE);
        if (this.level().isClientSide()) {
            this.playersVersus$grip.turnToward(face, this.getEntityData().get(CRAWL_HEADING), this.yBodyRot);
            return;
        }
        int touching = SurfaceGrip.touching(this);
        this.playersVersus$grip.setTouching(touching);
        Vec3 motion = new Vec3(this.getX() - this.xo, this.getY() - this.yo, this.getZ() - this.zo);
        face = SurfaceGrip.chooseFace(touching, motion.x, motion.y, motion.z, face, this.onGround());
        this.getEntityData().set(GRIP_FACE, face);
        if (face == Direction.DOWN) return;
        LivingEntity target = this.getTarget();
        Vector3f heading = GripState.heading(face, motion, target == null ? null : target.position().subtract(this.position()));
        if (heading != null && GripState.worthSyncing(this.getEntityData().get(CRAWL_HEADING), heading)) {
            this.getEntityData().set(CRAWL_HEADING, heading);
        }
    }

    /** Legs move with the distance crawled up and down walls too. */
    @Override
    public void calculateEntityAnimation(boolean includeHeight) {
        super.calculateEntityAnimation(includeHeight || this.getEntityData().get(GRIP_FACE) != Direction.DOWN);
    }

    @Override
    public Direction playersVersus$gripFace() {
        return this.getEntityData().get(GRIP_FACE);
    }

    @Override
    public int playersVersus$touching() {
        return this.playersVersus$grip.touching();
    }

    @Override
    public Vector3f playersVersus$surfaceNormal(float partialTick) {
        return this.playersVersus$grip.normal(partialTick);
    }

    @Override
    public Vector3f playersVersus$surfaceForward(float partialTick) {
        return this.playersVersus$grip.forward(partialTick);
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        if (super.doHurtTarget(world, target)) {
            if (target instanceof LivingEntity && !this.isBaby()) {
                int i = 0;
                if (this.level().getDifficulty() == Difficulty.NORMAL) i = 3;
                else if (this.level().getDifficulty() == Difficulty.HARD) i = 6;
                if (i > 0) {
                    ((LivingEntity)target).addEffect(new MobEffectInstance(MobEffects.SLOWNESS, i * 10, 0), this);
                    ((LivingEntity)target).addEffect(new MobEffectInstance(MobEffects.BLINDNESS, i * 10, 0), this);
                }
                if(this.level().getGameTime() % 5 == 0) {
                    if(this.level().getBlockState(target.blockPosition()) == Blocks.AIR.defaultBlockState()) {
                        if (Blocks.COBWEB.defaultBlockState().canSurvive(this.level(), target.blockPosition())) {
                            this.level().setBlockAndUpdate(target.blockPosition(), Blocks.COBWEB.defaultBlockState());
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isBaby() {
        return this.getEntityData().get(BABY);
    }

    @Override
    public void setBaby(boolean baby) {
        this.getEntityData().set(BABY, baby);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (BABY.equals(data)) {this.refreshDimensions();}
        super.onSyncedDataUpdated(data);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.putBoolean("IsBaby", this.isBaby());
    }

    @Override
    public void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        this.setBaby(view.getBooleanOr("IsBaby", false));
    }

    @Override
    public float getAgeScale() {
        return this.isBaby() ? 0.85F : 1.3F;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 multiplier) {
        if (!(state.getBlock() instanceof WebBlock)) {
            super.makeStuckInBlock(state, multiplier);
        }
    }
}