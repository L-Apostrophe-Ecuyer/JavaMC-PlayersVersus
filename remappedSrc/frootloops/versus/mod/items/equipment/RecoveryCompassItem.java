package frootloops.versus.mod.items.equipment;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;


public class RecoveryCompassItem extends Item {

    private static final double ENDERMAN_AGGRO_RANGE = 8.0;
    private static final int USE_TIME_TICKS = 160;

    public RecoveryCompassItem(net.minecraft.item.Item.Settings settings) {
        super(settings.maxDamage(USE_TIME_TICKS * 2).rarity(Rarity.RARE));
    }

    public int getMaxCount() {
        return 1;
    }

    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        stack.damage(1, user, EquipmentSlot.MAINHAND);

        // If attacked while using, cancel and apply cooldown:
        if(remainingUseTicks < USE_TIME_TICKS - 20 && world.getTime() - user.getLastAttackedTime() < 2 && user instanceof PlayerEntity player) {
            player.getItemCooldownManager().set(stack.getItem(), 240);
        }

        // Otherwise: Add effects
        else if(remainingUseTicks % 10 == 0 && user instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.getServerWorld().spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, user.getX(), user.getEyeY(), user.getZ(), 16, 0.0, 1.0, 0.0, 0.3);
            if(remainingUseTicks % 40 == 0) {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 120, 0));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 3, true, false));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 0));
                world.playSound(user, user.getBlockPos(), SoundEvents.ENTITY_PLAYER_BREATH, user.getSoundCategory(), 0.1F, 0.4f);

                Optional<GlobalPos> lastDeathPos = serverPlayer.getLastDeathPos();
                if(!lastDeathPos.isPresent() || lastDeathPos.get().dimension() != world.getRegistryKey()) {
                    serverPlayer.getItemCooldownManager().set(stack.getItem(), 60);
                }
            }
        }
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        Optional<GlobalPos> lastDeathPos = user.getLastDeathPos(); // Check if in same dimension as last death location:
        if(!lastDeathPos.isPresent() || lastDeathPos.get().dimension() != world.getRegistryKey()) return TypedActionResult.fail(itemStack);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if(user instanceof ServerPlayerEntity serverPlayer) {

            // Teleport near last death location:
            Optional<GlobalPos> lastDeathPos = serverPlayer.getLastDeathPos();
            if(lastDeathPos.isPresent() && lastDeathPos.get().dimension() == world.getRegistryKey()) {
                GlobalPos deathPos = lastDeathPos.get();
                ServerWorld serverWorld = world.getServer().getWorld(lastDeathPos.get().dimension());
                serverPlayer.teleportTo(createTeleportTarget(serverWorld, serverPlayer, deathPos.pos()));

                // Reset Player's last death location:
                serverPlayer.setLastDeathPos(Optional.empty());
                world.playSound(user, user.getBlockPos(), SoundEvents.BLOCK_PORTAL_TRIGGER, user.getSoundCategory(), 0.8f, 1.8f);
                world.playSound(user, user.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, user.getSoundCategory(), 1f, 0.8f);

                // Anger nerby Endermen:
                Box boundingBox = new Box(user.getX() - ENDERMAN_AGGRO_RANGE, user.getY() - ENDERMAN_AGGRO_RANGE, user.getZ() - ENDERMAN_AGGRO_RANGE, user.getX() + ENDERMAN_AGGRO_RANGE, user.getY() + ENDERMAN_AGGRO_RANGE, user.getZ() + ENDERMAN_AGGRO_RANGE);
                List<EndermanEntity> nearbyEndermen = serverPlayer.getServerWorld().getEntitiesByClass(EndermanEntity.class, boundingBox, EntityPredicates.VALID_LIVING_ENTITY);
                for (EndermanEntity enderman : nearbyEndermen) {
                    enderman.setTarget(user);
                }
                return new ItemStack(Items.COMPASS, 1);
            }
            else {
                world.playSound(user, user.getBlockPos(), SoundEvents.ENTITY_ITEM_BREAK, user.getSoundCategory(), 1f, 0.8f);
            }
        }

        // Return a basic compass item:
        return stack;
    }

    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return USE_TIME_TICKS;
    }

    private static TeleportTarget createTeleportTarget(ServerWorld serverWorld, ServerPlayerEntity entity, BlockPos pos) {
        return new TeleportTarget(serverWorld, entity.getWorldSpawnPos(serverWorld, pos).toBottomCenterPos(), entity.getVelocity(), entity.getYaw(), entity.getPitch(), TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET.then(TeleportTarget.ADD_PORTAL_CHUNK_TICKET));
    }

}
