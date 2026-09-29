package frootloops.versus.mixin.items_and_effects.equipment;

import frootloops.versus.VersusSettings;
import net.minecraft.world.entity.animal.chicken.ChickenSoundVariants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ParticleUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BrushItem.class)
public abstract class BrushItemMixin extends Item {
    public BrushItemMixin(Properties settings) {
        super(settings);
    }

    @ModifyConstant(method = "onUseTick", constant = @Constant(intValue = 10))
    private int fasterBrushingOne(int timeForFullBrushAnimation) {return VersusSettings.Gameplay.BRUSHING_TICKS_PER_STAGE * 2;}

    @ModifyConstant(method = "onUseTick", constant = @Constant(intValue = 5))
    private int fasterBrushingTwo(int timeTillNextUpdate) {return VersusSettings.Gameplay.BRUSHING_TICKS_PER_STAGE;}

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if(!stack.is(Items.BRUSH) || stack.getUseAnimation() != ItemUseAnimation.BRUSH) return InteractionResult.PASS;

        if (entity instanceof Chicken chicken && chicken.isAlive() && !chicken.isBaby()) {
            if(user.level() instanceof ServerLevel serverWorld) {
                chicken.gameEvent(GameEvent.ENTITY_INTERACT);
                chicken.playSound(SoundEvents.BRUSH_GENERIC);
                int random = user.getRandom().nextInt(5);
                if (random > 1) {
                    stack.hurtAndBreak(16, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    chicken.knockback(0.1, user.getX() - chicken.getX(), user.getZ() - chicken.getZ(), user.damageSources().playerAttack(user), 0.0f);
                    chicken.spawnAtLocation(serverWorld, new ItemStack(Items.FEATHER));
                    chicken.gameEvent(GameEvent.ENTITY_INTERACT);
                    chicken.makeSound(SoundEvents.ARMADILLO_BRUSH);
                    ParticleUtils.spawnParticleInBlock(user.level(), chicken.blockPosition(), random, ParticleTypes.HAPPY_VILLAGER);
                    chicken.eggTime -= random * 250;
                } else {
                    stack.hurtAndBreak(8, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    chicken.knockback(0.4, user.getX() - chicken.getX(), user.getZ() - chicken.getZ(), user.damageSources().playerAttack(user), 0.0f);
                    // The hurt sound of the classic chicken, which SoundEvents.CHICKEN_HURT was before chickens had sound variants.
                    chicken.playSound(SoundEvents.CHICKEN_SOUNDS.get(ChickenSoundVariants.SoundSet.CLASSIC).adultSounds().hurtSound().value());
                    ParticleUtils.spawnParticleInBlock(user.level(), chicken.blockPosition(), 3, ParticleTypes.SMOKE);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
