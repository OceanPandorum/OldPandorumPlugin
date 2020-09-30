package pandorum;

import arc.Core;
import arc.Events;
import arc.struct.Array;
import arc.struct.IntIntMap;
import arc.struct.ObjectMap;
import arc.util.*;
import arc.util.Log;
import components.*;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Mechs;
import mindustry.core.GameState;
import mindustry.core.NetClient;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.entities.type.Unit;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.Call;
import mindustry.net.Administration;
import mindustry.plugin.Plugin;
import mindustry.type.Item;
import mindustry.type.Mech;
import mindustry.type.UnitType;
import mindustry.world.Block;
import arc.util.CommandHandler;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.*;

public class Main extends Plugin{
    private ObjectMap<String, Ratekeeper> idToRate = new ObjectMap<>();
    private IntIntMap placed = new IntIntMap();
    private static final double ratio = 0.6;
    private Array<String> votes = new Array<>();
    private boolean targets;
    public static final Nick colornick = new Nick();
    public Main() {
        Config.main();
    }

    @Override
    public void init(){

        Vars.netServer.admins.addActionFilter(action -> {
            if(action.type != Administration.ActionType.breakBlock && action.type != Administration.ActionType.placeBlock &&
                    action.type != Administration.ActionType.tapTile && Administration.Config.antiSpam.bool()
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
                Call.sendMessage(Strings.format(Bundle.get("rtv.left"),
                                                NetClient.colorizeName(event.player.id, event.player.name), cur-1, req));
            }
        });
        Events.on(EventType.GameOverEvent.class, event -> votes.clear());
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        // на всякий
        handler.register(Bundle.get("despw.name"), Bundle.get("despw.description"), args -> {
            unitGroup.all().each(Unit::kill);
            Log.info(Bundle.get("despw.log"));
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
        // слегка переделанный rtv
        handler.<Player>register( Bundle.get("rtv.name"), Bundle.get("rtv.description"), (args, player) -> {
            if(player.uuid != null && votes.contains(player.uuid)){
                player.sendMessage(Bundle.get("rtv.x2"));
                return;
            }

            votes.add(player.uuid);
            int cur = votes.size;
            int req = (int) Math.ceil(ratio * playerGroup.size());
            Call.sendMessage(Strings.format(Bundle.get("rtv.ok"),
                    NetClient.colorizeName(player.id, player.name), cur, req));

            if (cur < req) {
                return;
            }

            votes.clear();
            Call.sendMessage(Bundle.get("rtv.successful"));
            Events.fire(new EventType.GameOverEvent(Team.crux));
        });

        //Отправка сообщения для всех в отдельнои окне
        handler.<Player>register(Bundle.get("bc.name"), Bundle.get("bc.params"), Bundle.get("bc.description"), (args, player) -> {
            if (player.isAdmin) Broadcast.bc(args);
        });

        //Конец игры
        handler.<Player>register( Bundle.get("go.name"), Bundle.get("go.description"), (args, player) -> {
            if (!player.isAdmin) return;
            if(state.is(GameState.State.menu)) {
                Log.err(Bundle.get("go.end"));
                return;
            }
            Events.fire(new EventType.GameOverEvent(Team.crux));
        });

        //Заспавнить юнитов
        handler.<Player>register( Bundle.get("spawn.name"), Bundle.get("spawn.params"), Bundle.get("spawn.description"), (args, player) ->{
            if(!player.isAdmin){
                player.sendMessage(Bundle.get("spawn.noAdmin"));
                return;
            }
            UnitType tunit = content.units().find(b -> b.name.equals(args[0]));

            int count;
            try {
                count = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Bundle.get("spawn.count"));
                return;
            }
            Team tteam;
            switch (args[2]) {
                case "sharded" -> tteam = Team.sharded;
                case "blue" -> tteam = Team.blue;
                case "crux" -> tteam = Team.crux;
                case "derelict" -> tteam = Team.derelict;
                case "green" -> tteam = Team.green;
                case "purple" -> tteam = Team.purple;
                default -> {
                    player.sendMessage(Bundle.get("spawn.team"));
                    return;
                }
            }

            if (tunit != null) {
                for (int i = 0; count > i; i++){
                    BaseUnit baseUnit = tunit.create(tteam);
                    baseUnit.set(player.x, player.y);
                    baseUnit.add();
                }
                player.sendMessage(Bundle.get("spawn.ok") + " " + count +" "+ tunit);
            } else  {
                player.sendMessage(Bundle.get("spawn.mobName"));
            }

        });
        //Заспавнить ядро (попытка искоренить шнеки)
        handler.<Player>register( Bundle.get("core.name"), Bundle.get("core.params"), Bundle.get("core.description"), (args, player) -> {

            if(!player.isAdmin){
                player.sendMessage(Bundle.get("core.notAdmin"));
                return;
            }

            Block core = switch (args[0]) {
                case "medium" -> Blocks.coreFoundation;
                case "big" -> Blocks.coreNucleus;
                default -> Blocks.coreShard;
            };

            Call.onConstructFinish(world.tile(player.tileX(),player.tileY()), core,0,(byte)0,player.getTeam(),false);

            if(world.tile(player.tileX(),player.tileY()).block() == core){
                player.sendMessage(Bundle.get("core.yes"));
            } else {
                player.sendMessage(Bundle.get("core.no"));
            }
        });

        //Анимированный ник
        handler.<Player>register( Bundle.get("nick.name"), Bundle.get("nick.description"), (args, player) -> {
            if(!player.isAdmin){
                player.sendMessage(Bundle.get("nick.notAdmin"));
                return;
            }
            colornick.targets.add(player);
            if (this.targets = true) colornick.targets.add(player) ;
            player.sendMessage(Bundle.get("nick.successful"));
        });

        //Выход в Хаб
        handler.<Player>register( Bundle.get("hub.name"), Bundle.get("hub.description"),(args , player) -> Call.onConnect(player.con, Config.get("ip1"), Integer.parseInt(Config.get("port1"))));

        //Смена меха
        handler.<Player>register( Bundle.get("setm.name"), Bundle.get("setm.params"), Bundle.get("setm.description"),(args , player) -> {
            Mech pmech = Mechs.starter;
            switch (args[0]) {
                case "alpha" -> pmech = Mechs.alpha;
                case "dart" -> pmech = Mechs.dart;
                case "glaive" -> pmech = Mechs.glaive;
                case "delta" -> pmech = Mechs.delta;
                case "javelin" -> pmech = Mechs.javelin;
                case "omega" -> pmech = Mechs.omega;
                case "tau" -> pmech = Mechs.tau;
                case "trident" -> pmech = Mechs.trident;
                default -> player.sendMessage(Bundle.get("setm.mechs"));
            }
            player.mech = pmech;
            player.sendMessage(Bundle.get("setm.yes") + " " + pmech);
        });

        //cмена команды
        handler.<Player>register(Bundle.get("teamp.name"), Bundle.get("teamp.params"), Bundle.get("teamp.description"), (args, player) -> {
            if (!player.isAdmin) {
                player.sendMessage(Bundle.get("teamp.notAdmin"));
                return;
            }
            Team cteam;
            switch (args[0]) {
                case "sharded" -> cteam = Team.sharded;
                case "blue" -> cteam = Team.blue;
                case "crux" -> cteam = Team.crux;
                case "derelict" -> cteam = Team.derelict;
                case "green" -> cteam = Team.green;
                case "purple" -> cteam = Team.purple;
                default -> {
                    player.sendMessage(Bundle.get("teamp.t"));
                    return;
                }
            }
            if (args.length == 1) {
                player.setTeam(cteam);
                player.sendMessage(Bundle.get("teamp.success") + " " + "[accent]"+cteam);
            } else {
                Player target = playerGroup.find(p -> p.name.equals(args[1]));
                if (target == null) {
                    player.sendMessage(Bundle.get("teamp.notPlayer"));
                    return;
                }
                target.setTeam(cteam);
                target.sendMessage((Bundle.get("teamp.setTeam1")) + " " +"[accent]"+cteam);
                player.sendMessage((Bundle.get("teamp.setTeam2"))+ " " +"[accent]"+target.name + " " + (Bundle.get("teamp.setTeam3")) + " " + "[accent]"+cteam);
            }
        });

    //Спект режим ("Ваниш")
        handler.<Player>register(Bundle.get("vanish.name"),Bundle.get("vanish.description"), (args, player) -> {
            if(!player.isAdmin){
                player.sendMessage(Bundle.get("vanish.notAdmin"));
                return;
            }
            if(player.getTeam() == Team.derelict){
                player.kill();
                player.setTeam(Team.sharded);
            }else{
                player.kill();
                player.setTeam(Team.derelict);
            }
        });

        //Выдача предметов в ядро
        handler.<Player>register(Bundle.get("give.name"), Bundle.get("give.params"), Bundle.get("give.description"), (args , player) -> {
            if(!player.isAdmin) {
                player.sendMessage(Bundle.get("give.notAdmin"));
                return;
            }

            int count;
            try {
                count = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(Bundle.get("give.count"));
                return;
            }

            Item item = content.items().find(b -> b.name.equals(args[1]));

            for (int i = 0; count > i; i++) {
                Teams.TeamData pteam = state.teams.get(player.getTeam());
                if (!pteam.hasCore()) {
                    player.sendMessage(Bundle.get("give.notCore"));
                    return;
                }
                CoreBlock.CoreEntity core = pteam.cores.first();
                core.items.set(item, count);
            }
            player.sendMessage(Bundle.get("give.success"));
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
