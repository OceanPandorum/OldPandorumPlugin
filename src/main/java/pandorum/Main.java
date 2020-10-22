package pandorum;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.*;
import components.*;
import mindustry.content.Blocks;
import mindustry.core.GameState;
import mindustry.core.NetClient;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.*;

public class Main extends Plugin{
    public static final Fi dir = Core.settings.getDataDirectory().child("/mods/pandorum/");
    public static final Nick colornick = new Nick();
    public static final Config config = new Config();
    public static final Bundle bundle = new Bundle();
    private static final double ratio = 0.6;
    private final Seq<String> votes = new Seq<>();

    public Main(){}

    @Override
    public void init(){
        Events.on(EventType.PlayerLeave.class, event -> {
            int cur = votes.size;
            int req = (int) Math.ceil(ratio * Groups.player.size());
            if(votes.contains(event.player.uuid())){
                votes.remove(event.player.uuid());
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
            Groups.unit.each(Unit::kill);
            Log.info(bundle.get("despw.log"));
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        // слегка переделанный rtv
        handler.<Player>register(bundle.get("rtv.name"), bundle.get("rtv.description"), (args, player) -> {
            if(player.uuid() != null && votes.contains(player.uuid())){
                Info.text(player, "$rtv.x2");
                return;
            }

            votes.add(player.uuid());
            int cur = votes.size;
            int req = (int) Math.ceil(ratio * Groups.player.size());
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
            if(!player.admin){
                Info.text(player, "$commands.permission-denied");
                return;
            }
            Info.broadCast(args);
        });

        //Конец игры
        handler.<Player>register(bundle.get("go.name"), bundle.get("go.description"), (args, player) -> {
            if(!player.admin){
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
            if(!player.admin){
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

            Team team = Structs.find(Team.all, t -> t.name.equalsIgnoreCase(args[1]));
            if(team == null){
                Info.text(player, "$teamp.teams");
                return;
            }

            for(int i = 0; i < count; i++){
                tunit.spawn(team, player.x, player.y);
            }
            Info.bundled(player, "spawn.ok", count, tunit.name);

        });

        //Заспавнить ядро (попытка искоренить шнеки)
        handler.<Player>register(bundle.get("core.name"), bundle.get("core.params"), bundle.get("core.description"), (args, player) -> {
            if(!player.admin){
                Info.text(player, "$commands.permission-denied");
                return;
            }

            Block core = switch(args[0]){
                case "medium" -> Blocks.coreFoundation;
                case "big" -> Blocks.coreNucleus;
                default -> Blocks.coreShard;
            };

            Call.constructFinish(world.tile(player.tileX(), player.tileY()), core, player.unit(), (byte) 0, player.team(), false);

            Info.text(player, world.tile(player.tileX(), player.tileY()).block() == core ? "$core.yes" : "$core.no");
        });

        //Анимированный ник (by Summet#4530)
        handler.<Player>register(bundle.get("nick.name"), bundle.get("nick.description"), (args, player) -> {
            if(!player.admin){
                Info.text(player, "$commands.permission-denied");
                return;
            }
            if(colornick.targets.contains(player)){
                colornick.targets.remove(player);
            }else{
                colornick.targets.add(player);
            }
            Info.text(player, "$nick.successful");
        });

        //Выход в Хаб
        handler.<Player>register(bundle.get("hub.name"), bundle.get("hub.description"), (args, player) -> {
            Call.connect(player.con, config.object.getString("hub-ip", null), config.object.getInt("hub-port", 0));
        });

        //cмена команды
        handler.<Player>register(bundle.get("teamp.name"), bundle.get("teamp.params"), bundle.get("teamp.description"), (args, player) -> {
            if(!player.admin){
                Info.text(player, "$commands.permission-denied");
                return;
            }
            Team team = Structs.find(Team.all, t -> t.name.equalsIgnoreCase(args[0]));
            if(team == null){
                Info.text(player, "$teamp.teams");
                return;
            }

            Player target = args.length > 1 ? Groups.player.find(p -> p.name.equalsIgnoreCase(args[1])) : player;
            if(target == null){
                Info.text(player, "$commands.player-not-found");
                return;
            }

            Info.bundled(target, "teamp.success", team.name);
            target.team(team);
        });

        //Спект режим ("Ваниш")
        handler.<Player>register(bundle.get("vanish.name"), bundle.get("vanish.description"), (args, player) -> {
            if(!player.admin){
                Info.text(player, "$commands.permission-denied");
                return;
            }

            player.clearUnit();
            player.team(player.team() == Team.derelict ? Team.sharded : Team.derelict);
        });

        //Выдача предметов в ядро
        handler.<Player>register(bundle.get("give.name"), bundle.get("give.params"), bundle.get("give.description"), (args, player) -> {
            if(!player.admin){
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

            Teams.TeamData pteam = state.teams.get(player.team());
            if(!pteam.hasCore()){
                Info.text(player, "$give.core-not-found");
                return;
            }
            CoreBlock.CoreBuild core = pteam.cores.first();

            for(int i = 0; i < count; i++){
                core.items.set(item, count);
            }
            Info.text(player, "$give.success");
        });

    }
}
