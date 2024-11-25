package frootloops.versus.mixin.client.mobs.passive;


import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PigEntityModel.class)
public abstract class PigModelMixin extends QuadrupedEntityModel<LivingEntityRenderState> {

    protected PigModelMixin(ModelPart root) {
        super(root);
    }

    @Override
    public void setAngles(LivingEntityRenderState renderState) {
        super.setAngles(renderState);
        this.head.pivotY = this.head.pivotY + this.head.pitch * 9.0F * renderState.ageScale;
    }
}
