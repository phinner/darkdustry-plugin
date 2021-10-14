package pandorum.comp;

import arc.Events;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.Log;
import arc.util.Timer;
import arc.util.io.Streams;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.net.Administration;
import pandorum.PandorumPlugin;
import pandorum.comp.admin.Authme;
import pandorum.events.*;

import java.io.InputStream;
import java.util.Objects;

import static mindustry.Vars.netServer;

public class Loader {
    public static void init() {
        try {
            InputStream stream = Loader.class.getClassLoader().getResourceAsStream("vpn-ipv4.txt");
            Objects.requireNonNull(stream, "stream");
            PandorumPlugin.forbiddenIps = Seq.with(Streams.copyString(stream).split(System.lineSeparator())).map(IpInfo::new);
        } catch(Exception e) {
            throw new ArcRuntimeException(e);
        }

        Administration.Config.showConnectMessages.set(false);
        Administration.Config.strict.set(true);
        Administration.Config.motd.set("off");
        Administration.Config.messageRateLimit.set(1);

        netServer.admins.addActionFilter(ActionFilter::call);
        netServer.admins.addChatFilter(ChatFilter::call);

        Events.on(EventType.PlayerUnbanEvent.class, PlayerUnbanListener::call);
        Events.on(EventType.PlayerBanEvent.class, PlayerBanListener::call);
        Events.on(EventType.ServerLoadEvent.class, ServerLoadListener::call);
        Events.on(EventType.WorldLoadEvent.class, WorldLoadListener::call);
        Events.on(EventType.BlockBuildEndEvent.class, BlockBuildEndListener::call);
        Events.on(EventType.ConfigEvent.class, ConfigListener::call);
        Events.on(EventType.TapEvent.class, TapListener::call);
        Events.on(EventType.DepositEvent.class, DepositListener::call);
        Events.on(EventType.WithdrawEvent.class, WithdrawListener::call);
        Events.on(EventType.BuildSelectEvent.class, BuildSelectListener::call);
        Events.on(EventType.PlayerJoin.class, PlayerJoinListener::call);
        Events.on(EventType.PlayerLeave.class, PlayerLeaveListener::call);
        Events.on(EventType.GameOverEvent.class, GameOverListener::call);
        Events.run(EventType.Trigger.update, TriggerUpdateListener::call);

        Timer.schedule(() -> PandorumPlugin.rainbow.each(r -> Groups.player.contains(p -> p == r.player), RainbowPlayerEntry::changeEntryColor), 0f, 0.05f);

        MenuListener.init();
        Icons.init();
        Authme.init();
        PandorumPlugin.socket.connect();

        Log.info("[Darkdustry]: Сервер запущен и готов к работе!");
    }
}