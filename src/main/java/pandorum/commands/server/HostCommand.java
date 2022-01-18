package pandorum.commands.server;

import arc.Core;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import mindustry.game.Gamemode;
import mindustry.maps.Map;
import mindustry.maps.MapException;

import static mindustry.Vars.*;
import static pandorum.Misc.findMap;
import static pandorum.Misc.getServerControl;

import java.util.Set;

public class HostCommand {
    public static void run(final String[] args) {
        if (!state.isMenu()) {
            Log.err("Сервер уже запущен. Используй 'stop', чтобы остановить его.");
            return;
        }

        final Gamemode mode = args.length > 1 ?
            Seq.select(
                Gamemode.values(),
                (enumValue) -> enumValue.name().equals(args[1])
            ).firstOpt() :
            Gamemode.survival;

        if (mode == null) {
            Log.err("Режим игры '@' не найден", args[1]);
            return;
        }

        Map map;
        if (args.length > 0) {
            map = findMap(args[0]);
            if (map == null) {
                Log.err("Карта '@' не найдена.", args[0]);
                return;
            }
        } else {
            map = maps.getShuffleMode().next(mode, state.map);
            Log.info("Случайным образом выбрана карта: '@'.", map.name());
        }

        Log.info("Загружаю карту...");

        logic.reset();
        Core.settings.put("lastServerMode", mode.name());
        Reflect.set(getServerControl(), "lastMode", mode);

        Core.app.post(() -> {
            try {
                world.loadMap(map, map.applyRules(mode));
                state.rules = map.applyRules(mode);
                logic.play();

                Log.info("Карта загружена.");

                netServer.openServer();
            } catch (MapException e) {
                Log.err("@: @", e.map.name(), e.getMessage());
            }
        });
    }
}
