package frootloops.versus.mixin.environment.archeology;

import frootloops.versus.VersusMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrushableBlock;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ShovelItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerInteractionManager.class)
public class BrushesNotRequiredMixin {

    @Shadow protected ServerWorld world;
    @Shadow protected final ServerPlayerEntity player;
    @Shadow private boolean mining;
    @Shadow private BlockPos miningPos;

    @Shadow private int startMiningTime;

    @Shadow private int tickCounter;


    public BrushesNotRequiredMixin(ServerPlayerEntity player) {
        this.player = player;
    }

    private Direction prevDirection = Direction.UP;


    @Inject(method = "processBlockBreakingAction", at = @At("RETURN"), cancellable = false)
    private void processBlockBreakingAction(BlockPos blockPos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo info) {
        if(mining) prevDirection = direction;
    }


    @Inject(method = "update", at = @At("HEAD"), cancellable = false)
    private void update(CallbackInfo info) {
        int ticksTillBrushing = player.getMainHandStack().getItem() instanceof ShovelItem ? 2 : 6;
        if(this.tickCounter - startMiningTime > ticksTillBrushing && this.tickCounter - startMiningTime < 36 && player.handSwinging) {
            BlockState blockState = world.getBlockState(miningPos);
            if (blockState.getBlock() instanceof BrushableBlock) {
                if (world.getBlockEntity(miningPos) instanceof BrushableBlockEntity brushableBlockEntity) {
                    addDustParticles(world, prevDirection, miningPos, blockState, player);
                    brushableBlockEntity.brush(world.getTime(), player, prevDirection);
                    brushableBlockEntity.scheduledTick();
                }
            }
        }
    }

    private static void addDustParticles(World world, Direction direction, BlockPos blockPos, BlockState state, PlayerEntity playerEntity) {
        int i = playerEntity.getMainArm() == Arm.RIGHT ? 1 : -1;
        int j = world.getRandom().nextBetweenExclusive(7, 12);
        BlockStateParticleEffect blockStateParticleEffect = new BlockStateParticleEffect(ParticleTypes.BLOCK, state);
        DustParticlesOffset dustParticlesOffset = DustParticlesOffset.fromSide(playerEntity.getRotationVector(), direction);

        for(int k = 0; k < j; ++k) {
            world.addParticle(blockStateParticleEffect, blockPos.getX() - (double)(direction == Direction.WEST ? 1.0E-6F : 0.0F), blockPos.getY(), blockPos.getZ() - (double)(direction == Direction.NORTH ? 1.0E-6F : 0.0F), dustParticlesOffset.xd() * (double)i * 3.0 * world.getRandom().nextDouble(), 0.0, dustParticlesOffset.zd() * (double)i * 3.0 * world.getRandom().nextDouble());
        }

    }

    private static record DustParticlesOffset(double xd, double yd, double zd) {
        private static final double field_42685 = 1.0;
        private static final double field_42686 = 0.1;

        private DustParticlesOffset(double xd, double yd, double zd) {
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
        }

        public static DustParticlesOffset fromSide(Vec3d userRotation, Direction side) {
            double d = 0.0;
            DustParticlesOffset var10000;
            switch (side) {
                case DOWN:
                case UP:
                    var10000 = new DustParticlesOffset(userRotation.getZ(), 0.0, -userRotation.getX());
                    break;
                case NORTH:
                    var10000 = new DustParticlesOffset(1.0, 0.0, -0.1);
                    break;
                case SOUTH:
                    var10000 = new DustParticlesOffset(-1.0, 0.0, 0.1);
                    break;
                case WEST:
                    var10000 = new DustParticlesOffset(-0.1, 0.0, -1.0);
                    break;
                case EAST:
                    var10000 = new DustParticlesOffset(0.1, 0.0, 1.0);
                    break;
                default:
                    var10000 = new DustParticlesOffset(userRotation.getZ(), 0.0, -userRotation.getX());
            }

            return var10000;
        }

        public double xd() {
            return this.xd;
        }

        public double zd() {
            return this.zd;
        }
    }
}
