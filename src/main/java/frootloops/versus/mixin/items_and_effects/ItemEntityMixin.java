package frootloops.versus.mixin.items_and_effects;

import frootloops.versus.mod.items_and_effects.BurningConversion;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
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
    private int health, itemAge;

    @Shadow
    public ItemStack getStack() {return null;}

    @Shadow
    public Entity getOwner() {return null;}

    @Shadow
    public void setStack(ItemStack stack) {}

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "initDataTracker", at = @At(value = "HEAD"))
    public void setHealth(CallbackInfo info) {
        health = 60;
        boolean playerDied = this.getOwner() != null && this.getOwner() instanceof PlayerEntity player && !player.isAlive();
        if(playerDied) itemAge = -12000;
    }

    @Override
    public boolean isFireImmune() {
        if(itemAge >= 0 && itemAge < 10) return true;
        if(this.getStack().getItem() instanceof BlockItem) {
            if (this.getStack().isOf(Items.OBSIDIAN)) return true;
            else if (this.getStack().isOf(Items.CRYING_OBSIDIAN)) return true;
            else if (this.getStack().isOf(Items.ENDER_CHEST)) return true;
            else if (this.getStack().isOf(Items.ENCHANTING_TABLE)) return true;
            else if (this.getStack().isOf(Items.ENCHANTED_GOLDEN_APPLE)) return true;
        }
        return this.getStack().getItem().getComponents().contains(DataComponentTypes.DAMAGE_RESISTANT ) || super.isFireImmune();
    }


    @Inject(method = "damage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;emitGameEvent(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/entity/Entity;)V"),
            cancellable = true)
    public void damage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(this.health == 0) {
            ItemStack currentItemStack = ((ItemEntity)((Object)this)).getStack();
            if(source.isOf(DamageTypes.LAVA)) doFireDamageTransformation(currentItemStack, true, false);
            else if(source.isIn(DamageTypeTags.IS_FIRE)) {
                BlockState blockState = this.getEntityWorld().getBlockState(this.getBlockPos());
                boolean isExtraHot = (blockState.isOf(Blocks.SOUL_FIRE) || blockState.isOf(Blocks.SOUL_CAMPFIRE));
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
                    ((ItemEntity) ((Object) this)).setStack(new ItemStack(Items.LAVA_BUCKET, currentItemStack.getCount()));
                    this.getEntityWorld().playSound(this, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1f, 1f);
                    health = 300;
                    return;
                }
            }
            else {
                if(this.getEntityWorld() instanceof ServerWorld) {
                    ((BucketItem)burningItem).placeFluid(null, this.getEntityWorld(), this.getBlockPos(), null);
                    ((BucketItem)burningItem).onEmptied(null, this.getEntityWorld(), currentItemStack, this.getBlockPos());
                }
                ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.BUCKET, currentItemStack.getCount()));
                this.getEntityWorld().playSound(this, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1f, 1f);
                health = 300;
                return;
            }
        }

        // Special effects: Totems!
        if(burningItem == Items.TOTEM_OF_UNDYING) {
            this.getEntityWorld().playSound(this, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1f, 1f);
            this.getEntityWorld().sendEntityStatus(this, (byte)35);
            Box boundingBox = new Box(this.getX() - 4d, this.getY() - 4d, this.getZ() - 4d, this.getX() + 4d, this.getY() + 4d, this.getZ() + 4d);
            List<ItemEntity> entitiesNearby = this.getEntityWorld().getEntitiesByClass(ItemEntity.class, boundingBox, EntityPredicates.VALID_ENTITY);
            for (ItemEntity entity: entitiesNearby) {
                entity.setNeverDespawn();
                entity.setGlowing(true);
                entity.setInvulnerable(true);
            }
            return;
        }

        // Anything else:
        int currentItemMaxHealth = isExtraHot ? 20 : itemAge > 200 ? 40 : 80;
        Item itemToConvertTo = null;
        if(BurningConversion.ITEM_BURNING_CONVERSION_MAP.containsKey(burningItem)) {
            BurningConversion.ItemBurningConversionRecord conversionRecord = BurningConversion.ITEM_BURNING_CONVERSION_MAP.get(burningItem);
            currentItemMaxHealth += isExtraHot ? conversionRecord.itemExtraHealth()/4 : conversionRecord.itemExtraHealth();
            itemToConvertTo = (isExtraHot ? conversionRecord.veryHotResultItem() : conversionRecord.resultItem());
        }

        // Check if the item should survive or not (if its age is smaller than its max health)
        if(itemAge < currentItemMaxHealth) {
            health = currentItemMaxHealth;
            return;
        }
        if(health > 0) return; // Item is spared for now

        // Otherwise, the item has burnt and should be transformed.
        this.getEntityWorld().playSound(this, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1f, 1f);
        if(!isExtraHot && currentItemStack.isIn(ItemTags.LOGS_THAT_BURN)) {
            ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.COAL, currentItemStack.getCount()));
            health = 20;
        }
        else if(itemToConvertTo != null && currentItemMaxHealth > 0) {
            int newItemMaxHealth = isExtraHot ? 20 : itemAge > 200 ? 40 : 80;
            if(BurningConversion.ITEM_BURNING_CONVERSION_MAP.containsKey(itemToConvertTo)) {
                newItemMaxHealth += BurningConversion.ITEM_BURNING_CONVERSION_MAP.get(itemToConvertTo).itemExtraHealth() - Math.max(0, itemAge - 200)/5;
                if(isExtraHot) newItemMaxHealth = newItemMaxHealth/4;
            }
            ((ItemEntity)((Object)this)).setStack(new ItemStack(itemToConvertTo, currentItemStack.getCount()));
            health = Math.max(isExtraHot ? 0 : 20, newItemMaxHealth);
        }
    }
    private void doRegularDamageTransformation(ItemStack currentItemStack) {

    }

}
