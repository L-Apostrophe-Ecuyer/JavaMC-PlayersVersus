package frootloops.versus.mod.mobs.melee;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Clients animate mob swings from the server's {@link MobMeleePayload}s: a regular wind-up draws the main arm back and
 * turns that shoulder away, a heavy one raises both arms overhead; the strike swings the arm through (the vanilla swing
 * plays with it), and a miss carries the mob forward, head down, before it straightens. The brows layer shows meanwhile
 * ({@link AngryBrowsLayer}). Rotations are in radians; an arm hanging down is at 0 and negative points it forward.
 */
@Environment(EnvType.CLIENT)
public final class MeleeAnimation {
    /** How far back a regular swing draws the main arm, and how far the body turns that shoulder away. */
    static final float DRAWN_BACK_ARM = 0.6F, DRAWN_BACK_TWIST = 0.45F;
    /** A heavy swing raises both arms overhead, spread a little, the body leaning back. */
    static final float HEAVY_ARMS = -3.0F, HEAVY_SPREAD = 0.25F, HEAVY_LEAN = -0.15F;
    /** Illagers raise their weapon overhead for a regular swing. */
    static final float ILLAGER_RAISED_ARM = -3.0F;
    /** A miss's lunge at its fullest: the body tips forward as far as this much of a crouch, the head tilts down. */
    static final float LUNGE_CROUCH = 0.7F, LUNGE_HEAD = 0.35F;

    private MeleeAnimation() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MobMeleePayload.TYPE, (payload, context) -> {
            ClientLevel level = context.client().level;
            if (level != null && level.getEntity(payload.entityId()) instanceof Mob mob) {
                MeleeState.of(mob).onEvent(payload.action(), payload.ticks(), mob.tickCount);
            }
        });
        AngryBrowsLayer.register();
    }

    /** Fills a render state with how the mob's swing looks this frame. */
    public static void extract(LivingEntity entity, LivingEntityRenderState state, float partialTick) {
        MeleeState melee = entity instanceof Mob mob ? MeleeState.peek(mob) : null;
        ((MeleeRenderState) state).playersVersus$setMeleeLook(melee == null ? MeleeState.Look.NONE : melee.look(entity.tickCount + partialTick));
    }

    public static MeleeState.Look look(LivingEntityRenderState state) {
        return ((MeleeRenderState) state).playersVersus$meleeLook();
    }

    /** Poses a humanoid (zombies, skeletons, piglins, endermen...) after its own animation. */
    public static void poseHumanoid(HumanoidModel<?> model, HumanoidRenderState state) {
        MeleeState.Look look = look(state);
        float windUp = look.windUp();
        if (windUp > 0.0F) {
            if (look.heavy()) {
                model.rightArm.xRot = Mth.lerp(windUp, model.rightArm.xRot, HEAVY_ARMS);
                model.leftArm.xRot = Mth.lerp(windUp, model.leftArm.xRot, HEAVY_ARMS);
                model.rightArm.zRot -= HEAVY_SPREAD * windUp;
                model.leftArm.zRot += HEAVY_SPREAD * windUp;
                model.body.xRot += HEAVY_LEAN * windUp;
            } else {
                float twist = (state.mainArm == HumanoidArm.RIGHT ? 1.0F : -1.0F) * DRAWN_BACK_TWIST * windUp;
                model.body.yRot += twist;
                turnShoulder(model.rightArm, twist);
                turnShoulder(model.leftArm, twist);
                ModelPart arm = model.getArm(state.mainArm);
                arm.xRot = Mth.lerp(windUp, arm.xRot, DRAWN_BACK_ARM);
            }
        }
        float lunge = look.lunge();
        if (lunge > 0.0F) {
            // Part of a crouch, as HumanoidModel crouches, with the head dipping further.
            float crouch = lunge * LUNGE_CROUCH * (state.isBaby ? 0.5F : 1.0F);
            model.body.xRot += 0.5F * crouch;
            model.body.y += 3.2F * crouch;
            model.head.y += 4.2F * crouch;
            model.head.xRot += LUNGE_HEAD * lunge;
            model.rightArm.y += 3.2F * crouch;
            model.leftArm.y += 3.2F * crouch;
            model.rightArm.xRot += 0.4F * crouch;
            model.leftArm.xRot += 0.4F * crouch;
            model.rightLeg.z += 4.0F * crouch;
            model.leftLeg.z += 4.0F * crouch;
        }
    }

    /** Poses an illager's arms and head after its own animation; its body isn't a part of its own. */
    public static void poseIllager(ModelPart head, ModelPart rightArm, ModelPart leftArm, IllagerRenderState state) {
        MeleeState.Look look = look(state);
        float windUp = look.windUp();
        if (windUp > 0.0F) {
            if (look.heavy()) {
                rightArm.xRot = Mth.lerp(windUp, rightArm.xRot, HEAVY_ARMS);
                leftArm.xRot = Mth.lerp(windUp, leftArm.xRot, HEAVY_ARMS);
                rightArm.zRot -= HEAVY_SPREAD * windUp;
                leftArm.zRot += HEAVY_SPREAD * windUp;
            } else {
                ModelPart arm = state.mainArm == HumanoidArm.RIGHT ? rightArm : leftArm;
                arm.xRot = Mth.lerp(windUp, arm.xRot, ILLAGER_RAISED_ARM);
            }
        }
        float lunge = look.lunge();
        if (lunge > 0.0F) {
            head.xRot += LUNGE_HEAD * lunge;
            rightArm.xRot += 0.4F * LUNGE_CROUCH * lunge;
            leftArm.xRot += 0.4F * LUNGE_CROUCH * lunge;
        }
    }

    /** Turns an arm's shoulder about the body's axis with the body, as HumanoidModel does for its own swing. */
    private static void turnShoulder(ModelPart arm, float angle) {
        float cos = Mth.cos(angle), sin = Mth.sin(angle);
        float x = arm.x, z = arm.z;
        arm.x = x * cos + z * sin;
        arm.z = -x * sin + z * cos;
        arm.yRot += angle;
    }
}
