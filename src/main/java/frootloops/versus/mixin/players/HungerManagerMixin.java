package frootloops.versus.mixin.players;

import frootloops.versus.VersusSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(FoodData.class)
public class HungerManagerMixin {

    @Shadow
    private int foodLevel;
    @Shadow
    private float exhaustionLevel;
    @Shadow
    private int tickTimer;
    @Shadow
    private float saturationLevel;

    private float prevSaturationLevel = 0.0f;

    private static final int REGEN_TIME_SLOW = 48, REGEN_TIME_FAST = 24;
    private static final int SPRINT_RECOVERY_TIME_SLOW = 80, SPRINT_RECOVERY_TIME_FAST = 20;
    private static final int FOOD_REQUIRED_FOR_FAST_REGEN = 2;
    private static final int FOOD_REQUIRED_FOR_SLOW_REGEN = 1;
    private static boolean IS_SLOW_REGEN_ENABLED = true;

    @Inject(method = "eat(Lnet/minecraft/world/food/FoodProperties;)V", at = @At("HEAD"), cancellable = false)
    public void eat(FoodProperties foodComponent, CallbackInfo info) {
        exhaustionLevel = 0.0F;
        foodLevel = Math.max(foodLevel, 0);
        saturationLevel = Math.max(0.1F, saturationLevel);
        tickTimer = Math.max(16, tickTimer);
    }


    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void update(ServerPlayer player, CallbackInfo ci) {

        if(!VersusSettings.Combat.DO_FOOD_OVERHAUL) return;

        // Hunger effect is more punishing:
        boolean hasHungerEffect = player.hasEffect(MobEffects.HUNGER);
        if (hasHungerEffect) this.exhaustionLevel += foodLevel > 0 ? 0.03125f : 0.00390625f;

        // Food exhaustion:
        this.doHungerExhaustion(player, hasHungerEffect);

        // Natural regeneration:
        this.doHealthRegeneration(player, hasHungerEffect);

        // Disable vanilla behavior:
        ci.cancel();

    }

    private void doHungerExhaustion(ServerPlayer player, boolean hasHungerEffect) {

        // Starvation: When starving, activities deal damage.
        if(foodLevel == 0) {
            if(VersusSettings.Combat.IS_STARVATION_ENABLED || hasHungerEffect) {
                //if(FOOD_REQUIRED_FOR_SLOW_REGEN > 0) saturationLevel = 0.0f;
                if (exhaustionLevel > 0.75F) {
                    exhaustionLevel = 0.0F;
                    if(saturationLevel == 0.0f) player.hurtServer(player.level(), player.damageSources().starve(), 1.0f);
                    else saturationLevel = Math.max(0.0F, saturationLevel - 0.5F);
                }
            }
            else {
                if(saturationLevel > 0.25f) {
                    if(exhaustionLevel > 2.0F){
                        exhaustionLevel = 0.0F;
                        saturationLevel = Math.min(0.25F, saturationLevel - 1.0F);
                    }
                    prevSaturationLevel = saturationLevel;
                }

                if(tickTimer == -1) saturationLevel = prevSaturationLevel;
                else if(tickTimer < 0) saturationLevel = 0.0f;
                else if(saturationLevel < 0.25f) saturationLevel = 0.25f;
            }
        }

        // Regular food exhaustion, accelerated, to disincentive players filling their food bar unnecessarily:
        else if(exhaustionLevel > 4.0F){
            exhaustionLevel = 0.0F;
            if(saturationLevel > 0.0f) foodLevel--;
            else saturationLevel = Math.max(0.0F, saturationLevel - 0.5F);
        }
    }

    private void doHealthRegeneration(ServerPlayer player, boolean hasHungerEffect) {
        float playerHealth = player.getHealth();
        boolean isPlayerSlowlyDying = playerHealth < 20.0f && (player.hasEffect(MobEffects.WITHER) || (player.isOnFire() && !player.hasEffect(MobEffects.FIRE_RESISTANCE)) || (playerHealth > 1 && player.hasEffect(MobEffects.POISON)));
        boolean canPlayerRegenHealth = player.isHurt() && player.level().getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
        boolean canPlayerFastHeal = canPlayerRegenHealth && foodLevel >= FOOD_REQUIRED_FOR_FAST_REGEN && !hasHungerEffect && isPlayerSlowlyDying;
        boolean canPlayerSlowHeal = canPlayerRegenHealth && ((foodLevel >= FOOD_REQUIRED_FOR_FAST_REGEN && !canPlayerFastHeal) || (IS_SLOW_REGEN_ENABLED && foodLevel >= FOOD_REQUIRED_FOR_SLOW_REGEN));
        boolean canPlayerFoodHeal = canPlayerFastHeal || canPlayerSlowHeal;

        // Damage resets slow regen, but not quick regen:
        tickTimer++;
        if (canPlayerFoodHeal) tickTimer = Math.max(tickTimer, 0);
        else if (player.hurtTime > 0 || player.getInvulnerableTime() > 0) tickTimer = isPlayerSlowlyDying ? -SPRINT_RECOVERY_TIME_FAST: -SPRINT_RECOVERY_TIME_SLOW;
        else if (foodLevel == 0 && hasHungerEffect) tickTimer = Math.min(tickTimer, -2);

        if(canPlayerFastHeal) {
            if (tickTimer > REGEN_TIME_FAST) {
                player.setHealth((float) Math.ceil(playerHealth) + 1);
                tickTimer = 0;
                exhaustionLevel = (exhaustionLevel + 0.5F)/2.0f;
                if(saturationLevel > 1.0F) saturationLevel = Math.max(1.0F, saturationLevel - 1.0F);
                else foodLevel--;
            }
        }
        else if(canPlayerSlowHeal) {
            if (tickTimer > REGEN_TIME_SLOW) {
                player.setHealth((float) Math.ceil(playerHealth) + 1f);
                tickTimer = 0;
                exhaustionLevel += 0.25F;
                if(saturationLevel > 1.0F) saturationLevel = Math.max(1.0F, saturationLevel - 1.0F);
                else if(foodLevel > 0) foodLevel--;
            }
        }
        else tickTimer = Math.min(tickTimer, 0);
    }
}
