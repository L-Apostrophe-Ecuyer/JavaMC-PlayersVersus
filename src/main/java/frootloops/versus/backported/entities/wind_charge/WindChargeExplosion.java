/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package frootloops.versus.backported.entities.wind_charge;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import frootloops.versus.VersusMod;
import frootloops.versus.backported.items.FutureItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WindChargeExplosion extends Explosion {
    private final Random random = Random.create();
    private final World world;
    private final double x;
    private final double y;
    private final double z;
    @Nullable
    private final Entity entity;
    private final float power;
    public static final WindChargeEntity.WindChargeExplosionBehavior EXPLOSION_BEHAVIOR = new WindChargeEntity.WindChargeExplosionBehavior();
    private final ObjectArrayList<BlockPos> affectedBlocks = new ObjectArrayList();
    private final Map<PlayerEntity, Vec3d> affectedPlayers = Maps.newHashMap();

    public WindChargeExplosion(World world, @Nullable Entity entity, double x, double y, double z, float power) {
        super(world, entity, null, EXPLOSION_BEHAVIOR, x, y, z, power, false,  Explosion.DestructionType.KEEP);
        this.world = world;
        this.entity = entity;
        this.power = power;
        this.x = x;
        this.z = z;
        this.y = y;
    }

    @Override
    public void collectBlocksAndDamageEntities() {
        int l;
        int k;
        this.world.emitGameEvent(this.entity, GameEvent.EXPLODE, new Vec3d(this.x, this.y, this.z));
        HashSet<BlockPos> set = Sets.newHashSet();
        int i = 16;
        for (int j = 0; j < 16; ++j) {
            for (k = 0; k < 16; ++k) {
                block2: for (l = 0; l < 16; ++l) {
                    if (j != 0 && j != 15 && k != 0 && k != 15 && l != 0 && l != 15) continue;
                    double d = (float)j / 15.0f * 2.0f - 1.0f;
                    double e = (float)k / 15.0f * 2.0f - 1.0f;
                    double f = (float)l / 15.0f * 2.0f - 1.0f;
                    double g = Math.sqrt(d * d + e * e + f * f);
                    d /= g;
                    e /= g;
                    f /= g;
                    double m = this.x;
                    double n = this.y;
                    double o = this.z;
                    float p = 0.3f;
                    for (float h = this.power * (0.7f + this.world.random.nextFloat() * 0.6f); h > 0.0f; h -= 0.22500001f) {
                        BlockPos blockPos = BlockPos.ofFloored(m, n, o);
                        BlockState blockState = this.world.getBlockState(blockPos);
                        if (!this.world.isInBuildLimit(blockPos)) continue block2;
                        if(blockState.emitsRedstonePower()) {
                            blockState.cycle(POWERED);
                            world.setBlockState(blockPos, blockState);
                            this.affectedBlocks.add(blockPos);
                        }
                        m += d * (double)0.3f;
                        n += e * (double)0.3f;
                        o += f * (double)0.3f;
                    }
                }
            }
        }
        double powerTimesTwo = 6.0;
        k = MathHelper.floor(this.x - (double)powerTimesTwo - 1.0);
        l = MathHelper.floor(this.x + (double)powerTimesTwo + 1.0);
        int r = MathHelper.floor(this.y - (double)powerTimesTwo - 1.0);
        int s = MathHelper.floor(this.y + (double)powerTimesTwo + 1.0);
        int t = MathHelper.floor(this.z - (double)powerTimesTwo - 1.0);
        int u = MathHelper.floor(this.z + (double)powerTimesTwo + 1.0);
        List<Entity> list = this.world.getOtherEntities(this.entity, new Box(k, r, t, l, s, u));
        Vec3d explosionPos = new Vec3d(this.x, this.y, this.z);
        PlayerEntity playerEntity;
        for (Entity entity : list) {

            double deltaZ = entity.getZ() - this.z;
            double deltaY = entity.getY() - this.y;
            double deltaX = entity.getX() - this.x;
            double squaredDistance = deltaZ * deltaZ + deltaX * deltaX + deltaY * deltaY;
            if(squaredDistance > 16.0) continue;

            double exposure = squaredDistance < 1.0 ? 1.0 : Explosion.getExposure(explosionPos, entity);
            double distance = Math.sqrt(squaredDistance);

            VersusMod.MOD_LOGGER.warn("Exposure for " + (this.world.isClient ? "client" : "server") + " entity at squared distance of " + distance + " to explosion is " + exposure);

            if(exposure < 0.1) continue;

            // Ensure that knockback is upwards:
            if(entity.getY() + 0.5 > this.y) deltaY = Math.abs(deltaY);

            // Set velocity:
            double explosionVelocity = (this.power/2.0) * exposure / Math.max(1.0, distance);
            Vec3d addedVelocity = new Vec3d((deltaX/distance) * explosionVelocity/2, (deltaY/distance) * explosionVelocity, (deltaZ/distance) * explosionVelocity/2);
            entity.move(MovementType.SELF, addedVelocity);
            entity.setVelocity(entity.getVelocity().add(addedVelocity));
            entity.move(MovementType.SELF, addedVelocity);

            VersusMod.MOD_LOGGER.warn("Entity sent flying with velocity " + explosionVelocity + " and vector " + entity.getVelocity());

            if (entity instanceof PlayerEntity player) {
                this.affectedPlayers.put(player, addedVelocity);
                VersusMod.MOD_LOGGER.warn("Added to list of affected players: " + player.getName());
            }
        }
    }

    @Override
    public void affectWorld(boolean particles) {
        if (this.world.isClient) {
            this.world.playSound(this.x, this.y, this.z, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 4.0f, (1.0f + (this.world.random.nextFloat() - this.world.random.nextFloat()) * 0.2f) * 0.7f, false);
            if (particles) {
                if (this.power < 3.0f) {
                    this.world.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1.0, 0.0, 0.0);
                } else {
                    this.world.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0, 0.0, 0.0);
                }
            }
        }
    }

    @Override
    public boolean shouldDestroy() {
        return false;
    }
}

