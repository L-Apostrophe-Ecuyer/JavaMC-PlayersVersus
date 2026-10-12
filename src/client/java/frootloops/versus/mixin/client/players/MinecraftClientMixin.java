package frootloops.versus.mixin.client.players;

import com.mojang.blaze3d.platform.WindowEventHandler;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.players.HandSelection;
import frootloops.versus.mod.players.building.ClientPlacement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = Minecraft.class, priority = 999)
public abstract class MinecraftClientMixin extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler {
    @Shadow public LocalPlayer player;
    @Shadow public ClientLevel level;
    @Shadow protected int missTime;
    @Shadow private int rightClickDelay;
    @Shadow @Nullable public HitResult hitResult;
    @Shadow @Nullable public MultiPlayerGameMode gameMode;
    @Shadow @Nullable public final GameRenderer gameRenderer;
    @Shadow public final Options options;

    public MinecraftClientMixin(String string, @Nullable GameRenderer gameRenderer, Options options) { super(string, true);
        this.gameRenderer = gameRenderer;
        this.options = options;
    }


    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void doItemUse(CallbackInfo info) {
        // As vanilla: no delay while a block is being mined, so that the use starts as soon as the mining stops
        if (this.gameMode.isDestroying()) {
            info.cancel();
            return;
        }
        this.rightClickDelay = 4;
        if (!this.player.isHandsBusy()) {

            if (this.hitResult == null) {
                VersusMod.MOD_LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
            }

            InteractionResult actionResult = null;
            // Which hand goes first, and whether the world or the item comes into it, is HandSelection's: it guesses what
            // the player means (blocking an attack, landing in water, opening the table in front of them) by fixed rules
            HandSelection.Plan plan = HandSelection.plan(this.player, this.level, this.hitResult);
            for (InteractionHand hand : plan.hands()) {
                ItemStack itemStack = this.player.getItemInHand(hand);
                if (!itemStack.isItemEnabled(this.level.enabledFeatures())) continue;
                // An item on cooldown leaves the block or entity to the other hand, which can use it without the item
                // the server would refuse
                boolean onCooldown = HandSelection.onCooldown(this.player, hand);
                if (plan.world() && !onCooldown && this.hitResult != null) {
                    switch (this.hitResult.getType()) {
                        case ENTITY:
                            EntityHitResult entityHitResult = (EntityHitResult)this.hitResult;
                            Entity entity = entityHitResult.getEntity();
                            if (!this.level.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                                info.cancel();
                                return;
                            }
                            // 26.3 also checks the reach: an entity can be picked from further than it can be interacted
                            // with, and then the held item is used, as when nothing is picked
                            if (!this.player.isWithinEntityInteractionRange(entity, 0.0)) break;

                            // Use chest if item frame clicked on accident
                            if(!this.player.isShiftKeyDown() && entity instanceof ItemFrame itemFrameEntity) {
                                BlockPos pos = itemFrameEntity.getPos().relative(itemFrameEntity.getNearestViewDirection(), -1);
                                BlockState state = level.getBlockState(pos);
                                if(state.getBlock() instanceof AbstractChestBlock) {
                                    BlockHitResult blockHitResult = new BlockHitResult(hitResult.getLocation(), itemFrameEntity.getNearestViewDirection(), pos, false);
                                    actionResult = this.gameMode.useItemOn(this.player, hand, blockHitResult);
                                    if (actionResult instanceof InteractionResult.Success success && success.swingSource() == InteractionResult.SwingSource.PREDICTED) this.player.swing(hand, itemStack.getInteractAnimation(), false);
                                    info.cancel();
                                    return;
                                }
                            }

                            actionResult = this.gameMode.interact(this.player, entity, entityHitResult, hand);
                            if (actionResult instanceof InteractionResult.Success success) {
                                if (success.swingSource() == InteractionResult.SwingSource.PREDICTED) {
                                    this.player.swing(hand, itemStack.getInteractAnimation(), false);
                                }
                                info.cancel();
                                return;
                            }
                            break;
                        case BLOCK:
                            actionResult = this.playersVersus$useOnBlock(hand, itemStack, (BlockHitResult)this.hitResult);
                            if (actionResult instanceof InteractionResult.Success || actionResult instanceof InteractionResult.Fail) {
                                info.cancel();
                                return;
                            }
                            break;
                        case MISS:
                            // Reach-around: nothing under the crosshair, looking down past the edge of the block below.
                            // If the block can't go there, the item is used as with nothing under the crosshair (a lily
                            // pad still goes on the water below)
                            BlockHitResult around = ClientPlacement.INSTANCE.reachAround(this.player, hand);
                            if (around != null && (ClientPlacement.INSTANCE.refuses(this.player, hand, around)
                                    || this.playersVersus$useOnBlock(hand, itemStack, around) instanceof InteractionResult.Success)) {
                                info.cancel();
                                return;
                            }
                            break;
                    }
                }

                if (plan.items() && !onCooldown && !itemStack.isEmpty() && this.gameMode.useItem(this.player, hand) instanceof InteractionResult.Success success3) {
                    if (success3.swingSource() == InteractionResult.SwingSource.PREDICTED) {
                        this.player.swing(hand, itemStack.getInteractAnimation(), false);
                    }

                    this.player.itemUsed(hand);
                    info.cancel();
                    return;
                }

            }
        }
        info.cancel();
    }

    /**
     * The item in hand used on a block, as vanilla's right-click does: a success or a failure ends the click. A repeat
     * of right-click held that the stroke refuses is a failure, and nothing happens at all.
     */
    @Unique
    private InteractionResult playersVersus$useOnBlock(InteractionHand hand, ItemStack itemStack, BlockHitResult hit) {
        if (ClientPlacement.INSTANCE.refuses(this.player, hand, hit)) return InteractionResult.FAIL;
        ClientPlacement.INSTANCE.tried(this.player, hand, hit);
        int i = itemStack.getCount();
        // A block is placed turned to match what's being built, clicked so that vanilla places it that way
        InteractionResult actionResult = ClientPlacement.INSTANCE.use(this.gameMode, this.player, hand, hit);
        if (actionResult instanceof InteractionResult.Success success && success.swingSource() == InteractionResult.SwingSource.PREDICTED) {
            this.player.swing(hand, itemStack.getInteractAnimation(), false);
            if (!itemStack.isEmpty() && (itemStack.getCount() != i || this.player.hasInfiniteMaterials())) {
                this.player.itemUsed(hand);
            }
        }
        return actionResult;
    }

    /** Right-click held in a stroke places as soon as the crosshair reaches a new spot for it, not every fourth tick. */
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void playersVersus$placeOnArrival(CallbackInfo info) {
        if (this.rightClickDelay > 0 && this.gameMode != null && !this.gameMode.isDestroying() && ClientPlacement.INSTANCE.arrived((Minecraft)(Object)this)) {
            this.rightClickDelay = 0;
        }
    }
}
