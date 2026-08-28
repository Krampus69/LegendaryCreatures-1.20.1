package sfiomn.legendarycreatures.entities.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import sfiomn.legendarycreatures.entities.FrogmanEntity;
import sfiomn.legendarycreatures.entities.render.model.FrogmanModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;

public class FrogmanRenderer extends GeoEntityRenderer<FrogmanEntity> {
    public FrogmanRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new FrogmanModel());
        this.withScale(0.75f);
        this.shadowRadius = 0.45f;
    }

    @Override
    public RenderType getRenderType(FrogmanEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
