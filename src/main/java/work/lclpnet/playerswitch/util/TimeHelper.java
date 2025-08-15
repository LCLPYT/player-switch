package work.lclpnet.playerswitch.util;

import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.text.TranslatedText;

public class TimeHelper {

    private TimeHelper() {}

    public static TranslatedText formatTime(Translations translations, long ticks) {
        long totalSeconds = ticks / 20;
        long seconds = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes = totalMinutes % 60;
        long totalHours = totalMinutes / 60;
        long hours = totalHours % 24;
        long days = totalHours / 24;

        if (days > 0) {
            return translations.translateText("player-switch.time.days_hours_minutes_seconds", new Object[]{
                    String.format("%d", days),
                    String.format("%02d", hours),
                    String.format("%02d", minutes),
                    String.format("%02d", seconds)
            });
        }

        if (hours > 0) {
            return translations.translateText("player-switch.time.hours_minutes_seconds", new Object[]{
                    String.format("%d", hours),
                    String.format("%02d", minutes),
                    String.format("%02d", seconds)
            });
        }

        if (minutes > 0) {
            return translations.translateText("player-switch.time.minutes_seconds", new Object[]{
                    String.format("%02d", minutes),
                    String.format("%02d", seconds)
            });
        }

        return translations.translateText("player-switch.time.seconds", seconds);
    }
}
