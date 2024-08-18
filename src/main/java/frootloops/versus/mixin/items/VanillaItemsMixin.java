package frootloops.versus.mixin.items;

import frootloops.versus.mod.items.equipment.RebalancedTools;
import frootloops.versus.mod.items.brewing.ConcentrateItem;
import frootloops.versus.mod.items.equipment.custom.RecoveryCompassItem;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(Items.class)
public class VanillaItemsMixin {


    @Shadow public static final Item RECOVERY_COMPASS = Items.register("recovery_compass", new RecoveryCompassItem(new Item.Settings()));

    @Shadow public static final Item GLISTERING_MELON_SLICE = Items.register("glistering_melon_slice", new Item(new Item.Settings().food(new FoodComponent.Builder().nutrition(3).saturationModifier(0.8f).build())));

    @Shadow public static final Item BLAZE_POWDER = Items.register("blaze_powder", new ConcentrateItem(StatusEffects.STRENGTH));

    @Shadow public static final Item MAGMA_CREAM = Items.register("magma_cream", new ConcentrateItem(StatusEffects.FIRE_RESISTANCE));

    @Shadow public static final Item FERMENTED_SPIDER_EYE = Items.register("fermented_spider_eye", new ConcentrateItem(StatusEffects.NAUSEA, 240, 2));


    // Tools:

    //int durability, float speed, float attackDamageBonus, int enchantmentValue;
    
    @Shadow public static final Item WOODEN_SWORD = Items.register("wooden_sword", new SwordItem(RebalancedTools.WOOD, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item WOODEN_SHOVEL = Items.register("wooden_shovel", new ShovelItem(RebalancedTools.WOOD, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item WOODEN_PICKAXE = Items.register("wooden_pickaxe", new PickaxeItem(RebalancedTools.WOOD, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item WOODEN_AXE = Items.register("wooden_axe", new AxeItem(RebalancedTools.WOOD, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item WOODEN_HOE = Items.register("wooden_hoe", new HoeItem(RebalancedTools.WOOD, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeDamageModifier(), new Item.Settings()));

    @Shadow public static final Item STONE_SWORD = Items.register("stone_sword", new SwordItem(RebalancedTools.STONE, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item STONE_SHOVEL = Items.register("stone_shovel", new ShovelItem(RebalancedTools.STONE, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item STONE_PICKAXE = Items.register("stone_pickaxe", new PickaxeItem(RebalancedTools.STONE, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item STONE_AXE = Items.register("stone_axe", new AxeItem(RebalancedTools.STONE, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item STONE_HOE = Items.register("stone_hoe", new HoeItem(RebalancedTools.STONE, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeDamageModifier(), new Item.Settings()));

    @Shadow public static final Item GOLDEN_SWORD = Items.register("golden_sword", new SwordItem(RebalancedTools.GOLD, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item GOLDEN_SHOVEL = Items.register("golden_shovel", new ShovelItem(RebalancedTools.GOLD, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item GOLDEN_PICKAXE = Items.register("golden_pickaxe", new PickaxeItem(RebalancedTools.GOLD, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item GOLDEN_AXE = Items.register("golden_axe", new AxeItem(RebalancedTools.GOLD, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item GOLDEN_HOE = Items.register("golden_hoe", new HoeItem(RebalancedTools.GOLD, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeDamageModifier(), new Item.Settings()));

    @Shadow public static final Item IRON_SWORD = Items.register("iron_sword", new SwordItem(RebalancedTools.IRON, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item IRON_SHOVEL = Items.register("iron_shovel", new ShovelItem(RebalancedTools.IRON, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item IRON_PICKAXE = Items.register("iron_pickaxe", new PickaxeItem(RebalancedTools.IRON, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item IRON_AXE = Items.register("iron_axe", new AxeItem(RebalancedTools.IRON, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item IRON_HOE = Items.register("iron_hoe", new HoeItem(RebalancedTools.IRON, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeDamageModifier(), new Item.Settings()));

    @Shadow public static final Item DIAMOND_SWORD = Items.register("diamond_sword", new SwordItem(RebalancedTools.DIAMOND, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item DIAMOND_SHOVEL = Items.register("diamond_shovel", new ShovelItem(RebalancedTools.DIAMOND, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item DIAMOND_PICKAXE = Items.register("diamond_pickaxe", new PickaxeItem(RebalancedTools.DIAMOND, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item DIAMOND_AXE = Items.register("diamond_axe", new AxeItem(RebalancedTools.DIAMOND, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings()));
    @Shadow public static final Item DIAMOND_HOE = Items.register("diamond_hoe", new HoeItem(RebalancedTools.DIAMOND, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeDamageModifier(), new Item.Settings()));
    
    @Shadow public static final Item NETHERITE_SWORD = Items.register("netherite_sword", new SwordItem(RebalancedTools.NETHERITE, RebalancedTools.getSwordDamageModifier(), RebalancedTools.getSwordSpeedModifier(), new Item.Settings().fireproof()));
    @Shadow public static final Item NETHERITE_SHOVEL = Items.register("netherite_shovel", new ShovelItem(RebalancedTools.NETHERITE, RebalancedTools.getShovelDamageModifier(), RebalancedTools.getShovelSpeedModifier(), new Item.Settings().fireproof()));
    @Shadow public static final Item NETHERITE_PICKAXE = Items.register("netherite_pickaxe", new PickaxeItem(RebalancedTools.NETHERITE, RebalancedTools.getPickaxeDamageModifier(), RebalancedTools.getPickaxeSpeedModifier(), new Item.Settings().fireproof()));
    @Shadow public static final Item NETHERITE_AXE = Items.register("netherite_axe", new AxeItem(RebalancedTools.NETHERITE, RebalancedTools.getAxeDamageModifier(), RebalancedTools.getAxeSpeedModifier(), new Item.Settings().fireproof()));
    @Shadow public static final Item NETHERITE_HOE = Items.register("netherite_hoe", new HoeItem(RebalancedTools.NETHERITE, RebalancedTools.getHoeDamageModifier(), RebalancedTools.getHoeDamageModifier(), new Item.Settings().fireproof()));

}
