/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package frootloops.versus.backported.entities.wind_charge;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.state.property.Properties.POWERED;

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
                        FluidState fluidState = this.world.getFluidState(blockPos);
                        if (!this.world.isInBuildLimit(blockPos)) continue block2;
                        Optional<Float> optional = EXPLOSION_BEHAVIOR.getBlastResistance(this, this.world, blockPos, blockState, fluidState);
                        if (optional.isPresent()) {
                            h -= (optional.get().floatValue() + 0.3f) * 0.3f;
                        }
                        if (h > 0.0f && EXPLOSION_BEHAVIOR.canDestroyBlock(this, this.world, blockPos, blockState, h)) {
                            set.add(blockPos);
                        }
                        m += d * (double)0.3f;
                        n += e * (double)0.3f;
                        o += f * (double)0.3f;
                    }
                }
            }
        }
        this.affectedBlocks.addAll((Collection<BlockPos>)set);
        float q = this.power * 2.0f;
        k = MathHelper.floor(this.x - (double)q - 1.0);
        l = MathHelper.floor(this.x + (double)q + 1.0);
        int r = MathHelper.floor(this.y - (double)q - 1.0);
        int s = MathHelper.floor(this.y + (double)q + 1.0);
        int t = MathHelper.floor(this.z - (double)q - 1.0);
        int u = MathHelper.floor(this.z + (double)q + 1.0);
        List<Entity> list = this.world.getOtherEntities(this.entity, new Box(k, r, t, l, s, u));
        Vec3d vec3d = new Vec3d(this.x, this.y, this.z);
        for (Entity entity : list) {
            PlayerEntity playerEntity;
            double ac;
            double y;
            double x;
            double w;
            double z;
            double v;
            if (entity.isImmuneToExplosion() || !((v = Math.sqrt(entity.squaredDistanceTo(vec3d)) / (double)q) <= 1.0) || (z = Math.sqrt((w = entity.getX() - this.x) * w + (x = (entity instanceof TntEntity ? entity.getY() : entity.getEyeY()) - this.y) * x + (y = entity.getZ() - this.z) * y)) == 0.0) continue;
            w /= z;
            x /= z;
            y /= z;
            double aa = Explosion.getExposure(vec3d, entity);
            double ab = (1.0 - v) * aa;
            ac = ab;
            Vec3d vec3d2 = new Vec3d(w *= ac, x *= ac, y *= ac);
            entity.setVelocity(entity.getVelocity().add(vec3d2));
            if (!(entity instanceof PlayerEntity) || (playerEntity = (PlayerEntity)entity).isSpectator() || playerEntity.isCreative() && playerEntity.getAbilities().flying) continue;
            this.affectedPlayers.put(playerEntity, vec3d2);
        }
    }

    @Override
    public void affectWorld(boolean particles) {
        if (this.world.isClient) {
            this.world.playSound(this.x, this.y, this.z, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 4.0f, (1.0f + (this.world.random.nextFloat() - this.world.random.nextFloat()) * 0.2f) * 0.7f, false);
        }
        if (particles) {
            if (this.power < 3.0f) {
                this.world.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1.0, 0.0, 0.0);
            } else {
                this.world.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0, 0.0, 0.0);
            }
        }
    }
}

