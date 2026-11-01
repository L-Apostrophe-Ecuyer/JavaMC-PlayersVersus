package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.hostile.overworld.MushroomCreeperEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public class CreeperMixin extends Monster {
    @Shadow private int swell;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_IS_IGNITED;

    protected CreeperMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    /**
     * A mushroom creeper fully swollen lets out its spores instead of exploding (MushroomCreeperEntity#puff), and starts
     * over from no swelling. On the client nothing happens either way: the server explodes or puffs.
     */
    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void playersVersus$sporePuff(CallbackInfo info) {
        if (!((Object) this instanceof MushroomCreeperEntity mushroom) || !(this.level() instanceof ServerLevel level)) return;
        this.swell = 0;
        // Lit, it would swell and puff again at once, for ever: it puffs out the flame with the spores
        this.getEntityData().set(DATA_IS_IGNITED, false);
        mushroom.puff(level);
        info.cancel();
    }
}
