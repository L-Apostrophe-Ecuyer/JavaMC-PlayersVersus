
package frootloops.versus.mixin.passive_mobs;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.passive_mobs.PiggingAroundGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mixin(PigEntity.class)
public abstract class PigMixin extends AnimalEntity {

    protected PigMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.3));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(3, new TemptGoal(this, 1.2, Ingredient.ofItems(Items.WHEAT), false));
        this.goalSelector.add(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.add(6, new PiggingAroundGoal(this));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(9, new LookAroundGoal(this));
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
