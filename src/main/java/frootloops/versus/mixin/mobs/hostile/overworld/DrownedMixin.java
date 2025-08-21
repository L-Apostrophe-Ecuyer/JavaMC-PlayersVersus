package frootloops.versus.mixin.mobs.hostile.overworld;


import frootloops.versus.VersusSettings;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import frootloops.versus.mod.items.brewing.CustomBrewingItems;
import frootloops.versus.mod.items.brewing.CustomPotions;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;

@Mixin(DrownedEntity.class)
public abstract class DrownedMixin extends ZombieEntity {


    public DrownedMixin(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyConstant(method = "canSpawn", constant = @Constant(intValue = 15))
    private static int lessSpawning(int spawnChance) {return 30;}

    @Inject(method = "initEquipment", at = @At("HEAD"), cancellable = true)
    public void changeProbability(Random random, LocalDifficulty localDifficulty, CallbackInfo ci) {
        int rand = random.nextInt(100);
        if (rand < 16 && this.getWorld().getBiome(this.getBlockPos()).isIn(BiomeTags.IS_OCEAN)) {
            int level = rand + random.nextInt(15);
            DynamicRegistryManager dynamicRegistryManager = this.getWorld().getRegistryManager();
            Optional<RegistryEntryList.Named<Enchantment>> optional = dynamicRegistryManager.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT);
            if(rand % 2 == 1) this.equipStack(EquipmentSlot.CHEST, EnchantmentHelper.enchant(random, new ItemStack(Items.GOLDEN_CHESTPLATE), level, dynamicRegistryManager, optional));
            if(rand % 3 == 1) this.equipStack(EquipmentSlot.LEGS, EnchantmentHelper.enchant(random, new ItemStack(Items.GOLDEN_LEGGINGS), level, dynamicRegistryManager, optional));
            if(rand % 4 == 1) this.equipStack(EquipmentSlot.HEAD, EnchantmentHelper.enchant(random, new ItemStack(Items.GOLDEN_HELMET), level, dynamicRegistryManager, optional));
            if(rand % 5 == 1) this.equipStack(EquipmentSlot.FEET, EnchantmentHelper.enchant(random, new ItemStack(Items.GOLDEN_BOOTS), level, dynamicRegistryManager, optional));
            if(rand % 6 == 1 || rand % 7 == 1) this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.GOLDEN_APPLE));
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            this.handDropChances[0] = 1f;
            this.handDropChances[1] = 1f;
        }
        else if (rand < 20)
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        else if(rand < 36) {
            if (rand < 25)
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.SPYGLASS));
            else if (rand < 29)
                this.equipStack(EquipmentSlot.MAINHAND, PotionContentsComponent.createStack(Items.POTION, CustomPotions.HASTE_STRONG));
            else if (rand < 32)
                this.equipStack(EquipmentSlot.MAINHAND, PotionContentsComponent.createStack(Items.POTION, CustomPotions.HASTE));
            else if (rand < 35)
                this.equipStack(EquipmentSlot.MAINHAND, PotionContentsComponent.createStack(Items.POTION, CustomPotions.MINING_FATIGUE));
            else if (rand < 38)
                this.equipStack(EquipmentSlot.MAINHAND, PotionContentsComponent.createStack(Items.POTION, CustomPotions.BUOYANCY));
            else if (rand < 42)
                this.equipStack(EquipmentSlot.MAINHAND, Items.GLASS_BOTTLE.getDefaultStack());
            if (rand % 5 == 0)
                this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));
            this.handDropChances[0] = 1f;
            this.handDropChances[1] = 1f;
        }
        ci.cancel();
    }

    @Override
    public void tick() {
        super.tick();
        this.setPose(this.isSwimming() && !this.hasVehicle() ? EntityPose.SWIMMING : EntityPose.STANDING);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityAttributeInstance followRange = this.getAttributes().getCustomInstance(EntityAttributes.FOLLOW_RANGE);
        if (followRange != null) followRange.setBaseValue(32.0d);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    @ModifyArg(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/DrownedEntity;updateVelocity(FLnet/minecraft/util/math/Vec3d;)V"))
    private float increaseVelocity(float speed) {
        if(this.isSwimming()) return 0.06F;
        else return speed;
    }
}