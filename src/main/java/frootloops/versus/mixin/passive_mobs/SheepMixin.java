
package frootloops.versus.mixin.passive_mobs;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Objects;
import java.util.function.Predicate;

@Mixin(SheepEntity.class)
public abstract class SheepMixin extends AnimalEntity {

    @Shadow
    private int eatGrassTimer;
    @Shadow
    private EatGrassGoal eatGrassGoal;

    private FleeEntityGoal fleePlayersGoal;

    protected SheepMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyArg(method = "sheared", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I"))
    private int doubleTheAmountOfWool(int three) {
        return 6;
    }

    @Override
    protected void eat(PlayerEntity player, Hand hand, ItemStack stack) {
        this.goalSelector.remove(fleePlayersGoal);
        super.eat(player, hand, stack);
    }

    @Override
    public void initGoals() {
        this.fleePlayersGoal = new FleeEntityGoal<>((SheepEntity) ((Object)this), PlayerEntity.class, 8.0F, 0.9, 1.2);
        this.eatGrassGoal = new EatGrassGoal(this);
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.3));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(3, new TemptGoal(this, 1.2, Ingredient.ofItems(Items.WHEAT), false));
        this.goalSelector.add(4, this.fleePlayersGoal);
        this.goalSelector.add(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.add(6, this.eatGrassGoal);
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(9, new LookAroundGoal(this));
    }
}
