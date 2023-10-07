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
        this.prevFoodLevel = this.foodLevel;

        // Hunger effect is more punishing:
        if(player.getStatusEffect(StatusEffects.HUNGER) != null) this.exhaustion += 0.08f;

        // Sprinting is more punishing:
        if(player.isSprinting()) this.exhaustion += 0.02f;
        if(!player.isSprinting()) this.exhaustion = Math.max(this.exhaustion - 0.005f, 0.0f);

        // Food exhaustion and starvation:
        if (exhaustion > 8.0F) {
            exhaustion = 0.0F;
            if (saturationLevel == 0.0F) {
                if(foodLevel > 0) foodLevel--;
                else player.damage(player.getDamageSources().starve(), 2.0f);
            }
            else saturationLevel = Math.max(0.0F, saturationLevel - 0.5F);
        }

        // Natural regeneration:
        //  - FoodLevel regenerates over time, back to health level, if below.
        //  - Health regenerates quickly up to food level, otherwise.
        boolean canPlayerRegenHealth = player.canFoodHeal() && (foodLevel == 20 || foodLevel > Math.ceil(player.getHealth())) && player.world.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION);
        boolean canPlayerRegenHunger = !player.isSprinting() && (foodLevel < Math.ceil(player.getHealth()) && foodLevel < 20);

        if (canPlayerRegenHealth) {
            foodTickTimer++;
            if(player.hurtTime > 0 && !player.isOnFire()) {
                foodTickTimer = -16;
            }
            else if(foodTickTimer > 32){
                foodTickTimer = 0;
                player.heal(1);
                if (saturationLevel > 0.0F) saturationLevel = Math.max(0.0F, saturationLevel - 1.0F);
                else if(foodLevel - Math.ceil(player.getHealth()) > 1) exhaustion += 4.0F;
                else if(exhaustion < 5.0f) exhaustion += 1.0F;
            }
        }
        else if (canPlayerRegenHunger) {
            foodTickTimer++;
            if(foodTickTimer > 16){
                foodTickTimer = 0;
                foodLevel += 1;
            }
        }
        else {
            foodTickTimer = -16;
        }


    }
}
