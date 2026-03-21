package com.apocalypsemod.client;

import com.apocalypsemod.entity.DoppelgangerEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class DoppelgangerModel extends GeoModel<DoppelgangerEntity> {

    @Override
    public Identifier getModelResource(DoppelgangerEntity entity) {
        return Identifier.of("apocalypsemod", "geo/doppelganger.geo.json");
    }

    @Override
    public Identifier getTextureResource(DoppelgangerEntity entity) {
        return Identifier.of("apocalypsemod", "textures/entity/doppelganger.png");
    }

    @Override
    public Identifier getAnimationResource(DoppelgangerEntity entity) {
        return Identifier.of("apocalypsemod", "animations/doppelganger.animation.json");
    }
}
