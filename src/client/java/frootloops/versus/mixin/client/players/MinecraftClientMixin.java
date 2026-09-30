package frootloops.versus.mixin.client.players;

import com.mojang.blaze3d.platform.WindowEventHandler;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

    private static final InteractionHand[] OFFHAND_FIRST =  new InteractionHand[] {InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND}, MAINHAND_FIRST = new InteractionHand[] {InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND};

    public MinecraftClientMixin(String string, @Nullable GameRenderer gameRenderer, Options options) { super(string, true);
        this.gameRenderer = gameRenderer;
        this.options = options;
    }


    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void doItemUse(CallbackInfo info) {
        this.rightClickDelay = 4;
        if (!this.gameMode.isDestroying() && !this.player.isHandsBusy()) {

            if (this.hitResult == null) {
                VersusMod.MOD_LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
            }

            InteractionResult actionResult = null;
            InteractionHand[] hands = this.shouldPrioritizeOffhand() ? OFFHAND_FIRST : MAINHAND_FIRST;
            for (InteractionHand hand : hands) {
                ItemStack itemStack = this.player.getItemInHand(hand);
                if (!itemStack.isItemEnabled(this.level.enabledFeatures())) continue;
                if (this.hitResult != null) {
                    switch (this.hitResult.getType()) {
                        case ENTITY:
                            EntityHitResult entityHitResult = (EntityHitResult)this.hitResult;
                            Entity entity = entityHitResult.getEntity();
                            if (!this.level.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                                info.cancel();
                                return;
                            }
                            // 26.3 also checks the reach: an entity can be picked from further than it can be interacted with
                            if (!this.player.isWithinEntityInteractionRange(entity, 0.0)) {
                                info.cancel();
                                return;
                            }

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
                            BlockHitResult blockHitResult = (BlockHitResult)this.hitResult;
                            int i = itemStack.getCount();
                            actionResult = this.gameMode.useItemOn(this.player, hand, blockHitResult);
                            if (actionResult instanceof InteractionResult.Success success) {
                                if (success.swingSource() == InteractionResult.SwingSource.PREDICTED) {
                                    this.player.swing(hand, itemStack.getInteractAnimation(), false);
                                    if (!itemStack.isEmpty() && (itemStack.getCount() != i || this.player.hasInfiniteMaterials())) {
                                        this.player.itemUsed(hand);
                                    }
                                }
                                info.cancel();
                                return;
                            }
                            if (actionResult instanceof InteractionResult.Fail) {
                                info.cancel();
                                return;
                            }
                    }
                }

                if (!itemStack.isEmpty() && this.gameMode.useItem(this.player, hand) instanceof InteractionResult.Success success3) {
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

    private boolean shouldPrioritizeOffhand(){
        ItemStack offhandStack = player.getOffhandItem();
        ItemStack mainhandStack = player.getMainHandItem();
        if(offhandStack.isEmpty() || mainhandStack.isEmpty() || player.getCooldowns().isOnCooldown(offhandStack)) return false;
        if(offhandStack.getUseAnimation() == ItemUseAnimation.BLOCK){
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                if(player.isUsingItem()) {
                    return player.getUseItem() == offhandStack;
                } else {
                    Entity target = ((EntityHitResult) this.hitResult).getEntity();
                    return (target instanceof Monster || target instanceof Player || target == player.getLastHurtByMob());
                }
            }
            else if (mainhandStack.getUseAnimation() == ItemUseAnimation.BLOCK) {
                return true;
            }
            else if (player.getLastHurtByMob() != null && player.getLastHurtByMob().isAlive()) {
                return (Combat.isLookingTowards(player,player.getLastHurtByMob().position()));
            }
        }
        else if(mainhandStack.getUseAnimation() == ItemUseAnimation.BLOCK){
            if(player.isShiftKeyDown()) {
                return !(mainhandStack.getItem() instanceof ShieldItem);
            }
            else if (hitResult.getType() == HitResult.Type.ENTITY) {
                if(player.isUsingItem()) {
                    return player.getUseItem() == offhandStack;
                } else {
                    Entity target = ((EntityHitResult) this.hitResult).getEntity();
                    return !(target instanceof Monster || target instanceof Player || target == player.getLastHurtByMob());
                }
            }
            else if (player.getLastHurtByMob() != null && player.getLastHurtByMob().isAlive()) {
                return (Combat.isLookingTowards(player,player.getLastHurtByMob().position()));
            }
            else if(offhandStack.getUseAnimation() == ItemUseAnimation.EAT || offhandStack.getUseAnimation() == ItemUseAnimation.DRINK) {
                if(offhandStack.getComponents().has(DataComponents.FOOD)) {
                    if(offhandStack.getItem().components().get(DataComponents.FOOD).canAlwaysEat()) return true;
                    return player.getFoodData().needsFood();
                }
                else return true;
            }
        }
        return false;
    }
}
