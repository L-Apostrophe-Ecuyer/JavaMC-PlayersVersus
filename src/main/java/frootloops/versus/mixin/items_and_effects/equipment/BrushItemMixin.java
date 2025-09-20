package frootloops.versus.mixin.items_and_effects.equipment;

import frootloops.versus.VersusSettings;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BrushItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticleUtil;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BrushItem.class)
public abstract class BrushItemMixin extends Item {
    public BrushItemMixin(Settings settings) {
        super(settings);
    }

    @ModifyConstant(method = "usageTick", constant = @Constant(intValue = 10))
    private int fasterBrushingOne(int timeForFullBrushAnimation) {return VersusSettings.Gameplay.BRUSHING_TICKS_PER_STAGE * 2;}

    @ModifyConstant(method = "usageTick", constant = @Constant(intValue = 5))
    private int fasterBrushingTwo(int timeTillNextUpdate) {return VersusSettings.Gameplay.BRUSHING_TICKS_PER_STAGE;}

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if(!stack.isOf(Items.BRUSH) || stack.getUseAction() != UseAction.BRUSH) return ActionResult.PASS;

        if (entity instanceof ChickenEntity chicken && chicken.isAlive() && !chicken.isBaby()) {
            if(user.getWorld() instanceof ServerWorld serverWorld) {
                chicken.emitGameEvent(GameEvent.ENTITY_INTERACT);
                chicken.playSoundIfNotSilent(SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC);
                int random = user.getRandom().nextInt(5);
                if (random > 1) {
                    stack.damage(16, user, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    chicken.takeKnockback(0.1, user.getX() - chicken.getX(), user.getZ() - chicken.getZ());
                    chicken.dropStack(serverWorld, new ItemStack(Items.FEATHER));
                    chicken.emitGameEvent(GameEvent.ENTITY_INTERACT);
                    chicken.playSound(SoundEvents.ENTITY_ARMADILLO_BRUSH);
                    ParticleUtil.spawnParticlesAround(user.getWorld(), chicken.getBlockPos(), random, ParticleTypes.HAPPY_VILLAGER);
                    chicken.eggLayTime -= random * 250;
                } else {
                    stack.damage(8, user, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    chicken.takeKnockback(0.4, user.getX() - chicken.getX(), user.getZ() - chicken.getZ());
                    chicken.playSoundIfNotSilent(SoundEvents.ENTITY_CHICKEN_HURT);
                    ParticleUtil.spawnParticlesAround(user.getWorld(), chicken.getBlockPos(), 3, ParticleTypes.SMOKE);
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
