package frootloops.versus.util;


import com.google.common.collect.ImmutableMultimap;
import frootloops.versus.mixin.players.accessors.ItemAccessor;
import frootloops.versus.mixin.players.accessors.MiningToolAccessor;
import frootloops.versus.mixin.players.accessors.SwordAccessor;
import frootloops.versus.mixin.players.accessors.TridentAccessor;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
//import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public abstract class Combat {

    public static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0d;
    public static final double PLAYER_BASE_ATTACK_SPEED = 3.0d;
    public static final double PLAYER_BASE_ATTACK_REACH = 0.0d;
    private static final String[] tools = new String[]{"axe", "sword", "hoe", "pickaxe", "shovel"};
    private static final float[] toolsSpeed  = new float[]{1.0F, 1.6F, 2.4F, 1.2F, 1.4F};
    private static final float[] toolsDamage = new float[]{7.0F, 4.0F, 1.0F, 3.0F, 3.0F};
    private static final float[] toolsReachBonus = new float[]{0.0F, 0.5F, 1.0F, 0.0F, 0.0F};

    private static final String[] toolTiers = new String[]{"wooden", "stone", "golden", "iron", "diamond", "netherite"};
    private static final float[] toolTierDamageBonuses = new float[]{0F, 0F, 1F, 1F, 2F, 3F};


    public static void init() {

        for(int toolIndex = 0; toolIndex < tools.length; toolIndex++) {
            for (int tierIndex = 0; tierIndex < toolTiers.length; tierIndex++) {
                String name = "minecraft:" + toolTiers[tierIndex] + "_" + tools[toolIndex];
                float damage = toolsDamage[toolIndex] + toolTierDamageBonuses[tierIndex] - (float)PLAYER_BASE_ATTACK_DAMAGE;
                float speed = toolsSpeed[toolIndex] - (float)PLAYER_BASE_ATTACK_SPEED;
                float reach = toolsReachBonus[toolIndex];
                setAttributes(name, damage, speed, reach);
            }
        }

        setAttributes("minecraft:trident",
                8.0F - (float)PLAYER_BASE_ATTACK_DAMAGE,
                1.0F - (float)PLAYER_BASE_ATTACK_SPEED,
                1.0F + (float)PLAYER_BASE_ATTACK_REACH);
    }

    private static void setAttributes(String itemName, float damageModifier, float speedModifier, float reachModifier) {
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> itemBuilder = ImmutableMultimap.builder();
        Item item = Registries.ITEM.get(new Identifier(itemName));
        String modifierType = item instanceof MiningToolItem ? "Tool modifier" : "Weapon modifier";

        itemBuilder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(((ItemAccessor) item).getATTACK_DAMAGE_MODIFIER_ID(), modifierType, damageModifier, EntityAttributeModifier.Operation.ADDITION));
        itemBuilder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(((ItemAccessor) item).getATTACK_SPEED_MODIFIER_ID(), modifierType, speedModifier, EntityAttributeModifier.Operation.ADDITION));
        //if(reachModifier != 0.0F) itemBuilder.put(ReachEntityAttributes.ATTACK_RANGE, new EntityAttributeModifier(modifierType, reachModifier, EntityAttributeModifier.Operation.ADDITION));

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
}