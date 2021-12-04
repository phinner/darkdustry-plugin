package pandorum.discord;

import arc.Core;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Strings;
import arc.util.Timer;
import arc.util.io.Streams;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields;
import discord4j.core.spec.MessageCreateSpec;
import mindustry.gen.Groups;
import mindustry.maps.Map;
import pandorum.Misc;
import pandorum.PandorumPlugin;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import static mindustry.Vars.customMapDirectory;
import static mindustry.Vars.state;
import static mindustry.Vars.maps;
import static pandorum.discord.BotMain.*;

public class BotHandler {
    public static final CommandHandler handler = new CommandHandler(PandorumPlugin.config.prefix);
    public static MessageChannel botChannel, adminChannel;

    public static void init() {
        registerChannels();
        registerCommands();

        handler.setPrefix(PandorumPlugin.config.prefix);
        Timer.schedule(() -> BotMain.client.updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.watching("Игроков на сервере: " + Groups.player.size()))).subscribe(null, e -> {}), 0f, 12f);
    }

    public static void registerChannels() {
        botChannel = (MessageChannel) client.getChannelById(Snowflake.of(PandorumPlugin.config.DiscordChannelID)).block();
        adminChannel = (MessageChannel) client.getChannelById(Snowflake.of(PandorumPlugin.config.DiscordAdminChannelID)).block();
    }

    private static void registerCommands() {
        handler.<Message>register("help", "Список команд.", (args, msg) -> {
            StringBuilder builder = new StringBuilder();
            for (CommandHandler.Command command : handler.getCommandList()) {
                builder.append(PandorumPlugin.config.prefix).append("**").append(command.text).append("**");
                if (command.params.length > 0) {
                    builder.append(" *").append(command.paramText).append("*");
                }
                builder.append(" - ").append(command.description).append("\n");
            }

            info(msg.getChannel().block(), "Команды", builder.toString());
        });

        handler.<Message>register("addmap", "Добавить карту на сервер.", (args, msg) -> {
            if (checkAdmin(Objects.requireNonNull(msg.getAuthorAsMember().block()))) {
                err(msg.getChannel().block(), "Эта команда недоступна для тебя.", "У тебя нет прав на ее использование.");
                return;
            }

            if (msg.getAttachments().size() != 1 || !msg.getAttachments().get(0).getFilename().endsWith(".msav")) {
                err(msg.getChannel().block(), "Ошибка", "Пожалуйста, прикрепи файл карты к сообщению.");
                return;
            }

            Attachment a = msg.getAttachments().get(0);

            try {
                Fi mapFile = customMapDirectory.child(a.getFilename());
                Streams.copy(download(a.getUrl()), new FileOutputStream(mapFile.file()));
                maps.reload();
                text(Objects.requireNonNull(msg.getChannel().block()), "*Карта добавлена на сервер.*");
            } catch (Exception e) {
                err(msg.getChannel().block(), "Ошибка добавления карты.", "Произошла непредвиденная ошибка.");
            }
        });

        handler.<Message>register("map", "<название...>", "Получить файл карты с сервера.", (args, msg) -> {
            if (checkAdmin(Objects.requireNonNull(msg.getAuthorAsMember().block()))) {
                err(msg.getChannel().block(), "Эта команда недоступна для тебя.", "У тебя нет прав на ее использование.");
                return;
            }

            maps.reload();
            Map map = Misc.findMap(args[0]);
            if (map == null) {
                err(msg.getChannel().block(), "Карта не найдена.", "Проверьте правильность ввода.");
                return;
            }

            try {
                Objects.requireNonNull(msg.getChannel().block()).createMessage(MessageCreateSpec.builder().addFile(MessageCreateFields.File.of(map.file.name(), new FileInputStream(map.file.file()))).build()).subscribe(null, e -> {});
            } catch (Exception e) {
                err(msg.getChannel().block(), "Возникла ошибка.", "Ошибка получения карты с сервера.");
            }
        });

        handler.<Message>register("removemap", "<название...>", "Удалить карту с сервера.", (args, msg) -> {
            if (checkAdmin(Objects.requireNonNull(msg.getAuthorAsMember().block()))) {
                err(msg.getChannel().block(), "Эта команда недоступна для тебя.", "У тебя нет прав на ее использование.");
                return;
            }

            maps.reload();
            Map map = Misc.findMap(args[0]);
            if (map == null) {
                err(msg.getChannel().block(), "Карта не найдена.", "Проверьте правильность ввода.");
                return;
            }

            try {
                maps.removeMap(map);
                maps.reload();
                text(Objects.requireNonNull(msg.getChannel().block()), "*Карта удалена с сервера.*");
            } catch (Exception e) {
                err(msg.getChannel().block(), "Возникла ошибка.", "Ошибка удаления карты с сервера.");
            }
        });

        handler.<Message>register("maps", "[страница]", "Список всех карт сервера.", (args, msg) -> {
            if (args.length > 0 && !Strings.canParseInt(args[0])) {
                err(msg.getChannel().block(), "Страница должна быть числом.", "Аргументы не верны.");
                return;
            }

            maps.reload();
            Seq<Map> mapList = maps.customMaps();
            if (mapList.size == 0) {
                err(msg.getChannel().block(), "На сервере нет карт.", "Список карт пуст.");
                return;
            }

            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil(mapList.size / 20f);

            if (--page >= pages || page < 0) {
                err(msg.getChannel().block(), "Указана неверная страница списка карт.", "Страница должна быть числом от 1 до " + pages);
                return;
            }

            StringBuilder maps = new StringBuilder();
            for (int i = 20 * page; i < Math.min(20 * (page + 1), mapList.size); i++) {
                maps.append("**").append(i + 1).append(".** ").append(Strings.stripColors(mapList.get(i).name())).append("\n");
            }

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .color(BotMain.normalColor)
                    .author("Server maps", null, "https://cdn.iconscout.com/icon/free/png-256/map-and-location-2569358-2148268.png")
                    .title("Список карт сервера (страница " + (page + 1) + " из " + pages + ")")
                    .addField("Карты:", maps.toString(), false)
                    .build();

            sendEmbed(Objects.requireNonNull(msg.getChannel().block()), embed);
        });

        handler.<Message>register("status","Узнать статус сервера.", (args, msg) -> {
            if (state.isMenu()) {
                err(msg.getChannel().block(), "Сервер отключен.", "Попросите администратора запустить его.");
                return;
            }

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .color(BotMain.successColor)
                    .author("Статус сервера", null, "https://icon-library.com/images/yes-icon/yes-icon-15.jpg")
                    .title("Сервер онлайн.")
                    .addField("Игроков:", Integer.toString(Groups.player.size()), false)
                    .addField("Карта:", state.map.name(), false)
                    .addField("Волна:", Integer.toString(state.wave), false)
                    .addField("Потребление ОЗУ:", Core.app.getJavaHeap() / 1024 / 1024 + " MB", false)
                    .footer("Используй " + PandorumPlugin.config.prefix + "players, чтобы посмотреть список всех игроков.", null)
                    .build();

            sendEmbed(Objects.requireNonNull(msg.getChannel().block()), embed);
        });

        handler.<Message>register("players", "[страница]", "Посмотреть список игроков на сервере.", (args, msg) -> {
            if (args.length > 0 && !Strings.canParseInt(args[0])) {
                err(msg.getChannel().block(), "Страница должна быть числом.", "Аргументы не верны.");
                return;
            }

            if (Groups.player.size() == 0) {
                err(msg.getChannel().block(), "На сервере нет игроков.", "Список игроков пуст.");
                return;
            }

            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil(Groups.player.size() / 20f);

            if (--page >= pages || page < 0) {
                err(msg.getChannel().block(), "Указана неверная страница списка карт.", "Страница должна быть числом от 1 до " + pages);
                return;
            }

            StringBuilder players = new StringBuilder();
            for (int i = 20 * page; i < Math.min(20 * (page + 1), Groups.player.size()); i++) {
                players.append(i).append(". ").append(Strings.stripColors(Groups.player.index(i).name)).append("\n");
            }

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .color(BotMain.normalColor)
                    .author("Server players", null, "https://cdn4.iconfinder.com/data/icons/symbols-vol-1-1/40/user-person-single-id-account-player-male-female-512.png")
                    .title("Список игроков на сервере (всего " + Groups.player.size() + ")")
                    .addField("Игроки:", players.toString(), false)
                    .build();

            sendEmbed(Objects.requireNonNull(msg.getChannel().block()), embed);
        });
    }

    public static void text(String text, Object... args) {
        text(botChannel, text, args);
    }

    public static void text(MessageChannel channel, String text, Object... args) {
        channel.createMessage(Strings.format(text, args)).subscribe(null, e -> {});
    }

    public static void info(MessageChannel channel, String title, String text, Object... args) {
        sendEmbed(channel, EmbedCreateSpec.builder().color(normalColor).addField(title, Strings.format(text, args), true).build());
    }

    public static void err(MessageChannel channel, String title, String text, Object... args) {
        sendEmbed(channel, EmbedCreateSpec.builder().color(errorColor).addField(title, Strings.format(text, args), true).build());
    }

    public static void sendEmbed(EmbedCreateSpec embed) {
        sendEmbed(botChannel, embed);
    }

    public static void sendEmbed(MessageChannel channel, EmbedCreateSpec embed) {
        channel.createMessage(embed).subscribe(null, e -> {});
    }

    public static InputStream download(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
            return connection.getInputStream();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkAdmin(Member member) {
        if (member.isBot()) return true;
        return member.getRoles().toStream().noneMatch(role -> role.getId().equals(Snowflake.of(810760273689444385L)));
    }
}
