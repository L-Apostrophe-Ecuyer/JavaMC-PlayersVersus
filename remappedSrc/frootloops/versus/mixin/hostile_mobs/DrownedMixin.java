package frootloops.versus.mixin.hostile_mobs;


import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
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

@Mixin(DrownedEntity.class)
public abstract class DrownedMixin extends ZombieEntity {
    public DrownedMixin(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initEquipment", at = @At("HEAD"), cancellable = true)
    public void changeProbability(Random random, LocalDifficulty localDifficulty, CallbackInfo ci) {
        int rand = random.nextInt(100);
        if (rand < 15) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        else if (rand < 18) this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        this.handDropChances[0] = 0.75f;
        this.handDropChances[1] = 0.75f;
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
        return 0.08F;
    }
}