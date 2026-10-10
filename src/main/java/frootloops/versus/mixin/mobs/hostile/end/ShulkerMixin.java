package frootloops.versus.mixin.mobs.hostile.end;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(Shulker.class)
public abstract class ShulkerMixin extends AbstractGolem implements Enemy {

    private static final Map<MapColor, DyeColor> DYE_COLOR_BY_MAP_COLOR = new HashMap<>();

    protected ShulkerMixin(EntityType<? extends AbstractGolem> type, Level level) {
        super(type, level);
    }

    @Shadow private void setVariant(final Optional<DyeColor> color) {};

    @Inject(method = "setAttachFace", at = @At("TAIL"))
    private void matchSurfaceColor(final Direction attachmentDirection, CallbackInfo ci) {
        BlockPos pos = this.blockPosition().relative(attachmentDirection);
        BlockState state = this.level().getBlockState(pos);
        if(state.getBlock().getDescriptionId().contains("purpur")) this.setVariant(Optional.empty());

        MapColor color = state.getMapColor(this.level(), pos);
        if(DYE_COLOR_BY_MAP_COLOR.isEmpty()) {
            for (DyeColor dyeColor : DyeColor.values()) {
                DYE_COLOR_BY_MAP_COLOR.putIfAbsent(dyeColor.getMapColor(), dyeColor);
            }
        }
        this.setVariant(Optional.of(DYE_COLOR_BY_MAP_COLOR.get(color)));
    }

}
