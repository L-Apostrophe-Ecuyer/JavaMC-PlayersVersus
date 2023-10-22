package frootloops.versus.mixin.players.consumables;

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

    private static final int REGEN_TIME_6_HAUNCHES = 96, REGEN_TIME_7_TO_10_HAUNCHES = 64, REGEN_TIME_11_TO_14_HAUNCHES = 48, REGEN_TIME_15_TO_20_HAUNCHES = 40, REGEN_TIME_SATURATION = 32;
    private static final int STARVATION_TIME = 96;
    private static final float SATURATION_REGEN_AMOUNT = 2.0f, MINIMUM_SATURATION_TO_QUICK_HEAL = 3.0f;

    /***
     * @author
     * @reason
     * @param player
     */
    @Overwrite
    public void update(PlayerEntity player) {

        // Hunger effect is more punishing:
        if(player.getStatusEffect(StatusEffects.HUNGER) != null) this.exhaustion += 0.025f;

        // Sprinting is more punishing, whilst choosing not to sprint is a lot less punishing:
        if(player.isSprinting()) this.exhaustion += 0.0075f;
        else if (foodLevel >= 6) {
            // Saturation regenerates back up to 2.0f after no activity:
            this.exhaustion = Math.max(0.0f, this.exhaustion - 0.0025f);
            if(this.exhaustion == 0.0f) {
                this.exhaustion = 1.0f;
                this.saturationLevel = Math.max(SATURATION_REGEN_AMOUNT, saturationLevel);
            }
        }

        // Food exhaustion:
        if(exhaustion > 1.0F && foodLevel == 0){
            exhaustion = 0.0F;
            player.damage(player.getDamageSources().starve(), 1.0f);

        } else if(exhaustion > 3.0F && (foodLevel <= 6 || saturationLevel > 0.0F)){
            exhaustion = 0.0F;
            if(saturationLevel > 0.0F) saturationLevel = Math.max(0.0F, saturationLevel - 1.0F);
            else foodLevel--;

        } else if(exhaustion > 9.0F){
            exhaustion = 0.0F;
            foodLevel--;
        }

        // Starvation
        if(foodLevel == 1) {
            foodTickTimer++;
            if(foodTickTimer > STARVATION_TIME){
                foodTickTimer = 0;
                player.damage(player.getDamageSources().starve(), 1.0f);
            }
        }

        // Natural regeneration:
        //  - FoodLevel regenerates over time, back to health level, if below.
        //  - Health regenerates quickly up to food level, otherwise.
        boolean canPlayerRegenHealth = player.canFoodHeal() && (foodLevel > 5) && player.world.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION);
        if (canPlayerRegenHealth) {
            foodTickTimer++;
            if(player.isOnFire()) {
                foodTickTimer = -24;
                player.setFireTicks(player.getFireTicks() - 1);
            }
            else if(player.hurtTime > 0) {
                foodTickTimer = -32;
            }
            else if(foodTickTimer > REGEN_TIME_SATURATION & saturationLevel > MINIMUM_SATURATION_TO_QUICK_HEAL){
                foodTickTimer = 0;
                player.heal(1);
                saturationLevel = Math.max(0.0F, saturationLevel - 1.5F);
            }
            else if(foodTickTimer > REGEN_TIME_15_TO_20_HAUNCHES && foodLevel > 14){
                foodTickTimer = 0;
                player.heal(1);
                foodLevel--;
            }
            else if(foodTickTimer > REGEN_TIME_11_TO_14_HAUNCHES && foodLevel > 10){
                foodTickTimer = 0;
                player.heal(1);
                foodLevel--;
            }
            else if(foodTickTimer > REGEN_TIME_7_TO_10_HAUNCHES && foodLevel > 6){
                foodTickTimer = 0;
                player.heal(1);
                foodLevel--;
            }
            else if(foodTickTimer > REGEN_TIME_6_HAUNCHES && foodLevel == 6){
                foodTickTimer = 0;
                player.heal(1);
                exhaustion += 0.5F;
            }
        }

        // Update:
        this.prevFoodLevel = this.foodLevel;
    }
}
