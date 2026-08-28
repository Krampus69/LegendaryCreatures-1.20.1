package sfiomn.legendarycreatures.entities.render;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import sfiomn.legendarycreatures.LegendaryCreatures;
import sfiomn.legendarycreatures.entities.JinxDartEntity;

public class JinxDartRenderer extends ArrowRenderer<JinxDartEntity> {

    // Vanilla arrow model with the mod's own dart texture.
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(LegendaryCreatures.MOD_ID, "textures/entity/jinx_dart.png");

    public JinxDartRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(JinxDartEntity entity) {
        return TEXTURE;
    }
}
