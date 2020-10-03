package pandorum;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.struct.Array;
import arc.util.*;
import components.*;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Mechs;
import mindustry.core.GameState;
import mindustry.core.NetClient;
import mindustry.entities.type.*;
import mindustry.game.*;
import mindustry.gen.Call;
import mindustry.plugin.Plugin;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.*;

public class Main extends Plugin{
    private static final double ratio = 0.6;
    private final Array<String> votes = new Array<>();
    public static final Fi dir = Core.settings.getDataDirectory().child("/mods/pandorum/");

    public static final Nick colornick = new Nick();
    public static final Config config = new Config();
    public static final Bundle bundle = new Bundle();

    public Main(){}

    @Override
    public void init(){

        /*netServer.admins.addActionFilter(action -> {
            try{
                if(action.type != ActionType.breakBlock &&
                        action.type != ActionType.placeBlock &&
                        action.type != ActionType.tapTile &&
                        antiSpam.bool()){
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
            }catch(Exception e){
                Log.err(e);
                return true;
            }
        });*/

        Events.on(EventType.PlayerLeave.class, event -> {
            int cur = votes.size;
            int req = (int) Math.ceil(ratio * Vars.playerGroup.size());
            if(votes.contains(event.player.uuid)) {
                votes.remove(event.player.uuid);
                Call.sendMessage(bundle.format("rtv.left",
                        NetClient.colorizeName(event.player.id, event.player.name), cur - 1, req));
            }
        });

        Events.on(EventType.GameOverEvent.class, event -> votes.clear());
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        // на всякий
        handler.register(bundle.get("despw.name"), bundle.get("despw.description"), args -> {
            unitGroup.all().each(Unit::kill);
            Log.info(bundle.get("despw.log"));
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
        handler.<Player>register(bundle.get("rtv.name"), bundle.get("rtv.description"), (args, player) -> {
            if(player.uuid != null && votes.contains(player.uuid)){
                Info.text(player, "$rtv.x2");
                return;
            }

            votes.add(player.uuid);
            int cur = votes.size;
            int req = (int) Math.ceil(ratio * playerGroup.size());
            Call.sendMessage(bundle.format("rtv.ok", NetClient.colorizeName(player.id, player.name), cur, req));

            if(cur < req){
                return;
            }

            votes.clear();
            Call.sendMessage(bundle.get("rtv.successful"));
            Events.fire(new EventType.GameOverEvent(Team.crux));
        });

        //Отправка сообщения для всех в отдельнои окне
        handler.<Player>register(bundle.get("bc.name"), bundle.get("bc.params"), bundle.get("bc.description"), (args, player) -> {
            if(!player.isAdmin){
                Info.text(player, "$commands.permission-denied");
                return;
            }
            Info.broadCast(args);
        });

        //Конец игры
        handler.<Player>register(bundle.get("go.name"), bundle.get("go.description"), (args, player) -> {
            if(!player.isAdmin){
                Info.text(player, "$commands.permission-denied");
                return;
            }
            if(state.is(GameState.State.menu)){
                Log.err(bundle.get("go.end"));
                return;
            }
            Events.fire(new EventType.GameOverEvent(Team.crux));
        });

        //Заспавнить юнитов
        handler.<Player>register(bundle.get("spawn.name"), bundle.get("spawn.params"), bundle.get("spawn.description"), (args, player) -> {
            if(!player.isAdmin){
                Info.text(player, "$commands.permission-denied");
                return;
            }
            if(!Strings.canParseInt(args[1])){
                Info.text(player, "$commands.count-not-int");
                return;
            }

            UnitType tunit = content.units().find(b -> b.name.equalsIgnoreCase(args[0]));
            if(tunit == null){
                Info.text(player, "$spawn.units");
                return;
            }

            int count = Strings.parseInt(args[1]);
            Team tteam;

            switch(args[2]){
                case "sharded" -> tteam = Team.sharded;
                case "blue" -> tteam = Team.blue;
                case "crux" -> tteam = Team.crux;
                case "derelict" -> tteam = Team.derelict;
                case "green" -> tteam = Team.green;
                case "purple" -> tteam = Team.purple;
                default -> {
                    Info.text(player, "$spawn.team");
                    return;
                }
            }

            for(int i = 0; i < count; i++){
                BaseUnit baseUnit = tunit.create(tteam);
                baseUnit.set(player.x, player.y);
                baseUnit.add();
            }
            Info.bundled(player,"spawn.ok", count, tunit.name);

        });
        //Заспавнить ядро (попытка искоренить шнеки)
        handler.<Player>register(bundle.get("core.name"), bundle.get("core.params"), bundle.get("core.description"), (args, player) -> {
            if(!player.isAdmin){
                Info.text(player, "$commands.permission-denied");
                return;
            }

            Block core = switch(args[0]){
                case "medium" -> Blocks.coreFoundation;
                case "big" -> Blocks.coreNucleus;
                default -> Blocks.coreShard;
            };

            Call.onConstructFinish(world.tile(player.tileX(), player.tileY()), core, 0, (byte) 0, player.getTeam(), false);

            Info.text(player, world.tile(player.tileX(), player.tileY()).block() == core ? "$core.yes" : "$core.no");
        });

        //Анимированный ник (by Summet#4530)
        handler.<Player>register(bundle.get("nick.name"), bundle.get("nick.description"), (args, player) -> {
            if(!player.isAdmin){
                Info.text(player, "$commands.permission-denied");
                return;
            }
            if(colornick.targets.contains(player))
                colornick.targets.remove(player);
            else
                colornick.targets.add(player);
            Info.text(player, "$nick.successful");
        });

        //Выход в Хаб
        handler.<Player>register(bundle.get("hub.name"), bundle.get("hub.description"), (args, player) -> {
            Call.onConnect(player.con, config.object.getString("hub-ip", "???"), config.object.getInt("hub-port", 0));
        });

        //Смена меха
        handler.<Player>register(bundle.get("setm.name"), bundle.get("setm.params"), bundle.get("setm.description"), (args, player) -> {
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
                default -> Info.text(player, "$setm.mechs");
            }
            player.mech = pmech;
            Info.bundled(player, "setm.yes", pmech.name);
        });

        //cмена команды
        handler.<Player>register(bundle.get("teamp.name"), bundle.get("teamp.params"), bundle.get("teamp.description"), (args, player) -> {
            if (!player.isAdmin) {
                Info.text(player, "$commands.permission-denied");
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
                    Info.text(player, "$teamp.teams");
                    return;
                }
            }

            if (args.length == 1) {
                player.setTeam(cteam);
                Info.bundled(player, "teamp.success", cteam.name);
            } else {
                Player target = playerGroup.find(p -> p.name.equals(args[1]));
                if (target == null) {
                    Info.text(player, "$commands.player-not-found");
                    return;
                }
                target.setTeam(cteam);
                Info.bundled(target, "teamp.target", cteam.name);
                Info.bundled(player, "teamp.player", target.name, cteam.name);
            }
        });

        //Спект режим ("Ваниш")
        handler.<Player>register(bundle.get("vanish.name"), bundle.get("vanish.description"), (args, player) -> {
            if(!player.isAdmin){
                Info.text(player, "$commands.permission-denied");
                return;
            }
            player.spawner = player.lastSpawner = null;
            player.kill();
            player.setTeam(player.getTeam() == Team.derelict ? Team.sharded : Team.derelict);
        });

        //Выдача предметов в ядро
        handler.<Player>register(bundle.get("give.name"), bundle.get("give.params"), bundle.get("give.description"), (args, player) -> {
            if(!player.isAdmin) {
                Info.text(player, "$commands.permission-denied");
                return;
            }
            if(!Strings.canParseInt(args[0])){
                Info.text(player, "$commands.count-not-int");
                return;
            }

            int count = Strings.parseInt(args[0]);

            Item item = content.items().find(b -> b.name.equalsIgnoreCase(args[1]));
            if(item == null){
                Info.text(player, "$give.item-not-found");
                return;
            }

            for (int i = 0; i < count; i++) {
                Teams.TeamData pteam = state.teams.get(player.getTeam());
                if (!pteam.hasCore()) {
                    Info.text(player, "$give.core-not-found");
                    return;
                }
                CoreBlock.CoreEntity core = pteam.cores.first();
                core.items.set(item, count);
            }
            Info.text(player, "$give.success");
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

            occurences++;
            return occurences <= cap;
        }
    }
}
