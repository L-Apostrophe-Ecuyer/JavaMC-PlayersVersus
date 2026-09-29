package frootloops.versus.mixin.environment.archeology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public class BrushesNotRequiredMixin {

    @Shadow protected ServerLevel level;
    @Shadow protected final ServerPlayer player;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private BlockPos destroyPos;

    @Shadow private int destroyProgressStart;

    @Shadow private int gameTicks;


    public BrushesNotRequiredMixin(ServerPlayer player) {
        this.player = player;
    }

    private Direction prevDirection = Direction.UP;


    @Inject(method = "handleBlockBreakAction", at = @At("RETURN"), cancellable = false)
    private void processBlockBreakingAction(BlockPos blockPos, ServerboundPlayerActionPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo info) {
        if(isDestroyingBlock) prevDirection = direction;
    }


    @Inject(method = "tick", at = @At("HEAD"), cancellable = false)
    private void update(CallbackInfo info) {
        if(level.isClientSide()) return;
        int ticksTillBrushing = 6;
        if(this.gameTicks - destroyProgressStart > ticksTillBrushing && this.gameTicks - destroyProgressStart < 36 && player.swinging) {
            BlockState blockState = level.getBlockState(destroyPos);
            if (blockState.getBlock() instanceof BrushableBlock brushableBlock) {
                if (level.getBlockEntity(destroyPos) instanceof BrushableBlockEntity brushableBlockEntity) {
                    addDustParticles(level, prevDirection, destroyPos, blockState, player);
                    brushableBlockEntity.brush(level.getGameTime(), (ServerLevel)level, player, prevDirection, player.getMainHandItem());
                    brushableBlockEntity.checkReset((ServerLevel)level);
                }
                else {
                    BrushableBlockEntity brushableBlockEntity = (BrushableBlockEntity)brushableBlock.newBlockEntity(destroyPos, blockState);
                    if(brushableBlock == Blocks.SUSPICIOUS_SAND) brushableBlockEntity.setLootTable(BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY, level.getSeed());
                    else brushableBlockEntity.setLootTable(BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY, level.getSeed());

                    addDustParticles(level, prevDirection, destroyPos, blockState, player);
                    brushableBlockEntity.brush(level.getGameTime(), (ServerLevel)level, player, prevDirection, player.getMainHandItem());
                    brushableBlockEntity.checkReset((ServerLevel)level);
                }
            }
        }
    }

    private static void addDustParticles(Level world, Direction direction, BlockPos blockPos, BlockState state, Player playerEntity) {
        int i = playerEntity.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        int j = world.getRandom().nextInt(7, 12);
        BlockParticleOption blockStateParticleEffect = new BlockParticleOption(ParticleTypes.BLOCK, state);
        DustParticlesOffset dustParticlesOffset = DustParticlesOffset.fromSide(playerEntity.getLookAngle(), direction);

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

        public static DustParticlesOffset fromSide(Vec3 userRotation, Direction side) {
            double d = 0.0;
            DustParticlesOffset var10000;
            switch (side) {
                case DOWN:
                case UP:
                    var10000 = new DustParticlesOffset(userRotation.z(), 0.0, -userRotation.x());
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
                    var10000 = new DustParticlesOffset(userRotation.z(), 0.0, -userRotation.x());
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
