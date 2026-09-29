
package frootloops.versus.mixin.mobs.passive;

import frootloops.versus.mod.mobs.passive.PiggingAroundGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pig.class)
public abstract class PigMixin extends Animal {

    protected PigMixin(EntityType<? extends Animal> entityType, Level world) {
        super(entityType, world);
    }

    private static final Ingredient BREEDING_INGREDIENT = Ingredient.of(Items.CARROT, Items.POTATO, Items.BEETROOT, Items.CARROT_ON_A_STICK);

    @Inject(method = "createAttributes", at = @At("HEAD"), cancellable = true)
    private static void createPigAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.setReturnValue(Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 9.0));
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        if(super.doHurtTarget(world, target)) {
            this.playSound(SoundEvents.HOGLIN_RETREAT, 0.5F, 1.8F);
            return true;
        }
        else return false;
    }

    @Override
    public void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, BREEDING_INGREDIENT, false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.goalSelector.addGoal(6, new PiggingAroundGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this, AbstractPiglin.class).setAlertOthers(new Class[0]).setAlertOthers((Class<?>) null));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Zombie>((Mob)this, Zombie.class, false));
    }

    public void ate() {
        super.ate();
        this.setXRot(60f);
        int count = 1 + this.random.nextIntBetweenInclusive(0,1);
        if(this.level().isClientSide()) return;

        Item dugUpItem = Items.BROWN_MUSHROOM;
        int rand = this.random.nextInt(100);
        if(rand % 2 == 0) {
            if (rand < 46) dugUpItem = Items.RED_MUSHROOM;
            else if (rand > 96) dugUpItem = Items.GOLDEN_APPLE;
            else if (rand > 92) dugUpItem = Items.GOLDEN_CARROT;
            else if (rand > 88) dugUpItem = Items.APPLE;
            else if (rand > 74) dugUpItem = Items.CARROT;
            else if (rand > 60) dugUpItem = Items.POTATO;
            else dugUpItem = Items.BEETROOT_SEEDS;
        }

        for(int j = 0; j < count; ++j) {
            ItemEntity itemEntity = this.drop(dugUpItem.getDefaultInstance(), true, false);
            if (itemEntity != null) itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().add((double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1F), (double)(this.random.nextFloat() * 0.05F), (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1F)));
        }
    }

    @Override
    public void setXRot(float pitch) {
        super.setXRot(pitch);
    }
}
