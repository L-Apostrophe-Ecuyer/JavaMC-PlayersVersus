package frootloops.versus.mixin.environment.archeology;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BrushableBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(BrushableBlockEntity.class)
public abstract class SuspiciousBlockEntityMixin extends BlockEntity {

    public SuspiciousBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow private ItemStack item = ItemStack.EMPTY;
    @Shadow @Nullable private RegistryKey<LootTable> lootTable;

    @Shadow public void setLootTable(RegistryKey<LootTable> lootTable, long seed) {}

    private static final RegistryKey<LootTable> LOOT_SAND_AQUATIC = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(VersusMod.MOD_ID, "archaeology/default_sand_aquatic"));
    private static final RegistryKey<LootTable> LOOT_SAND_SURFACE = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(VersusMod.MOD_ID, "archaeology/default_sand_surface"));
    private static final RegistryKey<LootTable> LOOT_SAND_DESERT = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(VersusMod.MOD_ID, "archaeology/default_sand_desert"));

    private static final RegistryKey<LootTable> LOOT_GRAVEL_AQUATIC = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(VersusMod.MOD_ID, "archaeology/default_gravel_aquatic"));
    private static final RegistryKey<LootTable> LOOT_GRAVEL_SURFACE = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(VersusMod.MOD_ID, "archaeology/default_gravel_surface"));
    private static final RegistryKey<LootTable> LOOT_GRAVEL_DEEP_CAVE = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(VersusMod.MOD_ID, "archaeology/default_gravel_deep_cave"));
    private static final RegistryKey<LootTable> LOOT_GRAVEL_REGULAR_CAVE = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(VersusMod.MOD_ID, "archaeology/default_gravel_regular_cave"));

    @Inject(method = "finishBrushing", at = @At("RETURN"), cancellable = false)
    private void finishBrushing(ServerWorld world, LivingEntity brusher, ItemStack itemStack, CallbackInfo info) {
        if(itemStack.getUseAction() != UseAction.BRUSH || !itemStack.isOf(Items.BRUSH)) {
            if(!(world.getBlockState(this.getPos()).getBlock() instanceof BrushableBlock)) {
                world.breakBlock(this.getPos(), true, brusher);
            }
        }
    }

    @ModifyConstant(method = "brush", constant = @Constant(longValue = 10L))
    private long fasterBrushing(long tickDelayUntilNextBrushStage) {return VersusSettings.Gameplay.BRUSHING_TICKS_PER_STAGE * 2L;}

    @Inject(method = "generateItem", at = @At("HEAD"), cancellable = false)
    private void generateRandomLootIfNoneAppended(ServerWorld world, LivingEntity brusher, ItemStack brush, CallbackInfo info) {
        if(item.isEmpty() && lootTable == null) {
            if(this.getCachedState().isOf(Blocks.SUSPICIOUS_SAND)) {
                if(world.getFluidState(pos.up()).isOf(Fluids.WATER)) this.setLootTable(LOOT_SAND_AQUATIC, this.getPos().asLong());
                else {
                    RegistryEntry<Biome> biome = world.getBiome(this.pos);
                    if(biome.isIn(BiomeTags.DESERT_PYRAMID_HAS_STRUCTURE)) this.setLootTable(LOOT_SAND_DESERT, this.getPos().asLong());
                    else if(biome.isIn(BiomeTags.PLAYS_UNDERWATER_MUSIC)) this.setLootTable(LOOT_SAND_AQUATIC, this.getPos().asLong());
                    else this.setLootTable(LOOT_SAND_SURFACE, this.getPos().asLong());
                }
            }
            else {
                if(pos.getY() < 8) this.setLootTable(LOOT_GRAVEL_DEEP_CAVE, this.getPos().asLong());
                else if(pos.getY() < 32) this.setLootTable(LOOT_GRAVEL_REGULAR_CAVE, this.getPos().asLong());
                else {
                    RegistryEntry<Biome> biome = world.getBiome(this.pos);
                    if(biome == CustomOverworldBiomes.REGULAR_CAVE) this.setLootTable(LOOT_GRAVEL_REGULAR_CAVE, this.getPos().asLong());
                    else if (biome.isIn(BiomeTags.PLAYS_UNDERWATER_MUSIC)) this.setLootTable(LOOT_GRAVEL_AQUATIC, this.getPos().asLong());
                    else if (pos.getY() < 56 || world.getLightLevel(LightType.SKY, pos) < 3) this.setLootTable(LOOT_GRAVEL_REGULAR_CAVE, this.getPos().asLong());
                    else this.setLootTable(LOOT_GRAVEL_SURFACE, this.getPos().asLong());
                }
            }
        }
    }
}
