package frootloops.versus.mixin.mobs.hostile.end;

import frootloops.versus.VersusMod;
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
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(Shulker.class)
public abstract class ShulkerMixin extends AbstractGolem implements Enemy {

    private static final Map<Integer, DyeColor> DYE_COLOR_BY_MAP_COLOR = new HashMap<>();

    protected ShulkerMixin(EntityType<? extends AbstractGolem> type, Level level) {
        super(type, level);
    }

    @Invoker("setVariant")
    protected abstract void versus$setVariant(Optional<DyeColor> color);

    @Inject(method = "setAttachFace", at = @At("HEAD"))
    private void matchSurfaceColor(final Direction attachmentDirection, CallbackInfo ci) {
        VersusMod.MOD_LOGGER.debug("Shulker ATTACHING");


        BlockPos pos = this.blockPosition().relative(attachmentDirection);
        BlockState state = this.level().getBlockState(pos);
        if(state.getBlock().getDescriptionId().contains("purpur")) this.versus$setVariant(Optional.empty());

        MapColor color = state.getMapColor(this.level(), pos);
        if(DYE_COLOR_BY_MAP_COLOR.isEmpty()) {
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.STONE.col, DyeColor.LIGHT_GRAY);
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.DEEPSLATE.col, DyeColor.GRAY);
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.GRASS.col, DyeColor.GREEN);
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.SAND.col, DyeColor.YELLOW);
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.WOOD.col, DyeColor.BROWN);
            for (DyeColor dyeColor : DyeColor.values()) {
                DYE_COLOR_BY_MAP_COLOR.putIfAbsent(dyeColor.getMapColor().col, dyeColor);
                DYE_COLOR_BY_MAP_COLOR.putIfAbsent(dyeColor.getTerracottaColor().col, dyeColor);
                DYE_COLOR_BY_MAP_COLOR.putIfAbsent(dyeColor.getTextureDiffuseColor(), dyeColor);
            }
        }
        DyeColor shulkerVariant = DYE_COLOR_BY_MAP_COLOR.get(color);
        VersusMod.MOD_LOGGER.debug("Shulker set to color " + (shulkerVariant == null ? "default" : color.toString()));
        this.versus$setVariant(shulkerVariant == null ? Optional.empty() : Optional.of(shulkerVariant));
    }

}
