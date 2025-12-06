package work.lclpnet.playerswitch.util.msg;

import work.lclpnet.playerswitch.config.PlayerEntry;
import work.lclpnet.playerswitch.util.DiscordBot;
import work.lclpnet.playerswitch.util.DiscordWebhook;

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

    public void onNextTurn() {
        discordBot.updateActivityStatus();
    }
}
