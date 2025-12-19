package work.lclpnet.playerswitch.cmd;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.playerswitch.util.SwitchManager;

public class SkipCommand implements KibuCommand {

    private final SwitchManager manager;

    public SkipCommand(SwitchManager manager) {
        this.manager = manager;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(Commands.literal("skip")
                .requires(s -> s.hasPermission(4))
                .executes(this::skip));
    }

    private int skip(CommandContext<CommandSourceStack> ctx) {
        manager.skipPlayer();
        ctx.getSource().sendSystemMessage(Component.literal("Skipped the current player."));
        return 1;
    }
}
