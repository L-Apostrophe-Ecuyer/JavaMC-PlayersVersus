package frootloops.versus.mixin.players.consumables;

import net.minecraft.entity.damage.DamageSource;
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
        if(player.isSprinting()) this.exhaustion += 0.01f;
        else if (foodLevel >= 6) {
            this.exhaustion = Math.max(0.0f, this.exhaustion - 0.0025f);
            if(this.exhaustion == 0.0f) {
                this.exhaustion = 1.0f;
                this.saturationLevel = Math.max(2.0F, saturationLevel);
            }
        }

        // Food exhaustion and starvation:
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
            if(foodTickTimer > 96){
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
                foodTickTimer = -16;
                player.setFireTicks(player.getFireTicks() - 1);
            }
            else if(player.hurtTime > 0) {
                foodTickTimer = -32;
            }
            else if(foodTickTimer > 24 & saturationLevel > 3.0F){
                foodTickTimer = 0;
                player.heal(1);
                saturationLevel = Math.max(0.0F, saturationLevel - 1.5F);
            }
            else if(foodTickTimer > 32 && foodLevel > 12){
                foodTickTimer = 0;
                player.heal(1);
                foodLevel--;
            }
            else if(foodTickTimer > 48 && foodLevel > 6){
                foodTickTimer = 0;
                player.heal(1);
                foodLevel--;
            }
            else if(foodTickTimer > 96 && foodLevel == 6){
                foodTickTimer = 0;
                player.heal(1);
                exhaustion += 0.5F;
            }
        }

        // Update:
        this.prevFoodLevel = this.foodLevel;
    }
}
