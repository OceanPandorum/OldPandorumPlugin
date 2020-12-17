package pandorum;

import arc.*;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.*;
import com.google.gson.*;
import mindustry.net.Packets.KickReason;
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

import java.util.Objects;

import static mindustry.Vars.*;

public class Main extends Plugin{
    private static final double ratio = 0.6;
    public static Config config;
    public static Bundle bundle;

    private final Seq<String> votes = new Seq<>();
    private final ObjectSet<String> alertIgnores = new ObjectSet<>();
    private final Interval alertInterval = new Interval();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Main(){
        Fi cfg = dataDirectory.child("config.json");
        if(!cfg.exists()){
            config = new Config();
            cfg.writeString(gson.toJson(config));
        }
        config = gson.fromJson(cfg.reader(), Config.class);
        bundle = new Bundle();
    }

    @Override
    public void init(){
        Events.on(BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildPlan() != null &&
               event.builder.buildPlan().block == Blocks.thoriumReactor && event.builder.isPlayer() &&
               event.team.cores().contains(c -> event.tile.dst(c.x, c.y) < config.alertDistance)){
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
                Call.sendMessage(bundle.format("commands.rtv.left", NetClient.colorizeName(event.player.id, event.player.name),
                                               cur - 1, req));
            }
        });

        Events.on(GameOverEvent.class, event -> votes.clear());
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        // на всякий
        handler.register("despw", bundle.get("commands.despw.description"), args -> {
            Groups.unit.each(Unit::kill);
            Log.info(bundle.get("commands.despw.log"));
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("alert", bundle.get("commands.alert.description"), (args, player) -> {
            if(alertIgnores.contains(player.uuid())){
                alertIgnores.remove(player.uuid());
                Info.bundled(player, "commands.alert.on");
            }else{
                alertIgnores.add(player.uuid());
                Info.bundled(player, "commands.alert.off");
            }
        });

        handler.<Player>register("ban", bundle.get("commands.admin.ban.params"), bundle.get("commands.admin.ban.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
                return;
            }
            if(!Strings.canParseInt(args[0])){
                Info.bundled(player, "commands.admin.ban.id-not-int");
                return;
            }
            if(!Strings.canParseInt(args[1])){
                Info.bundled(player, "commands.admin.ban.delay-not-int"); // todo пока ничего не делает
                return;
            }

            int id = Strings.parseInt(args[0]);
            Player target = Groups.player.find(p -> p.id() == id);
            if(target == null){
                Info.bundled(player, "commands.player-not-found");
                return;
            }
            if(Objects.equals(target, player) || target.admin()){
                Info.bundled(player, "commands.not-allowed-target");
                return;
            }

            netServer.admins.banPlayer(target.uuid());
            if(args.length > 2){
                target.kick(args[2]);
            }else{
                target.kick(KickReason.banned);
            }
        });

        handler.<Player>register("pl", bundle.get("commands.pl.params"), bundle.get("commands.pl.description"), (args, player) -> {
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                Info.bundled(player, "commands.pl.page-not-int");
                return;
            }

            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil((float)Groups.player.size() / 6);

            page--;

            if(page >= pages || page < 0){
                Info.bundled(player, "commands.pl.under-page", pages);
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(bundle.format("commands.pl.page", (page + 1), pages)).append("\n");

            for(int i = 6 * page; i < Math.min(6 * (page + 1), Groups.player.size()); i++){
                Player t = Groups.player.index(i);
                result.append("[lightgray]* ").append(t.name).append(" [lightgray]/ ID: ").append(t.id());

                if(player.admin){
                    result.append(" / raw: ").append(t.name.replaceAll("\\[", "[[")).append("\n");
                }
            }
            player.sendMessage(result.toString());
        });

        // слегка переделанный rtv
        handler.<Player>register("rtv", bundle.get("commands.rtv.description"), (args, player) -> {
            if(player.uuid() != null && votes.contains(player.uuid())){
                Info.bundled(player, "commands.rtv.contains");
                return;
            }

            votes.add(player.uuid());
            int cur = votes.size;
            int req = (int) Math.ceil(ratio * Groups.player.size());
            Call.sendMessage(bundle.format("commands.rtv.ok", NetClient.colorizeName(player.id, player.name), cur, req));

            if(cur < req){
                return;
            }

            votes.clear();
            Call.sendMessage(bundle.get("commands.rtv.successful"));
            Events.fire(new GameOverEvent(Team.crux));
        });

        //Отправка сообщения для всех в отдельном окне
        handler.<Player>register("bc", bundle.get("commands.admin.bc.params"), bundle.get("commands.admin.bc.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
            }else{
                String arg = args[0].toLowerCase();
                Player target = null;
                if(Strings.canParseInt(arg)){
                    int id = Strings.parseInt(args[0]);
                    target = Groups.player.find(p -> p.id == id);
                }
                Info.broadCast(target, args);
            }
        });

        //Конец игры
        handler.<Player>register("go", bundle.get("commands.admin.go.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
            }else{
                Events.fire(new GameOverEvent(Team.crux));
            }
        });

        //Заспавнить юнитов
        handler.<Player>register("spawn", bundle.get("commands.admin.spawn.params"), bundle.get("commands.admin.spawn.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
                return;
            }
            if(!Strings.canParseInt(args[1])){
                Info.bundled(player, "commands.count-not-int");
                return;
            }

            UnitType unit = content.units().find(b -> b.name.equalsIgnoreCase(args[0]));
            if(unit == null){
                Info.bundled(player, "commands.admin.spawn.units");
                return;
            }

            int count = Strings.parseInt(args[1]);

            Team team = args.length > 2 ? Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[2])) : player.team();
            if(team == null){
                Info.bundled(player, "commands.admin.teamp.teams");
                return;
            }

            for(int i = 0; i < count; i++){
                unit.spawn(team, player.x, player.y);
            }
            Info.bundled(player, "commands.admin.spawn.success", count, unit.name);

        });

        //Заспавнить ядро (попытка искоренить шнеки)
        handler.<Player>register("core", bundle.get("commands.admin.core.params"), bundle.get("commands.admin.core.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
                return;
            }

            Block core = switch(args[0].toLowerCase()){
                case "medium" -> Blocks.coreFoundation;
                case "big" -> Blocks.coreNucleus;
                default -> Blocks.coreShard;
            };

            Call.constructFinish(player.tileOn(), core, player.unit(), (byte)0, player.team(), false);

            Info.bundled(player, player.tileOn().block() == core ? "commands.admin.core.success" : "commands.admin.core.failed");
        });

        //Выход в Хаб
        handler.<Player>register("hub", bundle.get("commands.hub.description"), (args, player) -> Call.connect(player.con, config.hubIp, config.hubPort));

        //cмена команды
        handler.<Player>register("teamp", bundle.get("commands.admin.teamp.params"), bundle.get("commands.admin.teamp.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
                return;
            }
            Team team = Structs.find(Team.all, t -> t.name.equalsIgnoreCase(args[0]));
            if(team == null){
                Info.bundled(player, "commands.admin.teamp.teams");
                return;
            }

            Player target = args.length > 1 ? Groups.player.find(p -> p.name.equalsIgnoreCase(args[1])) : player;
            if(target == null){
                Info.bundled(player, "commands.player-not-found");
                return;
            }

            Info.bundled(target, "commands.admin.teamp.success", team.name);
            target.team(team);
        });

        //Спект режим ("Ваниш")
        handler.<Player>register("s", bundle.get("commands.admin.vanish.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
            }else{
                player.clearUnit();
                player.team(player.team() == Team.derelict ? Team.sharded : Team.derelict);
            }
        });

        //Выдача предметов в ядро
        handler.<Player>register("give", bundle.get("commands.admin.give.params"), bundle.get("commands.admin.give.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
                return;
            }
            if(!Strings.canParseInt(args[0])){
                Info.bundled(player, "commands.count-not-int");
                return;
            }

            int count = Strings.parseInt(args[0]);

            Item item = content.items().find(b -> b.name.equalsIgnoreCase(args[1]));
            if(item == null){
                Info.bundled(player, "commands.admin.give.item-not-found");
                return;
            }

            Teams.TeamData team = state.teams.get(player.team());
            if(!team.hasCore()){
                Info.bundled(player, "commands.admin.give.core-not-found");
                return;
            }
            CoreBlock.CoreBuild core = team.cores.first();

            for(int i = 0; i < count; i++){
                core.items.set(item, count);
            }
            Info.bundled(player, "commands.admin.give.success");
        });
    }
}
