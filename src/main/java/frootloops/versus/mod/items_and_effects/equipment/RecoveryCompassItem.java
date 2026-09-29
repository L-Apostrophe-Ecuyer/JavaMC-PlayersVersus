package frootloops.versus.mod.items_and_effects.equipment;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;


public class RecoveryCompassItem extends Item {

    private static final double ENDERMAN_AGGRO_RANGE = 8.0;
    private static final int USE_TIME_TICKS = 160;

    public RecoveryCompassItem(Properties settings) {
        super(settings.durability(USE_TIME_TICKS * 2).rarity(Rarity.RARE));
    }

    public int getDefaultMaxStackSize() {
        return 1;
    }

    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        stack.hurtAndBreak(1, user, EquipmentSlot.MAINHAND);

        // If attacked while using, cancel and apply cooldown:
        if(remainingUseTicks < USE_TIME_TICKS - 20 && world.getGameTime() - user.getLastHurtByMobTimestamp() < 2 && user instanceof Player player) {
            player.getCooldowns().addCooldown(stack, 240);
        }

        // Otherwise: Add effects
        else if(remainingUseTicks % 10 == 0 && user instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, user.getX(), user.getEyeY(), user.getZ(), 16, 0.0, 1.0, 0.0, 0.3);
            if(remainingUseTicks % 40 == 0) {
                user.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
                user.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 3, true, false));
                user.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0));
                world.playSound(user, user.blockPosition(), SoundEvents.PLAYER_BREATH, user.getSoundSource(), 0.1F, 0.4f);

                Optional<GlobalPos> lastDeathPos = serverPlayer.getLastDeathLocation();
                if(!lastDeathPos.isPresent() || lastDeathPos.get().dimension() != world.dimension()) {
                    serverPlayer.getCooldowns().addCooldown(stack, 60);
                }
            }
        }
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        Optional<GlobalPos> lastDeathPos = user.getLastDeathLocation(); // Check if in same dimension as last death location:
        if(!lastDeathPos.isPresent() || lastDeathPos.get().dimension() != world.dimension()) return InteractionResult.FAIL;
        user.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if(user instanceof ServerPlayer serverPlayer) {

            // Teleport near last death location:
            Optional<GlobalPos> lastDeathPos = serverPlayer.getLastDeathLocation();
            if(lastDeathPos.isPresent() && lastDeathPos.get().dimension() == world.dimension()) {
                GlobalPos deathPos = lastDeathPos.get();
                ServerLevel serverWorld = world.getServer().getLevel(lastDeathPos.get().dimension());
                serverPlayer.teleport(createTeleportTarget(serverWorld, serverPlayer, deathPos.pos()));

                // Reset Player's last death location:
                serverPlayer.setLastDeathLocation(Optional.empty());
                world.playSound(user, user.blockPosition(), SoundEvents.PORTAL_TRIGGER, user.getSoundSource(), 0.8f, 1.8f);
                world.playSound(user, user.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, user.getSoundSource(), 1f, 0.8f);

                // Anger nerby Endermen:
                AABB boundingBox = new AABB(user.getX() - ENDERMAN_AGGRO_RANGE, user.getY() - ENDERMAN_AGGRO_RANGE, user.getZ() - ENDERMAN_AGGRO_RANGE, user.getX() + ENDERMAN_AGGRO_RANGE, user.getY() + ENDERMAN_AGGRO_RANGE, user.getZ() + ENDERMAN_AGGRO_RANGE);
                List<Enderman> nearbyEndermen = serverPlayer.level().getEntitiesOfClass(Enderman.class, boundingBox, EntitySelector.LIVING_ENTITY_STILL_ALIVE);
                for (Enderman enderman : nearbyEndermen) {
                    enderman.setTarget(user);
                }
                return new ItemStack(Items.COMPASS, 1);
            }
            else {
                world.playSound(user, user.blockPosition(), SoundEvents.ITEM_BREAK.value(), user.getSoundSource(), 1f, 0.8f);
            }
        }

        // Return a basic compass item:
        return stack;
    }

    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TIME_TICKS;
    }

    private static TeleportTransition createTeleportTarget(ServerLevel serverWorld, ServerPlayer entity, BlockPos pos) {
        return new TeleportTransition(serverWorld, entity.adjustSpawnLocation(serverWorld, pos).getBottomCenter(), entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
    }

}
