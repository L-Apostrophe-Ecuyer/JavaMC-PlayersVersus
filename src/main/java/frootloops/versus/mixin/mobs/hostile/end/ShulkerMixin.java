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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.level.block.ColorCollection;

@Mixin(Shulker.class)
public abstract class ShulkerMixin extends AbstractGolem implements Enemy {

    private static final Map<Integer, DyeColor> DYE_COLOR_BY_MAP_COLOR = new HashMap<>();

    protected ShulkerMixin(EntityType<? extends AbstractGolem> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract Direction getAttachFace();

    @Shadow
    public abstract DyeColor getColor();

    @Invoker("setVariant")
    protected abstract void versus$setVariant(Optional<DyeColor> color);

    @Inject(method = "teleportSomewhere", at = @At("RETURN"))
    private void matchSurfaceColor(CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue().booleanValue()) applySurfaceColor(this.getAttachFace());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void matchSurfaceColorAfterLoad(final ValueInput input, CallbackInfo ci) {
        applySurfaceColor(this.getAttachFace());
    }

    private void applySurfaceColor(final Direction attachmentDirection) {
        BlockPos pos = this.blockPosition().relative(attachmentDirection);
        BlockState state = this.level().getBlockState(pos);
        if (state.getBlock().getDescriptionId().contains("purpur")) {
            this.versus$setVariant(Optional.empty());
            return;
        }

        MapColor color = state.getMapColor(this.level(), pos);
        if(DYE_COLOR_BY_MAP_COLOR.isEmpty()) {
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.STONE.col, DyeColor.LIGHT_GRAY);
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.DEEPSLATE.col, DyeColor.GRAY);
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.GRASS.col, DyeColor.GREEN);
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.DIRT.col, DyeColor.BROWN);
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.SNOW.col, DyeColor.WHITE);
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.SAND.col, DyeColor.YELLOW);
            DYE_COLOR_BY_MAP_COLOR.putIfAbsent(MapColor.WOOD.col, DyeColor.BROWN);
            // Since 26.4 a dye colour is only an id and a name; its map colours are collections keyed by colour.
            putDyeColors(MapColor.DYE_TO_DEFAULT_COLOR);
            putDyeColors(MapColor.DYE_TO_TERRACOTTA_COLOR);
        }
        DyeColor shulkerVariant = DYE_COLOR_BY_MAP_COLOR.get(color.col);
        this.versus$setVariant(shulkerVariant == null ? Optional.empty() : Optional.of(shulkerVariant));
    }

    private static void putDyeColors(ColorCollection<MapColor> colors) {
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.white().col, DyeColor.WHITE);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.orange().col, DyeColor.ORANGE);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.magenta().col, DyeColor.MAGENTA);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.lightBlue().col, DyeColor.LIGHT_BLUE);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.yellow().col, DyeColor.YELLOW);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.lime().col, DyeColor.LIME);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.pink().col, DyeColor.PINK);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.gray().col, DyeColor.GRAY);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.lightGray().col, DyeColor.LIGHT_GRAY);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.cyan().col, DyeColor.CYAN);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.purple().col, DyeColor.PURPLE);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.blue().col, DyeColor.BLUE);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.brown().col, DyeColor.BROWN);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.green().col, DyeColor.GREEN);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.red().col, DyeColor.RED);
        DYE_COLOR_BY_MAP_COLOR.putIfAbsent(colors.black().col, DyeColor.BLACK);
    }
}
