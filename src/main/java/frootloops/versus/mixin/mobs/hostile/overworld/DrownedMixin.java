package frootloops.versus.mixin.mobs.hostile.overworld;


import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(DrownedEntity.class)
public abstract class DrownedMixin extends ZombieEntity {


    public DrownedMixin(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initEquipment", at = @At("HEAD"), cancellable = true)
    public void changeProbability(Random random, LocalDifficulty localDifficulty, CallbackInfo ci) {
        int rand = random.nextInt(100);
        this.handDropChances[0] = 0.5f;
        this.handDropChances[1] = 0.5f;

        if (rand < 10) {
            if(rand % 2 == 1) this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            if(rand % 3 == 1) this.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
            if(rand % 4 == 1) this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
            if(rand % 5 == 1) this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.GOLDEN_APPLE));
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            this.handDropChances[0] = 1f;
            this.handDropChances[1] = 1f;

            EntityAttributeInstance followRange = this.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
            Objects.requireNonNull(followRange).addPersistentModifier(new EntityAttributeModifier("Trident_drowned", +36.0D, EntityAttributeModifier.Operation.ADDITION));
        }
        else if (rand < 20)
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        else if (rand < 25)
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.SPYGLASS));
        ci.cancel();
    }

    @Override
    public void tick() {
        super.tick();
        this.setPose(this.isSwimming() && !this.hasVehicle() ? EntityPose.SWIMMING : EntityPose.STANDING);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {

        entityData = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        if (this.getEquippedStack(EquipmentSlot.OFFHAND).isEmpty() && world.getRandom().nextFloat() < 0.03F)
            this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));

        EntityAttributeInstance followRange = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
        if (followRange != null) followRange.setBaseValue(32.0d);

        return entityData;
    }

    @ModifyArg(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/DrownedEntity;updateVelocity(FLnet/minecraft/util/math/Vec3d;)V"))
    private float increaseVelocity(float speed) {
        if(this.isWet()) return 0.07F;
        else return speed;
    }
}