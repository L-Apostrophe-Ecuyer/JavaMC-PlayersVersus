package frootloops.versus.mixin.hostile_mobs;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SkeletonEntity.class)
public abstract class SkeletonMixin extends AbstractSkeletonEntity {
    protected SkeletonMixin(EntityType<? extends AbstractSkeletonEntity> entityType, World world) {
        super(entityType, world);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityTag) {
        EntityAttributeInstance instance = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instance != null) {
            instance.setBaseValue(12.0D);
            this.setHealth(this.getMaxHealth());
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityTag);
    }

    @Override
    @Nullable
    protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 0.6F;

        int rand = random.nextInt(100);
        if (rand < 15) {
            this.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        }
        else if(rand < 30) {
            this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            if(rand % 3 == 1) {
                this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                this.getEquippedStack(EquipmentSlot.MAINHAND).setDamage(rand/2 + 100);
            }
        }
        else if(rand < 40) {
            this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        }
        else if(rand < 48) {
            this.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_HOE));
        }
        else if(rand < 55){
            this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
        }
        else {
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
    }
}
