package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BegGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A wolf the paleness took, down to its bones: a skeleton (tools/wolves.py: a rib cage, vertebrae, a skull whose sockets
 * glow the creaking's orange, PaleWolfRenderer). Packs of them run in the Pale Garden and, at night, in the forests
 * (MobSpawning#canSpawnPaleWolf), howling and rattling; a tamed zombie wolf dying fully blighted turns into one, its
 * owner forgotten (Undeath). It hunts players, and whoever hurts it (its pack with it); its bite blights, as every pale
 * thing's (BlightAttacks), and it takes no Blight itself. As a skeleton does, it fears wolves (living ones, which hunt
 * it: WolfHuntsPaleWolvesMixin). It's never tamed, fed or bred, never a pup. Golems take it for the monster it is.
 */
public class PaleWolfEntity extends Wolf implements Enemy {
    private static final Identifier WILD = texture("pale_wolf");
    private static final Identifier ANGRY = texture("pale_wolf_angry");
    /** Ticks between its calls, on average: a howl now and then, not a wolf's chatter. */
    private static final int CALL_INTERVAL = 200;

    public PaleWolfEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/" + name + ".png");
    }

    /**
     * A wolf's moves (it leaps, bites, wanders and looks about), none of a pet's (begging, breeding, following, sitting);
     * a skeleton's fear of wolves (living ones: not its own kind, nor zombie wolves); and a hunter's targets in place of
     * a wolf's: players, and whoever hurts it.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(goal -> goal instanceof BegGoal || goal instanceof BreedGoal
                || goal instanceof FollowOwnerGoal || goal instanceof SitWhenOrderedToGoal);
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Wolf.class, 6.0F, 1.0, 1.2, entity -> entity.getType() == EntityTypes.WOLF));
        this.targetSelector.removeAllGoals(goal -> true);
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Hunting or wild: never tame. */
    @Override
    public Identifier getTexture() {
        return this.isAngry() ? ANGRY : WILD;
    }

    /** A bone, meat, a dye do nothing to it (what a hand does to any mob still does: a name tag, a lead). */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Nullable
    @Override
    public Wolf getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    protected boolean canBeABaby() {
        return false;
    }

    @Override
    public void setAge(int age) {
        super.setAge(Math.max(age, 0));
    }

    /** Where it spawns is the pale line's to say (MobSpawning#canSpawnPaleWolf): not an animal's want of grass and light. */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return true;
    }

    /** It wanders off, as monsters do, once no one is near (one that turned is kept: Undeath). */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return CALL_INTERVAL;
    }

    /** Hunting, it growls as a wolf; otherwise it howls, or its bones rattle. */
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isAngry() ? super.getAmbientSound() : CustomSpecialEffects.PALE_WOLF_HOWL;
    }

    /**
     * A howl (its own, vanilla's wolves no longer howling: tools/pale-wolf-howl.py) carries, half again a wolf's call and
     * as far; one time in three, a rattle of bones instead.
     */
    @Override
    public void playAmbientSound() {
        if (this.isAngry()) {
            super.playAmbientSound();
        } else if (this.random.nextInt(3) == 0) {
            this.playSound(CustomSpecialEffects.PALE_WOLF_RATTLE, 0.6F, this.getVoicePitch());
        } else {
            this.playSound(CustomSpecialEffects.PALE_WOLF_HOWL, 1.5F, this.getVoicePitch());
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return CustomSpecialEffects.PALE_WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return CustomSpecialEffects.PALE_WOLF_DEATH;
    }

    /** It walks on bones, as a skeleton does. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SKELETON_STEP, 0.15F, 1.0F);
    }
}
