package frootloops.versus.mixin.environment;

import frootloops.versus.mod.items.brewing.ConcentrateItem;
import frootloops.versus.mod.items.equipment.RecoveryCompassItem;
import net.minecraft.block.*;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.BlockSoundGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Blocks.class)
public class VanillaBlocksMixin {

    // Diorite replaced by Calcite:
    @Shadow public static final Block DIORITE = Blocks.register("diorite", new Block(AbstractBlock.Settings.create().mapColor(MapColor.TERRACOTTA_WHITE).instrument(NoteBlockInstrument.BASEDRUM).sounds(BlockSoundGroup.CALCITE).requiresTool().strength(0.75f)));
    @Shadow public static final Block POLISHED_DIORITE = Blocks.register("polished_diorite", new Block(AbstractBlock.Settings.create().mapColor(MapColor.OFF_WHITE).instrument(NoteBlockInstrument.BASEDRUM).sounds(BlockSoundGroup.CALCITE).requiresTool().strength(0.75f)));

    // Torchflowers give off light:
    @Shadow public static final Block TORCHFLOWER = Blocks.register("torchflower", (Block)new FlowerBlock(StatusEffects.NIGHT_VISION, 24.0f, AbstractBlock.Settings.create().luminance(state -> 8).mapColor(MapColor.DARK_GREEN).noCollision().breakInstantly().sounds(BlockSoundGroup.GRASS).offset(AbstractBlock.OffsetType.XZ).pistonBehavior(PistonBehavior.DESTROY)));

    /*
    @Inject(method = "getBlastResistance()F", at = @At("RETURN"), cancellable = true)
    private void lowerBlastResistance(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * 0.4f);
    }*/
}
