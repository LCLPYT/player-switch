package work.lclpnet.playerswitch.cmd;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;

import java.util.concurrent.atomic.AtomicBoolean;

public class ResetRunCommand implements KibuCommand {

    private final AtomicBoolean resetRun;

    public ResetRunCommand(AtomicBoolean resetRun) {
        this.resetRun = resetRun;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(CommandManager.literal("reset_run")
                .requires(s -> s.hasPermissionLevel(4))
                .executes(this::resetRun));
    }

    private int resetRun(CommandContext<ServerCommandSource> ctx) {
        resetRun.set(true);
        ctx.getSource().getServer().stop(false);
        return 1;
    }
}
