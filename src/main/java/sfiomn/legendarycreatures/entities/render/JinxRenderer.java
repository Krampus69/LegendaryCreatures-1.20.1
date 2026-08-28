package sfiomn.legendarycreatures.entities.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import sfiomn.legendarycreatures.entities.JinxEntity;
import sfiomn.legendarycreatures.entities.render.model.JinxModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;

public class JinxRenderer extends GeoEntityRenderer<JinxEntity> {

    public JinxRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new JinxModel());
        this.withScale(0.8f);
        this.shadowRadius = 0.4f;
    }

    @Override
    protected void applyRotations(JinxEntity animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);

        // Wobble on hit. Done on the PoseStack (per-render) rather than on a bone: GeoModel bones are
        // SHARED between every Jinx, so nudging a bone here would make all of them shake at once.
        // vanilla sets hurtTime to 10 on each hit and ticks it down, so the shake decays by itself.
        if (animatable.hurtTime > 0) {
            float time = animatable.hurtTime - partialTick;
            float intensity = time / 10.0F;
            float wobble = Mth.sin(time * 3.0F) * intensity;

            poseStack.mulPose(Axis.ZP.rotationDegrees(wobble * 12.0F));
        }
    }

    @Override
    public RenderType getRenderType(JinxEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
