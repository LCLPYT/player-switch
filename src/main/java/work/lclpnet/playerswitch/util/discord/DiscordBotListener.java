package work.lclpnet.playerswitch.util.discord;

import lombok.Setter;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.config.PlayerEntry;
import work.lclpnet.playerswitch.util.SwitchManager;

public class DiscordBotListener extends ListenerAdapter {

    public static final String SKIP_CONFIRM_MODAL_ID = "skip_confirm";
    public static final String CONFIRM_VALUE = "SKIP";
    public static final String CONFIRM_INPUT_ID = "confirm";

    private final Translations translations;
    private final ConfigManager<Config> configManager;
    @Setter
    private @Nullable SwitchManager switchManager = null;

    public DiscordBotListener(Translations translations, ConfigManager<Config> configManager) {
        this.translations = translations;
        this.configManager = configManager;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (DiscordBot.SKIP_TURN_BUTTON_ID.equals(id)) {
            onSkipButton(event);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();

        if (SKIP_CONFIRM_MODAL_ID.equals(id)) {
            onSkipConfirm(event);
        }
    }

    private void onSkipButton(ButtonInteractionEvent event) {
        PlayerEntry playerEntry = configManager.config().getCurrentPlayerEntry().orElse(null);

        if (playerEntry == null
                || validateIsCurrentPlayer(event, playerEntry)
                || validateNotCurrentlyPlaying(event, playerEntry)) return;

        String lang = playerEntry.getSafeLanguage();

        String title = translations.translate(lang, "player-switch.discord.confirm_skip");
        String confirmLabel = translations.translate(lang, "player-switch.discord.confirm_label");
        String confirmText = translations.translate(lang, "player-switch.discord.confirm_text", CONFIRM_VALUE);

        var modal = Modal.create(SKIP_CONFIRM_MODAL_ID, title)
                .addComponents(
                        Label.of(confirmLabel, TextInput.create(CONFIRM_INPUT_ID, TextInputStyle.SHORT)
                                .setRequired(true)
                                .setPlaceholder(confirmText)
                                .setRequiredRange(CONFIRM_VALUE.length(), CONFIRM_VALUE.length())
                                .build()
                        )
                )
                .build();

        event.replyModal(modal).queue();
    }

    private void onSkipConfirm(ModalInteractionEvent event) {
        SwitchManager switchManager = this.switchManager;
        ModalMapping mapping = event.getValue(CONFIRM_INPUT_ID);

        if (switchManager == null || mapping == null) return;

        String value = mapping.getAsString();

        PlayerEntry playerEntry = configManager.config().getCurrentPlayerEntry().orElse(null);

        if (playerEntry == null
                || validateIsCurrentPlayer(event, playerEntry)
                || validateNotCurrentlyPlaying(event, playerEntry)) return;

        String lang = playerEntry.getSafeLanguage();

        if (!value.equalsIgnoreCase(CONFIRM_VALUE)) {
            String msg = translations.translate(lang, "player-switch.discord.confirm_invalid");
            event.reply(msg).queue();
            return;
        }

        switchManager.skipPlayer(false);

        Message message = event.getMessage();

        if (message != null) {
            message.editMessageComponents().queue();
        }

        String msg = translations.translate(lang, "player-switch.discord.skipped");

        event.reply(msg).queue();
    }

    private boolean validateIsCurrentPlayer(IReplyCallback event, PlayerEntry playerEntry) {
        if (playerEntry.getDiscordId().equals(event.getUser().getId())) {
            return false;
        }

        String lang = playerEntry.getSafeLanguage();
        String msg = translations.translate(lang, "player-switch.discord.not_your_turn");

        event.reply(msg).queue();

        return true;
    }

    private boolean validateNotCurrentlyPlaying(IReplyCallback event, PlayerEntry playerEntry) {
        SwitchManager switchManager = this.switchManager;

        if (switchManager == null) return true;

        if (switchManager.isNobodyPlaying()) return false;

        String lang = playerEntry.getSafeLanguage();
        String msg = translations.translate(lang, "player-switch.discord.cannot_skip");

        event.reply(msg).queue();

        return true;
    }
}
