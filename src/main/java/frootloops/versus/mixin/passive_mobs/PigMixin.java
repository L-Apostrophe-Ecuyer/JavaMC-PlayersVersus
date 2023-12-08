
package frootloops.versus.mixin.passive_mobs;

import frootloops.versus.mod.passive_mobs.PiggingAroundGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PigEntity.class)
public abstract class PigMixin extends AnimalEntity {

    protected PigMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    private static final Ingredient BREEDING_INGREDIENT = Ingredient.ofItems(Items.CARROT, Items.POTATO, Items.BEETROOT, Items.CARROT_ON_A_STICK);

    @Inject(method = "createPigAttributes", at = @At("HEAD"), cancellable = true)
    private static void createPigAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 12.0));
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.25));
        this.goalSelector.add(3, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(4, new TemptGoal(this, 1.2, BREEDING_INGREDIENT, false));
        this.goalSelector.add(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.goalSelector.add(6, new PiggingAroundGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.3, true));
        this.targetSelector.add(0, new RevengeGoal(this, AbstractPiglinEntity.class).setGroupRevenge(new Class[0]).setGroupRevenge(null));
        this.targetSelector.add(1, new ActiveTargetGoal<ZombieEntity>((MobEntity)this, ZombieEntity.class, false));
    }

    public void onEatingGrass() {
        super.onEatingGrass();
        this.setPitch(60f);
        int count = 1 + this.random.nextBetween(0,1);

        Item dugUpItem = Items.BROWN_MUSHROOM;
        int rand = this.random.nextInt(100);
        if(rand % 2 == 0) {
            if (rand < 46) dugUpItem = Items.RED_MUSHROOM;
            else if (rand > 96) dugUpItem = Items.GOLDEN_APPLE;
            else if (rand > 92) dugUpItem = Items.GOLDEN_CARROT;
            else if (rand > 88) dugUpItem = Items.APPLE;
            else if (rand > 74) dugUpItem = Items.CARROT;
            else if (rand > 60) dugUpItem = Items.POTATO;
            else dugUpItem = Items.WHEAT_SEEDS;
        }

        for(int j = 0; j < count; ++j) {
            ItemEntity itemEntity = this.dropItem(dugUpItem, 1);
            if (itemEntity != null) itemEntity.setVelocity(itemEntity.getVelocity().add((double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1F), (double)(this.random.nextFloat() * 0.05F), (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1F)));
        }
    }

    @Override
    public void setPitch(float pitch) {
        super.setPitch(pitch);
    }
}
