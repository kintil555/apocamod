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

    // Phase 100 end-sequence state
    private static float redFadeProgress = 0f;      // 0.0 -> 1.0
    private static boolean endSequenceActive = false;
    private static int endSequenceTick = 0;          // counts ticks after text appears
    private static final int CLOSE_AFTER_TICKS = 400; // 20 seconds = 400 ticks

    public static void onClientTick(MinecraftClient client) {
        if (!ApocalypseMod.apocalypseTriggered || client.player == null) {
            redFadeProgress = 0f;
            endSequenceActive = false;
            endSequenceTick = 0;
            return;
        }

        float level = ApocalypseMod.apocalypseLevel;
        ambientTick++;

        // ── Camera Shake (disabled during end sequence) ───────────────────────
        if (level >= 10f && !endSequenceActive) {
            float intensity = (level / 100f) * 3.5f;
            float dy = (RANDOM.nextFloat() - 0.5f) * intensity;
            float dp = (RANDOM.nextFloat() - 0.5f) * intensity;
            client.player.setYaw(client.player.getYaw() + dy);
            client.player.setPitch(client.player.getPitch() + dp);
        }

        // ── Fast Day/Night cycle ─────────────────────────────────────────────
        if (level >= 25f && client.world != null && !endSequenceActive) {
            int skip = (int)(level / 25f);
            if (ambientTick % 2 == 0) {
                client.world.setTime(client.world.getTimeOfDay() + skip * 10L);
            }
        }

        // ── Phase 100: end sequence ───────────────────────────────────────────
        if (level >= 100f) {
            // Fade red in over ~6 seconds
            if (redFadeProgress < 1f) {
                redFadeProgress = Math.min(1f, redFadeProgress + 0.003f);
            }

            // Once fully red, activate end sequence
            if (redFadeProgress >= 1f && !endSequenceActive) {
                endSequenceActive = true;
                endSequenceTick = 0;
            }

            if (endSequenceActive) {
                endSequenceTick++;

                // Freeze player — zero velocity, prevent input
                client.player.setVelocity(0, 0, 0);
                client.player.noClip = false;

                // Close game after 20 seconds
                if (endSequenceTick >= CLOSE_AFTER_TICKS) {
                    client.scheduleStop();
                }
            }
        }

        // ── Ambient Sounds ───────────────────────────────────────────────────
        if (!endSequenceActive) {
            int interval = Math.max(20, 200 - (int)(level * 1.8f));
            if (ambientTick % interval == 0 && client.getSoundManager() != null) {
                playAmbientSound(client, level);
            }
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

        // ── Phase 100: full red takeover ──────────────────────────────────────
        if (level >= 100f) {
            // Alpha: 0 -> 255 as redFadeProgress goes 0 -> 1
            int alpha = (int)(redFadeProgress * 255);
            alpha = Math.min(255, alpha);

            // Full opaque red
            drawContext.fill(0, 0, screenW, screenH, (alpha << 24) | 0xCC0000);

            // Hide entire HUD once end sequence is active (draw black over HUD area)
            if (endSequenceActive) {
                // Cover hotbar area at the bottom
                drawContext.fill(0, screenH - 60, screenW, screenH, 0xFF000000);
                // Cover top area (health, hunger etc)
                drawContext.fill(0, 0, screenW, 40, 0xFF000000);
            }

            // Show shaking text once red is at least 50% in
            if (redFadeProgress >= 0.5f) {
                String text = "This is the end for you.";
                int textW = client.textRenderer.getWidth(text) * 2; // scale 2x

                float shakeStrength = redFadeProgress * 8f;
                int shakeX = (int)((RANDOM.nextFloat() - 0.5f) * shakeStrength);
                int shakeY = (int)((RANDOM.nextFloat() - 0.5f) * shakeStrength);

                int x = (screenW - textW) / 2 + shakeX;
                int y = screenH / 2 - 10 + shakeY;

                // Draw text scaled 2x using matrix transform
                drawContext.getMatrices().push();
                drawContext.getMatrices().translate(x, y, 0);
                drawContext.getMatrices().scale(2f, 2f, 1f);

                long time = System.currentTimeMillis();
                float pulse = (float)(Math.sin(time / 200.0) * 0.5 + 0.5);
                int r = (int)(200 + pulse * 55); // 200 -> 255
                int textColor = (0xFF << 24) | (r << 16); // fully opaque red

                drawContext.drawTextWithShadow(client.textRenderer, text, 0, 0, textColor);
                drawContext.getMatrices().pop();

                // Countdown hint (small, bottom center) — only show last 5 seconds
                if (endSequenceActive && endSequenceTick >= CLOSE_AFTER_TICKS - 100) {
                    int secsLeft = (CLOSE_AFTER_TICKS - endSequenceTick) / 20;
                    String countdown = secsLeft > 0 ? secsLeft + "..." : "Goodbye.";
                    int cw = client.textRenderer.getWidth(countdown);
                    drawContext.drawTextWithShadow(client.textRenderer, countdown,
                            (screenW - cw) / 2, screenH / 2 + 20, 0xFFAA0000);
                }
            }
        }
    }

    public static boolean isEndSequenceActive() {
        return endSequenceActive;
    }
}
