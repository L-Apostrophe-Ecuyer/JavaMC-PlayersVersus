package frootloops.versus.mod.mobs.passive;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A chicken Blight turned undead, as a horse turns into a zombie horse: still a chicken, as calm and as easily
 * kept beside living ones, but dead. It lays no eggs, eats nothing, doesn't breed and is never a chick; it's always
 * the temperate kind (its skin is the temperate chicken's, gone the zombie's green), and its voice is lower. Baby
 * zombies ride them as chicken jockeys. It's undead (smite, healing harms it) and burns in daylight, as the zombie horse.
 */
public class ZombieChickenEntity extends Chicken {
    public ZombieChickenEntity(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        // Never lays: the egg timer never runs out
        this.eggTime = Integer.MAX_VALUE;
        super.aiStep();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public Chicken getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    /** Never a chick: a chick turning undead turns grown. */
    @Override
    protected boolean canBeABaby() {
        return false;
    }

    @Override
    public void setAge(int age) {
        super.setAge(Math.max(age, 0));
    }

    /** Always the temperate kind it's defined with: spawning, loading and spawn eggs don't choose another. */
    @Override
    public void setVariant(Holder<ChickenVariant> variant) {}

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }
}
