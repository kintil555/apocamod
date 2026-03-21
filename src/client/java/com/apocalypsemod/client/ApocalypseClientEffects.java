package com.apocalypsemod.client;

import com.apocalypsemod.ApocalypseMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.Random;

@Environment(EnvType.CLIENT)
public class ApocalypseClientEffects {

    private static final Random RANDOM = new Random();
    private static int fastDayNightTick = 0;

    // Camera shake offsets (applied via yaw/pitch nudge)
    public static float shakeDeltaYaw = 0f;
    public static float shakeDeltaPitch = 0f;

    public static void onClientTick(MinecraftClient client) {
        if (!ApocalypseMod.apocalypseTriggered || client.player == null) {
            shakeDeltaYaw = 0;
            shakeDeltaPitch = 0;
            return;
        }

        float level = ApocalypseMod.apocalypseLevel;

        // ── Camera Shake ──────────────────────────────────────────────────────
        // Starts at level 10, gets violent at 75+
        if (level >= 10f) {
            float intensity = (level / 100f) * 3.5f; // max ~3.5 degrees of shake
            shakeDeltaYaw   = (RANDOM.nextFloat() - 0.5f) * intensity;
            shakeDeltaPitch = (RANDOM.nextFloat() - 0.5f) * intensity;

            client.player.setYaw(client.player.getYaw() + shakeDeltaYaw);
            client.player.setPitch(client.player.getPitch() + shakeDeltaPitch);
        }

        // ── Fast Day/Night Cycle ──────────────────────────────────────────────
        // Starts at level 25: days cycle faster and faster
        if (level >= 25f && client.world != null) {
            fastDayNightTick++;

            // How many extra ticks to skip per real tick (more = faster cycle)
            int skip = (int) (level / 25f); // 1 at lvl25, 2 at lvl50, 3 at lvl75, 4 at lvl100
            if (fastDayNightTick % 2 == 0) {
                // We nudge the client world time visually
                long current = client.world.getTimeOfDay();
                client.world.setTime(current + skip * 10L);
            }
        }
    }

    public static void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!ApocalypseMod.apocalypseTriggered) return;

        float level = ApocalypseMod.apocalypseLevel;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int screenW = drawContext.getScaledWindowWidth();
        int screenH = drawContext.getScaledWindowHeight();

        // ── Red vignette overlay — starts at level 25, full red at 100 ────────
        if (level >= 25f) {
            // Alpha: 0 at level 25, up to 140 at level 100
            int alpha = (int) Math.min(140, ((level - 25f) / 75f) * 140f);

            // Pulsing effect at high levels
            if (level >= 75f) {
                double pulse = Math.sin(System.currentTimeMillis() / 300.0);
                alpha = (int) Math.min(180, alpha + pulse * 30);
            }

            int color = (alpha << 24) | 0x8B0000; // dark red with variable alpha
            drawContext.fill(0, 0, screenW, screenH, color);
        }

        // ── "KIAMAT" flashing text at level 100 ──────────────────────────────
        if (level >= 100f) {
            long time = System.currentTimeMillis();
            if ((time / 500) % 2 == 0) { // flash every 500ms
                String text = "☠ KIAMAT TOTAL ☠";
                int textW = client.textRenderer.getWidth(text);
                drawContext.drawTextWithShadow(
                        client.textRenderer, text,
                        (screenW - textW) / 2,
                        screenH / 2 - 20,
                        0xFF0000
                );
            }
        }
    }
}
EOF