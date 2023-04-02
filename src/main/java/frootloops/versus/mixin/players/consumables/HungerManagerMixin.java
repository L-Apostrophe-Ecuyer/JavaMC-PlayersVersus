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

        // Hunger is more punishing:
        if(player.getStatusEffect(StatusEffects.HUNGER) != null) this.exhaustion += 0.025f;

        // Natural Regeneration (works with >20 max health):
        if (player.canFoodHeal() && (foodLevel == 20 || foodLevel > Math.ceil(player.getHealth())) &&
                player.world.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION)) {

            foodTickTimer++;
            if(player.hurtTime > 0 && !player.isOnFire()) {
                foodTickTimer = -16;
            }
            else if(foodTickTimer > 40){
                foodTickTimer = 0;
                player.heal(1);
                if (saturationLevel > 0.0F) saturationLevel = Math.max(0.0F, saturationLevel - 1.0F);
                else if(foodLevel - Math.ceil(player.getHealth()) > 1) exhaustion += 3.0F;
                else if(exhaustion < 5.0f) exhaustion += 1.0F;
            }
        }

        // Starvation
        else if (foodLevel == 0 && exhaustion > 0.5F) {
            exhaustion = 0.0F;
            player.damage(player.getDamageSources().starve(), 1.0f);
        }

        // Food Exhaustion
        if (exhaustion > 6.0F) {
            exhaustion = 0.0F;
            if (saturationLevel == 0.0F) foodLevel--;
            else saturationLevel = Math.max(0.0F, saturationLevel - 0.2F);
        }
    }
}
