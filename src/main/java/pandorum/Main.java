package pandorum;

import arc.Events;
import arc.struct.Array;
import arc.util.*;
import mindustry.Vars;
import mindustry.core.NetClient;
import mindustry.entities.type.Player;
import mindustry.entities.type.Unit;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.plugin.Plugin;

import static mindustry.Vars.unitGroup;

public class Main extends Plugin{
    private static final double ratio = 0.6;
    private Array<String> votes = new Array<>();

    public Main(){}

    @Override
    public void init(){
        Events.on(EventType.PlayerLeave.class, event -> {
            int cur = votes.size;
            int req = (int) Math.ceil(ratio * Vars.playerGroup.size());
            if(votes.contains(event.player.uuid)) {
                votes.remove(event.player.uuid);
                Call.sendMessage(Strings.format("[lightgray][[RTV]: {0}[accent] left, [green]{1}[accent] votes, [green]{2}[accent] required",
                                                NetClient.colorizeName(event.player.id, event.player.name), cur, req));
            }
        });
        Events.on(EventType.GameOverEvent.class, event -> {
            votes.clear();
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        // на всякий
        handler.register("despw", "Despawn all enemy units.", args -> {
            unitGroup.all().each(Unit::kill);
            Log.info("All units destroyed.");
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        // слегка переделанный rtv
        handler.<Player>register("rtv", "Rock the vote to change map", (args, player) -> {
            if(player.uuid != null && votes.contains(player.uuid)){
                player.sendMessage("[scarlet]You've already voted. Sit down.");
                return;
            }

            votes.add(player.uuid);
            int cur = votes.size;
            int req = (int) Math.ceil(ratio * Vars.playerGroup.size());
            Call.sendMessage(Strings.format("[lightgray][[RTV]: {0}[accent] wants to change the map, [green]{1}[accent] votes, [green]{2}[accent] required",
                                            NetClient.colorizeName(player.id, player.name), cur, req));

            if (cur < req) {
                return;
            }

            votes.clear();
            Call.sendMessage("[lightgray][[RTV]: [green]vote passed, changing map.");
            Events.fire(new EventType.GameOverEvent(Team.crux));
        });
    }
}
