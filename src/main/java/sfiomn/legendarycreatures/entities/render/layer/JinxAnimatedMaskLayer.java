package sfiomn.legendarycreatures.entities.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import sfiomn.legendarycreatures.LegendaryCreatures;
import sfiomn.legendarycreatures.entities.JinxEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AnimatableTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Draws the .mcmeta-animated mask texture as a second pass on top of the base jinx.png.
 * The base texture and this overlay render at the same time, on the same geometry.
 */
public class JinxAnimatedMaskLayer extends GeoRenderLayer<JinxEntity> {

    private static final ResourceLocation MASK_TEXTURE =
            new ResourceLocation(LegendaryCreatures.MOD_ID, "textures/entity/jinx_mask_idle.png");

    public JinxAnimatedMaskLayer(GeoRenderer<JinxEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, JinxEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {

        // GeckoLib only auto-ticks the *base* texture's .mcmeta frames. A layer texture stays frozen
        // on frame 0 unless we advance it ourselves. getId() offsets each entity so they don't all
        // animate in lockstep; tickCount drives the frame forward over time.
        AnimatableTexture.setAndUpdate(MASK_TEXTURE, animatable.getId() + animatable.tickCount);

        // entityCutoutNoCull matches how the base model renders and safely handles the flat mask plane.
        // Switch to RenderType.entityTranslucent(MASK_TEXTURE) if your mask needs soft/partial alpha.
        RenderType maskRenderType = RenderType.entityCutoutNoCull(MASK_TEXTURE);

        this.getRenderer().reRender(getDefaultBakedModel(animatable), poseStack, bufferSource, animatable,
                maskRenderType, bufferSource.getBuffer(maskRenderType), partialTick,
                packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
