package frootloops.versus.mixin.mobs.hostile.overworld;


import frootloops.versus.mod.items_and_effects.brewing.CustomPotions;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Drowned.class)
public abstract class DrownedMixin extends Zombie {


    public DrownedMixin(EntityType<? extends Zombie> entityType, Level world) {
        super(entityType, world);
    }

    @ModifyConstant(method = "canSpawn", constant = @Constant(intValue = 15))
    private static int lessSpawning(int spawnChance) {return 30;}

    @Inject(method = "populateDefaultEquipmentSlots", at = @At("HEAD"), cancellable = true)
    public void changeProbability(RandomSource random, DifficultyInstance localDifficulty, CallbackInfo ci) {
        int rand = random.nextInt(100);
        if (rand < 16 && this.level().getBiome(this.blockPosition()).is(BiomeTags.IS_OCEAN)) {
            int level = rand + random.nextInt(15);
            RegistryAccess dynamicRegistryManager = this.level().registryAccess();
            Optional<HolderSet.Named<Enchantment>> optional = dynamicRegistryManager.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT);
            if(rand % 2 == 1) this.setItemSlot(EquipmentSlot.CHEST, EnchantmentHelper.enchantItem(random, new ItemStack(Items.GOLDEN_CHESTPLATE), level, dynamicRegistryManager, optional));
            if(rand % 3 == 1) this.setItemSlot(EquipmentSlot.LEGS, EnchantmentHelper.enchantItem(random, new ItemStack(Items.GOLDEN_LEGGINGS), level, dynamicRegistryManager, optional));
            if(rand % 4 == 1) this.setItemSlot(EquipmentSlot.HEAD, EnchantmentHelper.enchantItem(random, new ItemStack(Items.GOLDEN_HELMET), level, dynamicRegistryManager, optional));
            if(rand % 5 == 1) this.setItemSlot(EquipmentSlot.FEET, EnchantmentHelper.enchantItem(random, new ItemStack(Items.GOLDEN_BOOTS), level, dynamicRegistryManager, optional));
            if(rand % 6 == 1 || rand % 7 == 1) this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GOLDEN_APPLE));
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            this.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
            this.setDropChance(EquipmentSlot.OFFHAND, 1.0F);
        }
        else if (rand < 20)
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        else if(rand < 36) {
            if (rand < 25)
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SPYGLASS));
            else if (rand < 29)
                this.setItemSlot(EquipmentSlot.MAINHAND, PotionContents.createItemStack(Items.POTION, CustomPotions.HASTE_STRONG));
            else if (rand < 32)
                this.setItemSlot(EquipmentSlot.MAINHAND, PotionContents.createItemStack(Items.POTION, CustomPotions.HASTE));
            else if (rand < 35)
                this.setItemSlot(EquipmentSlot.MAINHAND, PotionContents.createItemStack(Items.POTION, CustomPotions.MINING_FATIGUE));
            else if (rand < 38)
                this.setItemSlot(EquipmentSlot.MAINHAND, PotionContents.createItemStack(Items.POTION, CustomPotions.BUOYANCY));
            else if (rand < 42)
                this.setItemSlot(EquipmentSlot.MAINHAND, Items.GLASS_BOTTLE.getDefaultInstance());
            if (rand % 5 == 0)
                this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.7F);
            this.setDropChance(EquipmentSlot.OFFHAND, 1.0F);
        }
        ci.cancel();
    }

    @Override
    public void tick() {
        super.tick();
        this.setPose(this.isSwimming() && !this.isPassenger() ? Pose.SWIMMING : Pose.STANDING);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData) {
        AttributeInstance followRange = this.getAttributes().getInstance(Attributes.FOLLOW_RANGE);
        if (followRange != null) followRange.setBaseValue(32.0d);
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData);
    }

    @ModifyArg(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Drowned;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"))
    private float increaseVelocity(float speed) {
        if(this.isSwimming()) return 0.06F;
        else return speed;
    }
}