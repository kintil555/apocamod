package com.apocalypsemod.client;

import com.apocalypsemod.entity.DoppelgangerEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DoppelgangerEntityRenderer extends BipedEntityRenderer<DoppelgangerEntity, PlayerEntityModel<DoppelgangerEntity>> {

    // Use a creepy red-tinted skin texture as the doppelganger
    private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/entity/zombie/zombie.png");

    public DoppelgangerEntityRenderer(EntityRendererFactory.Context context) {
        super(context,
                new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false),
                0.5f);
    }

    @Override
    public Identifier getTexture(DoppelgangerEntity entity) {
        // Use zombie skin as placeholder — gives a creepy "corrupted player" look
        return TEXTURE;
    }

    @Override
    public void render(DoppelgangerEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Render slightly larger to be more intimidating
        matrices.push();
        matrices.scale(1.0f, 1.05f, 1.0f);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }
}
