package frootloops.versus.mixin.mobs.hostile.illager;

import frootloops.versus.VersusMod;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PillagerEntity.class)
public abstract class PillagerMixin extends IllagerEntity {
    protected PillagerMixin(EntityType<? extends IllagerEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initialize", at = @At("HEAD"))
    private void increaseAttributes(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        EntityAttributeInstance instance = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (instance != null) instance.setBaseValue(0.36);

        EntityAttributeInstance instanceHP = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instanceHP != null) {
            instanceHP.setBaseValue(26.0D);
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    public void initEquipment(Random random, LocalDifficulty localDifficulty) {
        int rand = random.nextInt(100);
        if(random.nextInt(100) < 20) {
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            this.getEquippedStack(EquipmentSlot.MAINHAND).setDamage(rand + 10);
            this.getEquippedStack(EquipmentSlot.OFFHAND).setDamage(rand + 30);
            this.goalSelector.add(3, new  MeleeAttackGoal(this, 1.1, false));
        }
        else {
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            if(this.hasNoRaid()) this.goalSelector.add(1, new FleeEntityGoal<>(this, PlayerEntity.class, 6, 0.7, 0.9, (livingEntity) -> true));
        }
        if(this.isCaptain() && this.hasNoRaid()) {
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.RAID_OMEN, -1));
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if (this.isRemoved() || this.dead) return;
        if (!this.getWorld().isClient) {
            LivingEntity primeAdversary = this.getPrimeAdversary();
            if(this.isCaptain() && primeAdversary instanceof PlayerEntity && this.hasNoRaid()) {
                primeAdversary.addStatusEffect(new StatusEffectInstance(StatusEffects.BAD_OMEN, 3600));
            }
        }
        super.onDeath(damageSource);
    }

    @ModifyArg(method = "enchantMainHandItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I"))
    private int changeProbabilityMoreEnchant(int range) {
        return range / 3;
    }
}