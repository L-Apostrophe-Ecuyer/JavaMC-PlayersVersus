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
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.util.Mth;

import static frootloops.versus.mod.mobs.hostile.end.WatchlingAnimation.*;

/**
 * The watchling's model: the enderman's body and legs, with a taller head (12 where the enderman's is 8) and big arms (4
 * across where the enderman's are 2), as in Minecraft Dungeons, where it is a melee mob. The texture is 64 x 64: the head
 * box is 8 x 12 x 8 from texel (0, 0); each arm is 4 x 30 x 4 from texel (0, 30); the jaw, a box inside the head as the
 * enderman's jaw layer is, is 8 x 6 x 8 from texel (16, 32) (tools/watchling-head.py draws them).
 *
 * <p>It moves as WatchlingAnimation says: a humanoid's walk swung less, its maw opening as it fights (the head lifts off
 * the jaw, which stays), its head cocking as it haunts, and its own two-armed strike instead of a humanoid's one-armed
 * swing. Not the enderman's model, so none of the enderman's clamps hold its swings back.
 */
@Environment(EnvType.CLIENT)
public class WatchlingModel extends HumanoidModel<WatchlingRenderState> {
    private final ModelPart jaw;

    public WatchlingModel(ModelPart root) {
        super(root);
        this.jaw = this.head.getChild("jaw");
    }

    /** The enderman's layer with its head, jaw and arms swapped for the watchling's; baked by the renderer. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition[] built = new MeshDefinition[1];
        EndermanModel.createBodyLayer().apply(mesh -> {
            // Where the enderman's head sits: its neck, at the top of the body
            PartPose neck = PartPose.offset(0.0F, -13.0F, 0.0F);
            PartDefinition root = mesh.getRoot();
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -HEAD_HEIGHT, -4.0F, 8.0F, HEAD_HEIGHT, 8.0F), neck);
            // The enderman's jaw layer (its "hat") is left empty wherever a humanoid looks for it; the jaw is the watchling's
            head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
            root.addOrReplaceChild("hat", CubeListBuilder.create(), neck);
            head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, -JAW_HEIGHT, -4.0F, 8.0F, JAW_HEIGHT, 8.0F, new CubeDeformation(-0.5F)), PartPose.ZERO);
            // Arms twice as thick, flush against the body's sides (the body is 8 across, the arms 4)
            root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 30).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 30.0F, 4.0F), PartPose.offset(-6.0F, -12.0F, 0.0F));
            root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 30).mirror().addBox(-2.0F, -2.0F, -2.0F, 4.0F, 30.0F, 4.0F), PartPose.offset(6.0F, -12.0F, 0.0F));
            built[0] = mesh;
            return mesh;
        });
        return LayerDefinition.create(built[0], 64, 64);
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
