package frootloops.versus.mixin.items_and_effects;

import frootloops.versus.mod.items_and_effects.BurningConversion;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    // Note: this code is bad

    @Shadow
    private int health, age;

    @Shadow
    public ItemStack getItem() {return null;}

    @Shadow
    public Entity getOwner() {return null;}

    @Shadow
    public void setItem(ItemStack stack) {}

    public ItemEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "defineSynchedData", at = @At(value = "HEAD"))
    public void setHealth(CallbackInfo info) {
        health = 60;
        boolean playerDied = this.getOwner() != null && this.getOwner() instanceof Player player && !player.isAlive();
        if(playerDied) age = -12000;
    }

    @Override
    public boolean fireImmune() {
        if(age >= 0 && age < 10) return true;
        if(this.getItem().getItem() instanceof BlockItem) {
            if (this.getItem().is(Items.OBSIDIAN)) return true;
            else if (this.getItem().is(Items.CRYING_OBSIDIAN)) return true;
            else if (this.getItem().is(Items.ENDER_CHEST)) return true;
            else if (this.getItem().is(Items.ENCHANTING_TABLE)) return true;
            else if (this.getItem().is(Items.ENCHANTED_GOLDEN_APPLE)) return true;
        }
        return this.getItem().getItem().components().has(DataComponents.DAMAGE_RESISTANT ) || super.fireImmune();
    }


    @Inject(method = "hurtServer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/Entity;)V"),
            cancellable = true)
    public void damage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(this.health == 0) {
            ItemStack currentItemStack = ((ItemEntity)((Object)this)).getItem();
            if(source.is(DamageTypes.LAVA)) doFireDamageTransformation(currentItemStack, true, false);
            else if(source.is(DamageTypeTags.IS_FIRE)) {
                BlockState blockState = this.level().getBlockState(this.blockPosition());
                boolean isExtraHot = (blockState.is(Blocks.SOUL_FIRE) || blockState.is(Blocks.SOUL_CAMPFIRE));
                doFireDamageTransformation(currentItemStack, isExtraHot, false);
            }
            else {
                doRegularDamageTransformation(currentItemStack);
            }
            if(health > 0) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }




    private void doFireDamageTransformation(ItemStack currentItemStack, boolean isExtraHot, boolean canCookFood) {

        Item burningItem = currentItemStack.getItem();

        // Special effects: Buckets!
        if(burningItem instanceof BucketItem) {
            if (burningItem == Items.BUCKET) {
                if(isExtraHot) {
                    ((ItemEntity) ((Object) this)).setItem(new ItemStack(Items.LAVA_BUCKET, currentItemStack.getCount()));
                    this.level().playSound(this, this.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1f, 1f);
                    health = 300;
                    return;
                }
            }
            else {
                if(this.level() instanceof ServerLevel) {
                    ((BucketItem)burningItem).emptyContents(null, this.level(), this.blockPosition(), null);
                    ((BucketItem)burningItem).checkExtraContent(null, this.level(), currentItemStack, this.blockPosition());
                }
                ((ItemEntity)((Object)this)).setItem(new ItemStack(Items.BUCKET, currentItemStack.getCount()));
                this.level().playSound(this, this.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1f, 1f);
                health = 300;
                return;
            }
        }

        // Special effects: Totems!
        if(burningItem == Items.TOTEM_OF_UNDYING) {
            this.level().playSound(this, this.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1f, 1f);
            this.level().broadcastEntityEvent(this, (byte)35);
            AABB boundingBox = new AABB(this.getX() - 4d, this.getY() - 4d, this.getZ() - 4d, this.getX() + 4d, this.getY() + 4d, this.getZ() + 4d);
            List<ItemEntity> entitiesNearby = this.level().getEntitiesOfClass(ItemEntity.class, boundingBox, EntitySelector.ENTITY_STILL_ALIVE);
            for (ItemEntity entity: entitiesNearby) {
                entity.setUnlimitedLifetime();
                entity.setGlowingTag(true);
                entity.setPermanentlyInvulnerable(true);
            }
            return;
        }

        // Anything else:
        int currentItemMaxHealth = isExtraHot ? 20 : age > 200 ? 40 : 80;
        Item itemToConvertTo = null;
        if(BurningConversion.ITEM_BURNING_CONVERSION_MAP.containsKey(burningItem)) {
            BurningConversion.ItemBurningConversionRecord conversionRecord = BurningConversion.ITEM_BURNING_CONVERSION_MAP.get(burningItem);
            currentItemMaxHealth += isExtraHot ? conversionRecord.itemExtraHealth()/4 : conversionRecord.itemExtraHealth();
            itemToConvertTo = (isExtraHot ? conversionRecord.veryHotResultItem() : conversionRecord.resultItem());
        }

        // Check if the item should survive or not (if its age is smaller than its max health)
        if(age < currentItemMaxHealth) {
            health = currentItemMaxHealth;
            return;
        }
        if(health > 0) return; // Item is spared for now

        // Otherwise, the item has burnt and should be transformed.
        this.level().playSound(this, this.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1f, 1f);
        if(!isExtraHot && currentItemStack.is(ItemTags.LOGS_THAT_BURN)) {
            ((ItemEntity)((Object)this)).setItem(new ItemStack(Items.COAL, currentItemStack.getCount()));
            health = 20;
        }
        else if(itemToConvertTo != null && currentItemMaxHealth > 0) {
            int newItemMaxHealth = isExtraHot ? 20 : age > 200 ? 40 : 80;
            if(BurningConversion.ITEM_BURNING_CONVERSION_MAP.containsKey(itemToConvertTo)) {
                newItemMaxHealth += BurningConversion.ITEM_BURNING_CONVERSION_MAP.get(itemToConvertTo).itemExtraHealth() - Math.max(0, age - 200)/5;
                if(isExtraHot) newItemMaxHealth = newItemMaxHealth/4;
            }
            ((ItemEntity)((Object)this)).setItem(new ItemStack(itemToConvertTo, currentItemStack.getCount()));
            health = Math.max(isExtraHot ? 0 : 20, newItemMaxHealth);
        }
    }
    private void doRegularDamageTransformation(ItemStack currentItemStack) {

    }

}
