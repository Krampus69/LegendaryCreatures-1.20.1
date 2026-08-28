package sfiomn.legendarycreatures.entities.render.model;

import net.minecraft.resources.ResourceLocation;
import sfiomn.legendarycreatures.LegendaryCreatures;
import sfiomn.legendarycreatures.entities.JinxEntity;
import software.bernie.geckolib.model.GeoModel;

public class JinxModel extends GeoModel<JinxEntity> {
    private final ResourceLocation model = new ResourceLocation(LegendaryCreatures.MOD_ID, "geo/jinx.geo.json");
    private final ResourceLocation texture = new ResourceLocation(LegendaryCreatures.MOD_ID, "textures/entity/jinx.png");
    private final ResourceLocation animations = new ResourceLocation(LegendaryCreatures.MOD_ID, "animations/jinx.animation.json");

    @Override
    public ResourceLocation getModelResource(JinxEntity object) {
        return this.model;
    }

    @Override
    public ResourceLocation getTextureResource(JinxEntity object) {
        return this.texture;
    }

    @Override
    public ResourceLocation getAnimationResource(JinxEntity animatable) {
        return this.animations;
    }

    // No setCustomAnimations override: the "head" bone is driven purely by the animation files.
    // (GeckoLib does not auto-rotate a bone just because it is called "head".)
}
