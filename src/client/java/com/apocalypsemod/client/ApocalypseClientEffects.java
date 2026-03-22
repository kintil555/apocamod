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

    // Tracks red fade-in progress (0.0 -> 1.0) when level hits 100
    private static float redFadeProgress = 0f;
    private static boolean fullRedActive = false;

    public static void onClientTick(MinecraftClient client) {
        if (!ApocalypseMod.apocalypseTriggered || client.player == null) {
            redFadeProgress = 0f;
            fullRedActive = false;
            return;
        }

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

        // ── Red screen fade-in at level 100 ──────────────────────────────────
        if (level >= 100f) {
            if (redFadeProgress < 1f) {
                redFadeProgress = Math.min(1f, redFadeProgress + 0.003f); // ~6 seconds to full red
            } else {
                fullRedActive = true;
            }
        }

        // ── Ambient Sounds ───────────────────────────────────────────────────
        int interval = Math.max(20, 200 - (int)(level * 1.8f));
        if (ambientTick % interval == 0 && client.getSoundManager() != null) {
            playAmbientSound(client, level);
        }
    }

    private static void playAmbientSound(MinecraftClient client, float level) {
        SoundInstance sound;
        if (level < 25f) {
            sound = switch (RANDOM.nextInt(3)) {
                case 0 -> PositionedSoundInstance.ambient(SoundEvents.AMBIENT_CAVE.value(), 0.4f, 0.8f + RANDOM.nextFloat() * 0.4f);
                case 1 -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_ZOMBIE_AMBIENT, 0.2f, 0.5f + RANDOM.nextFloat() * 0.3f);
                default -> PositionedSoundInstance.ambient(SoundEvents.BLOCK_PORTAL_AMBIENT, 0.15f, 0.5f);
            };
        } else if (level < 50f) {
            sound = switch (RANDOM.nextInt(4)) {
                case 0 -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_GHAST_AMBIENT, 0.5f, 0.6f + RANDOM.nextFloat() * 0.3f);
                case 1 -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_WITHER_AMBIENT, 0.4f, 0.7f);
                case 2 -> PositionedSoundInstance.ambient(SoundEvents.AMBIENT_CAVE.value(), 0.7f, 0.6f);
                default -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_ZOMBIE_HURT, 0.3f, 0.5f);
            };
        } else if (level < 75f) {
            sound = switch (RANDOM.nextInt(4)) {
                case 0 -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_WITHER_SHOOT, 0.6f, 0.5f + RANDOM.nextFloat() * 0.3f);
                case 1 -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_GHAST_SCREAM, 0.7f, 0.6f);
                case 2 -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.7f);
                default -> PositionedSoundInstance.ambient(SoundEvents.BLOCK_PORTAL_TRIGGER, 0.4f, 0.5f);
            };
        } else {
            sound = switch (RANDOM.nextInt(5)) {
                case 0 -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.9f, 0.6f + RANDOM.nextFloat() * 0.3f);
                case 1 -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_WITHER_DEATH, 0.8f, 0.5f);
                case 2 -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.7f, 0.6f);
                case 3 -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_ENDER_DRAGON_HURT, 0.8f, 0.7f);
                default -> PositionedSoundInstance.ambient(SoundEvents.ENTITY_WITHER_AMBIENT, 1.0f, 0.4f);
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

        // ── Red vignette (phase 25-99) ────────────────────────────────────────
        if (level >= 25f && level < 100f) {
            int alpha = (int) Math.min(120, ((level - 25f) / 75f) * 120f);
            if (level >= 75f) {
                double pulse = Math.sin(System.currentTimeMillis() / 300.0);
                alpha = (int) Math.min(150, alpha + pulse * 30);
            }
            drawContext.fill(0, 0, screenW, screenH, (alpha << 24) | 0x8B0000);
        }

        // ── Phase 100: gradual red fade-in ────────────────────────────────────
        if (level >= 100f) {
            int alpha = (int)(redFadeProgress * 210); // max alpha 210 (slightly transparent)
            if (alpha > 0) {
                drawContext.fill(0, 0, screenW, screenH, (alpha << 24) | 0xAA0000);
            }

            // Show shaking text once red is mostly visible
            if (redFadeProgress >= 0.5f) {
                String text = "This is the end for you.";
                int textW = client.textRenderer.getWidth(text);

                // Shake amount increases with fade progress
                float shakeStrength = (redFadeProgress - 0.5f) * 2f * 6f; // 0 -> 6 pixels
                int shakeX = (int)((RANDOM.nextFloat() - 0.5f) * shakeStrength);
                int shakeY = (int)((RANDOM.nextFloat() - 0.5f) * shakeStrength);

                int x = (screenW - textW) / 2 + shakeX;
                int y = screenH / 2 - 10 + shakeY;

                // Pulsing red color
                long time = System.currentTimeMillis();
                float pulse = (float)(Math.sin(time / 200.0) * 0.5 + 0.5); // 0.0 -> 1.0
                int r = (int)(180 + pulse * 75); // 180 -> 255
                int textColor = (r << 16); // pure red varying intensity

                drawContext.drawTextWithShadow(client.textRenderer, text, x, y, textColor);
            }
        }
    }
}
