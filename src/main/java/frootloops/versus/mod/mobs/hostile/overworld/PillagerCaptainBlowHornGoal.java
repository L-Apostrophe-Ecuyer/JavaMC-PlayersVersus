
package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.level.gameevent.GameEvent;
import java.util.Optional;

public class PillagerCaptainBlowHornGoal extends Goal {

    private AbstractIllager illager;

    private int timeSpentTootingHorn;
    private int timeLeftToStart;

    private ItemStack prevOffhandStack;
    private ItemStack prevMainhandStack;
    private boolean isCaptain;
    private boolean isInRaid;

    public PillagerCaptainBlowHornGoal(AbstractIllager pillagerEntity) {
        this.illager = pillagerEntity;
        this.timeLeftToStart = 0;
        isCaptain = this.illager.isCaptain();
        isInRaid = this.illager.hasRaid();
    }

    @Override
    public boolean canUse() {
        if(isInRaid) return false;
        if(!isCaptain) {
            if(illager.tickCount % 91 != 0) return false;
            isCaptain = this.illager.isCaptain();
        }
        isInRaid = isCaptain && this.illager.hasRaid();
        return (!isInRaid && isCaptain && illager.isAlive() && illager.hurtTime == 0 && illager.getKillCredit() instanceof Player player && player.distanceToSqr(illager) < 144.0d && !player.hasEffect(MobEffects.BAD_OMEN) && !player.hasEffect(MobEffects.RAID_OMEN));
    }

    @Override
    public boolean canContinueToUse() {
        return this.illager.isAlive() && this.timeSpentTootingHorn < 80 && (timeLeftToStart == 0 || illager.hurtTime == 0);
    }

    @Override
    public boolean isInterruptable(){
        return true;
    }

    @Override
    public void start() {
        this.prevOffhandStack = this.illager.getOffhandItem();
        this.prevMainhandStack = this.illager.getMainHandItem();

        Holder.Reference<Instrument> entry = illager.level().registryAccess().lookupOrThrow(Registries.INSTRUMENT).get(Identifier.withDefaultNamespace("seek_goat_horn")).get();
        ItemStack goatHornStack = InstrumentItem.create(Items.GOAT_HORN, entry);
        this.illager.setItemSlot(EquipmentSlot.OFFHAND, goatHornStack);
        this.illager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        timeLeftToStart = 20;
        illager.setAggressive(true);
        this.illager.setPose(Pose.STANDING);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if(timeLeftToStart > 0) timeLeftToStart--;
        else {
            if(timeSpentTootingHorn == 0) {
                illager.setAggressive(true);
                this.illager.setPose(Pose.CROAKING);
                InstrumentComponent instrumentComponent = this.illager.getOffhandItem().get(DataComponents.INSTRUMENT);
                if (instrumentComponent != null) {
                    Holder<Instrument> instrumentRegistryEntry = instrumentComponent.unwrap(this.illager.registryAccess()).get();
                    float volume = instrumentRegistryEntry.value().range() / 16.0f;
                    illager.level().playSound(null, illager, instrumentRegistryEntry.value().soundEvent().value(), SoundSource.HOSTILE, volume, 1.0f);
                    illager.level().gameEvent(GameEvent.INSTRUMENT_PLAY, illager.position(), GameEvent.Context.of(illager));
                }
            }
            timeSpentTootingHorn++;
        }
    }

    @Override
    public void stop() {
        if(timeSpentTootingHorn >= 70) {
            if(this.illager.getLastHurtByMob() instanceof Player attacker) attacker.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, 3200));
            else if(this.illager.getKillCredit() instanceof Player adversary) adversary.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, 2400));
        }
        this.illager.setItemSlot(EquipmentSlot.OFFHAND, prevOffhandStack);
        this.illager.setItemSlot(EquipmentSlot.MAINHAND, prevMainhandStack);
        this.illager.setPose(Pose.STANDING);
    }
}
