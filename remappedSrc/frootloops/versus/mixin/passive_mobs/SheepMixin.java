
package frootloops.versus.mixin.passive_mobs;

import frootloops.versus.mod.items.equipment.KnifeItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Shearable;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.function.Predicate;

@Mixin(SheepEntity.class)
public abstract class SheepMixin extends AnimalEntity implements Shearable {
    @Shadow private EatGrassGoal eatGrassGoal;

    protected SheepMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyArg(method = "sheared", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I"))
    private int muchMoreWool(int three) {
        return 8;
    }

    public Predicate<Entity> NOTICEABLE_PLAYER_FILTER = (entity) -> {
        return !entity.isSneaky() && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(entity);
    };

    protected void eat(PlayerEntity player, Hand hand, ItemStack stack) {
        if (this.isBreedingItem(stack)) {
            if(this.getLovingPlayer() == null) {

                // Reset goals, but without the fleeing.
                // Bad code practice, but ya gotta do what ya gotta do.
                this.goalSelector.getGoals().clear();
                eatGrassGoal = new EatGrassGoal(this);
                this.goalSelector.add(0, new SwimGoal(this));
                this.goalSelector.add(1, new EscapeDangerGoal(this, 1.3));
                this.goalSelector.add(2, new AnimalMateGoal(this, 1.0));
                this.goalSelector.add(3, new TemptGoal(this, 1.2, Ingredient.ofItems(Items.WHEAT), false));
                this.goalSelector.add(5, new FollowParentGoal(this, 1.1));
                this.goalSelector.add(6, this.eatGrassGoal);
                this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
                this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
                this.goalSelector.add(9, new LookAroundGoal(this));
            }
            this.lovePlayer(player);
        }
        super.eat(player, hand, stack);
    }

    @Override
    public void initGoals() {
        eatGrassGoal = new EatGrassGoal(this);
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.3));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(3, new TemptGoal(this, 1.2, Ingredient.ofItems(Items.WHEAT), false));
        if(!this.isBaby()) this.goalSelector.add(4, new FleeEntityGoal(this, PlayerEntity.class, 16.0F, 1.6, 1.4, (entity) -> {return !((Entity) entity).isSneaky();}));
        this.goalSelector.add(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.add(6, this.eatGrassGoal);
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(9, new LookAroundGoal(this));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player2, Hand hand) {
        ItemStack itemStack = player2.getStackInHand(hand);
        if (itemStack.isOf(Items.SHEARS) || itemStack.getItem() instanceof KnifeItem) {
            if (!this.world.isClient && this.isShearable()) {
                this.sheared(SoundCategory.PLAYERS);
                this.emitGameEvent(GameEvent.SHEAR, player2);
                itemStack.damage(1, player2, player -> player.sendToolBreakStatus(hand));
                return ActionResult.SUCCESS;
            }
            return ActionResult.CONSUME;
        }
        return super.interactMob(player2, hand);
    }
}
