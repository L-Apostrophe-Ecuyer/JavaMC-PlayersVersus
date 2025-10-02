package frootloops.versus.mixin.items_and_effects.equipment;

import frootloops.versus.mod.enchantments.CustomEnchants;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBlockingMixin extends Entity {
    public LivingEntityBlockingMixin(EntityType<?> type, World world) {super(type, world);}
    private static final float MAX_ANGLE_TO_BLOCK = (float) (Math.PI / 180.0) * 90F;
    private static final int PARRY_TIME_TICKS = 6;

    @Shadow protected int itemUseTimeLeft;
    @Shadow protected ItemStack activeItemStack;
    @Shadow protected static TrackedData<Byte> LIVING_FLAGS;

    @Shadow
    public Hand getActiveHand() {
        return (this.dataTracker.get(LIVING_FLAGS) & 2) > 0 ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    @Shadow
    @Nullable
    public ItemStack getBlockingItem() {return null;}

    @Shadow
    protected void takeShieldHit(ServerWorld world, LivingEntity attacker) {
        attacker.takeKnockback(0.5, attacker.getX() - this.getX(), attacker.getZ() - this.getZ());
    }

    @Overwrite
    public float getDamageBlockedAmount(ServerWorld world, DamageSource source, float damageAmount) {
        if (damageAmount <= 0.0F) {
            return 0.0F;
        } else {
            ItemStack blockingItem = this.getBlockingItem();
            if (blockingItem == null) {
                return 0.0F;
            } else {
                BlocksAttacksComponent blocksAttacksComponent = blockingItem.get(DataComponentTypes.BLOCKS_ATTACKS);
                if (blocksAttacksComponent != null) {
                    int levelRiposte = EnchantRegistryHelper.getLevel(getWorld(), blockingItem, CustomEnchants.RIPOSTE);
                    int ticksToParry = PARRY_TIME_TICKS + levelRiposte;
                    int useTime =  activeItemStack.getMaxUseTime((LivingEntity) ((Object)this)) - itemUseTimeLeft;
                    boolean wasAttackParried = useTime <= ticksToParry;
                    boolean doesAttackBypassShield = blocksAttacksComponent.bypassedBy().map(source::isIn).orElse(false) && !(wasAttackParried && source.isOf(DamageTypes.SONIC_BOOM));
                    if (doesAttackBypassShield) return 0.0F;

                    // Check if we can repel arrow:
                    if ((!blockingItem.isIn(ItemTags.SWORDS) || wasAttackParried) && source.getSource() instanceof PersistentProjectileEntity persistentProjectileEntity && persistentProjectileEntity.getPierceLevel() > 0) {
                        return 0.0F;
                    }

                    else {
                        // Get the angle of attack vs blocking:
                        double angle = 3.1416F;
                        if (source.getPosition() != null) {
                            Vec3d distanceVector = source.getPosition().subtract(this.getPos());
                            distanceVector = new Vec3d(distanceVector.x, 0.0, distanceVector.z).normalize();
                            angle = Math.acos(distanceVector.dotProduct(this.getRotationVector(0.0F, this.getHeadYaw())));
                        }

                        // Calculate the actual damage reduction amount:
                        float damageReductionAmount = blocksAttacksComponent.getDamageReductionAmount(source, damageAmount, angle);
                        blocksAttacksComponent.onShieldHit(this.getWorld(), blockingItem, (LivingEntity) ((Object)this), this.getActiveHand(), damageReductionAmount);
                        if (!source.isIn(DamageTypeTags.IS_PROJECTILE) && source.getSource() instanceof LivingEntity livingEntity) this.takeShieldHit(world, livingEntity);
                        if (damageReductionAmount <= 0.0F) return damageReductionAmount;

                        // Thorns will deal damage to the attacker
                        int levelThorns = EnchantRegistryHelper.getLevel(getWorld(), blockingItem, Enchantments.THORNS);
                        float reflectedDamage = 0.1F * damageAmount * levelThorns;

                        // Do parrying logic:
                        // If you blocked within 6-9 ticks of an attack, you reflect the attack back (partially)
                        if(wasAttackParried && angle < MAX_ANGLE_TO_BLOCK) {
                            if ((LivingEntity) (Object) this instanceof PlayerEntity player) {
                                player.getItemCooldownManager().set(blockingItem, PARRY_TIME_TICKS << 1);
                                player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.2F, 1F);
                            }
                            if(levelRiposte > 0f) reflectedDamage += 0.2F * damageAmount * levelRiposte;
                            ((LivingEntity) ((Object) this)).clearActiveItem();
                            damageReductionAmount = damageAmount; // Block all damage
                        }

                        // Deal extra knockback and reflect damage onto attacker:
                        if((wasAttackParried || reflectedDamage > 0.0f) && damageReductionAmount > 0.0F && source.getSource() instanceof LivingEntity attacker && !attacker.equals(this)) {
                            double extraKnockbackStrength = wasAttackParried ? 0.8 : 0.4;
                            if(reflectedDamage > 0 && source.getName() != "thorns") {
                                if ((LivingEntity) (Object) this instanceof PlayerEntity player) attacker.damage((ServerWorld) this.getWorld(), this.getDamageSources().playerAttack(player), reflectedDamage);
                                else attacker.damage((ServerWorld) this.getWorld(), this.getDamageSources().mobAttack((LivingEntity) ((Object)this)), reflectedDamage);
                            }
                            attacker.takeKnockback(extraKnockbackStrength, this.getX() - attacker.getX(), this.getZ() - attacker.getZ());
                        }

                        // Check if mob was blocking shield and the shield is now disabled:
                        else if(this.getWorld() instanceof ServerWorld serverWorld && (LivingEntity) (Object) this instanceof MobEntity mob && source.getSource() instanceof LivingEntity attacker) {
                            if (attacker.getWeaponDisableBlockingForSeconds() > 0.0F) {

                                // Drop the shield:
                                ItemStack shieldItemStack = mob.getOffHandStack();
                                if(mob.isPersistent() || serverWorld.getRandom().nextDouble() < 0.1) {
                                    ItemEntity itemEntity = new ItemEntity(this.getWorld(), this.getX(), this.getY(), this.getZ(), shieldItemStack.copy());
                                    itemEntity.setPickupDelay(40);
                                    serverWorld.spawnEntity(itemEntity);
                                }
                                shieldItemStack.setCount(0);

                                // Stop blocking
                                mob.playSound(SoundEvents.ITEM_SHIELD_BREAK.value(), 0.8F, 1.0F);
                                mob.setPose(EntityPose.STANDING);
                                mob.stopUsingItem();
                            }
                        }

                        // Return:
                        return damageReductionAmount;
                    }
                } else {
                    return 0.0F;
                }
            }
        }
    }
}
