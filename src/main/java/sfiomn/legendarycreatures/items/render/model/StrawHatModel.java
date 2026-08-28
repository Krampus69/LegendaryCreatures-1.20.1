package sfiomn.legendarycreatures.items.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import sfiomn.legendarycreatures.LegendaryCreatures;

public class StrawHatModel<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(LegendaryCreatures.MOD_ID, "straw_hat"), "main");
	private final ModelPart bipedHead;

	public StrawHatModel(ModelPart root) {
		this.bipedHead = root.getChild("bipedHead");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bipedHead = partdefinition.addOrReplaceChild("bipedHead", CubeListBuilder.create()
		.texOffs(0, 1).addBox(-4.5F, -8.6F, -4.5F, 9.0F, 4.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(-10, 34).addBox(-5.5F, -4.6F, -5.5F, 12.0F, 0.0F, 12.0F, new CubeDeformation(0.0F)),
		PartPose.offset(0.0F, 0.4F, 0.0F));

		PartDefinition hat_r1 = bipedHead.addOrReplaceChild("hat_r1", CubeListBuilder.create().texOffs(-14, 18).addBox(-7.7956F, -1.5529F, -8.0F, 2.0F, 0.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -4.6F, 0.5F, 1.5708F, -1.309F, -1.5708F));

		PartDefinition hat_r2 = bipedHead.addOrReplaceChild("hat_r2", CubeListBuilder.create().texOffs(-14, 18).addBox(-7.7956F, -1.5529F, -8.0F, 2.0F, 0.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -4.6F, 0.5F, -3.1416F, 0.0F, -2.8798F));

		PartDefinition hat_r3 = bipedHead.addOrReplaceChild("hat_r3", CubeListBuilder.create().texOffs(-13, 19).addBox(-7.7956F, -1.5529F, -8.0F, 2.0F, 0.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -4.6F, -0.5F, -1.5708F, 1.309F, -1.5708F));

		PartDefinition hat_r4 = bipedHead.addOrReplaceChild("hat_r4", CubeListBuilder.create().texOffs(-14, 18).addBox(-2.0F, 0.0F, -8.0F, 2.0F, 0.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.5F, -4.6F, 0.5F, 0.0F, 0.0F, -0.2618F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bipedHead.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public StrawHatModel<?> copyHead(ModelPart model) {
		bipedHead.copyFrom(model);
		return this;
	}
}