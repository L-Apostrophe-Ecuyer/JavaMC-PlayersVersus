package frootloops.versus.mixin.players.bridging;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.players.RayTraceHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public HitResult crosshairTarget;
    @Shadow private int itemUseCooldown;

    private static final double leniency = 1.5f;
    private static boolean verticalOrientation = true;

    private static Pair<BlockPos, Direction> currentTarget = null;


    @Inject(at = @At("HEAD"), method = "doItemUse()V", cancellable = true)
    public void onItemUse(CallbackInfo info) {
        if(VersusSettings.DO_BEDROCK_BRIDGING == false) return;
        if (this.interactionManager == null || this.interactionManager.isBreakingBlock()) return;
        if(this.crosshairTarget.getType() != HitResult.Type.MISS) return;
        if(this.player != null) {
            for(Hand hand : Hand.values()) {
                ItemStack itemStack = this.player.getStackInHand(hand);
                if(!(itemStack.getItem() instanceof BlockItem)) continue;
                Pair<BlockPos, Direction> pair = getPlayerReacharoundTarget(this.player);
                if (pair != null) {
                    BlockPos pos = pair.getLeft();
                    Direction dir = pair.getRight();

                    if (!this.player.canPlaceOn(pos, dir, itemStack)) return;
                    BlockHitResult blockHitResult = new BlockHitResult(new Vec3d(0, 1F, 0).add(Vec3d.ofCenter(pos)), dir, pos, false);

                    int i = itemStack.getCount();
                    ActionResult blockPlaceResult = this.interactionManager.interactBlock(this.player, hand, blockHitResult);

                    if (blockPlaceResult.isAccepted()) {
                        if (blockPlaceResult.shouldSwingHand()) {
                            this.player.swingHand(hand);
                            if (!itemStack.isEmpty() && (itemStack.getCount() != i || this.interactionManager.hasCreativeInventory())) {
                                MinecraftClient.getInstance().gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                            }
                        }
                        itemUseCooldown = 4;
                        info.cancel();
                    }
                }
            }
        }
    }

    private static Pair<BlockPos, Direction> getPlayerReacharoundTarget(PlayerEntity player) {
        Pair<Vec3d, Vec3d> rayDetails = RayTraceHandler.getEntityParams(player);
        World world = player.getWorld();

        double range = player.getReachDistance(true);
        Vec3d rayPos = rayDetails.getLeft();
        Vec3d ray = rayDetails.getRight().multiply(range);
        HitResult regularCollision = RayTraceHandler.rayTrace(player, world, rayPos, rayPos.add(ray), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE);

        // If there is not a normal block for the player to hit, attempt to raycast
        // reacharound targets.
        if (regularCollision.getType() != HitResult.Type.ENTITY) {

            Pair<BlockPos, Direction>  target = getVerticalTarget(player, world, rayPos, ray);
            if(target != null) {
                verticalOrientation = true;
                return target;
            }

            target = getHorizontalTarget(player, world, rayPos, ray);
            if(target != null) {
                verticalOrientation = false;
                return target;
            }
        }

        return null;
    }

    private static Pair<BlockPos, Direction> getVerticalTarget(PlayerEntity player, World world, Vec3d rayPos, Vec3d ray) {
        if(player.getPitch() < 0) return null;
        Vec3d endPos = rayPos.add(new Vec3d(0, leniency, 0)).add(ray);
        HitResult take2Res = RayTraceHandler.rayTrace(player, world, rayPos, endPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE);
        if (take2Res.getType() == HitResult.Type.BLOCK && take2Res instanceof BlockHitResult) {
            BlockPos pos = ((BlockHitResult) take2Res).getBlockPos().down();
            BlockState state = world.getBlockState(pos);

            if (player.getPos().y - pos.getY() > 1 && (world.isAir(pos) || state.isReplaceable()))
                return new Pair<>(pos, Direction.DOWN);
        }
        return null;
    }

    private static Pair<BlockPos, Direction> getHorizontalTarget(PlayerEntity player, World world, Vec3d rayPos, Vec3d ray) {
        Direction dir = Direction.fromRotation(player.headYaw);
        Vec3d newPos = rayPos.add(new Vec3d(-(leniency * dir.getOffsetX()), 0, -(leniency * dir.getOffsetZ())));
        HitResult take2Res = RayTraceHandler.rayTrace(player, world, newPos, newPos.add(ray), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE);
        if (take2Res.getType() == HitResult.Type.BLOCK && take2Res instanceof BlockHitResult) {
            BlockPos pos = ((BlockHitResult) take2Res).getBlockPos().offset(dir);
            BlockState state = world.getBlockState(pos);

            if ((world.isAir(pos) || state.isReplaceable())) return new Pair<>(pos, dir.getOpposite());
        }
        return null;
    }

    private static boolean isSupportedStack(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BlockItem;
    }
}