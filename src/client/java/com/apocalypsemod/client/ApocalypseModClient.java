package com.apocalypsemod.client;

import com.apocalypsemod.ApocalypseMod;
import com.apocalypsemod.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

@Environment(EnvType.CLIENT)
public class ApocalypseModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ApocalypseMod.LOGGER.info("[ApocalypseMod] Client dimuat.");

        EntityRendererRegistry.register(ModEntities.DOPPELGANGER, DoppelgangerEntityRenderer::new);

        // Register camera shake + fast day/night cycle tick
        ClientTickEvents.END_CLIENT_TICK.register(ApocalypseClientEffects::onClientTick);

        // Register red screen HUD overlay
        HudRenderCallback.EVENT.register(ApocalypseClientEffects::onHudRender);
    }
}
