package frootloops.versus.mixin.items;

import frootloops.versus.mod.items.VanillaItemsAndStacks;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ClickType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ItemStack.class)
public abstract class VanillaItemsMixin implements ComponentHolder, FabricItemStack {

        @Shadow private final Item item;
        @Shadow private int count;
        @Shadow final MergedComponentMap components;

        protected VanillaItemsMixin(@Nullable Item item, int count, MergedComponentMap components) {
                this.item = item;
                this.count = count;
                this.components = components;
        }

        @ModifyVariable(method = "Lnet/minecraft/item/ItemStack;<init>(Lnet/minecraft/item/ItemConvertible;I)V", at = @At("HEAD"), ordinal = 0)
        private static ItemConvertible injected(ItemConvertible item) {
                return (ItemConvertible) VanillaItemsAndStacks.getReplacementItem(item.asItem());
        }

        @Inject(method = "isItemEnabled", at = @At("HEAD"), cancellable = true)
        public void isItemEnabled(FeatureSet enabledFeatures, CallbackInfoReturnable<Boolean> cir) {
                if(VanillaItemsAndStacks.hasModdedReplacementItem(item)) cir.setReturnValue(false);
        }

        @Inject(method = "onClicked", at = @At("HEAD"), cancellable = false)
        public void onClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference, CallbackInfoReturnable<Boolean> cir) {
                if(VanillaItemsAndStacks.hasVanillaReplacementItem(item)) {
                        ItemStack newStack = new ItemStack(VanillaItemsAndStacks.getVanillaReplacementItem(item).getRegistryEntry(), count, components.getChanges());
                        slot.setStack(newStack);
                }
        }

        @Inject(method = "getMaxCount", at = @At("HEAD"), cancellable = true)
        public void getMaxCount(CallbackInfoReturnable<Integer> cir) {
                int customMaxCount = VanillaItemsAndStacks.getOverhauledMaxStackSize(item);
                if(customMaxCount > 0) {
                        cir.setReturnValue(VanillaItemsAndStacks.getOverhauledMaxStackSize(item));
                        cir.cancel();
                }
        }



        /*
        private static Item registerVanillaItem(String id, Item item) {
                overridenVanillaIDs.add(id);
                return Items.register(Identifier.ofVanilla(id), item);
        }



        @Shadow public static final Item RECOVERY_COMPASS = registerVanillaItem("recovery_compass", new RecoveryCompassItem(new Item.Settings()));
        @Shadow public static final Item GLISTERING_MELON_SLICE = registerVanillaItem("glistering_melon_slice", new Item(new Item.Settings().food(new FoodComponent.Builder().nutrition(3).saturationModifier(0.8f).build())));
        @Shadow public static final Item BLAZE_POWDER = registerVanillaItem("blaze_powder", new ConcentrateItem(StatusEffects.STRENGTH));
        @Shadow public static final Item MAGMA_CREAM = registerVanillaItem("magma_cream", new ConcentrateItem(StatusEffects.FIRE_RESISTANCE));
        @Shadow public static final Item FERMENTED_SPIDER_EYE = registerVanillaItem("fermented_spider_eye", new ConcentrateItem(StatusEffects.NAUSEA, 240, 2));


        // Tools:
        @Shadow public static final Item WOODEN_SWORD = registerVanillaItem("wooden_sword", new SwordItem(RebalancedTools.WOOD, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getSwordReachModifier())));
        @Shadow public static final Item WOODEN_SHOVEL = registerVanillaItem("wooden_shovel", new ShovelItem(RebalancedTools.WOOD, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item WOODEN_PICKAXE = registerVanillaItem("wooden_pickaxe", new PickaxeItem(RebalancedTools.WOOD, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item WOODEN_AXE = registerVanillaItem("wooden_axe", new AxeItem(RebalancedTools.WOOD, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item WOODEN_HOE = registerVanillaItem("wooden_hoe", new HoeItem(RebalancedTools.WOOD, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getHoeReachModifier())));

        @Shadow public static final Item STONE_SWORD = registerVanillaItem("stone_sword", new SwordItem(RebalancedTools.STONE, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getSwordReachModifier())));
        @Shadow public static final Item STONE_SHOVEL = registerVanillaItem("stone_shovel", new ShovelItem(RebalancedTools.STONE, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item STONE_PICKAXE = registerVanillaItem("stone_pickaxe", new PickaxeItem(RebalancedTools.STONE, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item STONE_AXE = registerVanillaItem("stone_axe", new AxeItem(RebalancedTools.STONE, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item STONE_HOE = registerVanillaItem("stone_hoe", new HoeItem(RebalancedTools.STONE, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getHoeReachModifier())));

        @Shadow public static final Item GOLDEN_SWORD = registerVanillaItem("golden_sword", new SwordItem(RebalancedTools.GOLD, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getSwordReachModifier())));
        @Shadow public static final Item GOLDEN_SHOVEL = registerVanillaItem("golden_shovel", new ShovelItem(RebalancedTools.GOLD, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item GOLDEN_PICKAXE = registerVanillaItem("golden_pickaxe", new PickaxeItem(RebalancedTools.GOLD, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item GOLDEN_AXE = registerVanillaItem("golden_axe", new AxeItem(RebalancedTools.GOLD, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item GOLDEN_HOE = registerVanillaItem("golden_hoe", new HoeItem(RebalancedTools.GOLD, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getHoeReachModifier())));

        @Shadow public static final Item IRON_SWORD = registerVanillaItem("iron_sword", new SwordItem(RebalancedTools.IRON, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getSwordReachModifier())));
        @Shadow public static final Item IRON_SHOVEL = registerVanillaItem("iron_shovel", new ShovelItem(RebalancedTools.IRON, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item IRON_PICKAXE = registerVanillaItem("iron_pickaxe", new PickaxeItem(RebalancedTools.IRON, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item IRON_AXE = registerVanillaItem("iron_axe", new AxeItem(RebalancedTools.IRON, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item IRON_HOE = registerVanillaItem("iron_hoe", new HoeItem(RebalancedTools.IRON, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getHoeReachModifier())));

        @Shadow public static final Item DIAMOND_SWORD = registerVanillaItem("diamond_sword", new SwordItem(RebalancedTools.DIAMOND, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getSwordReachModifier())));
        @Shadow public static final Item DIAMOND_SHOVEL = registerVanillaItem("diamond_shovel", new ShovelItem(RebalancedTools.DIAMOND, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item DIAMOND_PICKAXE = registerVanillaItem("diamond_pickaxe", new PickaxeItem(RebalancedTools.DIAMOND, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item DIAMOND_AXE = registerVanillaItem("diamond_axe", new AxeItem(RebalancedTools.DIAMOND, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings()));
        @Shadow public static final Item DIAMOND_HOE = registerVanillaItem("diamond_hoe", new HoeItem(RebalancedTools.DIAMOND, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getHoeReachModifier())));

        @Shadow public static final Item NETHERITE_SWORD = registerVanillaItem("netherite_sword", new SwordItem(RebalancedTools.NETHERITE, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getSwordReachModifier()).fireproof()));
        @Shadow public static final Item NETHERITE_SHOVEL = registerVanillaItem("netherite_shovel", new ShovelItem(RebalancedTools.NETHERITE, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings().fireproof()));
        @Shadow public static final Item NETHERITE_PICKAXE = registerVanillaItem("netherite_pickaxe", new PickaxeItem(RebalancedTools.NETHERITE, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings().fireproof()));
        @Shadow public static final Item NETHERITE_AXE = registerVanillaItem("netherite_axe", new AxeItem(RebalancedTools.NETHERITE, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings().fireproof()));
        @Shadow public static final Item NETHERITE_HOE = registerVanillaItem("netherite_hoe", new HoeItem(RebalancedTools.NETHERITE, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeSpeedModifier(), new Item.Settings().attributeModifiers(RebalancedTools.getHoeReachModifier()).fireproof()));

        */
}
