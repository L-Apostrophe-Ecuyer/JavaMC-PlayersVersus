package frootloops.versus.mod.mobs.passive;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.block.BlockStatePredicate;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.function.Predicate;

public class PiggingAroundGoal extends Goal {
    private static final int COOLDOWN = 1200;
    private static final int MAX_TIMER = 40;
    private static final int EATING_TICKS = 4;
    private static final Predicate<BlockState> YUMMY_CROPS_PREDICATE;
    private final AnimalEntity mob;
    private final World world;
    private int timer;

    public PiggingAroundGoal(AnimalEntity mob) {
        this.mob = mob;
        this.world = mob.getWorld();
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
        this.timer = -mob.getRandom().nextBetween(80, COOLDOWN);
    }

    public boolean canStart() {
        if (!this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) return false;
        if (this.mob.isBaby()) return false;
        if (this.mob.getLovingPlayer() == null || this.mob.getLovingPlayer().squaredDistanceTo(this.mob) > 400.0) return false;

        // If the pig is in a crop, eat it:
        BlockPos blockPos = this.mob.getBlockPos();
        if (YUMMY_CROPS_PREDICATE.test(this.world.getBlockState(blockPos))) {
            return true;

        // Pig has to be on grass and feel free to move around, or they won't dig:
        } else {
            Vec3d wanderTarget = NoPenaltyTargeting.find(this.mob, 10, 7);
            if (wanderTarget == null) return false;

            Path pathToWander = this.mob.getNavigation().findPathTo(wanderTarget.x, wanderTarget.y, wanderTarget.z, 0);
            if(pathToWander == null) return false;

            if( this.world.getBlockState(blockPos.down()).isOf(Blocks.GRASS_BLOCK)) {
                this.timer++;
                if(timer >= 0) {
                    this.world.playSoundFromEntity(null, this.mob, SoundEvents.ENTITY_SNIFFER_SCENTING, SoundCategory.AMBIENT, 1.0F, 1.3F);
                    timer = MAX_TIMER;
                    return true;
                }
            }
            return false;
        }
    }

    public void start() {
        this.timer = this.getTickCount(MAX_TIMER);
        this.world.sendEntityStatus(this.mob, (byte)10);
        this.mob.getNavigation().stop();
    }

    public void stop() {
        this.timer = -COOLDOWN;
    }

    public boolean shouldContinue() {
        return this.timer > 0;
    }

    public void tick() {
        this.timer--;
        this.makeHeadBobDown();
        this.spawnDiggingParticles();
        if (this.timer == EATING_TICKS) {
            BlockPos blockPos = this.mob.getBlockPos();
            BlockState blockState = this.world.getBlockState(blockPos);
            if (blockState.getBlock() == Blocks.CARROTS) {
                this.world.setBlockState(blockPos, Blocks.CARROTS.getDefaultState());
                this.mob.eatFood(world, Items.CARROT.getDefaultStack(), Items.CARROT.getComponents().get(DataComponentTypes.FOOD));

            } else {
                blockPos = blockPos.down();
                if (this.world.getBlockState(blockPos).isOf(Blocks.GRASS_BLOCK)) {
                    this.world.syncWorldEvent(2001, blockPos, Block.getRawIdFromState(Blocks.GRASS_BLOCK.getDefaultState()));
                    this.world.setBlockState(blockPos, Blocks.DIRT.getDefaultState(), 2);
                    this.mob.playAmbientSound();
                    this.mob.onEatingGrass();
                }
            }
        }
    }

    static {
        YUMMY_CROPS_PREDICATE = BlockStatePredicate.forBlock(Blocks.CARROTS);
    }

    private void makeHeadBobDown() {
        float f = ((float)this.timer) / 32.0F;
        float pitch = 100f * (0.62831855F + 0.21991149F * MathHelper.sin(f * 28.7F));
        this.mob.getLookControl().lookAt(this.mob.getX(), this.mob.getY() - 1D, this.mob.getZ());
        this.mob.setPitch(pitch);
        this.mob.prevPitch = pitch;
    }

    private void spawnDiggingParticles() {
        BlockState blockState = this.mob.getSteppingBlockState();
        BlockPos blockPos = this.mob.getBlockPos();
        for(int i = 0; i < 30; ++i) {
            Vec3d vec3d = Vec3d.ofCenter(blockPos);
            this.world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState), vec3d.x, vec3d.y, vec3d.z, 0.0, 0.0, 0.0);
        }

        if (this.timer % 4 == 0) {
            this.world.playSound(this.mob.getX(), this.mob.getY(), this.mob.getZ(), blockState.getSoundGroup().getHitSound(), this.mob.getSoundCategory(), 0.5F, 0.5F, false);
        }
    }
}
