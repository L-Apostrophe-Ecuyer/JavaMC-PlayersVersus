package frootloops.versus.gametest;

import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.fruit.Fruits;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** A lemon is a little food and a quick Sour. (Nothing grows lemons now: the lemon trees and bushes are gone.) */
public class LemonsGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    /**
     * A lemon is a little food and a quick Sour: 1 hunger, no saturation, 40 ticks to eat; eaten, it makes one Sour for 4
     * seconds, slow for 4 and hasty (Haste I) for 24.
     */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aLemonIsALittleFoodAndAQuickSour(GameTestHelper helper) {
        ItemStack lemon = new ItemStack(Fruits.LEMON);
        FoodProperties food = lemon.get(DataComponents.FOOD);
        helper.assertTrue(food != null && food.nutrition() == 1 && food.saturation() == 0.0F,
                "a lemon fills " + (food == null ? "nothing" : food.nutrition() + " hunger and " + food.saturation() + " saturation") + ", not 1 and 0");
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        helper.assertTrue(lemon.getUseDuration(pig) == 40, "a lemon takes " + lemon.getUseDuration(pig) + " ticks to eat, not 40");
        lemon.finishUsingItem(helper.getLevel(), pig);
        MobEffectInstance sour = pig.getEffect(CustomStatusEffects.SOUR);
        MobEffectInstance slow = pig.getEffect(MobEffects.SLOWNESS);
        MobEffectInstance haste = pig.getEffect(MobEffects.HASTE);
        helper.assertTrue(sour != null && sour.getDuration() == 80, "Sour lasts " + (sour == null ? 0 : sour.getDuration()) + " ticks, not 80");
        helper.assertTrue(slow != null && slow.getDuration() == 80 && slow.getAmplifier() == 0, "the lemon's slowness: " + slow);
        helper.assertTrue(haste != null && haste.getDuration() == 480 && haste.getAmplifier() == 0, "the lemon's haste: " + haste);
        helper.succeed();
    }
}
