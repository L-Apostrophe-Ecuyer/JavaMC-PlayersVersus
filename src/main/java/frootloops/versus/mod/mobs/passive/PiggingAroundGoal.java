package frootloops.versus.mod.mobs.passive;

import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class PiggingAroundGoal extends Goal {
    private static final int COOLDOWN = 1200;
    private static final int MAX_TIMER = 40;
    private static final int EATING_TICKS = 4;
    private static final Predicate<BlockState> YUMMY_CROPS_PREDICATE;
    private final Animal mob;
    private int timer;

    public PiggingAroundGoal(Animal mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.timer = -mob.getRandom().nextIntBetweenInclusive(80, COOLDOWN);
    }

    public boolean canUse() {
        if (this.mob.isBaby()) return false;
        if (this.mob.getLoveCause() == null || this.mob.getLoveCause().distanceToSqr(this.mob) > 400.0) return false;

        // If the pig is in a crop, eat it:
        BlockPos blockPos = this.mob.blockPosition();
        if (YUMMY_CROPS_PREDICATE.test(this.mob.level().getBlockState(blockPos))) {
            return true;

        // Pig has to be on grass and feel free to move around, or they won't dig:
        } else {
            Vec3 wanderTarget = DefaultRandomPos.getPos(this.mob, 10, 7);
            if (wanderTarget == null) return false;

            Path pathToWander = this.mob.getNavigation().createPath(wanderTarget.x, wanderTarget.y, wanderTarget.z, 0);
            if(pathToWander == null) return false;

            if(this.mob.level().getBlockState(blockPos.below()).is(Blocks.GRASS_BLOCK)) {
                this.timer++;
                if(timer >= 0) {
                    this.mob.level().playSound(null, this.mob, SoundEvents.SNIFFER_SCENTING, SoundSource.AMBIENT, 1.0F, 1.3F);
                    timer = MAX_TIMER;
                    return true;
                }
            }
            return false;
        }
    }

    public void start() {
        this.timer = this.adjustedTickDelay(MAX_TIMER);
        this.mob.level().broadcastEntityEvent(this.mob, (byte)10);
        this.mob.getNavigation().stop();
    }

    public void stop() {
        this.timer = -COOLDOWN;
    }

    public boolean canContinueToUse() {
        return this.timer > 0;
    }

    public void tick() {
        this.timer--;
        this.makeHeadBobDown();
        this.spawnDiggingParticles();
        if (this.timer == EATING_TICKS) {
            BlockPos blockPos = this.mob.blockPosition();
            BlockState blockState = this.mob.level().getBlockState(blockPos);
            if (blockState.getBlock() == Blocks.CARROTS) {
                this.mob.level().setBlockAndUpdate(blockPos, Blocks.CARROTS.defaultBlockState());
                this.mob.setInLove(null);

            } else {
                blockPos = blockPos.below();
                if (this.mob.level().getBlockState(blockPos).is(Blocks.GRASS_BLOCK)) {
                    this.mob.level().levelEvent(2001, blockPos, Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
                    this.mob.level().setBlock(blockPos, Blocks.DIRT.defaultBlockState(), 2);
                    this.mob.playAmbientSound();
                    this.mob.ate();
                }
            }
        }
    }

    static {
        YUMMY_CROPS_PREDICATE = BlockStatePredicate.forBlock(Blocks.CARROTS);
    }

    private void makeHeadBobDown() {
        float f = ((float)this.timer) / 32.0F;
        float pitch = 100f * (0.62831855F + 0.21991149F * Mth.sin(f * 28.7F));
        this.mob.getLookControl().setLookAt(this.mob.getX(), this.mob.getY() - 1D, this.mob.getZ());
        this.mob.setXRot(pitch);
        this.mob.xRotO = pitch;
    }

    private void spawnDiggingParticles() {
        BlockState blockState = this.mob.getBlockStateOn();
        BlockPos blockPos = this.mob.blockPosition();
        for(int i = 0; i < 30; ++i) {
            Vec3 vec3d = Vec3.atCenterOf(blockPos);
            this.mob.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState), vec3d.x, vec3d.y, vec3d.z, 0.0, 0.0, 0.0);
        }

        if (this.timer % 4 == 0) {
            this.mob.playSound(blockState.getSoundType().getHitSound(), 0.5F, 0.5F);
        }
    }
}
