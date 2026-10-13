package frootloops.versus.mixin.mobs.passive;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.mobs.passive.GolemChestRanking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Copper golems, which are the only mobs that run this behavior, carry a whole stack instead of 16 and take it to the
 * best chest for it instead of the nearest one that will do ({@link GolemChestRanking}), the furthest chest with room
 * if nothing suits it better.
 */
@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {

    /** Vanilla's {@code TRANSPORTED_ITEM_MAX_STACK_SIZE}. */
    private static final int VANILLA_CARRY_LIMIT = 16;

    /**
     * {@code getTransportTarget} keeps the chest with the smallest distance to the golem among those it may use. While
     * the golem carries something, that distance becomes the chest's cost for the item. Picking up is left to vanilla.
     */
    @WrapOperation(
            method = "getTransportTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D")
    )
    private double playersVersus$bestChestForTheItem(BlockPos chestPos, Position golemPos, Operation<Double> original,
                                                     @Local(argsOnly = true) ServerLevel level,
                                                     @Local(argsOnly = true) PathfinderMob golem) {
        double distanceSq = original.call(chestPos, golemPos);
        if (!VersusSettings.Gameplay.DO_SMARTER_COPPER_GOLEMS) {
            return distanceSq;
        }
        ItemStack carried = golem.getMainHandItem();
        if (carried.isEmpty()) {
            return distanceSq;
        }
        TransportItemsBetweenContainers.TransportItemTarget target = TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(chestPos, level);
        return target == null ? distanceSq : GolemChestRanking.cost(target.container(), carried, distanceSq);
    }

    /**
     * Vanilla's golem drops its item only into an empty chest or one holding it; anything else is "no drop". A chest
     * it was sent to as the last resort holds other things, so any chest with room for the item takes it.
     */
    @ModifyReturnValue(method = "matchesLeavingItemsRequirement", at = @At("RETURN"))
    private static boolean playersVersus$anyChestWithRoomTakesIt(boolean matches, PathfinderMob golem, Container container) {
        return matches || VersusSettings.Gameplay.DO_SMARTER_COPPER_GOLEMS && GolemChestRanking.roomFor(container, golem.getMainHandItem()) > 0;
    }

    /** {@code pickupItemFromContainer} takes {@code min(stack size, 16)} from the first stack it finds. */
    @WrapOperation(
            method = "pickupItemFromContainer",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I")
    )
    private static int playersVersus$wholeStack(int stackSize, int limit, Operation<Integer> original) {
        if (VersusSettings.Gameplay.DO_SMARTER_COPPER_GOLEMS && limit == VANILLA_CARRY_LIMIT) {
            return stackSize;
        }
        return original.call(stackSize, limit);
    }
}
