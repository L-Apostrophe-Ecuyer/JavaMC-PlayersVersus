package frootloops.versus.mixin.players;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static frootloops.versus.VersusSettings.IS_STARVATION_ENABLED;

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

    private float prevSaturationLevel = 0.0f;

    private static final int REGEN_TIME_SLOW = 48, REGEN_TIME_FAST = 32;
    private static final int SPRINT_RECOVERY_TIME_SLOW = 80, SPRINT_RECOVERY_TIME_FAST = 20;
    private static final int FOOD_REQUIRED_FOR_FAST_REGEN = 2;
    private static final int FOOD_REQUIRED_FOR_SLOW_REGEN = 1;
    private static boolean IS_SLOW_REGEN_ENABLED = true;

    @Inject(method = "eat", at = @At("HEAD"), cancellable = false)
    public void eat(FoodComponent foodComponent, CallbackInfo info) {
        exhaustion = 0.0F;
        foodLevel = Math.max(foodLevel, 0);
        saturationLevel = Math.max(0.1F, saturationLevel);
        foodTickTimer = Math.max(16, foodTickTimer);
    }


    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    public void update(ServerPlayerEntity player, CallbackInfo ci) {

        if(!VersusSettings.DO_FOOD_OVERHAUL) return;

        // Hunger effect is more punishing:
        boolean hasHungerEffect = player.hasStatusEffect(StatusEffects.HUNGER);
        if (hasHungerEffect) this.exhaustion += foodLevel > 0 ? 0.025f : 0.005f;

        // Food exhaustion:
        this.doHungerExhaustion(player, hasHungerEffect);

        // Natural regeneration:
        this.doHealthRegeneration(player, hasHungerEffect);

        // Disable vanilla behavior:
        ci.cancel();

    }

    private void doHungerExhaustion(ServerPlayerEntity player, boolean hasHungerEffect) {

        // Starvation: When starving, activities deal damage.
        if(foodLevel == 0) {
            if(IS_STARVATION_ENABLED || hasHungerEffect) {
                //if(FOOD_REQUIRED_FOR_SLOW_REGEN > 0) saturationLevel = 0.0f;
                if (exhaustion > 0.75F) {
                    exhaustion = 0.0F;
                    if(saturationLevel == 0.0f) player.damage(player.getServerWorld(), player.getDamageSources().starve(), 1.0f);
                    else saturationLevel = Math.max(0.0F, saturationLevel - 0.5F);
                }
            }
            else {
                if(saturationLevel > 0.25f) {
                    if(exhaustion > 2.0F){
                        exhaustion = 0.0F;
                        saturationLevel = Math.min(0.25F, saturationLevel - 1.0F);
                    }
                    prevSaturationLevel = saturationLevel;
                }

                if(foodTickTimer == -1) saturationLevel = prevSaturationLevel;
                else if(foodTickTimer < 0) saturationLevel = 0.0f;
                else if(saturationLevel < 0.25f) saturationLevel = 0.25f;
            }
        }

        // Regular food exhaustion, accelerated, to disincentive players filling their food bar unnecessarily:
        else if(exhaustion > 4.0F){
            exhaustion = 0.0F;
            if(saturationLevel > 0.0f) foodLevel--;
            else saturationLevel = Math.max(0.0F, saturationLevel - 0.5F);
        }
    }

    private void doHealthRegeneration(ServerPlayerEntity player, boolean hasHungerEffect) {
        float playerHealth = player.getHealth();
        boolean canPlayerRegenHealth = player.canFoodHeal() && player.getServerWorld().getGameRules().getBoolean(GameRules.NATURAL_REGENERATION);
        boolean canPlayerFastHeal = canPlayerRegenHealth && foodLevel >= FOOD_REQUIRED_FOR_FAST_REGEN && !hasHungerEffect;
        boolean canPlayerSlowHeal = canPlayerRegenHealth && ((foodLevel >= FOOD_REQUIRED_FOR_FAST_REGEN && !canPlayerFastHeal) || (IS_SLOW_REGEN_ENABLED && foodLevel >= FOOD_REQUIRED_FOR_SLOW_REGEN));
        boolean canPlayerFoodHeal = canPlayerFastHeal || canPlayerSlowHeal;

        // Damage resets slow regen, but not quick regen:
        foodTickTimer++;
        if (canPlayerFoodHeal) foodTickTimer = Math.max(foodTickTimer, 0);
        else if (player.isOnFire()) foodTickTimer = -SPRINT_RECOVERY_TIME_FAST;
        else if (player.hurtTime > 0 || player.timeUntilRegen > 0) foodTickTimer = -SPRINT_RECOVERY_TIME_SLOW;
        else if (foodLevel == 0 && hasHungerEffect) foodTickTimer = Math.min(foodTickTimer, -2);

        if(canPlayerFastHeal) {
            if (foodTickTimer > REGEN_TIME_FAST) {
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
                exhaustion += 0.25F;
                if(saturationLevel > 1.0F) saturationLevel = Math.max(1.0F, saturationLevel - 1.0F);
                else if(foodLevel > 0) foodLevel--;
            }
        }
        else foodTickTimer = Math.min(foodTickTimer, 0);
    }
}
