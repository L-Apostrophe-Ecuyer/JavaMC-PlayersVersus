package frootloops.versus.mod.items_and_effects.fruit;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Sour, from eating a lemon: the view widens absurdly for a moment (SourFov, on the client's camera). Nothing else. */
public class SourStatusEffect extends MobEffect {

    /** Lemon yellow. */
    public static final int COLOR = 0xF4E04D;

    public SourStatusEffect() {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }
}
