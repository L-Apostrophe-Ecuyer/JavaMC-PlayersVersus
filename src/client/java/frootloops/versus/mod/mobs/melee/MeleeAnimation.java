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
 * turns that shoulder away, and the strike swings it through (the vanilla swing plays with it). A heavy swing is a leap:
 * the mob crouches with both arms swung back, raises them overhead in the air, and brings them down with the strike. A
 * miss carries the mob forward, head down, before it straightens. The brows layer shows meanwhile
 * ({@link AngryBrowsLayer}). Rotations are in radians; an arm hanging down is at 0 and negative points it forward.
 */
@Environment(EnvType.CLIENT)
public final class MeleeAnimation {
    /** How far back a regular swing draws the main arm, and how far the body turns that shoulder away. */
    static final float DRAWN_BACK_ARM = 0.6F, DRAWN_BACK_TWIST = 0.45F;
    /** A leap's crouch swings both arms back. */
    static final float CROUCH_ARMS = 0.9F;
    /** In the air, both arms go up overhead, spread a little, the body leaning back. */
    static final float RAISED_ARMS = -3.0F, RAISED_SPREAD = 0.25F, RAISED_LEAN = -0.15F;
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
        float drawBack = look.drawBack();
        if (drawBack > 0.0F) {
            float twist = (state.mainArm == HumanoidArm.RIGHT ? 1.0F : -1.0F) * DRAWN_BACK_TWIST * drawBack;
            model.body.yRot += twist;
            turnShoulder(model.rightArm, twist);
            turnShoulder(model.leftArm, twist);
            ModelPart arm = model.getArm(state.mainArm);
            arm.xRot = Mth.lerp(drawBack, arm.xRot, DRAWN_BACK_ARM);
        }
        float crouch = look.crouch();
        if (crouch > 0.0F) {
            crouch(model, crouch, state.isBaby);
            model.rightArm.xRot = Mth.lerp(crouch, model.rightArm.xRot, CROUCH_ARMS);
            model.leftArm.xRot = Mth.lerp(crouch, model.leftArm.xRot, CROUCH_ARMS);
        }
        float raise = look.raise();
        if (raise > 0.0F) {
            model.rightArm.xRot = Mth.lerp(raise, model.rightArm.xRot, RAISED_ARMS);
            model.leftArm.xRot = Mth.lerp(raise, model.leftArm.xRot, RAISED_ARMS);
            model.rightArm.zRot -= RAISED_SPREAD * raise;
            model.leftArm.zRot += RAISED_SPREAD * raise;
            model.body.xRot += RAISED_LEAN * raise;
        }
        float lunge = look.lunge();
        if (lunge > 0.0F) {
            crouch(model, lunge * LUNGE_CROUCH, state.isBaby);
            model.head.xRot += LUNGE_HEAD * lunge;
        }
    }

    /** Poses an illager's arms and head after its own animation; its body isn't a part of its own. */
    public static void poseIllager(ModelPart head, ModelPart rightArm, ModelPart leftArm, IllagerRenderState state) {
        MeleeState.Look look = look(state);
        float drawBack = look.drawBack();
        if (drawBack > 0.0F) {
            ModelPart arm = state.mainArm == HumanoidArm.RIGHT ? rightArm : leftArm;
            arm.xRot = Mth.lerp(drawBack, arm.xRot, ILLAGER_RAISED_ARM);
        }
        float crouch = look.crouch();
        if (crouch > 0.0F) {
            rightArm.xRot = Mth.lerp(crouch, rightArm.xRot, CROUCH_ARMS);
            leftArm.xRot = Mth.lerp(crouch, leftArm.xRot, CROUCH_ARMS);
        }
        float raise = look.raise();
        if (raise > 0.0F) {
            rightArm.xRot = Mth.lerp(raise, rightArm.xRot, RAISED_ARMS);
            leftArm.xRot = Mth.lerp(raise, leftArm.xRot, RAISED_ARMS);
            rightArm.zRot -= RAISED_SPREAD * raise;
            leftArm.zRot += RAISED_SPREAD * raise;
        }
        float lunge = look.lunge();
        if (lunge > 0.0F) {
            head.xRot += LUNGE_HEAD * lunge;
            rightArm.xRot += 0.4F * LUNGE_CROUCH * lunge;
            leftArm.xRot += 0.4F * LUNGE_CROUCH * lunge;
        }
    }

    /**
     * Bends a humanoid {@code amount} of the way into a crouch, as HumanoidModel crouches: the body tips forward, head
     * and arms drop with it and the legs step back (half as far on a baby's smaller parts).
     */
    private static void crouch(HumanoidModel<?> model, float amount, boolean baby) {
        float offset = amount * (baby ? 0.5F : 1.0F);
        model.body.xRot += 0.5F * amount;
        model.body.y += 3.2F * offset;
        model.head.y += 4.2F * offset;
        model.rightArm.y += 3.2F * offset;
        model.leftArm.y += 3.2F * offset;
        model.rightArm.xRot += 0.4F * amount;
        model.leftArm.xRot += 0.4F * amount;
        model.rightLeg.z += 4.0F * offset;
        model.leftLeg.z += 4.0F * offset;
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
