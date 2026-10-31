package frootloops.versus.mod.items_and_effects.conversion;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * The entities shaking before they shroomify (Shroomify), as the server says: they shake as frozen ones do, until a
 * little after the server last said so.
 */
@Environment(EnvType.CLIENT)
public final class ShroomShaking {
    /** Until when (the level's game time) each entity, by id, shakes. */
    private static final Map<Integer, Long> UNTIL = new HashMap<>();

    private ShroomShaking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ShroomShakingPayload.TYPE, (payload, context) -> {
            ClientLevel level = context.client().level;
            if (level == null) return;
            long now = level.getGameTime();
            UNTIL.values().removeIf(until -> until < now);
            UNTIL.put(payload.entityId(), now + payload.ticks());
        });
    }

    /** Fills a render state: the entity shakes if it's shroomifying. */
    public static void extract(LivingEntity entity, LivingEntityRenderState state) {
        if (UNTIL.isEmpty()) return;
        Long until = UNTIL.get(entity.getId());
        if (until != null && until >= entity.level().getGameTime()) state.isFullyFrozen = true;
    }
}
