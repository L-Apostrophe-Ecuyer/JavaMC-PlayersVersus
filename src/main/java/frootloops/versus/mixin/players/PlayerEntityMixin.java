package frootloops.versus.mixin.players;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.Prediction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world, ItemCooldowns itemCooldownManager, FoodData hungerManager, Inventory inventory) {
        super(entityType, world);
        this.cooldowns = itemCooldownManager;
        this.inventory = inventory;
    }

    @Shadow private final ItemCooldowns cooldowns;
    @Shadow private FoodData foodData;
    @Shadow private ItemStack lastItemInMainHand;

    @Shadow final Inventory inventory;

    @Shadow private final Abilities abilities = new Abilities();

    @Shadow public int totalExperience, experienceLevel, enchantmentSeed;

    @Shadow protected void removeEntitiesOnShoulder() {}

    @Overwrite
    public double entityInteractionRange() {
        return Combat.getAttackRange((Player) ((Object)this));
    }

    @Inject(method = "createAttributes", at = @At(value = "HEAD"), cancellable = true)
    private static void createPlayerAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.setReturnValue(
                LivingEntity.createLivingAttributes()
                        .add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(Combat.CRITICAL_ATTACK_DAMAGE_ATTRIBUTE))
                        .add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(Combat.SPRINT_ATTACK_DAMAGE_ATTRIBUTE))
                        .add(Attributes.ATTACK_DAMAGE, Combat.PLAYER_BASE_ATTACK_DAMAGE)
                        .add(Attributes.ATTACK_SPEED,  Combat.PLAYER_BASE_ATTACK_SPEED)
                        .add(Attributes.MOVEMENT_SPEED, 0.1F)
                        .add(Attributes.LUCK)
                        .add(Attributes.BLOCK_INTERACTION_RANGE, 5.0)
                        .add(Attributes.ENTITY_INTERACTION_RANGE,  Combat.PLAYER_BASE_ATTACK_REACH)
                        .add(Attributes.BLOCK_BREAK_SPEED)
                        .add(Attributes.SUBMERGED_MINING_SPEED)
                        .add(Attributes.SNEAKING_SPEED)
                        .add(Attributes.MINING_EFFICIENCY)
                        .add(Attributes.SWEEPING_DAMAGE_RATIO)
                        .add(Attributes.WAYPOINT_TRANSMIT_RANGE, 6.0E7)
                        .add(Attributes.WAYPOINT_RECEIVE_RANGE, 6.0E7)
        );
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (this.isInvulnerableTo(world, source)) return false;
        else if (this.abilities.invulnerable && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        else {
            this.noActionTime = 0;
            if (this.isDeadOrDying()) {
                return false;
            } else {
                this.removeEntitiesOnShoulder();
                if (source.scalesWithDifficulty()) {
                    if (world.getDifficulty() == Difficulty.EASY || world.getDifficulty() == Difficulty.PEACEFUL) amount = Math.min(amount / 2.0F + 1.0F, amount);
                    else if (world.getDifficulty() == Difficulty.HARD) amount = amount * 3.0F / 2.0F;
                }
                if(amount > 0f && this.isSleeping()) {
                    ((Player)((Object)this)).stopSleepInBed(false, true);
                    if(source.getEntity() instanceof LivingEntity attackingEntity) {
                        this.lookAt(EntityAnchorArgument.Anchor.EYES, attackingEntity.getEyePosition());
                    }
                }
                if (VersusSettings.Combat.DO_FOOD_EATING_INTERRUPTION && source.getEntity() != null && amount > 1.0F) {
                    int maxUseTime = useItem.getUseDuration(this);
                    if (maxUseTime < 64) {
                        useItemRemaining = maxUseTime;
                    }
                }
                return amount == 0.0F ? false : super.hurtServer(world, source, amount);
            }
        }
    }

    @Inject(method = "hasCorrectToolForDrops", at = @At("RETURN"), cancellable = true)
    public void canMineCopperWithWood(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue() && this.getMainHandItem().is(Items.WOODEN_PICKAXE) && (state.getSoundType() == SoundType.COPPER || state.is(Blocks.COPPER_ORE) || state.is(Blocks.RAW_COPPER_BLOCK))) cir.setReturnValue(true);
    }

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    public void getBlockBreakingSpeed(BlockState blockState, CallbackInfoReturnable<Float> cir) {
        if(abilities.instabuild) cir.setReturnValue(Float.MAX_VALUE);
        float breakingSpeed = cir.getReturnValue();
        if (!this.onGround()) breakingSpeed *= this.getDeltaMovement().y >= 0.0 ? 4f : 3f;

        Block block = blockState.getBlock();
        if(block == Blocks.COBWEB) {
            cir.setReturnValue(breakingSpeed * 0.75f + 6f);
            return;
        }

        if(blockState.getSoundType() == SoundType.DEEPSLATE) {
            Tool toolComponent = this.getMainHandItem().getOrDefault(DataComponents.TOOL, null);
            if(toolComponent == null) return;
            else if(this.getMainHandItem().is(Items.GOLDEN_PICKAXE)) cir.setReturnValue(breakingSpeed + 0.5F);
            else if(!toolComponent.isCorrectForDrops(Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState())) {
                cir.setReturnValue(breakingSpeed/3f);
            }
            else cir.setReturnValue(breakingSpeed + (toolComponent.defaultMiningSpeed() >= 9.0F ? 2.0f : 0.5f));
            return;
        }

        if(blockState.ignitedByLava() && blockState.getSoundType() == SoundType.WOOD) {
            cir.setReturnValue(breakingSpeed + 2f);
            return;
        }

        if(block instanceof ShulkerBoxBlock) {
            cir.setReturnValue(breakingSpeed + 8f);
            return;
        }

        if(block instanceof BrushableBlock) {
            cir.setReturnValue(breakingSpeed - 0.6f);
            return;
        }
        cir.setReturnValue(breakingSpeed);
    }

    @Override
    protected float getDamageAfterMagicAbsorb(DamageSource source, float amount) {
        if(source.is(DamageTypes.SONIC_BOOM) && this.level() instanceof ServerLevel serverWorld) {
            float protectionAmount = EnchantmentHelper.getDamageProtection(serverWorld, this, source);
            if (protectionAmount > 0) amount = CombatRules.getDamageAfterMagicAbsorb(amount, protectionAmount);
        }
        return super.getDamageAfterMagicAbsorb(source, amount);
    }

    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 0)
    private float modifyAttackDamage(float amount) {
        Combat.AttackType type = Combat.getAttackType(this);
        if(type == Combat.AttackType.SPRINT) amount = Math.max(1.0f, amount + (float)this.getAttributeValue(Combat.SPRINT_ATTACK_DAMAGE));
        else if(type == Combat.AttackType.CRITICAL) {
            float critExtraDamage = (float)this.getAttributeValue(Combat.CRITICAL_ATTACK_DAMAGE);
            if(critExtraDamage == 0.0f) return Math.max(amount, 1.0f);
            else {
                float critAmountExpected = Math.max(amount, 1.0f) * 1.5F + critExtraDamage;
                amount = critAmountExpected/1.5F;
            }
        }
        return amount;
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    public void attackTypes(Entity target, CallbackInfo ci) {
        if(this.level().isClientSide()) return;

        // After attacking, the shield is interrupted:
        if(this.getOffhandItem().getItem() instanceof ShieldItem) {
            this.stopUsingItem();
            cooldowns.addCooldown(this.getOffhandItem(), 6);
        }

        double amount = this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Combat.AttackType type = Combat.getAttackType(this);
        if(type == Combat.AttackType.CRITICAL) return;

        if(amount < 1.0) {
            if (target instanceof VehicleEntity || target instanceof ArmorStand) {
                target.hurtServer((ServerLevel) this.level(), this.damageSources().playerAttack((Player)((Object)this)), 2.0f);
            }
            else if (target instanceof LivingEntity livingEntity) {
                double strength = type == Combat.AttackType.SPRINT ? 0.8 : 0.6;
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0f, 1.0f);
                livingEntity.knockback(strength, this.getX() - target.getX(), this.getZ() - target.getZ(), this.damageSources().playerAttack((Player)((Object)this)), (float) amount);
            }
            // The deflection power vanilla's own deflection passes since 26.3.
            else if (target.is(EntityTypeTags.REDIRECTABLE_PROJECTILE)
                    && target instanceof Projectile projectileEntity
                    && projectileEntity.deflect(ProjectileDeflection.AIM_DEFLECT, this, EntityReference.of(this), true, 1.0)) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource());
            }
            ci.cancel(); // Cancel attack
        }
    }


    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 3)
    private boolean doSweepingAttacksOnRegularSwings(boolean isSweep) {
        return isSweep && (EnchantRegistryHelper.getLevel(this.level(), this.getMainHandItem(), Enchantments.SWEEPING_EDGE) >= VersusSettings.Combat.MIN_SWEEPING_LEVEL_FOR_SWEEPING_ATTACKS);
    }

    @Inject(method = "attack", at = @At("TAIL"))
    public void attackKnockbackKitingNerf(Entity target, CallbackInfo ci) {
        // Attacking while walking backwards deals less knockback:
        boolean isStillOrWalkingBackwards = (this.onGround() && !this.isSprinting()) && (this.getDeltaMovement().x == 0d) && (this.getDeltaMovement().z == 0d);

        // Attacking while walking backwards deals less knockback:
        if(isStillOrWalkingBackwards) target.setDeltaMovement(target.getDeltaMovement().multiply(0.3d, 0.8d, 0.3d));
        else if(target.getDeltaMovement().lengthSqr() < 1.0 && target instanceof LivingEntity livingEntity)
            livingEntity.knockback(0.4, this.getX() - target.getX(), this.getZ() - target.getZ(), this.damageSources().playerAttack((Player)((Object)this)), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @Override
    public void setUUID(UUID uuid) {
        if(VersusSettings.Combat.DO_FOOD_REDUCED_ON_SPAWN) this.foodData.setFoodLevel(6);
        super.setUUID(uuid);
    }

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    public void canConsume(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        if(VersusSettings.Combat.DO_FOOD_OVERHAUL) {
            cir.setReturnValue(ignoreHunger || (this.foodData.needsFood() && (this.foodData.getFoodLevel() < 6 + this.getMaxHealth() - this.getHealth())));
        }
    }

    @Override
    public void setSprinting(boolean sprinting) {
        if(sprinting && !Combat.canPlayerSprint(this.foodData, this.hasEffect(MobEffects.HUNGER))) return;
        super.setSprinting(sprinting);
    }


    // Upon death, important items take much longer to despawn! And players that killed another player get dibs on the loot, as well.
    @Override
    public void dropEquipment(ServerLevel world) {
        super.dropEquipment(world);
        Player murderer = null;
        boolean wasKilledByPlayer = false;
        if(this.getKillCredit() instanceof Player primeAdversary) {
            murderer = primeAdversary;
            wasKilledByPlayer = true;
        }

        if (!world.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack itemStack = inventory.getItem(i);
                if (EnchantRegistryHelper.hasEnchantment(itemStack, Enchantments.VANISHING_CURSE)) inventory.setItem(i, ItemStack.EMPTY);
                else if (!itemStack.isEmpty()) {
                    // Dropped by the server, not thrown from the hand (the old third argument, false).
                    ItemEntity entity = this.drop(itemStack, true, Prediction.SERVER_ONLY);
                    boolean shouldNeverDespawn = (itemStack.getRarity() == Rarity.EPIC || itemStack.getComponents().has(DataComponents.CONTAINER) || itemStack.getComponents().has(DataComponents.DAMAGE_RESISTANT));
                    boolean shouldTakeLongerToDespawn = !shouldNeverDespawn && (itemStack.getCount() > 56 || itemStack.getRarity() != Rarity.COMMON || itemStack.getComponents().has(DataComponents.CUSTOM_NAME) || itemStack.getComponents().has(DataComponents.STORED_ENCHANTMENTS));
                    if(shouldNeverDespawn) entity.setUnlimitedLifetime();
                    else if(shouldTakeLongerToDespawn) entity.setExtendedLifetime();
                    else entity.setPickUpDelay(20);
                    if(wasKilledByPlayer) entity.setTarget(murderer.getUUID());
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    // Increase hitbox size when gliding:
    @Override
    public float getPickRadius() {
        return this.isFallFlying() ? 2.0f : 0.0f;
    }
}