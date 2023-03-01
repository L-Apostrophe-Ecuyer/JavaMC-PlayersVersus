package frootloops.versus.mixin.players;

import net.minecraft.entity.damage.DamageSource;
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

        // Natural Regeneration
        boolean hasSufficientHungerToHeal = (foodLevel == 20) || (foodLevel > Math.ceil(player.getHealth()) && player.canFoodHeal());
        if (hasSufficientHungerToHeal && player.world.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION)) {
            foodTickTimer++;
            if(player.hurtTime > 0 && !player.isOnFire()) {
                foodTickTimer = -16;
            }
            else if(foodTickTimer > 40){
                foodTickTimer = 0;
                player.heal(1);
                if (saturationLevel > 0.0F) saturationLevel = Math.max(0.0F, saturationLevel - 0.5F);
                else if(foodLevel - Math.ceil(player.getHealth()) > 1) exhaustion += 3.0F;
            }
        }

        // Starvation
        else if (foodLevel == 0 && exhaustion > 0.5F) {
            exhaustion = 0.0F;
            player.damage(DamageSource.field_5852, 1.0F);
        }

        // Food Exhaustion
        if (exhaustion > 6.0F) {
            exhaustion = 0.0F;
            if (saturationLevel == 0.0F) foodLevel--;
            else saturationLevel = Math.max(0.0F, saturationLevel - 0.5F);
        }
    }
}
