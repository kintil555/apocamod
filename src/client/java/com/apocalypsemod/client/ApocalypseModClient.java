package com.apocalypsemod.client;

import com.apocalypsemod.ApocalypseMod;
import com.apocalypsemod.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.PlayerEntityRenderer;

@Environment(EnvType.CLIENT)
public class ApocalypseModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ApocalypseMod.LOGGER.info("[ApocalypseMod] Client dimuat.");

        // Register renderer for DoppelgangerEntity — use the vanilla player renderer (slim=false)
        EntityRendererRegistry.register(ModEntities.DOPPELGANGER, DoppelgangerEntityRenderer::new);
    }
}
