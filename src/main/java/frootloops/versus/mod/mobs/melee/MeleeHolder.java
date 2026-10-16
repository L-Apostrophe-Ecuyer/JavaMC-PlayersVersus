package frootloops.versus.mod.mobs.melee;

import org.jetbrains.annotations.Nullable;

/** Implemented on every {@code Mob} by a mixin: the mob's melee swing state ({@link MeleeState}). */
public interface MeleeHolder {
    /** The mob's swing state, made on first use. */
    MeleeState playersVersus$melee();

    /** The mob's swing state, or null if it never swung. */
    @Nullable
    MeleeState playersVersus$meleeIfAny();
}
