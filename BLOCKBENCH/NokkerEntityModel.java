// Made with Blockbench 4.10.4
// Exported for Minecraft version 1.17+ for Yarn
// Paste this class into your mod and generate all required imports

package com.example.mod;
   
public class NokkerEntityModel extends EntityModel<Entity> {
	private final ModelPart sheep_form;
	private final ModelPart sheep_head;
	private final ModelPart sheep_body;
	private final ModelPart hind_legs_sheep;
	private final ModelPart front_legs_sheep;
	private final ModelPart true_form;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart hind_legs;
	private final ModelPart front_legs;
	public NokkerEntityModel(ModelPart root) {
		this.sheep_form = root.getChild("sheep_form");
		this.sheep_head = root.getChild("sheep_head");
		this.sheep_body = root.getChild("sheep_body");
		this.hind_legs_sheep = root.getChild("hind_legs_sheep");
		this.front_legs_sheep = root.getChild("front_legs_sheep");
		this.true_form = root.getChild("true_form");
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.hind_legs = root.getChild("hind_legs");
		this.front_legs = root.getChild("front_legs");
	}
	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData sheep_form = modelPartData.addChild("sheep_form", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

		ModelPartData sheep_head = sheep_form.addChild("sheep_head", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -24.0F, -17.0F, 6.0F, 6.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		ModelPartData sheep_body = sheep_form.addChild("sheep_body", ModelPartBuilder.create().uv(16, 8).cuboid(-1.0F, -6.0F, -20.0F, 9.0F, 6.0F, 15.0F, new Dilation(0.0F)), ModelTransform.pivot(-3.0F, -15.0F, 10.0F));

		ModelPartData hind_legs_sheep = sheep_form.addChild("hind_legs_sheep", ModelPartBuilder.create().uv(-6, -3).cuboid(-8.0F, -15.0F, -10.0F, 5.0F, 15.0F, 5.0F, new Dilation(0.0F))
		.uv(-5, -3).cuboid(1.0F, -16.0F, -10.0F, 5.0F, 16.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		ModelPartData front_legs_sheep = sheep_form.addChild("front_legs_sheep", ModelPartBuilder.create().uv(-6, -3).cuboid(-8.0F, -15.0F, 6.0F, 5.0F, 15.0F, 5.0F, new Dilation(0.0F))
		.uv(-5, -3).cuboid(1.0F, -15.0F, 6.0F, 5.0F, 15.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		ModelPartData true_form = modelPartData.addChild("true_form", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

		ModelPartData head = true_form.addChild("head", ModelPartBuilder.create().uv(-2, -2).cuboid(-5.0F, -23.0F, 11.0F, 8.0F, 8.0F, 10.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		ModelPartData body = true_form.addChild("body", ModelPartBuilder.create(), ModelTransform.pivot(-3.0F, -15.0F, 10.0F));

		ModelPartData body_r1 = body.addChild("body_r1", ModelPartBuilder.create().uv(4, 13).cuboid(2.0F, -7.0F, -21.0F, 8.0F, 6.0F, 24.0F, new Dilation(0.0F)), ModelTransform.of(-2.0F, 1.0F, -1.0F, 0.0873F, 0.0F, -0.1745F));

		ModelPartData hind_legs = true_form.addChild("hind_legs", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		ModelPartData hind_leg_right_r1 = hind_legs.addChild("hind_leg_right_r1", ModelPartBuilder.create().uv(-5, -3).cuboid(-3.0F, -22.0F, -2.0F, 5.0F, 22.0F, 5.0F, new Dilation(0.0F)), ModelTransform.of(8.0F, 0.0F, -13.0F, 0.0F, 0.0F, -0.0873F));

		ModelPartData hind_leg_left_r1 = hind_legs.addChild("hind_leg_left_r1", ModelPartBuilder.create().uv(-6, -3).cuboid(-4.0F, -19.0F, -2.0F, 5.0F, 19.0F, 5.0F, new Dilation(0.0F)), ModelTransform.of(-6.0F, 0.0F, -15.0F, -0.1745F, 0.0F, 0.1309F));

		ModelPartData front_legs = true_form.addChild("front_legs", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		ModelPartData front_leg_right_r1 = front_legs.addChild("front_leg_right_r1", ModelPartBuilder.create().uv(-5, -3).cuboid(-3.0F, -26.0F, -2.0F, 5.0F, 26.0F, 5.0F, new Dilation(0.0F)), ModelTransform.of(8.0F, 0.0F, 9.0F, 0.1745F, 0.0F, -0.0873F));

		ModelPartData front_leg_left_r1 = front_legs.addChild("front_leg_left_r1", ModelPartBuilder.create().uv(-6, -3).cuboid(-4.0F, -23.0F, -2.0F, 5.0F, 23.0F, 5.0F, new Dilation(0.0F)), ModelTransform.of(-6.0F, 0.0F, 9.0F, 0.0873F, 0.0F, 0.0873F));
		return TexturedModelData.of(modelData, 64, 32);
	}
	@Override
	public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
		sheep_form.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		true_form.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
	}
}