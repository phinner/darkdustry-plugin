package rewrite.features;

import arc.func.Cons;
import arc.struct.StringMap;
import arc.util.Http;
import arc.util.Strings;
import arc.util.serialization.Jval;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import rewrite.DarkdustryPlugin;

import static arc.util.Strings.*;
import static mindustry.Vars.netServer;
import static rewrite.PluginVars.*;
import static rewrite.components.Database.*;

public class Translator {

    public static int left = 500000;

    public static void load() {
        translatorLanguages.putAll(
                "id", "Indonesia",
                "da", "Dansk",
                "de", "Deutsch",
                "et", "Eesti",
                "en", "English",
                "es", "Español",
                "eu", "Euskara",
                "fr", "Français",
                "it", "Italiano",
                "lt", "Lietuvių",
                "hu", "Magyar",
                "nl", "Nederlands",
                "pl", "Polski",
                "pt", "Português",
                "ro", "Română",
                "fi", "Suomi",
                "sv", "Svenska",
                "vi", "Tiếng Việt",
                "tk", "Türkmen dili",
                "tr", "Türkçe",
                "cs", "Čeština",
                "be", "Беларуская",
                "bg", "Български",
                "ru", "Русский",
                "uk", "Українська",
                "th", "ไทย",
                "zh", "简体中文",
                "ja", "日本語",
                "ko", "한국어");

        mindustry2Api.putAll(
                "in_ID", "id",
                "nl_BE", "nl",
                "pt_BR", "pt",
                "pt_PT", "pt",
                "uk_UA", "uk",
                "zh_CN", "zh",
                "zh_TW", "zh");

        DarkdustryPlugin.info("Загружено @ языков для переводчика.", translatorLanguages.size);
    }

    public static void translate(String to, String text, Cons<String> cons) {
        Http.post(translatorApiUrl, "to=" + to + "&text=" + text)
                .header("content-type", "application/x-www-form-urlencoded")
                .header("X-RapidAPI-Key", config.translatorApiKey)
                .header("X-RapidAPI-Host", translatorApiHost)
                .error(throwable -> cons.get(""))
                .submit(response -> {
                    left = Strings.parseInt(response.getHeader("x-ratelimit-requests-remaining"));
                    cons.get(Jval.read(response.getResultAsString()).getString("translated_text"));
                });
    }

    public static void translate(Player author, String text) {
        StringMap cache = new StringMap();
        String message = netServer.chatFormatter.format(author, text);

        Groups.player.each(player -> player != author, player -> {
            String language = getPlayerData(player).language;
            if (language.equals("off") || left == 0) player.sendMessage(message, author, text);
            else {
                if (cache.containsKey(language)) {
                    player.sendMessage(cache.get(language), author, text);
                    return;
                }

                translate(language, stripColors(text), translated -> {
                    cache.put(language, message + " [white]([lightgray]" + translated + "[])");
                    player.sendMessage(cache.get(language), author, text);
                });
            }
        });
    }
}
