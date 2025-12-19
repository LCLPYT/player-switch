package work.lclpnet.playerswitch.cmd;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
        registrar.registerCommand(Commands.literal("reset_run")
                .requires(s -> s.hasPermission(4))
                .executes(this::resetRun));
    }

    private int resetRun(CommandContext<CommandSourceStack> ctx) {
        resetRun.set(true);
        ctx.getSource().sendSystemMessage(Component.literal("Resetting the current run. The server will be stopped for this. Upon restart, a new run is automatically started."));
        ctx.getSource().getServer().halt(false);
        return 1;
    }
}
