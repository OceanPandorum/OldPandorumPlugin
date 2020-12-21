package pandorum;

import arc.Events;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.ObjectMap.Entry;
import arc.struct.*;
import arc.util.Timer;
import arc.util.*;
import arc.util.io.Streams;
import com.google.gson.*;
import com.google.gson.stream.*;
import mindustry.content.Blocks;
import mindustry.core.NetClient;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.TeamData;
import mindustry.gen.*;
import mindustry.maps.Map;
import mindustry.mod.Plugin;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import pandorum.components.*;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static mindustry.Vars.*;

public class PandorumPlugin extends Plugin{
    public static VoteSession[] current = {null};
    public static Config config;
    public static Bundle bundle;

    protected static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .registerTypeAdapter(Instant.class, new TypeAdapter<Instant>(){
                @Override
                public void write(JsonWriter out, Instant value) throws IOException{
                    out.value(value.toString());
                }

                @Override
                public Instant read(JsonReader in) throws IOException{
                    return Instant.parse(in.nextString());
                }
            })
            .disableHtmlEscaping()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    private final ObjectSet<String> votes = new ObjectSet<>();
    private final ObjectSet<String> alertIgnores = new ObjectSet<>();
    private final Seq<IpInfo> forbiddenIps;
    private final Interval alertInterval = new Interval();

    private final DateTimeFormatter formatter;

    public PandorumPlugin(){
        Fi cfg = dataDirectory.child("config.json");
        if(!cfg.exists()){
            config = new Config();
            cfg.writeString(gson.toJson(config));
        }
        config = gson.fromJson(cfg.reader(), Config.class);
        bundle = new Bundle();
        formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss", Locale.forLanguageTag(config.locale));
        try{
            forbiddenIps = Seq.with(Streams.copyString(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("vpn-ipv4.txt"))).split("\n")).map(IpInfo::new);
        }catch(IOException e){
            throw new ArcRuntimeException(e);
        }
    }

    @Override
    public void init(){

        // netServer.admins.addChatFilter((target, text) -> {
        //    // todo Команда для мьюта
        // });

        netServer.admins.addChatFilter((target, text) -> {
            String lower = text.toLowerCase();
            if(current[0] != null && (lower.equals("y") || lower.equals("n"))){
                if((current[0].voted().contains(target.uuid()) || current[0].voted().contains(netServer.admins.getInfo(target.uuid()).lastIP))){
                    Info.bundled(target, "commands.nominate.already-voted");
                    return null;
                }

                int sign = lower.equals("y") ? 1 : -1;
                current[0].vote(target, sign);
                return null;
            }
            return text;
        });

        Events.on(PlayerConnect.class, event -> {
            Player player = event.player;
            if(config.bannedNames.contains(player.name())){
                player.con.kick(bundle.get("events.unofficial-mindustry"), 60000);
            }

            forbiddenIps.filter(i -> i.matchIp(player.con.address)).each(i -> player.con.kick(bundle.get("events.vpn-ip")));

            ActionService.get(AdminActionType.ban, player.uuid(), actions -> {
                AdminAction action = actions.isEmpty() ? null : actions.get(0);
                if(action != null && action.endTimestamp() != null && !Instant.now().isAfter(action.endTimestamp())){
                    action.reason().ifPresentOrElse(player::kick, () -> player.kick(KickReason.banned));
                }
            });
        });

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
            int req = (int) Math.ceil(config.voteRatio * Groups.player.size());
            if(votes.contains(event.player.uuid())){
                votes.remove(event.player.uuid());
                Call.sendMessage(bundle.format("commands.rtv.left", NetClient.colorizeName(event.player.id, event.player.name),
                                               cur - 1, req));
            }
        });

        Events.on(GameOverEvent.class, event -> votes.clear());

        Timer.schedule(() -> ActionService.get(AdminActionType.ban, actions -> {
            for(AdminAction a : actions){
                Log.debug(gson.toJson(a));
                if(a.endTimestamp() != null && Instant.now().isAfter(a.endTimestamp())){
                    netServer.admins.unbanPlayerID(a.targetId());
                    Log.info("Unbanned: @", a.targetId());
                    ActionService.delete(AdminActionType.ban, a.targetId());
                }
            }
        }), 5, 20);
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        // на всякий
        handler.register("despw", bundle.get("commands.despw.description"), args -> {
            Groups.unit.each(Unit::kill);
            Log.info(bundle.get("commands.despw.log"));
        });

        handler.register("ban-sync", bundle.get("commands.ban-sync.description"), args -> {
            ActionService.get(AdminActionType.ban, actions -> {
                int pre = netServer.admins.getBanned().size;
                actions.forEach(action -> {
                    try{
                        PlayerInfo playerInfo = netServer.admins.getInfo(action.targetId());
                        netServer.admins.banPlayerID(playerInfo.id);
                        netServer.admins.banPlayerIP(playerInfo.lastIP);
                    }catch(Throwable ignored){}
                });
                Log.info(bundle.format("commands.ban-sync.count", netServer.admins.getBanned().size - pre));
            });
        });

        handler.register("kicks", bundle.get("commands.kicks.description"), args -> {
            Log.info("Kicks: @", netServer.admins.kickedIPs.isEmpty() ? "<none>" : "");
            for(Entry<String, Long> e : netServer.admins.kickedIPs){
                PlayerInfo info = netServer.admins.findByIPs(e.key).first();
                Log.info("  @ / ID: '@' / IP: '@' / END: @", info.lastName, info.id, info.lastIP, formatter.format(Instant.ofEpochMilli(e.value).atZone(ZoneId.systemDefault())));
            }
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
            Instant delay = CommonUtil.parseTime(args[1]);
            if(delay == null){
                Info.bundled(player, "commands.admin.delay-not-int");
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
            Optional<String> reason = args.length > 2 ? Optional.ofNullable(args[0]) : Optional.empty();

            AdminAction action = new AdminAction();
            action.targetId(target.uuid());
            action.adminId(player.uuid());
            action.type(AdminActionType.ban);
            reason.ifPresent(action::reason);
            action.timestamp(Instant.now());
            action.endTimestamp(delay);

            ActionService.save(action);
            if(netServer.admins.banPlayer(target.uuid())){
                reason.ifPresentOrElse(target::kick, () -> target.kick(KickReason.banned));
            }
        });

        handler.<Player>register("unban", bundle.get("commands.admin.unban.params"), bundle.get("commands.admin.unban.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
                return;
            }

            if(netServer.admins.unbanPlayerID(args[0]) || netServer.admins.unbanPlayerIP(args[0])){
                Info.bundled(player, "commands.admin.unban.successful");
                PlayerInfo target = Optional.ofNullable(netServer.admins.findByIP(args[0])).orElse(netServer.admins.getInfo(args[0]));
                ActionService.delete(AdminActionType.ban, target.id);
            }else{
                Info.bundled(player, "commands.admin.unban.not-banned");
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
            result.append(bundle.format("commands.pl.page", page + 1, pages)).append("\n");

            for(int i = 6 * page; i < Math.min(6 * (page + 1), Groups.player.size()); i++){
                Player t = Groups.player.index(i);
                result.append("[lightgray]* ").append(t.name).append(" [lightgray]/ ID: ").append(t.id());

                if(player.admin){
                    result.append(" / raw: ").append(t.name.replaceAll("\\[", "[["));
                }
                result.append("\n");
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
            int req = (int)Math.ceil(config.voteRatio * Groups.player.size());
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
                Info.bundled(player, "commands.admin.team.teams");
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
        handler.<Player>register("team", bundle.get("commands.admin.team.params"), bundle.get("commands.admin.teamp.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
                return;
            }
            Team team = Structs.find(Team.all, t -> t.name.equalsIgnoreCase(args[0]));
            if(team == null){
                Info.bundled(player, "commands.admin.team.teams");
                return;
            }

            Player target = args.length > 1 ? Groups.player.find(p -> p.name.equalsIgnoreCase(args[1])) : player;
            if(target == null){
                Info.bundled(player, "commands.player-not-found");
                return;
            }

            Info.bundled(target, "commands.admin.team.success", team.name);
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

            TeamData team = state.teams.get(player.team());
            if(!team.hasCore()){
                Info.bundled(player, "commands.admin.give.core-not-found");
                return;
            }
            CoreBuild core = team.cores.first();

            for(int i = 0; i < count; i++){
                core.items.set(item, count);
            }
            Info.bundled(player, "commands.admin.give.success");
        });

        handler.<Player>register("tpp", "<x> <y>", "Teleport to coordinates.", (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
                return;
            }

            int x = Mathf.clamp(Strings.parseInt(args[0]), 0, world.width());
            int y = Mathf.clamp(Strings.parseInt(args[1]), 0, world.height());

            Call.setPosition(player.con, x * tilesize, y * tilesize);
        });

        handler.<Player>register("tp", "<target>", "Teleport to other players", (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
                return;
            }

            Player target = Groups.player.find(p -> Strings.canParseInt(args[0]) ? p.id() == Strings.parseInt(args[0]) : Objects.equals(p.uuid(), args[0]) || Objects.equals(p.con().address, args[0]));
            if(target == null){
                Info.bundled(player, "commands.player-not-found");
                return;
            }

            Call.setPosition(player.con, target.x(), target.y());
        });

        handler.<Player>register("tpa", "[target]", "Teleport to other players", (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
                return;
            }

            Player target = args.length > 0 ? Groups.player.find(p -> Strings.canParseInt(args[0]) ? p.id() == Strings.parseInt(args[0]) : Objects.equals(p.uuid(), args[0]) || Objects.equals(p.con().address, args[0])) : player;
            if(target == null){
                Info.bundled(player, "commands.player-not-found");
                return;
            }

            Groups.player.each(p -> Call.setPosition(p.con, target.x(), target.y()));
        });

        handler.<Player>register("maps", "[page]", "Lists all server maps.", (args, player) -> {
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }

            Seq<Map> mapList = maps.all();
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil(mapList.size / 6.0F);

            if(--page >= pages || page < 0){
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Strings.format("[orange]-- Server Maps Page[lightgray] @[gray]/[lightgray]@[orange] --\n", page + 1, pages));
            for(int i = 6 * page; i < Math.min(6 * (page + 1), mapList.size); i++){
                result.append("[lightgray] ").append(i + 1).append("[orange] ").append(mapList.get(i).name()).append("[white] ").append("\n");
            }
            player.sendMessage(result.toString());
        });

        handler.<Player>register("saves", "[page]", "Lists all server maps.", (args, player) -> {
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }

            Seq<Fi> saves = Seq.with(saveDirectory.list()).filter(f -> Objects.equals(f.extension(), saveExtension));
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil(saves.size / 6.0F);

            if(--page >= pages || page < 0){
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Strings.format("[orange]-- Server Saves Page[lightgray] @[gray]/[lightgray]@[orange] --\n", page + 1, pages));
            for(int i = 6 * page; i < Math.min(6 * (page + 1), saves.size); i++){
                result.append("[lightgray] ").append(i + 1).append("[orange] ").append(saves.get(i).nameWithoutExtension()).append("[white] ").append("\n");
            }
            player.sendMessage(result.toString());
        });

        handler.<Player>register("nominate", bundle.get("commands.nominate.params"), bundle.get("commands.nominate.description"), (args, player) -> {
            VoteMode mode;
            try{
                mode = VoteMode.valueOf(args[0].toLowerCase());
            }catch(Throwable t){
                Info.bundled(player, "commands.nominate.incorrect-mode");
                return;
            }

            if(current[0] != null){
                Info.bundled(player, "commands.nominate.already-started");
                return;
            }

            switch(mode){
                case map -> {
                    if(args.length == 1){
                        Info.bundled(player, "commands.nominate.required-second-arg");
                        return;
                    }

                    Map map = CommonUtil.findMap(args[1]);
                    if(map == null){
                        Info.bundled(player, "commands.nominate.map.not-found");
                        return;
                    }

                    VoteSession session = new VoteMapSession(current, map);
                    current[0] = session;
                    session.vote(player, 1);
                }
                case save -> {
                    if(args.length == 1){
                        Info.bundled(player, "commands.nominate.required-second-arg");
                        return;
                    }

                    VoteSession session = new VoteSaveSession(current, args[1]);
                    current[0] = session;
                    session.vote(player, 1);
                }
                case load -> {
                    if(args.length == 1){
                        Info.bundled(player, "commands.nominate.required-second-arg");
                        return;
                    }

                    Fi save = CommonUtil.findSave(args[1]);
                    if(save == null){
                        player.sendMessage("commands.nominate.load.not-found");
                        return;
                    }

                    VoteSession session = new VoteLoadSession(current, save);
                    current[0] = session;
                    session.vote(player, 1);
                }
            }
        });
    }
}
