package sfiomn.legendarycreatures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import sfiomn.legendarycreatures.util.PlayerRules;

public class LcRuleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("lcrule");

        for (PlayerRules.Rule rule : PlayerRules.Rule.values()) {
            root.then(Commands.literal(rule.name)
                    .executes(ctx -> query(ctx.getSource(), ctx.getSource().getPlayerOrException(), rule))
                    .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> set(ctx.getSource(), ctx.getSource().getPlayerOrException(), rule, BoolArgumentType.getBool(ctx, "value")))
                            .then(Commands.argument("target", EntityArgument.player())
                                    .requires(src -> src.hasPermission(2))
                                    .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), rule, BoolArgumentType.getBool(ctx, "value"))))));
        }

        dispatcher.register(root);
    }

    private static int query(CommandSourceStack source, ServerPlayer player, PlayerRules.Rule rule) {
        boolean value = PlayerRules.get(player, rule);
        source.sendSuccess(() -> Component.literal(rule.name + " is " + value + " for " + player.getName().getString()), false);
        return value ? 1 : 0;
    }

    private static int set(CommandSourceStack source, ServerPlayer player, PlayerRules.Rule rule, boolean value) throws CommandSyntaxException {
        PlayerRules.set(player, rule, value);
        source.sendSuccess(() -> Component.literal(rule.name + " set to " + value + " for " + player.getName().getString()), true);
        return 1;
    }
}
