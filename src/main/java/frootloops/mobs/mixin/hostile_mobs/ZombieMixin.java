package frootloops.mobs.mixin.hostile_mobs;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieEntity.class)
public abstract class ZombieMixin extends HostileEntity {
    protected ZombieMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    private boolean canBreakDoors;

    @Inject(method = "createZombieAttributes", at = @At("HEAD"), cancellable = true)
    private static void createZombieAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(
                HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_FOLLOW_RANGE, 8.0)
                        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4f)
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                        .add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0)
                        .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS, 0.025));
    }

    @Overwrite
    public void initCustomGoals() {

        this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 0.6F;
        this.handDropChances[EquipmentSlot.OFFHAND.getEntitySlotId()] = 0.6F;

        this.goalSelector.add(2, new ZombieAttackGoal((ZombieEntity) ((Object)this), 1.0, false));
        this.goalSelector.add(6, new MoveThroughVillageGoal(this, 1.0, true, 4, ((ZombieEntity) ((Object)this))::canBreakDoors));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.7, 0.66F));
        this.targetSelector.add(1, (new RevengeGoal(this, new Class[0])).setGroupRevenge(new Class[]{ZombifiedPiglinEntity.class}));
        this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal(this, MerchantEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal(this, IronGolemEntity.class, false));
        this.targetSelector.add(5, new ActiveTargetGoal(this, TurtleEntity.class, 10, true, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
    }

    @Override
    public void setBaby(boolean baby) {
        // No.
    }
}