package frootloops.versus.mixin.environment.archeology;

import frootloops.versus.VersusMod;
import net.minecraft.world.item.ItemInstance;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootTable;
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
    @Shadow @Nullable private ResourceKey<LootTable> lootTable;

    @Shadow public void setLootTable(ResourceKey<LootTable> lootTable, long seed) {}

    private static final ResourceKey<LootTable> LOOT_SAND_AQUATIC = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "archaeology/default_sand_aquatic"));
    private static final ResourceKey<LootTable> LOOT_SAND_SURFACE = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "archaeology/default_sand_surface"));
    private static final ResourceKey<LootTable> LOOT_SAND_DESERT = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "archaeology/default_sand_desert"));

    private static final ResourceKey<LootTable> LOOT_GRAVEL_AQUATIC = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "archaeology/default_gravel_aquatic"));
    private static final ResourceKey<LootTable> LOOT_GRAVEL_SURFACE = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "archaeology/default_gravel_surface"));
    private static final ResourceKey<LootTable> LOOT_GRAVEL_DEEP_CAVE = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "archaeology/default_gravel_deep_cave"));
    private static final ResourceKey<LootTable> LOOT_GRAVEL_REGULAR_CAVE = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "archaeology/default_gravel_regular_cave"));

    @Inject(method = "brushingCompleted", at = @At("RETURN"), cancellable = false)
    private void finishBrushing(ServerLevel world, LivingEntity brusher, ItemStack itemStack, CallbackInfo info) {
        if(itemStack.getUseAnimation() != ItemUseAnimation.BRUSH || !itemStack.is(Items.BRUSH)) {
            if(!(world.getBlockState(this.getBlockPos()).getBlock() instanceof BrushableBlock)) {
                world.destroyBlock(this.getBlockPos(), true, brusher);
            }
        }
    }

    @ModifyConstant(method = "brush", constant = @Constant(longValue = 10L))
    private long fasterBrushing(long tickDelayUntilNextBrushStage) {return VersusSettings.Gameplay.BRUSHING_TICKS_PER_STAGE * 2L;}

    @Inject(method = "unpackLootTable", at = @At("HEAD"), cancellable = false)
    private void generateRandomLootIfNoneAppended(ServerLevel world, LivingEntity brusher, ItemInstance brush, CallbackInfo info) {
        if(item.isEmpty() && lootTable == null) {
            if(this.getBlockState().is(Blocks.SUSPICIOUS_SAND)) {
                if(world.getFluidState(worldPosition.above()).is(Fluids.WATER)) this.setLootTable(LOOT_SAND_AQUATIC, this.getBlockPos().asLong());
                else {
                    Holder<Biome> biome = world.getBiome(this.worldPosition);
                    if(biome.is(BiomeTags.HAS_DESERT_PYRAMID)) this.setLootTable(LOOT_SAND_DESERT, this.getBlockPos().asLong());
                    else if(biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) this.setLootTable(LOOT_SAND_AQUATIC, this.getBlockPos().asLong());
                    else this.setLootTable(LOOT_SAND_SURFACE, this.getBlockPos().asLong());
                }
            }
            else {
                if(worldPosition.getY() < 8) this.setLootTable(LOOT_GRAVEL_DEEP_CAVE, this.getBlockPos().asLong());
                else if(worldPosition.getY() < 32) this.setLootTable(LOOT_GRAVEL_REGULAR_CAVE, this.getBlockPos().asLong());
                else {
                    Holder<Biome> biome = world.getBiome(this.worldPosition);
                    // By key: a Holder never equalled a ResourceKey, so this never applied before 26.3.
                    if(biome.is(CustomOverworldBiomes.REGULAR_CAVE)) this.setLootTable(LOOT_GRAVEL_REGULAR_CAVE, this.getBlockPos().asLong());
                    else if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) this.setLootTable(LOOT_GRAVEL_AQUATIC, this.getBlockPos().asLong());
                    else if (worldPosition.getY() < 56 || world.getBrightness(LightLayer.SKY, worldPosition) < 3) this.setLootTable(LOOT_GRAVEL_REGULAR_CAVE, this.getBlockPos().asLong());
                    else this.setLootTable(LOOT_GRAVEL_SURFACE, this.getBlockPos().asLong());
                }
            }
        }
    }
}
