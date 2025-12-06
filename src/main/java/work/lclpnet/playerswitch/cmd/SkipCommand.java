package work.lclpnet.playerswitch.cmd;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
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
        registrar.registerCommand(CommandManager.literal("skip")
                .requires(s -> s.hasPermissionLevel(4))
                .executes(this::skip));
    }

    private int skip(CommandContext<ServerCommandSource> ctx) {
        manager.skipPlayer();
        ctx.getSource().sendMessage(Text.literal("Skipped the current player."));
        return 1;
    }
}
