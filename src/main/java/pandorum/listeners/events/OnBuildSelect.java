package pandorum.listeners.events;

import arc.func.Cons;
import mindustry.game.EventType.BuildSelectEvent;
import pandorum.components.Icons;
import pandorum.data.PlayerData;
import pandorum.util.Utils;

import static pandorum.PluginVars.*;
import static pandorum.data.Database.getPlayerData;
import static pandorum.util.Utils.*;

public class OnBuildSelect implements Cons<BuildSelectEvent> {

    public void get(BuildSelectEvent event) {
        if (!alertsEnabled() || event.breaking || event.builder == null || event.builder.buildPlan() == null) return;

        if (isDangerousBuild(event.builder.buildPlan().block, event.team, event.tile) && interval.get(0, alertsTimer)) {
            Utils.eachPlayer(event.team, player -> {
                PlayerData data = getPlayerData(player.uuid());
                if (data.alertsEnabled) {
                    bundled(player, "events.dangerous-build", getName(event.builder), Icons.get(event.builder.buildPlan().block.name), event.tile.x, event.tile.y);
                }
            });
        }
    }
}
