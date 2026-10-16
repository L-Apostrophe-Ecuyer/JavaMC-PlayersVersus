package frootloops.versus.mixin.mobs.hostile.illager;

import frootloops.versus.mod.mobs.hostile.overworld.PillagerCaptainBlowHornGoal;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pillager.class)
public abstract class PillagerMixin extends AbstractIllager {
    protected PillagerMixin(EntityType<? extends AbstractIllager> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void increaseAttributes(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData, CallbackInfoReturnable<SpawnGroupData> cir) {
        AttributeInstance instance = this.getAttributes().getInstance(Attributes.MOVEMENT_SPEED);
        if (instance != null) instance.setBaseValue(0.36);

        AttributeInstance instanceHP = this.getAttributes().getInstance(Attributes.MAX_HEALTH);
        if (instanceHP != null) {
            instanceHP.setBaseValue(26.0D);
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    public void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance localDifficulty) {
        int rand = random.nextInt(100);
        if(rand < 20) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            this.getItemBySlot(EquipmentSlot.MAINHAND).setDamageValue(rand + 10);
            this.getItemBySlot(EquipmentSlot.OFFHAND).setDamageValue(rand + 30);
            this.goalSelector.addGoal(3, new  MeleeAttackGoal(this, 1.1, false));
        }
        else {
            this.setHealth(16.0F);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            if(this.canJoinPatrol() && !this.isCaptain()) this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 6, 0.7, 0.9, (livingEntity) -> true));
        }
        this.goalSelector.addGoal(1, new PillagerCaptainBlowHornGoal(this));
    }

    @ModifyArg(method = "enchantSpawnedWeapon", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int changeProbabilityMoreEnchant(int range) {
        return range / 3;
    }
}