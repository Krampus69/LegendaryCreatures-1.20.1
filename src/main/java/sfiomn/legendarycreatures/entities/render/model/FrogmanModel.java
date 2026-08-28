package sfiomn.legendarycreatures.entities.render.model;

import net.minecraft.resources.ResourceLocation;
import sfiomn.legendarycreatures.LegendaryCreatures;
import sfiomn.legendarycreatures.entities.FrogmanEntity;
import software.bernie.geckolib.model.GeoModel;

public class FrogmanModel extends GeoModel<FrogmanEntity> {
    private final ResourceLocation model = new ResourceLocation(LegendaryCreatures.MOD_ID, "geo/frogman.geo.json");
    private final ResourceLocation texture_level1 = new ResourceLocation(LegendaryCreatures.MOD_ID, "textures/entity/frogman.png");
    private final ResourceLocation texture_level2 = new ResourceLocation(LegendaryCreatures.MOD_ID, "textures/entity/frogman_level2.png");
    private final ResourceLocation texture_level3 = new ResourceLocation(LegendaryCreatures.MOD_ID, "textures/entity/frogman_level3.png");
    private final ResourceLocation animations = new ResourceLocation(LegendaryCreatures.MOD_ID, "animations/frogman.animation.json");

    @Override
    public ResourceLocation getModelResource(FrogmanEntity object) {
        return this.model;
    }

    @Override
    public ResourceLocation getTextureResource(FrogmanEntity entity) {
        if (entity.isLevel3())
            return texture_level3;
        else if (entity.isLevel2())
            return texture_level2;
        else
            return texture_level1;
    }

    @Override
    public ResourceLocation getAnimationResource(FrogmanEntity animatable) {
        return this.animations;
    }
}
