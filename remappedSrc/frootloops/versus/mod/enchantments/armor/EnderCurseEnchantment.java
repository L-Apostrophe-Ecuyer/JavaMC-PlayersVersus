package frootloops.versus.mod.enchantments.armor;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.enchantment.BindingCurseEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

public class EnderCurseEnchantment extends Enchantment {
    public EnderCurseEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.ARMOR, new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
    }

    @Override
    public boolean isCursed() {
        return true;
    }

    @Override
    public boolean isTreasure() {
        return true;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return false;
    }

    @Override
    public boolean canAccept(Enchantment other) {
        return !(other instanceof EnderCurseEnchantment || other instanceof BindingCurseEnchantment);
    }

    @Override
    public void onUserDamaged(LivingEntity user, Entity attacker, int level) {
        if(attacker instanceof LivingEntity && user != null & user.isAlive()) {
            if (!user.method_48926().isClient) {
                user.damage(user.getDamageSources().magic(), 2.0f);
                if(!user.isAlive())
                    return;

                user.timeUntilRegen = 18; // 8 ticks of invincibility frames
                double d = user.getX();
                double e = user.getY();
                double f = user.getZ();
                for (int i = 0; i < 16; ++i) {
                    double g = user.getX() + (user.getRandom().nextDouble() - 0.5) * 16.0;
                    double h = MathHelper.clamp(user.getY() + (double)(user.getRandom().nextInt(16) - 8), user.method_48926().getBottomY(), user.method_48926().getBottomY() + ((ServerWorld)user.method_48926()).getLogicalHeight() - 1);
                    double j = user.getZ() + (user.getRandom().nextDouble() - 0.5) * 16.0;
                    if (user.hasVehicle()) {
                        user.stopRiding();
                    }
                    Vec3d vec3d = user.getPos();
                    if (!user.teleport(g, h, j, true)) continue;

                    user.method_48926().emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(user));
                    user.method_48926().playSound(null, d, e, f, SoundEvents.ENTITY_ENDERMAN_TELEPORT, user.getSoundCategory(), 1.0f, 1.0f);
                    user.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    user.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, attacker.getEyePos());
                    break;
                }
            }
        }
    }
}
