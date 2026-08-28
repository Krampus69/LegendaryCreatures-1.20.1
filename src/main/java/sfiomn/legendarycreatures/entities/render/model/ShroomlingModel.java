package sfiomn.legendarycreatures.entities.render.model;

import net.minecraft.resources.ResourceLocation;
import sfiomn.legendarycreatures.LegendaryCreatures;
import sfiomn.legendarycreatures.entities.ShroomlingEntity;
import software.bernie.geckolib.model.GeoModel;

public class ShroomlingModel extends GeoModel<ShroomlingEntity> {
    private final ResourceLocation model = new ResourceLocation(LegendaryCreatures.MOD_ID, "geo/shroomling.geo.json");
    private final ResourceLocation texture = new ResourceLocation(LegendaryCreatures.MOD_ID, "textures/entity/shroomling.png");
    private final ResourceLocation animations = new ResourceLocation(LegendaryCreatures.MOD_ID, "animations/shroomling.animation.json");

    @Override
    public ResourceLocation getModelResource(ShroomlingEntity object) {
        return this.model;
    }

    @Override
    public ResourceLocation getTextureResource(ShroomlingEntity entity) {
        return this.texture;
    }

    @Override
    public ResourceLocation getAnimationResource(ShroomlingEntity animatable) {
        return this.animations;
    }
}
