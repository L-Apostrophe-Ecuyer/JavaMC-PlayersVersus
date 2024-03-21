/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package frootloops.versus.backported.entities.wind_charge;

import frootloops.versus.backported.entities.FutureEntitiesClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(value=EnvType.CLIENT)
public class WindChargeEntityRenderer
extends EntityRenderer<WindChargeEntity> {
    private static final Identifier TEXTURE = new Identifier("textures/entity/projectiles/wind_charge.png");
    private final WindChargeEntityModel model;

    public WindChargeEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new WindChargeEntityModel(context.getPart(FutureEntitiesClient.WIND_CHARGE_MODEL_LAYER));
    }

    @Override
    public void render(WindChargeEntity windChargeEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        float h = (float)windChargeEntity.age + g;
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEnergySwirl(TEXTURE, this.getXOffset(h) % 1.0f, 0.0f));
        this.model.setAngles(windChargeEntity, 0.0f, 0.0f, h, 0.0f, 0.0f);
        this.model.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 0.5f);
        super.render(windChargeEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }

    protected float getXOffset(float tickDelta) {
        return tickDelta * 0.03f;
    }

    @Override
    public Identifier getTexture(WindChargeEntity windChargeEntity) {
        return TEXTURE;
    }
}

