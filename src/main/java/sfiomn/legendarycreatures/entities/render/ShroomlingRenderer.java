package sfiomn.legendarycreatures.entities.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import sfiomn.legendarycreatures.entities.ShroomlingEntity;
import sfiomn.legendarycreatures.entities.render.model.ShroomlingModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;

public class ShroomlingRenderer extends GeoEntityRenderer<ShroomlingEntity> {
    public ShroomlingRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ShroomlingModel());
        this.withScale(0.9f);
        this.shadowRadius = 0.36f;
    }

    @Override
    public RenderType getRenderType(ShroomlingEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
