package frootloops.versus.mixin.players;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HungerManager.class)
public class HungerManagerMixin {

    @Shadow
    private int foodLevel;
    @Shadow
    private float exhaustion;
    @Shadow
    private int foodTickTimer;
    @Shadow
    private float saturationLevel;
    @Shadow
    private int prevFoodLevel;

    private static final int REGEN_TIME_6_HAUNCHES = 128, REGEN_TIME_7_TO_10_HAUNCHES = 64, REGEN_TIME_11_TO_14_HAUNCHES = 48, REGEN_TIME_15_TO_20_HAUNCHES = 40, REGEN_TIME_SATURATION = 32;

    private static final float MINIMUM_SATURATION_TO_QUICK_HEAL = 4.0f;

    /***
     * @author
     * @reason
     * @param player
     */
    @Overwrite
    public void update(PlayerEntity player) {

        // Hunger effect is more punishing:
        if (player.getStatusEffect(StatusEffects.HUNGER) != null) this.exhaustion += 0.025f;

        // Food exhaustion:
        this.doHungerExhaustion(player);

        // Natural regeneration:
        this.doHealthRegeneration(player);

        // Update value:
        this.prevFoodLevel = this.foodLevel;
    }

    private void doHungerExhaustion(PlayerEntity player) {

        // Food exhaustion:

        //  - Hunger is twice as slow as previously when over or equal to 3 haunches (6 foodLevel).
        //    Food loss is primarily due to damage taken, and healing.

        //  - When over 3 haunches, but not jumping around or sprinting, saturation will slowly build back up to 3.
        //    This is to reduce the stress or need to constantly eat to prevent food loss.

        //  - Finally, when below 3 haunches, and unable to heal, that's when we want players to feel like
        //    they need to scavenge to survive, and feel the threat of starvation looming. Players will need to totally
        //    neglect their food bar to get to that point, which drives interesting (and stressful) gameplay!

        // Saturation regenerates back up to 3 after no activity, when over 3 haunches:
        if (foodLevel > 6) {
            this.exhaustion = Math.max(-0.01f, this.exhaustion - 0.0025f);
            if (this.exhaustion == -0.01f) {
                this.exhaustion = 1.0f;
                this.saturationLevel = Math.min(3f, saturationLevel + 1f);
            }

        // Food exhaustion: When starving, activities deal damage.
        } else if(foodLevel == 0) {
            if(exhaustion > 0.5F) {
                exhaustion = 0.0F;
                player.damage(player.getDamageSources().starve(), 1.0f);
            }

        // Food exhaustion: Faster when the player has saturation, slower otherwise.
        } else if(exhaustion > 2.0F && (foodLevel < 6 || saturationLevel > 0.0F)){
            exhaustion = 0.0F;
            if(saturationLevel > 0.0F) saturationLevel = Math.max(0.0F, saturationLevel - 1.0F);
            else foodLevel--;

        } else if(exhaustion > 8.0F){
            exhaustion = 0.0F;
            foodLevel--;
        }
    }

    private void doHealthRegeneration(PlayerEntity player) {
        // Natural regeneration:
        //  - Players can start healing from food about 2 seconds after they were damaged.
        //  - Saturation is used up first to quick heal, until under 4.0f.
        //  - Each haunch then heals one heart, one to one. Until sprint loss (3 haunches, or up to 6 hearts healed).
        //  - Finally, when at 3 haunches, sprint is lost, but players still slowly, passively heal (almost for free).

        // Design notes:
        //  - This helps rebalance sprinting, especially in combat; taking too much damage, without eating, endangers you. Players
        //    have to be smart about when to disengage from fights and when to eat.

        //  - Food eating time also plays a huge role in making this new system work as well as it does. Foods are much quicker to eat, and
        //    much less interrupting, yet eating can be interrupted by being attacked. Players can pick foods depending on their activity
        //    (snacks or stews for sprinting in combat, or meats for better inventory efficiency when outside immediate danger.
        //  - If food eating time stayed at 32 ticks, like vanilla, this system would probably be annoying rather than engaging.

        //  - It also means foods stacking isn't much of a balance issue, since while you're not required to eat much to stay full health,
        //    taking damage can mean quickly going through your stack of food.

        //  - When building, you don't need to worry about constantly eating after taking small damage since healing is almost free on 3 haunches.
        //    I want to drive players to build infrastructure with a lack of sprinting in mind (old school vibes!)

        //  - When exploring, so long as you don't make too many mistakes, hunger won't go down much, either. I want to give incentive for eating
        //    food that you find on your travels, which makes for.

        boolean canPlayerRegenHealth = player.canFoodHeal() && (foodLevel > 5) && player.world.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION);
        if (canPlayerRegenHealth) {
            foodTickTimer++;
            if(player.isOnFire()) {
                foodTickTimer = -8;
            }
            else if(player.hurtTime > 0) {
                foodTickTimer = -32;
            }
            else if(foodTickTimer > REGEN_TIME_SATURATION & saturationLevel > MINIMUM_SATURATION_TO_QUICK_HEAL){
                foodTickTimer = 0;
                player.heal(1);
                saturationLevel = Math.max(0.0F, saturationLevel - 1.5F);
                exhaustion = 1.0F;
            }
            else if(foodTickTimer > REGEN_TIME_15_TO_20_HAUNCHES && foodLevel > 14){
                foodTickTimer = 0;
                player.heal(1);
                foodLevel--;
                exhaustion = 1.0F;
            }
            else if(foodTickTimer > REGEN_TIME_11_TO_14_HAUNCHES && foodLevel > 10){
                foodTickTimer = 0;
                player.heal(1);
                foodLevel--;
                exhaustion = 1.0F;
            }
            else if(foodTickTimer > REGEN_TIME_7_TO_10_HAUNCHES && foodLevel > 6){
                foodTickTimer = 0;
                player.heal(1);
                foodLevel--;
                exhaustion = 1.0F;
            }
            else if(foodTickTimer > REGEN_TIME_6_HAUNCHES && foodLevel == 6){
                foodTickTimer = 0;
                player.heal(1);
                exhaustion += 0.5F;
            }
        }
    }
}
