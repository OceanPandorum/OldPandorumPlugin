package pandorum;

import arc.*;
import arc.files.Fi;
import arc.struct.*;
import arc.util.*;
import pandorum.components.*;
import mindustry.content.Blocks;
import mindustry.core.NetClient;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.*;

public class Main extends Plugin{
    private static final double ratio = 0.6;

    public static final Fi dir = Core.settings.getDataDirectory().child("/mods/pandorum/");
    public static final Config config = new Config();
    public static final Bundle bundle = new Bundle();

    private final Seq<String> votes = new Seq<>();
    private final ObjectSet<String> alertIgnores = new ObjectSet<>();

    private Interval alertInterval = new Interval();

    public Main(){}

    @Override
    public void init(){
        Events.on(BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildPlan() != null &&
               event.builder.buildPlan().block == Blocks.thoriumReactor && event.builder.isPlayer() &&
               event.team.cores().contains(c -> event.tile.dst(c.x, c.y) < 350)){
                Player target = event.builder.getPlayer();

                if(alertInterval.get(200)){
                    Groups.player.each(p -> !alertIgnores.contains(p.uuid()), player -> player.sendMessage(bundle.format("events.alert", target.name, event.tile.x, event.tile.y)));
                }
            }
        });

        Events.on(PlayerLeave.class, event -> {
            int cur = votes.size;
            int req = (int) Math.ceil(ratio * Groups.player.size());
            if(votes.contains(event.player.uuid())){
                votes.remove(event.player.uuid());
                Call.sendMessage(bundle.format("rtv.left", NetClient.colorizeName(event.player.id, event.player.name),
                                               cur - 1, req));
            }
        });

        Events.on(GameOverEvent.class, event -> votes.clear());
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
        handler.<Player>register(bundle.get("alert.name"), bundle.get("alert.description"), (args, player) -> {
            if(alertIgnores.contains(player.uuid())){
                alertIgnores.remove(player.uuid());
                Info.text(player, "$alert.on");
            }else{
                alertIgnores.add(player.uuid());
                Info.text(player, "$alert.off");
            }
        });

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
            Events.fire(new GameOverEvent(Team.crux));
        });

        //Отправка сообщения для всех в отдельном окне
        handler.<Player>register(bundle.get("bc.name"), bundle.get("bc.params"), bundle.get("bc.description"), (args, player) -> {
            if(!player.admin){
                Info.text(player, "$commands.permission-denied");
            }else{
                Info.broadCast(args);
            }
        });

        //Конец игры
        handler.<Player>register(bundle.get("go.name"), bundle.get("go.description"), (args, player) -> {
            if(!player.admin){
                Info.text(player, "$commands.permission-denied");
            }else{
                Events.fire(new GameOverEvent(Team.crux));
            }
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

            UnitType unit = content.units().find(b -> b.name.equalsIgnoreCase(args[0]));
            if(unit == null){
                Info.text(player, "$spawn.units");
                return;
            }

            int count = Strings.parseInt(args[1]);

            Team team = args.length > 2 ? Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[2])) : player.team();
            if(team == null){
                Info.text(player, "$teamp.teams");
                return;
            }

            for(int i = 0; i < count; i++){
                unit.spawn(team, player.x, player.y);
            }
            Info.bundled(player, "spawn.ok", count, unit.name);

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

            Call.constructFinish(player.tileOn(), core, player.unit(), (byte)0, player.team(), false);

            Info.text(player, player.tileOn().block() == core ? "$core.yes" : "$core.no");
        });

        //Выход в Хаб
        handler.<Player>register(bundle.get("hub.name"), bundle.get("hub.description"), (args, player) -> Call.connect(player.con, config.object.getString("hub-ip", null), config.object.getInt("hub-port", 0)));

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
            }else{
                player.clearUnit();
                player.team(player.team() == Team.derelict ? Team.sharded : Team.derelict);
            }
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

            Teams.TeamData team = state.teams.get(player.team());
            if(!team.hasCore()){
                Info.text(player, "$give.core-not-found");
                return;
            }
            CoreBlock.CoreBuild core = team.cores.first();

            for(int i = 0; i < count; i++){
                core.items.set(item, count);
            }
            Info.text(player, "$give.success");
        });

    }
}
