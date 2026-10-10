package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * A spider of the Pale Garden: it crawls and bites like any other, but holds still while a player has it in view, and
 * only creeps closer once they look away ({@link FreezeWhenWatchedGoal}).
 */
public class PaleSpiderEntity extends Spider {

    public PaleSpiderEntity(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new FreezeWhenWatchedGoal(this));
    }

    /** The garden's shade is enough: none of the depth and open-sky limits other spiders have. */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return this.getWalkTargetValue(this.blockPosition(), level) >= 0.0F;
    }
}
