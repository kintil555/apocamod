package com.apocalypsemod.mixin.client;

import com.apocalypsemod.ApocalypseMod;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public class DebugHudMixin {

    @Inject(method = "getLeftText", at = @At("RETURN"))
    private void addApocalypseInfo(CallbackInfoReturnable<List<String>> cir) {
        List<String> list = cir.getReturnValue();

        if (!ApocalypseMod.apocalypseTriggered) {
            list.add("");
            list.add("[ApocalypseMod] Status: AMAN");
            return;
        }

        float level = ApocalypseMod.apocalypseLevel;
        int phase = level < 25 ? 1 : level < 50 ? 2 : level < 75 ? 3 : level < 100 ? 4 : 5;

        String phaseLabel = switch (phase) {
            case 1 -> "Fase 1 - Badai Awal";
            case 2 -> "Fase 2 - Eskalasi Chaos";
            case 3 -> "Fase 3 - Api & Gravitasi";
            case 4 -> "Fase 4 - Kehancuran";
            case 5 -> "Fase 5 - KIAMAT TOTAL";
            default -> "???";
        };

        list.add("");
        list.add("§c[ApocalypseMod] ☠ KIAMAT AKTIF ☠");
        list.add(String.format("§c  Level: %.1f%%", level));
        list.add("§c  " + phaseLabel);
    }
}
