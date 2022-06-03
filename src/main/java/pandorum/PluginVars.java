package pandorum;

import arc.func.Boolp;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.CommandHandler.Command;
import arc.util.Interval;
import arc.util.Timekeeper;
import arc.util.io.ReusableByteOutStream;
import arc.util.io.Writes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Player;
import mindustry.type.Item;
import mindustry.world.Block;
import net.dv8tion.jda.api.entities.Message;
import pandorum.components.Config;
import pandorum.components.Gamemode;
import pandorum.features.history.HistoryMap;
import pandorum.vote.VoteKickSession;
import pandorum.vote.VoteSession;
import redis.clients.jedis.JedisPool;

import static mindustry.Vars.tilesize;

public class PluginVars {

    /** Максимальный размер заполняемого пространства через /fill. */
    public static final int maxFillSize = 512;
    /** Максимальное количество заспавненных юнитов через /spawn. */
    public static final int maxSpawnAmount = 25;

    /** Время кулдауна для команды /nominate. В секундах. */
    public static final int nominateCooldownTime = 300;
    /** Время кулдауна для команды /votekick. В секундах */
    public static final int voteKickCooldownTime = 300;
    /** Время кулдауна для команды /login. В секундах */
    public static final int loginCooldownTime = 900;
    /** Время кулдауна для команды /sync. В секундах */
    public static final int syncCooldownTime = 15;

    /** Необходимое количество игроков для успешного завершения голосования. */
    public static final float voteRatio = 0.6f;

    /** Максимальное количество записей на один тайл. */
    public static final int maxTileHistoryCapacity = 6;
    /** Ёмкость кэша, хранящего историю. */
    public static final int allHistorySize = 100000;

    /** Расстояние до ядер, в котором отслеживаются опасные блоки. */
    public static final int alertsDistance = 8 * tilesize;
    /** Таймер для оповещения об опасных блоков. */
    public static final float alertsTimer = 150f;

    /** Время голосования через /nominate. В секундах. */
    public static final float voteDuration = 120f;
    /** Время голосования через /votekick. В секундах. */
    public static final float voteKickDuration = 45f;
    /** Время, на которое игрок будет выгнан голосованием или через команду. В миллисекундах. */
    public static final long kickDuration = 2700000L;

    /** Время, на которое игрок будет выгнан за абьюз команды /login. В миллисекундах. */
    public static final long loginAbuseKickDuration = 2700000L;

    /** Локаль по умолчанию. */
    public static final String defaultLocale = "en", defaultTranslatorLocale = "en_GB";

    /** Ссылка на наш Discord сервер */
    public static final String discordServerUrl = "discord.gg/45NNzjGCmY";

    /** Название файла с конфигурацией. */
    public static final String configFileName = "config.json";

    /** Порт для подключения базы данных Jedis. */
    public static final int jedisPoolPort = 6379;

    /** Команда для наблюдателей. */
    public static final Team spectateTeam = Team.derelict;

    /** Эффект при входе на сервер. */
    public static final Effect joinEffect = Fx.greenBomb;
    /** Эффект при выходе с сервера. */
    public static final Effect leaveEffect = Fx.greenLaserCharge;
    /** Эффект при движении игрока. */
    public static final Effect moveEffect = Fx.freezing;

    public static final VoteSession[] currentVote = {null};
    public static final VoteKickSession[] currentVoteKick = {null};

    public static final ObjectMap<Team, Seq<String>> votesSurrender = new ObjectMap<>();
    public static final ObjectMap<String, Timekeeper> nominateCooldowns = new ObjectMap<>(), voteKickCooldowns = new ObjectMap<>(), loginCooldowns = new ObjectMap<>();
    public static final ObjectMap<String, Team> activeSpectatingPlayers = new ObjectMap<>();
    public static final ObjectMap<String, String> codeLanguages = new ObjectMap<>();
    public static final ObjectMap<Message, Player> loginWaiting = new ObjectMap<>();

    public static final Seq<String> votesRtv = new Seq<>(), votesVnw = new Seq<>(), activeHistoryPlayers = new Seq<>();
    public static final Seq<Command> clientAdminOnlyCommands = new Seq<>(), discordAdminOnlyCommands = new Seq<>();

    public static final Seq<Gamemode> defaultModes = Seq.with(Gamemode.attack, Gamemode.pvp, Gamemode.sandbox, Gamemode.survival, Gamemode.tower);

    public static final Interval interval = new Interval(2);

    public static final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();

    public static final HistoryMap history = new HistoryMap(maxTileHistoryCapacity, allHistorySize);

    /** Блоки, которые опасно строить рядом с ядром. */
    public static final ObjectMap<Block, Boolp> dangerousBuildBlocks = new ObjectMap<>();
    /** Блоки, в которые опасно переносить конкретные ресурсы. */
    public static final ObjectMap<Block, Item> dangerousDepositBlocks = new ObjectMap<>();

    /** Время непрерывной работы сервера. */
    public static int serverUpTime = 0;

    /** Время, проведенное на текущей карте. */
    public static int mapPlayTime = 0;

    /** Могут ли игроки голосовать в данный момент. */
    public static boolean canVote = false;

    /** База данных Jedis. */
    public static JedisPool jedisPool;

    public static CommandHandler clientCommands, serverCommands, discordCommands;

    public static Config config;
    public static ReusableByteOutStream writeBuffer;
    public static Writes outputBuffer;

    public static boolean alertsEnabled() {
        return defaultModes.contains(config.mode);
    }

    public static boolean historyEnabled() {
        return defaultModes.contains(config.mode);
    }
}
