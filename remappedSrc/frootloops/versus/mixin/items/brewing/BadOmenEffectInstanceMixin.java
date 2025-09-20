package frootloops.versus.mixin.items_and_effects.brewing;

import frootloops.versus.VersusMod;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.world.Difficulty;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffectInstance.class)
public abstract class BadOmenEffectInstanceMixin {

    @Shadow private final RegistryEntry<StatusEffect> type;

    @Shadow private int amplifier;

    @Shadow private int duration;

    public BadOmenEffectInstanceMixin(RegistryEntry<StatusEffect> type) {
        this.type = type;
    }

    @Shadow public int mapDuration(Int2IntFunction mapper) {
        if (this.duration <= 0) {
            return this.duration;
        }
        return mapper.applyAsInt(this.duration);
    }

    @Inject(method = "update", at = @At(value = "HEAD"), cancellable = true)
    public void update(LivingEntity entity, Runnable overwriteCallback, CallbackInfoReturnable<Boolean> cir) {
        if(duration > 0 && entity instanceof ServerPlayerEntity serverPlayer) {
            ServerWorld serverWorld = serverPlayer.getWorld();

            if (type == StatusEffects.BAD_OMEN) {
                if(serverWorld.getTime() % 57L != 0) return;

                // RAIDS: Check if Bad Omen can be replaced by Raid Omen
                boolean isPlayerInsideVillage = serverWorld.isNearOccupiedPointOfInterest(serverPlayer.getBlockPos());
                boolean isPlayerInsideTheirBase = !isPlayerInsideVillage && serverPlayer.getSpawnPointPosition() != null && (serverPlayer.getSpawnPointPosition().isWithinDistance(serverPlayer.getBlockPos(), 32)) && !serverWorld.getPointOfInterestStorage().getInCircle(poiType -> poiType.isIn(PointOfInterestTypeTags.ACQUIRABLE_JOB_SITE), serverPlayer.getBlockPos(), 24, PointOfInterestStorage.OccupationStatus.ANY).toList().isEmpty();
                if ((isPlayerInsideVillage || isPlayerInsideTheirBase) && serverWorld.getRaidAt(serverPlayer.getBlockPos()) == null) {
                    if(isPlayerInsideVillage) serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RAID_OMEN, 600, amplifier));
                    else serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RAID_OMEN, 3000, 0));

                    serverPlayer.setStartRaidPos(serverPlayer.getBlockPos());
                    serverPlayer.removeStatusEffect(StatusEffects.BAD_OMEN);
                    cir.setReturnValue(false);
                }

                // You can add more Bad Omen variants here ;-)

            }
            else if (type == StatusEffects.RAID_OMEN && duration > 2995 && duration < 2999) {
                if(duration > 2995 && duration < 2999) {
                    Style textStyle = Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GRAY)).withItalic(true);
                    serverPlayer.sendMessage(Text.literal("Something's coming...").fillStyle(textStyle));
                    duration = this.mapDuration(duration -> 2995);
                }
                else if(duration > 595 && duration < 599) {
                    Style textStyle = Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GRAY)).withItalic(true);
                    serverPlayer.sendMessage(Text.literal("You're being watched.").fillStyle(textStyle));
                    duration = this.mapDuration(duration -> 595);
                }
            }

        }
    }

}
