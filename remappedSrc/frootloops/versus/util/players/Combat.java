package frootloops.versus.mod.players;


import com.google.common.collect.ImmutableMultimap;
import frootloops.versus.mixin.players.accessors.ItemAccessor;
import frootloops.versus.mixin.players.accessors.MiningToolAccessor;
import frootloops.versus.mixin.players.accessors.SwordAccessor;
import frootloops.versus.mixin.players.accessors.TridentAccessor;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public abstract class Combat {

    public static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0d;
    public static final double PLAYER_BASE_ATTACK_SPEED = 4.0d;
    public static final double PLAYER_BASE_ATTACK_REACH = 2.5d;
    private static final String[] tools = new String[]{"axe", "sword", "hoe", "pickaxe", "shovel"};
    private static final float[] toolsSpeed  = new float[]{1.0F, 1.6F, 2.4F, 1.2F, 1.4F};
    private static final float[] toolsDamage = new float[]{7.0F, 4.0F, 1.0F, 2.0F, 3.0F};
    private static final float[] toolsReach = new float[]{2.5F, 3.0F, 3.5F, 2.5F, 2.5F};

    private static final String[] toolTiers = new String[]{"wooden", "stone", "golden", "iron", "diamond", "netherite"};
    private static final float[] toolTierDamageBonuses = new float[]{0F, 0F, 1F, 1F, 2F, 3F};


    public static void init() {

        for(int toolIndex = 0; toolIndex < tools.length; toolIndex++) {
            for (int tierIndex = 0; tierIndex < toolTiers.length; tierIndex++) {
                String name = "minecraft:" + toolTiers[tierIndex] + "_" + tools[toolIndex];
                float damage = toolsDamage[toolIndex] + toolTierDamageBonuses[tierIndex] - (float)PLAYER_BASE_ATTACK_DAMAGE;
                float speed = toolsSpeed[toolIndex] - (float)PLAYER_BASE_ATTACK_SPEED;
                float reach = toolsReach[toolIndex] - (float)PLAYER_BASE_ATTACK_REACH;
                setAttributes(name, damage, speed, reach);
            }
        }

        setAttributes("minecraft:trident",
                8.0F - (float)PLAYER_BASE_ATTACK_DAMAGE,
                1.0F - (float)PLAYER_BASE_ATTACK_SPEED,
                3.5F  - (float)PLAYER_BASE_ATTACK_REACH);
    }

    private static void setAttributes(String itemName, float damage, float speed, float reach) {
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> itemBuilder = ImmutableMultimap.builder();
        Item item = Registries.ITEM.get(new Identifier(itemName));
        String modifierType = item instanceof MiningToolItem ? "Tool modifier" : "Weapon modifier";

        itemBuilder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                new EntityAttributeModifier(((ItemAccessor) item).getATTACK_DAMAGE_MODIFIER_ID(), modifierType, damage, EntityAttributeModifier.Operation.ADDITION));

        itemBuilder.put(EntityAttributes.GENERIC_ATTACK_SPEED,
                new EntityAttributeModifier(((ItemAccessor) item).getATTACK_SPEED_MODIFIER_ID(), modifierType, speed, EntityAttributeModifier.Operation.ADDITION));

        itemBuilder.put(ReachEntityAttributes.ATTACK_RANGE,
                new EntityAttributeModifier(modifierType, reach, EntityAttributeModifier.Operation.ADDITION));

        if (item instanceof MiningToolItem) {
            ((MiningToolAccessor) item).setAttackDamage(damage);
            ((MiningToolAccessor) item).setAttributeModifiers(itemBuilder.build());
        } else if (item instanceof SwordItem) {
            ((SwordAccessor) item).setAttackDamage(damage);
            ((SwordAccessor) item).setAttributeModifiers(itemBuilder.build());
        } else if (item instanceof TridentItem) {
            ((TridentAccessor) item).setAttributeModifiers(itemBuilder.build());
        }
    }
}