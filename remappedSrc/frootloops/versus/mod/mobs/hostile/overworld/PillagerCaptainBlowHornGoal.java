
package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.InstrumentComponent;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;

public class PillagerCaptainBlowHornGoal extends Goal {

    private IllagerEntity illager;

    private int timeSpentTootingHorn;
    private int timeLeftToStart;

    private ItemStack prevOffhandStack;
    private ItemStack prevMainhandStack;
    private boolean isCaptain;
    private boolean isInRaid;

    public PillagerCaptainBlowHornGoal(IllagerEntity pillagerEntity) {
        this.illager = pillagerEntity;
        this.timeLeftToStart = 0;
        isCaptain = this.illager.isCaptain();
        isInRaid = this.illager.hasRaid();
    }

    @Override
    public boolean canStart() {
        if(isInRaid) return false;
        if(!isCaptain) {
            if(illager.age % 91 != 0) return false;
            isCaptain = this.illager.isCaptain();
        }
        isInRaid = isCaptain && this.illager.hasRaid();
        return (!isInRaid && isCaptain && illager.isAlive() && illager.hurtTime == 0 && illager.getPrimeAdversary() instanceof PlayerEntity player && player.squaredDistanceTo(illager) < 144.0d && !player.hasStatusEffect(StatusEffects.BAD_OMEN) && !player.hasStatusEffect(StatusEffects.RAID_OMEN));
    }

    @Override
    public boolean shouldContinue() {
        return this.illager.isAlive() && this.timeSpentTootingHorn < 80 && (timeLeftToStart == 0 || illager.hurtTime == 0);
    }

    @Override
    public boolean canStop(){
        return true;
    }

    @Override
    public void start() {
        this.prevOffhandStack = this.illager.getOffHandStack();
        this.prevMainhandStack = this.illager.getMainHandStack();

        RegistryEntry.Reference<Instrument> entry = illager.getWorld().getRegistryManager().getOrThrow(RegistryKeys.INSTRUMENT).getEntry(Identifier.ofVanilla("seek_goat_horn")).get();
        ItemStack goatHornStack = GoatHornItem.getStackForInstrument(Items.GOAT_HORN, entry);
        this.illager.equipStack(EquipmentSlot.OFFHAND, goatHornStack);
        this.illager.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        timeLeftToStart = 20;
        illager.setAttacking(true);
        this.illager.setPose(EntityPose.STANDING);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if(timeLeftToStart > 0) timeLeftToStart--;
        else {
            if(timeSpentTootingHorn == 0) {
                illager.setAttacking(true);
                this.illager.setPose(EntityPose.CROAKING);
                InstrumentComponent instrumentComponent = this.illager.getOffHandStack().get(DataComponentTypes.INSTRUMENT);
                if (instrumentComponent != null) {
                    RegistryEntry<Instrument> instrumentRegistryEntry = instrumentComponent.getInstrument(this.illager.getRegistryManager()).get();
                    float volume = instrumentRegistryEntry.value().range() / 16.0f;
                    illager.getWorld().playSoundFromEntity(null, illager, instrumentRegistryEntry.value().soundEvent().value(), SoundCategory.HOSTILE, volume, 1.0f);
                    illager.getWorld().emitGameEvent(GameEvent.INSTRUMENT_PLAY, illager.getPos(), GameEvent.Emitter.of(illager));
                }
            }
            timeSpentTootingHorn++;
        }
    }

    @Override
    public void stop() {
        if(timeSpentTootingHorn >= 70) {
            if(this.illager.getAttacker() instanceof PlayerEntity attacker) attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.BAD_OMEN, 3200));
            else if(this.illager.getPrimeAdversary() instanceof PlayerEntity adversary) adversary.addStatusEffect(new StatusEffectInstance(StatusEffects.BAD_OMEN, 2400));
        }
        this.illager.equipStack(EquipmentSlot.OFFHAND, prevOffhandStack);
        this.illager.equipStack(EquipmentSlot.MAINHAND, prevMainhandStack);
        this.illager.setPose(EntityPose.STANDING);
    }
}
