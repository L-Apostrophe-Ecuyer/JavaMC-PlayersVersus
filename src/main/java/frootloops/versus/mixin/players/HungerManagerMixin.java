package frootloops.versus.mixin.players;

import frootloops.versus.VersusSettings;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    private static final int REGEN_TIME_SLOW = 120, REGEN_TIME_7_TO_10_HAUNCHES = 40, REGEN_TIME_11_TO_14_HAUNCHES = 30, REGEN_TIME_15_TO_20_HAUNCHES = 20;
    private static final int FOOD_LEVEL_FOR_SLOW_REGEN = 0;
    private static boolean IS_STARVATION_ENABLED = false;

    @Inject(method = "eat", at = @At("HEAD"), cancellable = false)
    public void eat(ItemStack stack, CallbackInfo info) {
        if(stack.contains(DataComponentTypes.FOOD)) foodTickTimer = Math.max(8, foodTickTimer);
    }


    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    public void update(PlayerEntity player, CallbackInfo ci) {

        if(!VersusSettings.DO_FOOD_OVERHAUL) return;

        // Hunger effect is more punishing:
        if (player.getStatusEffect(StatusEffects.HUNGER) != null) this.exhaustion += 0.025f;

        // Food exhaustion:
        this.doHungerExhaustion(player);

        // Natural regeneration:
        this.doHealthRegeneration(player);

        // Update value:
        this.prevFoodLevel = this.foodLevel;

        // Disable vanilla behavior:
        ci.cancel();

    }

    private void doHungerExhaustion(PlayerEntity player) {

        // Starvation: When starving, activities deal damage.
        if(foodLevel == 0) {
            if(IS_STARVATION_ENABLED) {
                if(FOOD_LEVEL_FOR_SLOW_REGEN > 0) saturationLevel = 0.0f;
                if (exhaustion > 0.5F) {
                    exhaustion = 0.0F;
                    if(saturationLevel == 0.0f) player.damage(player.getDamageSources().starve(), 1.0f);
                    else saturationLevel = Math.max(0.0F, saturationLevel - 0.5F);
                }
            }
            else {
                if(player.isSprinting()) saturationLevel = 0.0f;
                else saturationLevel = 3.0f;
            }
        }

        // Regular food exhaustion, accelerated, to disincentive players filling their food bar unnecessarily:
        else if(exhaustion > 2.0F){
            exhaustion = 0.0F;
            saturationLevel = Math.max(0.0F, saturationLevel - 0.5F);
        }
    }

    private void doHealthRegeneration(PlayerEntity player) {
        float playerHealth = player.getHealth();
        boolean canPlayerRegenHealth = player.canFoodHeal() && player.getWorld().getGameRules().getBoolean(GameRules.NATURAL_REGENERATION);
        boolean canPlayerFoodHeal = canPlayerRegenHealth && (foodLevel > FOOD_LEVEL_FOR_SLOW_REGEN) && !player.hasStatusEffect(StatusEffects.HUNGER);
        boolean canPlayerSlowHeal = canPlayerRegenHealth && (foodLevel > FOOD_LEVEL_FOR_SLOW_REGEN && !canPlayerFoodHeal) || (foodLevel == FOOD_LEVEL_FOR_SLOW_REGEN && (FOOD_LEVEL_FOR_SLOW_REGEN != 0 || !player.isSprinting()));
        if (canPlayerFoodHeal || canPlayerSlowHeal) {
            foodTickTimer++;

            // Damage resets slow regen, but not quick regen:
            if (canPlayerFoodHeal) foodTickTimer = Math.max(foodTickTimer, 0);
            else if (player.isOnFire()) foodTickTimer = -8;
            else if (player.hurtTime > 0) foodTickTimer = -64;

            if(canPlayerFoodHeal) {
                if ((foodTickTimer > REGEN_TIME_15_TO_20_HAUNCHES && foodLevel > 14)
                        || (foodTickTimer > REGEN_TIME_11_TO_14_HAUNCHES && foodLevel > 10)
                        || (foodTickTimer > REGEN_TIME_7_TO_10_HAUNCHES && foodLevel > FOOD_LEVEL_FOR_SLOW_REGEN)) {
                    player.setHealth((float) Math.ceil(playerHealth) + 1);
                    foodTickTimer = 0;
                    exhaustion = (exhaustion + 0.5F)/2.0f;
                    if(saturationLevel > 1.0F) saturationLevel = Math.max(1.0F, saturationLevel - 1.0F);
                    else foodLevel--;
                }
            }
            else if(canPlayerSlowHeal) {
                if (foodTickTimer > REGEN_TIME_SLOW) {
                    player.setHealth((float) Math.ceil(playerHealth) + 1f);
                    foodTickTimer = 0;
                    exhaustion += 0.125F;
                }
            }
        }
        else {
            foodTickTimer = 0;
        }
    }
}
