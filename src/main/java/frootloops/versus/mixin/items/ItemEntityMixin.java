package frootloops.versus.mixin.items;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

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
        return this.getStack().getItem().isFireproof() || super.isFireImmune();
    }


    @Inject(method = "damage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;emitGameEvent(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/entity/Entity;)V"),
            cancellable = true)
    public void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(this.health == 0) {
            ItemStack currentItemStack = ((ItemEntity)((Object)this)).getStack();
            if(source.isOf(DamageTypes.LAVA)) doFireDamageTransformation(currentItemStack, true);
            else if(source.isIn(DamageTypeTags.IS_FIRE)) doFireDamageTransformation(currentItemStack, false);
            else doRegularDamageTransformation(currentItemStack);
            if(health > 0) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }




    private void doFireDamageTransformation(ItemStack currentItemStack, boolean isLava) {

        // Lava is less forgiving:
        if(!isLava) {
            // FutureItems should survive in the fire for a bit, and take some time to cook:
            if(itemAge < 40) {
                health = 40;
                return;
            }

            // Foods and organics turn to soot:
            if(currentItemStack.isFood() && !currentItemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)){
                ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.BLACK_DYE, currentItemStack.getCount()));
                health = 60;
            }

            // Most blocks turn to charred blocks variants:
            else if(currentItemStack.isIn(ItemTags.LOGS_THAT_BURN)) {
                ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.CHARCOAL, currentItemStack.getCount()));
                health = 120;
            }
        }

        // FutureItems break down into components, or get used/placed:
        if(currentItemStack.isOf(Items.TOTEM_OF_UNDYING)) {
            this.getWorld().sendEntityStatus(this, (byte)35);
            Box boundingBox = new Box(this.getX() - 4d, this.getY() - 4d, this.getZ() - 4d, this.getX() + 4d, this.getY() + 4d, this.getZ() + 4d);
            List<ItemEntity> entitiesNearby = this.getWorld().getEntitiesByClass(ItemEntity.class, boundingBox, EntityPredicates.VALID_ENTITY);
            for (ItemEntity entity: entitiesNearby) {
                entity.setNeverDespawn();
                entity.setGlowing(true);
                entity.setInvulnerable(true);
            }
        }
        else if(currentItemStack.isIn(ItemTags.ANVIL)) {
            ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.IRON_BLOCK, Math.min(1,currentItemStack.getCount()/3)));
            health = 300;
        }
        else if(currentItemStack.getItem() instanceof CompassItem || currentItemStack.isOf(Items.CLOCK)) {
            health = 300;
        }
        else if(currentItemStack.getItem() instanceof BucketItem) {
            if(this.getWorld() instanceof ServerWorld) {
                ((BucketItem)currentItemStack.getItem()).placeFluid(null, this.getWorld(), this.getBlockPos(), null);
                ((BucketItem)currentItemStack.getItem()).onEmptied(null, this.getWorld(), currentItemStack, this.getBlockPos());
            }
            if(isLava) ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.LAVA_BUCKET, currentItemStack.getCount()));
            else ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.BUCKET, currentItemStack.getCount()));
            health = 300;
        }
        else if(currentItemStack.getItem() instanceof PowderSnowBucketItem) {
            if(isLava) ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.LAVA_BUCKET, currentItemStack.getCount()));
            else if(this.getWorld() instanceof ServerWorld) {
                this.getWorld().setBlockState(this.getBlockPos(), Blocks.AIR.getDefaultState());
                ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.BUCKET, currentItemStack.getCount()));
            }
            health = 300;
        }
        else {
            String translationKeyStr = currentItemStack.getItem().getTranslationKey();
            if (translationKeyStr.endsWith("nugget") && this.itemAge < 200) {
                health = 0;
            }
            else if (translationKeyStr.contains("emerald") && this.itemAge < 200) {
                ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.EMERALD, currentItemStack.getCount()));
                health = 400;
            }
            else if (translationKeyStr.contains("lapis") && this.itemAge < 200) {
                ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.LAPIS_LAZULI, currentItemStack.getCount()));
                health = 200;
            }
            else if (translationKeyStr.contains("diamond_")) {
                if(!currentItemStack.isOf(Items.DIAMOND_BLOCK)) ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.DIAMOND, currentItemStack.getCount()));
                health = 600;
            }
            else if(translationKeyStr.contains("iron_") || translationKeyStr.contains("hopper") || translationKeyStr.endsWith("_minecart")) {
                if(translationKeyStr.endsWith("iron_block")) ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.IRON_BLOCK, currentItemStack.getCount()));
                else if(!currentItemStack.isOf(Items.IRON_INGOT)) ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.IRON_INGOT, currentItemStack.getCount()));
                else ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.IRON_NUGGET, currentItemStack.getCount()));
                health = 300;
            }
            else if(translationKeyStr.contains("gold")) {
                if(translationKeyStr.endsWith("gold_block")) ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.GOLD_BLOCK, currentItemStack.getCount()));
                else if(!currentItemStack.isOf(Items.GOLD_INGOT)) ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.GOLD_INGOT, currentItemStack.getCount()));
                else ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.GOLD_NUGGET, currentItemStack.getCount()));
                health = 150;
            }
            else if(translationKeyStr.contains("copper")) {
                if(translationKeyStr.endsWith("copper_block")) {
                    ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.COPPER_BLOCK, currentItemStack.getCount()));
                    health = 100;
                }
                else if(!currentItemStack.isOf(Items.COPPER_INGOT)) {
                    ((ItemEntity)((Object)this)).setStack(new ItemStack(Items.COPPER_INGOT, currentItemStack.getCount()));
                    health = 100;
                }
            }
        }

        // Make some noise!
        if(health > 40 || health == 0) {
            this.getWorld().playSound(this, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1f, 1f);
        }
    }
    private void doRegularDamageTransformation(ItemStack currentItemStack) {

    }

}
