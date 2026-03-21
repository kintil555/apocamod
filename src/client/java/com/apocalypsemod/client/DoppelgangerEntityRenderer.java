package com.apocalypsemod.client;

import com.apocalypsemod.entity.DoppelgangerEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Environment(EnvType.CLIENT)
public class DoppelgangerEntityRenderer extends GeoEntityRenderer<DoppelgangerEntity> {

    public DoppelgangerEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new DoppelgangerModel());
        this.shadowRadius = 0.4f;
    }
}
