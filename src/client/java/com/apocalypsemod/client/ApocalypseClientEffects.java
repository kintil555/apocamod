package com.apocalypsemod.client;

import com.apocalypsemod.ApocalypseMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class ApocalypseClientEffects {

    private static final Random RANDOM = Random.create();
    private static int ambientTick = 0;

    public static void onClientTick(MinecraftClient client) {
        if (!ApocalypseMod.apocalypseTriggered || client.player == null) return;

        float level = ApocalypseMod.apocalypseLevel;
        ambientTick++;

        // ── Camera Shake ──────────────────────────────────────────────────────
        if (level >= 10f) {
            float intensity = (level / 100f) * 3.5f;
            float dy = (RANDOM.nextFloat() - 0.5f) * intensity;
            float dp = (RANDOM.nextFloat() - 0.5f) * intensity;
            client.player.setYaw(client.player.getYaw() + dy);
            client.player.setPitch(client.player.getPitch() + dp);
        }

        // ── Fast Day/Night cycle ─────────────────────────────────────────────
        if (level >= 25f && client.world != null) {
            int skip = (int)(level / 25f);
            if (ambientTick % 2 == 0) {
                client.world.setTime(client.world.getTimeOfDay() + skip * 10L);
            }
        }

        // ── Ambient Sounds ───────────────────────────────────────────────────
        // Interval turun seiring level naik: dari tiap 200 tick → tiap 20 tick
        int interval = Math.max(20, 200 - (int)(level * 1.8f));

        if (ambientTick % interval == 0 && client.getSoundManager() != null) {
            playAmbientSound(client, level);
        }
    }

    private static void playAmbientSound(MinecraftClient client, float level) {
        // Pilih suara berdasarkan level kiamat
        SoundInstance sound;

        if (level < 25f) {
            // Phase 1: suara angin & zombie jauh
            sound = switch (RANDOM.nextInt(3)) {
                case 0 -> PositionedSoundInstance.ambient(
                        SoundEvents.AMBIENT_CAVE.value(), 0.4f, 0.8f + RANDOM.nextFloat() * 0.4f);
                case 1 -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_ZOMBIE_AMBIENT, 0.2f, 0.5f + RANDOM.nextFloat() * 0.3f);
                default -> PositionedSoundInstance.ambient(
                        SoundEvents.BLOCK_PORTAL_AMBIENT, 0.15f, 0.5f);
            };
        } else if (level < 50f) {
            // Phase 2: suara lebih keras, wither & ghast
            sound = switch (RANDOM.nextInt(4)) {
                case 0 -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_GHAST_AMBIENT, 0.5f, 0.6f + RANDOM.nextFloat() * 0.3f);
                case 1 -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_WITHER_AMBIENT, 0.4f, 0.7f);
                case 2 -> PositionedSoundInstance.ambient(
                        SoundEvents.AMBIENT_CAVE.value(), 0.7f, 0.6f);
                default -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_ZOMBIE_HURT, 0.3f, 0.5f);
            };
        } else if (level < 75f) {
            // Phase 3: suara chaos, ledakan & screaming
            sound = switch (RANDOM.nextInt(4)) {
                case 0 -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_WITHER_SHOOT, 0.6f, 0.5f + RANDOM.nextFloat() * 0.3f);
                case 1 -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_GHAST_SCREAM, 0.7f, 0.6f);
                case 2 -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.7f);
                default -> PositionedSoundInstance.ambient(
                        SoundEvents.BLOCK_PORTAL_TRIGGER, 0.4f, 0.5f);
            };
        } else {
            // Phase 4-5: TOTAL CHAOS - dragon, wither, ledakan terus menerus
            sound = switch (RANDOM.nextInt(5)) {
                case 0 -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.9f, 0.6f + RANDOM.nextFloat() * 0.3f);
                case 1 -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_WITHER_DEATH, 0.8f, 0.5f);
                case 2 -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.7f, 0.6f);
                case 3 -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_ENDER_DRAGON_HURT, 0.8f, 0.7f);
                default -> PositionedSoundInstance.ambient(
                        SoundEvents.ENTITY_WITHER_AMBIENT, 1.0f, 0.4f);
            };
        }

        client.getSoundManager().play(sound);
    }

    public static void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!ApocalypseMod.apocalypseTriggered) return;

        float level = ApocalypseMod.apocalypseLevel;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int screenW = drawContext.getScaledWindowWidth();
        int screenH = drawContext.getScaledWindowHeight();

        // ── Red vignette overlay ──────────────────────────────────────────────
        if (level >= 25f) {
            int alpha = (int) Math.min(140, ((level - 25f) / 75f) * 140f);
            if (level >= 75f) {
                double pulse = Math.sin(System.currentTimeMillis() / 300.0);
                alpha = (int) Math.min(180, alpha + pulse * 30);
            }
            int color = (alpha << 24) | 0x8B0000;
            drawContext.fill(0, 0, screenW, screenH, color);
        }

        // ── Flashing KIAMAT text at level 100 ────────────────────────────────
        if (level >= 100f) {
            long time = System.currentTimeMillis();
            if ((time / 500) % 2 == 0) {
                String text = "\u2620 KIAMAT TOTAL \u2620";
                int textW = client.textRenderer.getWidth(text);
                drawContext.drawTextWithShadow(
                        client.textRenderer, text,
                        (screenW - textW) / 2, screenH / 2 - 20,
                        0xFF0000
                );
            }
        }
    }
}
