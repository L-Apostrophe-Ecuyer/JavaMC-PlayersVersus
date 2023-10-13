package frootloops.versus.util;


import com.google.common.collect.ImmutableMultimap;
import frootloops.versus.VersusMod;
import frootloops.versus.mixin.players.accessors.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
//import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public abstract class Combat {

    public static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0d;
    public static final double PLAYER_BASE_ATTACK_SPEED = 2.0d;
    public static final double PLAYER_BASE_ATTACK_REACH = 2.5d;
    private static final String[] tools = new String[]{"axe", "sword", "hoe", "pickaxe", "shovel"};
    private static final float[] toolsSpeed  = new float[]{1.0F, 1.6F, 2.4F, 1.2F, 1.4F};
    private static final float[] toolsDamage = new float[]{7.0F, 4.0F, 2.0F, 4.0F, 3.0F};

    private static final String[] toolTiers = new String[]{"wooden", "stone", "golden", "iron", "diamond", "netherite"};
    private static final float[] toolTierDamageBonuses = new float[]{0F, 0F, 1F, 1F, 2F, 3F};



    public static void init() {
        for(int toolIndex = 0; toolIndex < tools.length; toolIndex++) {
            for (int tierIndex = 0; tierIndex < toolTiers.length; tierIndex++) {
                String name = "minecraft:" + toolTiers[tierIndex] + "_" + tools[toolIndex];
                float damage = toolsDamage[toolIndex] + toolTierDamageBonuses[tierIndex] - (float)PLAYER_BASE_ATTACK_DAMAGE;
                float speed = toolsSpeed[toolIndex] - (float)PLAYER_BASE_ATTACK_SPEED;
                setAttributes(name, damage, speed);
            }
        }
        setAttributes("minecraft:trident", 8.0F - (float)PLAYER_BASE_ATTACK_DAMAGE, 1.0F - (float)PLAYER_BASE_ATTACK_SPEED);
    }

    private static void setAttributes(String itemName, float damageModifier, float speedModifier) {
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> itemBuilder = ImmutableMultimap.builder();
        Item item = Registries.ITEM.get(new Identifier(itemName));
        String modifierType = item instanceof MiningToolItem ? "Tool modifier" : "Weapon modifier";

        itemBuilder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(((ItemAccessor) item).getATTACK_DAMAGE_MODIFIER_ID(), modifierType, damageModifier, EntityAttributeModifier.Operation.ADDITION));
        itemBuilder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(((ItemAccessor) item).getATTACK_SPEED_MODIFIER_ID(), modifierType, speedModifier, EntityAttributeModifier.Operation.ADDITION));

        if (item instanceof MiningToolItem) {
            ((MiningToolAccessor) item).setAttackDamage(damageModifier);
            ((MiningToolAccessor) item).setAttributeModifiers(itemBuilder.build());
        } else if (item instanceof SwordItem) {
            ((SwordAccessor) item).setAttackDamage(damageModifier);
            ((SwordAccessor) item).setAttributeModifiers(itemBuilder.build());
        } else if (item instanceof TridentItem) {
            ((TridentAccessor) item).setAttributeModifiers(itemBuilder.build());
        }
    }

    public static int getTicksPerAttackOf(PlayerEntity player) {
        return (int)(20d / player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED));
    }

    public static double getAttackChargeProgress(PlayerEntity player) {
        int lastAttackTicks = ((LivingEntityAccessor)player).getLastAttackedTicks();
        double attackSpeed = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
        return Math.min(1.0d, (attackSpeed * (double)lastAttackTicks) / 20.0d);
    }

    public static double getAttackRangeBonusOf(ItemStack itemStack) {
        if(itemStack == null || itemStack.isEmpty() || !itemStack.isDamageable()) return 0.0d;
        Item item = itemStack.getItem();
        if(item instanceof TridentItem) return 1.0d;
        if(item instanceof HoeItem) return 1.0d;
        if(item instanceof SwordItem) return 0.5d;
        return 0.0d;
    }

    public static double getAttackRange(PlayerEntity player, double attackChargeProgress) {
        double toolReachBonus = Combat.getAttackRangeBonusOf(player.getEquippedStack(EquipmentSlot.MAINHAND));
        double chargeTimeBonus = attackChargeProgress * attackChargeProgress;
        double ridingBonus = player.hasVehicle() && player.getVehicle().isAlive() ? 0.5d : 0d;
        return Combat.PLAYER_BASE_ATTACK_REACH + chargeTimeBonus + toolReachBonus + ridingBonus;
    }

    public static double getAttackRange(PlayerEntity player) {
        return Combat.getAttackRange(player, Combat.getAttackChargeProgress(player));
    }
}