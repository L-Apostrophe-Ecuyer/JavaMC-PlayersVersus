package frootloops.versus.mod.mobs.passive;

import frootloops.versus.VersusMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A wolf Blight turned undead, as a horse turns into a zombie horse: still a wolf, as wild or as tame as it was and its
 * owner's still (Undeath), following them, sitting when told and fighting for them, its collar and armour kept; but
 * dead. It's never a pup and never breeds; it eats only rotten flesh (a tame one heals on it), and its voice is lower.
 * It's undead (smite, healing harms it) and burns in daylight, as the zombie chicken. It isn't spared the Blight:
 * dying fully blighted, it turns into a pale wolf, its owner forgotten (Undeath).
 */
public class ZombieWolfEntity extends Wolf {
    private static final Identifier WILD = texture("zombie_wolf");
    private static final Identifier TAME = texture("zombie_wolf_tame");
    private static final Identifier ANGRY = texture("zombie_wolf_angry");

    public ZombieWolfEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/" + name + ".png");
    }

    /** Its own skin, whatever kind of wolf it was: tame, angry or wild, as a wolf's (tools/wolves.py). */
    @Override
    public Identifier getTexture() {
        if (this.isTame()) return TAME;
        return this.isAngry() ? ANGRY : WILD;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.ROTTEN_FLESH);
    }

    /** Fed, it heals; it never falls in love. */
    @Override
    public boolean canFallInLove() {
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

    /** Never a pup: a pup turning undead turns grown. */
    @Override
    protected boolean canBeABaby() {
        return false;
    }

    @Override
    public void setAge(int age) {
        super.setAge(Math.max(age, 0));
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }
}
