package rewrite.utils;

import arc.files.Fi;
import arc.struct.OrderedMap;
import mindustry.game.Team;
import mindustry.gen.Player;
import mindustry.net.NetConnection;

import java.text.*;
import java.time.*;
import java.util.*;

import static arc.util.Strings.*;
import static mindustry.Vars.*;
import static rewrite.PluginVars.*;
import static rewrite.components.Bundle.*;

public class Utils {

    public static <T> T notNullElse(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static Fi getPluginResource(String name) {
        return mods.getMod("darkdustry-plugin").root.child(name);
    }

    public static String coloredTeam(Team team) {
        return team.emoji + "[#" + team.color + "]" + team.name + "[]";
    }

    public static boolean deepEquals(String first, String second) {
        first = stripAll(first);
        second = stripAll(second);
        return first.equalsIgnoreCase(second) || first.toLowerCase().contains(second.toLowerCase());
    }

    public static String stripAll(CharSequence str) {
        return stripColors(stripGlyphs(str));
    }

    public static String formatDate(long time) {
        DateFormat format = new SimpleDateFormat("HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Europe/Moscow")));
        return format.format(new Date(time));
    }

    public static String formatDuration(long time) {
        return formatDuration(time, defaultLocale);
    }

    public static String formatDuration(long time, Locale locale) {
        Duration duration = Duration.ofMillis(time);
        StringBuilder builder = new StringBuilder();
        OrderedMap.<String, Integer>of(
                "time.days", (int) duration.toDaysPart(),
                "time.hours", duration.toHoursPart(),
                "time.minutes", duration.toMinutesPart(),
                "time.seconds", duration.toSecondsPart()).each((key, value) -> {
                    if (value > 0) builder.append(format(key, locale, value)).append(" ");
                });

        return builder.toString().trim();
    }

    public static void kick(NetConnection con, long duration, boolean showDisclaimer, String key, Locale locale, Object... values) {
        String reason = format(key, locale, values);
        if (duration > 0) reason += format("kick.time", locale, Utils.formatDuration(duration, locale));
        if (showDisclaimer) reason += format("kick.disclaimer", locale, discordServerUrl);
        con.kick(reason, duration);
    }

    public static void kick(Player player, long duration, boolean showDisclaimer, String key, Object... values) {
        kick(player.con, duration, showDisclaimer, key, Find.locale(player.locale), values);
    }

    public static void kick(Player player, boolean showDisclaimer, String key, Object... values) {
        kick(player, 0, showDisclaimer, key, values);
    }
}
