package com.apocalypsemod;

import com.apocalypsemod.entity.DoppelgangerEntity;
import com.apocalypsemod.entity.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ApocalypseMod implements ModInitializer {

    public static final String MOD_ID = "apocalypsemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Tracks doppelganger spawn cooldown per player (ticks)
    private static final Map<UUID, Integer> spawnCooldowns = new HashMap<>();
    // Tracks how far into the apocalypse we are (0-100)
    public static float apocalypseLevel = 0f;
    public static boolean apocalypseTriggered = false;
    private static int apocalypseTick = 0;
    private static final Random RANDOM = new Random();

    // How often (in ticks) to spawn a doppelganger per player (20 ticks = 1 sec)
    private static final int SPAWN_INTERVAL_TICKS = 600; // every 30 seconds

    @Override
    public void onInitialize() {
        LOGGER.info("[ApocalypseMod] Kiamat Chaos sedang dimuat...");

        // Register entities
        ModEntities.registerEntities();

        // Register entity default attributes (required for LivingEntity subclasses)
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                ModEntities.DOPPELGANGER,
                com.apocalypsemod.entity.DoppelgangerEntity.createAttributes()
        );

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ApocalypseCommands.register(dispatcher)
        );

        // Tick event: manage doppelganger spawning + apocalypse chaos
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        // Listen for entity kill: check if player killed a doppelganger
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && entity instanceof DoppelgangerEntity doppelganger) {
                if (doppelganger.getOwnerUuid() != null &&
                        doppelganger.getOwnerUuid().equals(player.getUuid())) {
                    // Doppelganger is dying — will be checked via death logic
                    // We'll handle this in DoppelgangerEntity death
                }
            }
            return ActionResult.PASS;
        });

        LOGGER.info("[ApocalypseMod] Kiamat Chaos berhasil dimuat!");
    }

    private void onServerTick(MinecraftServer server) {
        if (apocalypseTriggered) {
            apocalypseTick++;
            runApocalypseChaos(server);
        }

        // Every SPAWN_INTERVAL_TICKS, try to spawn doppelgangers near players
        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                UUID playerId = player.getUuid();
                int cooldown = spawnCooldowns.getOrDefault(playerId, 0);

                if (cooldown <= 0) {
                    // Spawn a doppelganger near this player
                    trySpawnDoppelganger(player, world);
                    spawnCooldowns.put(playerId, SPAWN_INTERVAL_TICKS);
                } else {
                    spawnCooldowns.put(playerId, cooldown - 1);
                }
            }
        }
    }

    private void trySpawnDoppelganger(ServerPlayerEntity player, ServerWorld world) {
        // Don't spawn if there's already a doppelganger for this player nearby
        long existingCount = world.getEntitiesByType(
                ModEntities.DOPPELGANGER,
                player.getBoundingBox().expand(64),
                e -> player.getUuid().equals(e.getOwnerUuid())
        ).size();

        if (existingCount >= 2) return; // max 2 doppelgangers per player at once

        // Pick a random spot 10-30 blocks away from player
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double dist = 10 + RANDOM.nextDouble() * 20;
        double x = player.getX() + Math.cos(angle) * dist;
        double z = player.getZ() + Math.sin(angle) * dist;
        double y = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, (int) x, (int) z);

        DoppelgangerEntity doppelganger = new DoppelgangerEntity(ModEntities.DOPPELGANGER, world);
        doppelganger.setOwner(player);
        doppelganger.copyAppearanceFrom(player);
        doppelganger.refreshPositionAndAngles(x, y, z, RANDOM.nextFloat() * 360f, 0f);
        world.spawnEntity(doppelganger);

        player.sendMessage(
                Text.literal("⚠ Sebuah bayangan dirimu muncul di kejauhan...")
                        .formatted(Formatting.DARK_RED, Formatting.ITALIC),
                true
        );
    }

    /**
     * Called when a doppelganger dies at the hand of its owner.
     * Triggers apocalypse!
     */
    public static void onDoppelgangerKilledByOwner(ServerPlayerEntity killer, ServerWorld world) {
        if (apocalypseTriggered) return; // already triggered

        apocalypseTriggered = true;
        apocalypseLevel = 0f;
        apocalypseTick = 0;

        // Announce to all players
        world.getServer().getPlayerManager().broadcast(
                Text.literal("☠ " + killer.getName().getString() + " TELAH MEMBUNUH DIRINYA SENDIRI! KIAMAT DIMULAI! ☠")
                        .formatted(Formatting.DARK_RED, Formatting.BOLD),
                false
        );
        world.getServer().getPlayerManager().broadcast(
                Text.literal("⚡ Dunia mulai runtuh! Selamatkan dirimu jika masih bisa... ⚡")
                        .formatted(Formatting.RED),
                false
        );

        LOGGER.info("[ApocalypseMod] KIAMAT DIPICU oleh: {}", killer.getName().getString());
    }

    /**
     * Manually trigger apocalypse (via command, for testing).
     */
    public static void triggerApocalypseManually(MinecraftServer server) {
        if (apocalypseTriggered) {
            server.getPlayerManager().broadcast(
                    Text.literal("[ApocalypseMod] Kiamat sudah berjalan!")
                            .formatted(Formatting.YELLOW), false
            );
            return;
        }
        apocalypseTriggered = true;
        apocalypseLevel = 0f;
        apocalypseTick = 0;

        server.getPlayerManager().broadcast(
                Text.literal("☠ KIAMAT DIPICU! CHAOS MELANDA DUNIA! ☠")
                        .formatted(Formatting.DARK_RED, Formatting.BOLD),
                false
        );
    }

    public static void resetApocalypse(MinecraftServer server) {
        apocalypseTriggered = false;
        apocalypseLevel = 0f;
        apocalypseTick = 0;

        // Reset game rules to normal
        for (ServerWorld world : server.getWorlds()) {
            world.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(true, server);
            world.getGameRules().get(net.minecraft.world.GameRules.DO_WEATHER_CYCLE).set(true, server);
            world.setWeather(0, 6000, false, false); // reset weather
        }

        server.getPlayerManager().broadcast(
                Text.literal("[ApocalypseMod] Kiamat telah direset.")
                        .formatted(Formatting.GREEN), false
        );
    }

    // ==============================
    // APOCALYPSE CHAOS LOGIC
    // ==============================

    private void runApocalypseChaos(MinecraftServer server) {
        // Ramp up apocalypse level over time (fully chaotic after ~10 minutes)
        if (apocalypseLevel < 100f) {
            apocalypseLevel = Math.min(100f, apocalypseLevel + 0.0167f); // ~0.0167 per tick = 100 in 6000 ticks = 5 min
        }

        int level = (int) apocalypseLevel;

        for (ServerWorld world : server.getWorlds()) {
            applyWorldChaos(world, server, level);
        }
    }

    private void applyWorldChaos(ServerWorld world, MinecraftServer server, int level) {
        // === PHASE 1 (0-25): Thunder, eternal night, random lightning ===
        if (level >= 1) {
            // Set permanent thunderstorm
            world.setWeather(0, 6000, true, true);
            // Make it always night
            world.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
            world.setTimeOfDay(18000); // midnight

            // Random lightning strikes near players every 3 seconds
            if (apocalypseTick % 60 == 0) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (RANDOM.nextFloat() < 0.3f) {
                        double ox = (RANDOM.nextDouble() - 0.5) * 20;
                        double oz = (RANDOM.nextDouble() - 0.5) * 20;
                        BlockPos pos = new BlockPos((int)(player.getX() + ox), (int)player.getY(), (int)(player.getZ() + oz));
                        { net.minecraft.entity.LightningEntity _bolt = net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(world, net.minecraft.entity.SpawnReason.TRIGGERED); if (_bolt != null) { _bolt.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f); world.spawnEntity(_bolt); } }
                    }
                }
            }
        }

        // === PHASE 2 (25+): Explosions, mobs go berserk ===
        if (level >= 25) {
            // Random explosions near players every 5 seconds
            if (apocalypseTick % 100 == 0) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (RANDOM.nextFloat() < 0.4f) {
                        double ox = (RANDOM.nextDouble() - 0.5) * 30;
                        double oz = (RANDOM.nextDouble() - 0.5) * 30;
                        double oy = RANDOM.nextDouble() * 5;
                        world.createExplosion(null,
                                player.getX() + ox,
                                player.getY() + oy,
                                player.getZ() + oz,
                                level >= 50 ? 4f : 2f,
                                level >= 75,
                                World.ExplosionSourceType.TNT);
                    }
                }
            }

            // Make all hostile mobs attack players regardless of distance
            // Apply Speed + Strength to all hostile mobs
            if (apocalypseTick % 200 == 0) {
                world.iterateEntities().forEach(entity -> {
                    if (entity instanceof net.minecraft.entity.mob.HostileEntity hostile) {
                        hostile.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                                net.minecraft.entity.effect.StatusEffects.SPEED, 300, 2));
                        hostile.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                                net.minecraft.entity.effect.StatusEffects.STRENGTH, 300, 2));
                    }
                });
            }

            // Random fire in the sky (fireballs)
            if (apocalypseTick % 80 == 0 && level >= 50) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (RANDOM.nextFloat() < 0.3f) {
                        spawnFireball(world, player);
                    }
                }
            }
        }

        // === PHASE 3 (50+): Gravity anomalies, massive explosions, lava geysers ===
        if (level >= 50) {
            // Give players Levitation sometimes (gravity anomaly)
            if (apocalypseTick % 150 == 0) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (RANDOM.nextFloat() < 0.25f) {
                        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                                net.minecraft.entity.effect.StatusEffects.LEVITATION, 60, 0));
                        player.sendMessage(
                                Text.literal("⚠ Gravitasi mulai kacau!")
                                        .formatted(Formatting.GOLD), true
                        );
                    }
                }
            }

            // Spawn lava sources in the world (randomly beneath players)
            if (apocalypseTick % 300 == 0) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (RANDOM.nextFloat() < 0.3f) {
                        spawnLavaGeyser(world, player);
                    }
                }
            }

            // Spawn multiple doppelgangers more aggressively
            if (apocalypseTick % 200 == 0) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    trySpawnDoppelganger(player, world);
                    trySpawnDoppelganger(player, world);
                }
            }
        }

        // === PHASE 4 (75+): Near-total destruction ===
        if (level >= 75) {
            // Giant explosions every 10 seconds
            if (apocalypseTick % 200 == 0) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    double ox = (RANDOM.nextDouble() - 0.5) * 50;
                    double oz = (RANDOM.nextDouble() - 0.5) * 50;
                    world.createExplosion(null,
                            player.getX() + ox, player.getY(), player.getZ() + oz,
                            8f, true,
                            World.ExplosionSourceType.TNT);
                }
            }

            // Hurt all players periodically (world crumbling)
            if (apocalypseTick % 100 == 0) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (!player.isCreative() && !player.isSpectator()) {
                        player.damage(world.getDamageSources().magic(), 2f);
                        player.sendMessage(
                                Text.literal("💀 Dunia merobek jiwamu...")
                                        .formatted(Formatting.DARK_RED), true
                        );
                    }
                }
            }
        }

        // === PHASE 5 (100): TOTAL CHAOS - Max destruction ===
        if (level >= 100) {
            if (apocalypseTick % 40 == 0) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    // Constant random explosions, lightning, damage
                    if (RANDOM.nextFloat() < 0.5f) {
                        double ox = (RANDOM.nextDouble() - 0.5) * 40;
                        double oz = (RANDOM.nextDouble() - 0.5) * 40;
                        world.createExplosion(null,
                                player.getX() + ox, player.getY() + RANDOM.nextDouble() * 10, player.getZ() + oz,
                                6f, true,
                                World.ExplosionSourceType.TNT);
                    }
                    if (RANDOM.nextFloat() < 0.3f) {
                        BlockPos pos = player.getBlockPos().add(
                                RANDOM.nextInt(20) - 10, 0, RANDOM.nextInt(20) - 10
                        );
                        { net.minecraft.entity.LightningEntity _bolt = net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(world, net.minecraft.entity.SpawnReason.TRIGGERED); if (_bolt != null) { _bolt.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f); world.spawnEntity(_bolt); } }
                    }
                }
            }

            // Announce chaos every 30 seconds
            if (apocalypseTick % 600 == 0) {
                server.getPlayerManager().broadcast(
                        Text.literal("☠ KIAMAT TOTAL! DUNIA SUDAH HANCUR LEBUR! ☠")
                                .formatted(Formatting.DARK_RED, Formatting.BOLD),
                        false
                );
            }
        }
    }

    private void spawnFireball(ServerWorld world, ServerPlayerEntity player) {
        double x = player.getX() + (RANDOM.nextDouble() - 0.5) * 40;
        double z = player.getZ() + (RANDOM.nextDouble() - 0.5) * 40;
        double y = player.getY() + 30 + RANDOM.nextDouble() * 20;

        net.minecraft.entity.projectile.FireballEntity fireball =
                new net.minecraft.entity.projectile.FireballEntity(world, null,
                        new Vec3d(0, -1, 0), 2);
        fireball.setPosition(x, y, z);
        world.spawnEntity(fireball);
    }

    private void spawnLavaGeyser(ServerWorld world, ServerPlayerEntity player) {
        double ox = (RANDOM.nextDouble() - 0.5) * 40;
        double oz = (RANDOM.nextDouble() - 0.5) * 40;
        int x = (int)(player.getX() + ox);
        int z = (int)(player.getZ() + oz);
        int y = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);

        // Place lava blocks in a small column
        for (int i = 0; i < 5; i++) {
            BlockPos pos = new BlockPos(x, y + i, z);
            if (world.getBlockState(pos).isAir()) {
                world.setBlockState(pos, net.minecraft.block.Blocks.LAVA.getDefaultState());
            }
        }
    }
}
