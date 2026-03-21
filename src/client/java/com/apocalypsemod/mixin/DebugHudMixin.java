package com.apocalypsemod.mixin;

import com.apocalypsemod.ApocalypseMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.gui.hud.DebugHud")
public class DebugHudMixin {

    @Inject(method = "getLeftText", at = @At("RETURN"))
    private void addApocalypseInfo(CallbackInfoReturnable<List<String>> cir) {
        List<String> list = cir.getReturnValue();

        if (!ApocalypseMod.apocalypseTriggered) {
            list.add("");
            list.add("\u00a77[ApocalypseMod] Status: \u00a7aNORMAL");
        } else {
            float level = ApocalypseMod.apocalypseLevel;
            String phase, color;

            if (level < 25f)       { phase = "Phase 1 - Awal";         color = "\u00a7e"; }
            else if (level < 50f)  { phase = "Phase 2 - Eskalasi";     color = "\u00a76"; }
            else if (level < 75f)  { phase = "Phase 3 - Chaos";        color = "\u00a7c"; }
            else if (level < 100f) { phase = "Phase 4 - Kehancuran";   color = "\u00a74"; }
            else                   { phase = "Phase 5 - KIAMAT TOTAL"; color = "\u00a74\u00a7l"; }

            list.add("");
            list.add("\u00a77[ApocalypseMod] \u00a7cKIAMAT AKTIF");
            list.add("\u00a77Level: " + color + String.format("%.1f", level) + "%");
            list.add("\u00a77Fase : " + color + phase);
        }
    }
}
