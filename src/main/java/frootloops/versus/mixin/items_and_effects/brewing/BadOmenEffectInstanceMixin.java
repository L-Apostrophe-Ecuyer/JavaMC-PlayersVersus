package frootloops.versus.mixin.items_and_effects.brewing;

import frootloops.versus.VersusSettings;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class BadOmenEffectInstanceMixin {

    @Shadow private final Holder<MobEffect> effect;

    @Shadow private int amplifier;

    @Shadow private int duration;

    public BadOmenEffectInstanceMixin(Holder<MobEffect> type) {
        this.effect = type;
    }

    @Shadow public int mapDuration(Int2IntFunction mapper) {
        if (this.duration <= 0) {
            return this.duration;
        }
        return mapper.applyAsInt(this.duration);
    }

    @Inject(method = "tickServer", at = @At(value = "HEAD"), cancellable = true)
    public void update(ServerLevel world, LivingEntity entity, Runnable hiddenEffectCallback, CallbackInfoReturnable<Boolean> cir) {
        if(duration > 0 && entity instanceof ServerPlayer serverPlayer) {
            ServerLevel serverWorld = serverPlayer.level();

            if (effect == MobEffects.BAD_OMEN) {
                if(serverWorld.getGameTime() % 57L != 0) return;

                // RAIDS: Check if Bad Omen can be replaced by Raid Omen
                boolean isPlayerInsideVillage = serverWorld.isVillage(serverPlayer.blockPosition());
                boolean isPlayerInsideTheirBase = false;
                if(!isPlayerInsideVillage && VersusSettings.Gameplay.DO_RAIDS_OUTSIDE_VILLAGES && serverPlayer.getRespawnConfig() != null && serverPlayer.getScore() > 999) {
                    boolean isNearRespawn = serverPlayer.getRespawnConfig().respawnData().dimension() == serverPlayer.level().dimension() && (serverPlayer.getRespawnConfig().respawnData().pos().closerThan(serverPlayer.blockPosition(), 32));
                    if(isNearRespawn) isPlayerInsideTheirBase = !serverWorld.getPoiManager().getInRange(poiType -> poiType.is(PoiTypeTags.ACQUIRABLE_JOB_SITE), serverPlayer.blockPosition(), 24, PoiManager.Occupancy.ANY).toList().isEmpty();
                }

                if ((isPlayerInsideVillage || isPlayerInsideTheirBase) && serverWorld.getRaidAt(serverPlayer.blockPosition()) == null) {
                    if(isPlayerInsideVillage) serverPlayer.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 600, amplifier));
                    else serverPlayer.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 3000, 0));

                    serverPlayer.setRaidOmenPosition(serverPlayer.blockPosition());
                    serverPlayer.removeEffect(MobEffects.BAD_OMEN);
                    cir.setReturnValue(false);
                }

                // You can add more Bad Omen variants here ;-)

            }
            else if (effect == MobEffects.RAID_OMEN && duration > 2995 && duration < 2999) {
                if(duration > 2995 && duration < 2999) {
                    Style textStyle = Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.GRAY)).withItalic(true);
                    serverPlayer.sendSystemMessage(Component.literal("Something's coming...").withStyle(textStyle));
                    duration = this.mapDuration(duration -> 2995);
                }
                else if(duration > 595 && duration < 599) {
                    Style textStyle = Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.GRAY)).withItalic(true);
                    serverPlayer.sendSystemMessage(Component.literal("You're being watched.").withStyle(textStyle));
                    duration = this.mapDuration(duration -> 595);
                }
            }

        }
    }

}
