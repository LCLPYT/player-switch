package work.lclpnet.playerswitch.util.msg;

import work.lclpnet.playerswitch.config.PlayerEntry;
import work.lclpnet.playerswitch.util.discord.DiscordBot;
import work.lclpnet.playerswitch.util.discord.DiscordWebhook;

public class Messenger {

    private final DiscordWebhook discordWebhook;
    private final DiscordBot discordBot;

    public Messenger(DiscordWebhook discordWebhook, DiscordBot discordBot) {
        this.discordWebhook = discordWebhook;
        this.discordBot = discordBot;
    }

    public void sendTurnNotification(PlayerEntry playerEntry) {
        discordWebhook.sendTurnNotification(playerEntry);
        discordBot.sendTurnNotification(playerEntry);
    }

    public void updateStatus() {
        discordBot.updateActivityStatus();
    }

    public void sendSkipNotification(PlayerEntry playerEntry) {
        discordBot.sendSkipNotification(playerEntry);
    }
}
