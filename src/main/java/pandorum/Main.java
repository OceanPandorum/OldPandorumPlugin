package pandorum;

import arc.Core;
import arc.Events;
import arc.struct.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.core.NetClient;
import mindustry.entities.type.Player;
import mindustry.entities.type.Unit;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.net.Administration.*;
import mindustry.plugin.Plugin;

import static mindustry.Vars.unitGroup;

public class Main extends Plugin{
    private static final double ratio = 0.6;

    private Array<String> votes = new Array<>();
    private ObjectMap<String, Ratekeeper> idToRate = new ObjectMap<>();
    private IntIntMap placed = new IntIntMap();

    public Main(){}

    @Override
    public void init(){
        Vars.netServer.admins.addActionFilter(action -> {
            if(action.type != ActionType.breakBlock && action.type != ActionType.placeBlock &&
                    action.type != ActionType.tapTile && Config.antiSpam.bool()
                    && placed.get(action.tile.pos(), -1) != action.player.id){

                int window = Core.settings.getInt("rateWindow", 6);
                int limit = Core.settings.getInt("rateLimit", 25);
                int kickLimit = Core.settings.getInt("rateKickLimit", 60);

                Ratekeeper rate = idToRate.getOr(action.player.uuid, Ratekeeper::new);
                if(rate.allow(window * 1000, limit)){
                    return true;
                }else{
                    if(rate.occurences > kickLimit){
                        action.player.con.kick("You are interacting with too many blocks.", 1000 * 30);
                    }else{
                        action.player.sendMessage("[scarlet]You are interacting with blocks too quickly.");
                    }

                    return false;
                }
            }
            return true;
        });


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
        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            if(event.player != null){
                placed.put(event.tile.pos(), event.player.id);
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        // на всякий
        handler.register("despw", "Despawn all enemy units.", args -> {
            unitGroup.all().each(Unit::kill);
            Log.info("All units destroyed.");
        });
        // https://github.com/Anuken/RateLimitPlugin
        handler.register("rateconfig", "<window/limit/kickLimit> <value>", "Set configuration values for the rate limit.", args -> {
            String key = "rate" + Strings.capitalize(args[0]);

            if(!(key.equals("rateWindow") || key.equals("rateLimit") || key.equals("rateKickLimit"))){
                Log.err("Not a valid config value: {0}", args[0]);
                return;
            }

            if(Strings.canParseInt(args[1])){
                Core.settings.putSave(key, Integer.parseInt(args[1]));
                Log.info("Ratelimit config value '{0}' set to '{1}'.", key, args[1]);
            }else{
                Log.err("Not a number: {0}", args[1]);
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        // https://github.com/mayli/RockTheVotePlugin
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

    static class Ratekeeper{
        public int occurences;
        public long lastTime;

        public boolean allow(long spacing, int cap){
            if(Time.timeSinceMillis(lastTime) > spacing){
                occurences = 0;
                lastTime = Time.millis();
            }

            occurences ++;
            return occurences <= cap;
        }
    }
}
