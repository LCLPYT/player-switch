package work.lclpnet.playerswitch.util;

import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.text.TranslatedText;

public class TimeHelper {

    private TimeHelper() {}

    public static TranslatedText formatTime(Translations translations, int seconds) {
        int minutes = seconds / 60;
        seconds %= 60;

        if (minutes > 0) {
            return translations.translateText("player-switch.time.minutes_seconds", new Object[]{
                    String.format("%02d", minutes), String.format("%02d", seconds)
            });
        }

        return translations.translateText("player-switch.time.seconds", seconds);
    }
}
