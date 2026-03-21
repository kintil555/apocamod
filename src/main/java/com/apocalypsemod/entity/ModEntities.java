package com.apocalypsemod.entity;

import com.apocalypsemod.ApocalypseMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static EntityType<DoppelgangerEntity> DOPPELGANGER;

    public static void registerEntities() {
        DOPPELGANGER = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(ApocalypseMod.MOD_ID, "doppelganger"),
                FabricEntityTypeBuilder.<DoppelgangerEntity>create(SpawnGroup.MONSTER, DoppelgangerEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                        .trackRangeChunks(10)
                        .build()
        );

        ApocalypseMod.LOGGER.info("[ApocalypseMod] Entity Doppelganger terdaftar.");
    }
}
