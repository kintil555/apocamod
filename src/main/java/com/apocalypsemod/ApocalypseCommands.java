package com.apocalypsemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ApocalypseCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        // /apocalypse trigger  — trigger the apocalypse manually (no need to kill doppelganger)
        dispatcher.register(
                CommandManager.literal("apocalypse")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("trigger")
                                .executes(ApocalypseCommands::triggerApocalypse))
                        .then(CommandManager.literal("reset")
                                .executes(ApocalypseCommands::resetApocalypse))
                        .then(CommandManager.literal("status")
                                .executes(ApocalypseCommands::status))
                        .then(CommandManager.literal("spawn_doppelganger")
                                .executes(ApocalypseCommands::spawnDoppelganger))
        );
    }

    private static int triggerApocalypse(CommandContext<ServerCommandSource> ctx) {
        ApocalypseMod.triggerApocalypseManually(ctx.getSource().getServer());
        ctx.getSource().sendFeedback(
                () -> Text.literal("[ApocalypseMod] Apocalypse manually triggered!")
                        .formatted(Formatting.RED, Formatting.BOLD),
                true
        );
        return 1;
    }

    private static int resetApocalypse(CommandContext<ServerCommandSource> ctx) {
        ApocalypseMod.resetApocalypse(ctx.getSource().getServer());
        ctx.getSource().sendFeedback(
                () -> Text.literal("[ApocalypseMod] Apocalypse reset.")
                        .formatted(Formatting.GREEN),
                true
        );
        return 1;
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        String status = ApocalypseMod.apocalypseTriggered
                ? "ACTIVE (Level: " + String.format("%.1f", ApocalypseMod.apocalypseLevel) + "%)"
                : "INACTIVE";
        ctx.getSource().sendFeedback(
                () -> Text.literal("[ApocalypseMod] Apocalypse status: " + status)
                        .formatted(ApocalypseMod.apocalypseTriggered ? Formatting.RED : Formatting.GREEN),
                false
        );
        return 1;
    }

    private static int spawnDoppelganger(CommandContext<ServerCommandSource> ctx) {
        try {
            if (ctx.getSource().getPlayer() instanceof net.minecraft.server.network.ServerPlayerEntity player) {
                net.minecraft.server.world.ServerWorld world = player.getServerWorld();
                com.apocalypsemod.entity.DoppelgangerEntity doppelganger =
                        new com.apocalypsemod.entity.DoppelgangerEntity(
                                com.apocalypsemod.entity.ModEntities.DOPPELGANGER, world
                        );
                doppelganger.setOwner(player);
                doppelganger.copyAppearanceFrom(player);
                doppelganger.refreshPositionAndAngles(
                        player.getX() + 3, player.getY(), player.getZ(), 0f, 0f
                );
                world.spawnEntity(doppelganger);
                ctx.getSource().sendFeedback(
                        () -> Text.literal("[ApocalypseMod] Doppelganger spawned right in front of you!")
                                .formatted(Formatting.GOLD),
                        false
                );
            }
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Error: " + e.getMessage()));
        }
        return 1;
    }
}
