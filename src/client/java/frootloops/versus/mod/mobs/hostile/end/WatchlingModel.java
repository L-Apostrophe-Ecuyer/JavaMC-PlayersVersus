package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.mod.mobs.melee.MeleeAnimation;
import frootloops.versus.mod.mobs.melee.MeleeState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import static frootloops.versus.mod.mobs.hostile.end.WatchlingAnimation.*;

/**
 * The watchling's model: a cube head on a long body, thick arms hanging from its shoulders nearly to the ground, and
 * thin legs, as tall as an enderman. Its eyes are on its skin (tools/watchling-skin.py: four down the body's front, more
 * about the head and back), glowing. The texture is 64 x 64: head 8 x 8 x 8 from texel (0, 0); jaw 8 x 4 x 8 from
 * (32, 0), inside the head; body 8 x 20 x 4 from (0, 16); arms 4 x 40 x 4 from (24, 16); legs 2 x 22 x 2 from (40, 16).
 *
 * <p>It moves as WatchlingAnimation says: a humanoid's walk swung less, its maw opening as it fights (the head lifts off
 * the jaw, which stays), its head cocking as it haunts, and its own two-armed strike instead of a humanoid's one-armed
 * swing.
 */
@Environment(EnvType.CLIENT)
public class WatchlingModel extends HumanoidModel<WatchlingRenderState> {
    /** Model space puts the ground at 24: the legs stand on it, the body on them, the head on the body. */
    private static final float GROUND = 24.0F, HIPS = GROUND - LEG_LENGTH, NECK = HIPS - BODY_HEIGHT;

    private final ModelPart jaw;

    public WatchlingModel(ModelPart root) {
        super(root);
        this.jaw = this.head.getChild("jaw");
    }

    /** Its parts, named as a humanoid's; baked by the renderer. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartPose neck = PartPose.offset(0.0F, NECK, 0.0F);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -HEAD_HEIGHT, -4.0F, 8.0F, HEAD_HEIGHT, 8.0F), neck);
        // A humanoid's hat layer, empty: the jaw is the watchling's
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), neck);
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -JAW_HEIGHT, -4.0F, 8.0F, JAW_HEIGHT, 8.0F, new CubeDeformation(-0.5F)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, BODY_HEIGHT, 4.0F), neck);
        // The arms flush against the body's sides (the body is 8 across, the arms 4), from the shoulders
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 16).addBox(-2.0F, -2.0F, -2.0F, 4.0F, ARM_LENGTH, 4.0F), PartPose.offset(-6.0F, NECK + 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 16).mirror().addBox(-2.0F, -2.0F, -2.0F, 4.0F, ARM_LENGTH, 4.0F), PartPose.offset(6.0F, NECK + 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, LEG_LENGTH, 2.0F), PartPose.offset(-2.0F, HIPS, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, LEG_LENGTH, 2.0F), PartPose.offset(2.0F, HIPS, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WatchlingRenderState state) {
        // Its long limbs swing less than a humanoid's, and its strike is its own (below): the humanoid's walk and swing
        // are worked out with those, then the state is put back as it was
        float walkSpeed = state.walkAnimationSpeed, swing = state.swingAnimation;
        state.walkAnimationSpeed = walkSpeed * WALK_SWING;
        state.swingAnimation = 0.0F;
        super.setupAnim(state);
        state.walkAnimationSpeed = walkSpeed;
        state.swingAnimation = swing;
        this.rightLeg.xRot = Math.clamp(this.rightLeg.xRot, -LEG_LIMIT, LEG_LIMIT);
        this.leftLeg.xRot = Math.clamp(this.leftLeg.xRot, -LEG_LIMIT, LEG_LIMIT);

        // Fighting, its maw opens: the head lifts off the jaw, which stays where it was. Its arms reach forward, unless
        // they're busy winding up a blow (MeleeAnimation's poses, already on them)
        float open = smooth(state.jawOpen);
        this.head.y -= JAW_LIFT * open;
        this.jaw.y += JAW_LIFT * open;
        MeleeState.Look look = MeleeAnimation.look(state);
        float posing = Math.max(Math.max(look.drawBack(), look.crouch()), Math.max(look.raise(), look.lunge()));
        float reach = FIGHT_REACH * open * (1.0F - Math.min(1.0F, posing));
        this.rightArm.xRot -= reach;
        this.leftArm.xRot -= reach;

        // Haunting, its head cocks to one side as it watches, swaying a little
        float tilt = smooth(state.headTilt);
        this.head.zRot += tilt * (HEAD_TILT + HEAD_TILT_SWAY * Mth.sin(state.ageInTicks * 0.05F));

        // A strike: both arms sweep forward, the main one all the way and the other most of it, and the head lunges
        float strike = WatchlingAnimation.strike(swing);
        if (strike > 0.0F) {
            this.getArm(state.mainArm).xRot -= STRIKE_ARMS * strike;
            this.getArm(state.mainArm.getOpposite()).xRot -= STRIKE_ARMS * OFF_ARM * strike;
            this.head.xRot += STRIKE_HEAD * strike;
        }
    }
}
