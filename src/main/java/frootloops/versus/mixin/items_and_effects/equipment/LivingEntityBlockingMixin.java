package frootloops.versus.mixin.items_and_effects.equipment;

import frootloops.versus.mod.enchantments.CustomEnchants;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBlockingMixin extends Entity {
    public LivingEntityBlockingMixin(EntityType<?> type, Level world) {super(type, world);}
    private static final float MAX_ANGLE_TO_BLOCK = (float) (Math.PI / 180.0) * 90F;
    private static final int PARRY_TIME_TICKS = 6;

    @Shadow protected int useItemRemaining;
    @Shadow protected ItemStack useItem;
    @Shadow protected static EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS;

    @Shadow
    public InteractionHand getUsedItemHand() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Shadow
    @Nullable
    public ItemStack getItemBlockingWith() {return null;}

    @Shadow
    protected void blockUsingItem(ServerLevel world, LivingEntity attacker) {
        attacker.knockback(0.5, attacker.getX() - this.getX(), attacker.getZ() - this.getZ());
    }

    @Overwrite
    public float applyItemBlocking(ServerLevel world, DamageSource source, float damageAmount) {
        if (damageAmount <= 0.0F) {
            return 0.0F;
        } else {
            ItemStack blockingItem = this.getItemBlockingWith();
            if (blockingItem == null) {
                return 0.0F;
            } else {
                BlocksAttacks blocksAttacksComponent = blockingItem.get(DataComponents.BLOCKS_ATTACKS);
                if (blocksAttacksComponent != null) {
                    int levelRiposte = EnchantRegistryHelper.getLevel(this.level(), blockingItem, CustomEnchants.RIPOSTE);
                    int ticksToParry = PARRY_TIME_TICKS + levelRiposte;
                    int useTime =  useItem.getUseDuration((LivingEntity) ((Object)this)) - useItemRemaining;
                    boolean wasAttackParried = useTime <= ticksToParry;
                    boolean doesAttackBypassShield = blocksAttacksComponent.bypassedBy().map(source::is).orElse(false) && !(wasAttackParried && source.is(DamageTypes.SONIC_BOOM));
                    if (doesAttackBypassShield) return 0.0F;

                    // Check if we can repel arrow:
                    if ((!blockingItem.is(ItemTags.SWORDS) || wasAttackParried) && source.getDirectEntity() instanceof AbstractArrow persistentProjectileEntity && persistentProjectileEntity.getPierceLevel() > 0) {
                        return 0.0F;
                    }

                    else {
                        // Get the angle of attack vs blocking:
                        double angle = 3.1416F;
                        if (source.getSourcePosition() != null) {
                            Vec3 distanceVector = source.getSourcePosition().subtract(this.position());
                            distanceVector = new Vec3(distanceVector.x, 0.0, distanceVector.z).normalize();
                            angle = Math.acos(distanceVector.dot(this.calculateViewVector(0.0F, this.getYHeadRot())));
                        }

                        // Calculate the actual damage reduction amount:
                        float damageTaken = blocksAttacksComponent.resolveBlockedDamage(source, damageAmount, angle);
                        blocksAttacksComponent.hurtBlockingItem(this.level(), blockingItem, (LivingEntity) ((Object)this), this.getUsedItemHand(), damageTaken);
                        if (!source.is(DamageTypeTags.IS_PROJECTILE) && source.getDirectEntity() instanceof LivingEntity livingEntity) this.blockUsingItem(world, livingEntity);
                        if (damageTaken <= 0.0F) return damageTaken;

                        // Thorns will deal damage to the attacker
                        int levelThorns = EnchantRegistryHelper.getLevel(this.level(), blockingItem, Enchantments.THORNS);
                        float reflectedDamage = 0.1F * damageAmount * levelThorns;

                        // Do parrying logic:
                        // If you blocked within 6-9 ticks of an attack, you reflect the attack back (partially)
                        if(wasAttackParried && angle < MAX_ANGLE_TO_BLOCK) {
                            if ((LivingEntity) (Object) this instanceof Player player) {
                                player.getCooldowns().addCooldown(blockingItem, PARRY_TIME_TICKS << 1);
                                player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2F, 1F);
                            }
                            if(levelRiposte > 0f) reflectedDamage += 0.2F * damageAmount * levelRiposte;
                            ((LivingEntity) ((Object) this)).stopUsingItem();
                            damageTaken = damageAmount; // Block all damage
                        }

                        // Deal extra knockback and reflect damage onto attacker:
                        if((wasAttackParried || reflectedDamage > 0.0f) && damageTaken > 0.0F && source.getDirectEntity() instanceof LivingEntity attacker && !attacker.equals(this)) {
                            double extraKnockbackStrength = wasAttackParried ? 0.8 : 0.4;
                            if(reflectedDamage > 0 && source.getMsgId() != "thorns") {
                                if (this.isAlwaysTicking() && (LivingEntity) (Object) this instanceof Player player) attacker.hurtServer((ServerLevel) this.level(), this.damageSources().playerAttack(player), reflectedDamage);
                                else attacker.hurtServer((ServerLevel) this.level(), this.damageSources().mobAttack((LivingEntity) ((Object)this)), reflectedDamage);
                            }
                            attacker.knockback(extraKnockbackStrength, this.getX() - attacker.getX(), this.getZ() - attacker.getZ());
                        }

                        else if(this.level() instanceof ServerLevel serverWorld && source.getDirectEntity() instanceof LivingEntity attacker) {
                            float weaponDisableForBlocking = attacker.getSecondsToDisableBlocking();

                            // For player, disable shield:
                            if(this.isAlwaysTicking() && (LivingEntity) (Object) this instanceof Player player) {
                                player.resetAttackStrengthTicker();
                                if(!wasAttackParried) player.getCooldowns().addCooldown(blockingItem, 2 + (int)(20f * weaponDisableForBlocking));
                            }

                            // Check if mob was blocking shield and the shield is now disabled:
                            else if(weaponDisableForBlocking > 0.0f && (LivingEntity) (Object) this instanceof Mob mob){
                                ItemStack shieldItemStack = mob.getOffhandItem();
                                if(mob.isPersistenceRequired() || serverWorld.getRandom().nextDouble() < 0.1) {
                                    ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), shieldItemStack.copy());
                                    itemEntity.setPickUpDelay(40);
                                    serverWorld.addFreshEntity(itemEntity);
                                }
                                shieldItemStack.setCount(0);

                                // Stop blocking
                                mob.playSound(SoundEvents.SHIELD_BREAK.value(), 0.8F, 1.0F);
                                mob.setPose(Pose.STANDING);
                                mob.releaseUsingItem();
                            }
                        }

                        // Return:
                        return damageTaken;
                    }
                } else {
                    return 0.0F;
                }
            }
        }
    }
}
