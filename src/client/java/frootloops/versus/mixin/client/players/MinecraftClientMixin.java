package frootloops.versus.mixin.client.players;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.players.RayTraceHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = MinecraftClient.class, priority = 999)
public abstract class MinecraftClientMixin extends ReentrantThreadExecutor<Runnable> implements WindowEventHandler {
    @Shadow public ClientPlayerEntity player;
    @Shadow public ClientWorld world;
    @Shadow protected int attackCooldown;
    @Shadow private int itemUseCooldown;
    @Shadow @Nullable public HitResult crosshairTarget;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Shadow @Nullable public final GameRenderer gameRenderer;
    @Shadow public final GameOptions options;

    private static final double leniency = 1.5f;
    private static boolean verticalOrientation = true;

    private static Pair<BlockPos, Direction> currentTarget = null;

    @Shadow private void addBlockEntityNbt(ItemStack stack, BlockEntity blockEntity, DynamicRegistryManager registryManager) {
        NbtCompound nbtCompound = blockEntity.createComponentlessNbtWithIdentifyingData(registryManager);
        blockEntity.removeFromCopiedStackNbt(nbtCompound);
        BlockItem.setBlockEntityData(stack, blockEntity.getType(), nbtCompound);
        stack.applyComponentsFrom(blockEntity.createComponentMap());
    }

    public MinecraftClientMixin(String string, @Nullable GameRenderer gameRenderer, GameOptions options) { super(string);
        this.gameRenderer = gameRenderer;
        this.options = options;
    }


    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void doItemUse(CallbackInfo info) {
        if (!this.interactionManager.isBreakingBlock()) {
            this.itemUseCooldown = 4;
            if (!this.player.isRiding()) {
                if (this.crosshairTarget == null) {
                    VersusMod.MOD_LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
                }

                ActionResult actionResult = null;
                Hand[] hands = this.shouldPrioritizeOffhand() ? new Hand[] {Hand.OFF_HAND, Hand.MAIN_HAND} :  new Hand[] {Hand.MAIN_HAND, Hand.OFF_HAND};
                for (Hand hand : hands) {
                    ItemStack itemStack = this.player.getStackInHand(hand);
                    if (this.crosshairTarget != null) {
                        switch (this.crosshairTarget.getType()) {
                            case ENTITY:
                                EntityHitResult entityHitResult = (EntityHitResult)this.crosshairTarget;
                                Entity entity = entityHitResult.getEntity();
                                if (!this.world.getWorldBorder().contains(entity.getBlockPos())) {
                                    return;
                                }

                                actionResult = this.interactionManager.interactEntityAtLocation(this.player, entity, entityHitResult, hand);
                                if (!actionResult.isAccepted()) {
                                    actionResult = this.interactionManager.interactEntity(this.player, entity, hand);
                                }

                                if (actionResult instanceof ActionResult.Success success) {
                                    if (success.swingSource() == ActionResult.SwingSource.CLIENT) {
                                        this.player.swingHand(hand);
                                    }

                                    return;
                                }
                                break;


                            case BLOCK:
                                BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
                                int count = itemStack.getCount();

                                if(itemStack.getComponents().contains(DataComponentTypes.FOOD) && !this.player.isSneaking() && this.player.getHungerManager().isNotFull())
                                    actionResult = this.interactionManager.interactItem(this.player, hand);

                                if(actionResult == null || !actionResult.isAccepted())
                                    actionResult = this.interactionManager.interactBlock(this.player, hand, blockHitResult);

                                if (actionResult instanceof ActionResult.Success successfulBlockInteraction) {
                                    if (successfulBlockInteraction.swingSource() == ActionResult.SwingSource.CLIENT) {
                                        this.player.swingHand(hand);
                                        if (!itemStack.isEmpty() && (itemStack.getCount() != count || this.interactionManager.hasCreativeInventory())) {
                                            this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                                        }
                                    }
                                    return;
                                }

                                if (actionResult instanceof ActionResult.Fail) {
                                    return;
                                }
                        }
                    }
                    else if(itemStack.getItem() instanceof BlockItem) {
                        Pair<BlockPos, Direction> pair = getBlockPlacingReacharoundTarget(this.player);
                        if (pair != null) {
                            BlockPos pos = pair.getLeft();
                            Direction dir = pair.getRight();

                            if (!this.player.canPlaceOn(pos, dir, itemStack)) return;
                            BlockHitResult blockHitResult = new BlockHitResult(new Vec3d(0, 1F, 0).add(Vec3d.ofCenter(pos)), dir, pos, false);

                            int count = itemStack.getCount();
                            actionResult = this.interactionManager.interactBlock(this.player, hand, blockHitResult);

                            if (actionResult instanceof ActionResult.Success successfulBlockInteraction) {
                                if (successfulBlockInteraction.swingSource() == ActionResult.SwingSource.CLIENT) {
                                    this.player.swingHand(hand);
                                    if (!itemStack.isEmpty() && (itemStack.getCount() != count || this.interactionManager.hasCreativeInventory())) {
                                        this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                                    }
                                }
                                return;
                            }
                        }
                    }

                    if (!itemStack.isEmpty() && this.interactionManager.interactItem(this.player, hand) instanceof ActionResult.Success success3) {
                        if (success3.swingSource() == ActionResult.SwingSource.CLIENT) {
                            this.player.swingHand(hand);
                        }

                        this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                        return;
                    }
                }
            }
        }
        info.cancel();
    }

    private boolean shouldPrioritizeOffhand(){
        ItemStack offhandStack = player.getOffHandStack();
        ItemStack mainhandStack = player.getMainHandStack();
        if(offhandStack.isEmpty() || mainhandStack.isEmpty()) return false;
        if(offhandStack.getItem() instanceof ShieldItem){
            if(player.isSneaking()) {
                return true;
            }
            else if (crosshairTarget.getType() == HitResult.Type.ENTITY) {
                if(player.isUsingItem()) {
                    return player.getActiveItem() == offhandStack;
                } else {
                    Entity target = ((EntityHitResult) this.crosshairTarget).getEntity();
                    return (target instanceof HostileEntity || target instanceof PlayerEntity || target == player.getAttacker());
                }
            }
            else if (mainhandStack.getUseAction() == UseAction.BLOCK) {
                return true;
            }
            else if (player.getAttacker() != null && player.getAttacker().isAlive()) {
                return (Combat.isLookingTowards(player,player.getAttacker().getPos()));
            }
        }
        else if(mainhandStack.getUseAction() == UseAction.BLOCK){
            if(player.isSneaking()) {
                return !(mainhandStack.getItem() instanceof ShieldItem);
            }
            else if (crosshairTarget.getType() == HitResult.Type.ENTITY) {
                if(player.isUsingItem()) {
                    return player.getActiveItem() == offhandStack;
                } else {
                    Entity target = ((EntityHitResult) this.crosshairTarget).getEntity();
                    return !(target instanceof HostileEntity || target instanceof PlayerEntity || target == player.getAttacker());
                }
            }
            else if (player.getAttacker() != null && player.getAttacker().isAlive()) {
                return (Combat.isLookingTowards(player,player.getAttacker().getPos()));
            }
            else if(offhandStack.getUseAction() == UseAction.EAT || offhandStack.getUseAction() == UseAction.DRINK) {
                if(offhandStack.getComponents().contains(DataComponentTypes.FOOD)) {
                    if(offhandStack.getItem().getComponents().get(DataComponentTypes.FOOD).canAlwaysEat()) return true;
                    return player.getHungerManager().isNotFull();
                }
                else return true;
            }
        }
        return false;
    }

    private static Pair<BlockPos, Direction> getBlockPlacingReacharoundTarget(PlayerEntity player) {
        Pair<Vec3d, Vec3d> rayDetails = RayTraceHandler.getEntityParams(player);
        World world = player.getWorld();

        double range = player.getBlockInteractionRange();
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
