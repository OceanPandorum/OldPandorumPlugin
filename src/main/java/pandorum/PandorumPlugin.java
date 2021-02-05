package pandorum;

import arc.Events;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.*;
import arc.struct.ObjectMap.Entry;
import arc.util.*;
import arc.util.io.Streams;
import com.google.gson.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.maps.Map;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;
import mindustry.world.*;
import pandorum.comp.*;
import pandorum.comp.Config.PluginType;
import pandorum.entry.*;
import pandorum.rest.*;
import pandorum.struct.*;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

import static mindustry.Vars.*;

@SuppressWarnings("unchecked")
public final class PandorumPlugin extends Plugin{
    public static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .disableHtmlEscaping()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    public static VoteSession[] current = {null};
    public static Config config;
    public static Bundle bundle;

    private final ObjectMap<Team, ObjectSet<String>> surrendered = new ObjectMap<>();
    private final ObjectSet<String> votes = new ObjectSet<>();                //
    private final ObjectSet<String> alertIgnores = new ObjectSet<>();         // Соединить
    private final ObjectSet<String> activeHistoryPlayers = new ObjectSet<>(); //
    private final Interval interval = new Interval(2);

    private CacheSeq<HistoryEntry>[][] history;
    private long delay;

    private Seq<IpInfo> forbiddenIps;
    private DateTimeFormatter shortFormatter = DateTimeFormatter.ofPattern("MM dd yyyy HH:mm:ss")
            .withLocale(Locale.forLanguageTag("ru"))
            .withZone(ZoneId.systemDefault());

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")
            .withLocale(Locale.forLanguageTag("ru"))
            .withZone(ZoneId.systemDefault());

    private ActionService actionService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public PandorumPlugin(){

        Fi cfg = dataDirectory.child("config.json");
        if(!cfg.exists()){
            cfg.writeString(gson.toJson(config = new Config()));
            Log.info("Config created...");
        }else{
            config = gson.fromJson(cfg.reader(), Config.class);
        }

        bundle = new Bundle();
    }

    @Override
    public void init(){
        Router router = new ForwardRouter();
        actionService = new ActionService(router);

        try{
            forbiddenIps = Seq.with(Streams.copyString(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("vpn-ipv4.txt"))).split(System.lineSeparator())).map(IpInfo::new);
        }catch(IOException e){
            throw new ArcRuntimeException(e);
        }

        netServer.admins.addActionFilter(action -> {
            if(action.type == Administration.ActionType.rotate){
                Building building = action.tile.build;
                CacheSeq<HistoryEntry> entries = history[action.tile.x][action.tile.y];
                HistoryEntry entry = new RotateEntry(Misc.colorizedName(action.player), building.block, action.rotation);
                entries.add(entry);
            }
            return true;
        });

        if(config.rest() && Administration.Config.debug.bool()){
            netServer.admins.addChatFilter((target, text) -> {
                AdminAction action = actionService.getAction(AdminActionType.mute, target.uuid());
                if(action != null){
                    target.sendMessage(bundle.format("events.mute", shortFormatter.format(action.endTimestamp()), action.reason().orElse(bundle.get("events.mute.reason.unknown"))));
                    return null;
                }

                return text;
            });
        }

        netServer.admins.addChatFilter((target, text) -> {
            String lower = text.toLowerCase();
            if(current[0] != null && (lower.equals("y") || lower.equals("n"))){
                if(current[0].voted().contains(target.uuid()) || current[0].voted().contains(netServer.admins.getInfo(target.uuid()).lastIP)){
                    Info.bundled(target, "commands.already-voted");
                    return null;
                }

                int sign = lower.equals("y") ? 1 : -1;
                current[0].vote(target, sign);
                return null;
            }
            return text;
        });

        // история

        Events.on(WorldLoadEvent.class, event -> {
            delay = Time.millis();
            history = new CacheSeq[world.width()][world.height()];

            for(Tile tile : world.tiles){
                history[tile.x][tile.y] = Seqs.newBuilder()
                        .maximumSize(config.historyLimit)
                        .expireAfterWrite(Duration.ofMillis(config.expireDelay))
                        .build();
            }
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            HistoryEntry historyEntry = new BlockEntry(event);

            Seq<Tile> linkedTile = event.tile.getLinkedTiles(new Seq<>());
            for(Tile tile : linkedTile){
                history[tile.x][tile.y].add(historyEntry);
            }
        });

        Events.on(ConfigEvent.class, event -> {
            if(event.player == null || event.tile.tileX() > world.width() || event.tile.tileX() > world.height()){
                return;
            }

            Log.debug("@ > @", event.tile.block, event.value instanceof byte[] ? Arrays.toString((byte[])event.value) : event.value);
            CacheSeq<HistoryEntry> entries = history[event.tile.tileX()][event.tile.tileY()];
            boolean connect = true;

            HistoryEntry last = entries.peek();
            if(!entries.isEmpty() && last instanceof ConfigEntry){
                ConfigEntry lastConfigEntry = (ConfigEntry)last;

                connect = !event.tile.getPowerConnections(new Seq<>()).isEmpty() &&
                          !(lastConfigEntry.value instanceof Integer && event.value instanceof Integer &&
                          (int)lastConfigEntry.value == (int)event.value && lastConfigEntry.connect);
            }

            HistoryEntry entry = new ConfigEntry(event, connect);

            Seq<Tile> linkedTile = event.tile.tile.getLinkedTiles(new Seq<>());
            for(Tile tile : linkedTile){
                history[tile.x][tile.y].add(entry);
            }
        });

        Events.on(TapEvent.class, event -> {
            if(activeHistoryPlayers.contains(event.player.uuid())){
                CacheSeq<HistoryEntry> entries = history[event.tile.x][event.tile.y];

                StringBuilder message = new StringBuilder(bundle.format("events.history.title", event.tile.x, event.tile.y));

                entries.cleanUp();
                if(entries.isOverflown()){
                    message.append(bundle.get("events.history.overflow"));
                }

                int i = 0;
                for(HistoryEntry entry : entries){
                    message.append("\n").append(entry.getMessage());
                    if(++i > 6){
                        break;
                    }
                }

                if(entries.isEmpty()){
                    message.append(bundle.get("events.history.empty"));
                }

                event.player.sendMessage(message.toString());
            }
        });

        Events.on(PlayerLeave.class, event -> activeHistoryPlayers.remove(event.player.uuid()));

        //

        Events.on(PlayerJoin.class, event -> forbiddenIps.each(i -> i.matchIp(event.player.con.address), i -> event.player.con.kick(bundle.get("events.vpn-ip"))));

        Events.on(PlayerConnect.class, event -> {
            Player player = event.player;
            if(config.bannedNames.contains(player.name())){
                player.con.kick(bundle.get("events.unofficial-mindustry"), 60000);
            }

            if(config.rest()){
                executor.submit(() -> {
                    AdminAction action = actionService.getAction(AdminActionType.ban, player.uuid());
                    if(action != null && action.endTimestamp() != null && !Instant.now().isAfter(action.endTimestamp())){
                        action.reason().ifPresentOrElse(player::kick, () -> player.kick(KickReason.banned));
                    }
                });
            }
        });

        Events.on(DepositEvent.class, event -> {
            Building building = event.tile;
            Player target = event.player;
            if(building.block() == Blocks.thoriumReactor && event.item == Items.thorium && target.team().cores().contains(c -> event.tile.dst(c.x, c.y) < config.alertDistance)){
                Groups.player.each(p -> !alertIgnores.contains(p.uuid()), player -> player.sendMessage(bundle.format("events.withdraw-thorium", Misc.colorizedName(target), building.tileX(), building.tileY())));
            }
        });

        Events.on(BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildPlan() != null &&
               event.builder.buildPlan().block == Blocks.thoriumReactor && event.builder.isPlayer() &&
               event.team.cores().contains(c -> event.tile.dst(c.x, c.y) < config.alertDistance)){
                Player target = event.builder.getPlayer();

                if(interval.get(300)){
                    Groups.player.each(p -> !alertIgnores.contains(p.uuid()), p -> p.sendMessage(bundle.format("events.alert", target.name, event.tile.x, event.tile.y)));
                }
            }
        });

        Events.on(PlayerLeave.class, event -> {
            int cur = votes.size;
            int req = (int)Math.ceil(config.voteRatio * Groups.player.size());
            if(votes.contains(event.player.uuid())){
                votes.remove(event.player.uuid());
                Call.sendMessage(bundle.format("commands.rtv.left", Misc.colorizedName(event.player), cur - 1, req));
            }
        });

        Events.on(GameOverEvent.class, __ -> votes.clear());

        if(config.type == PluginType.pvp){
            Events.on(PlayerLeave.class, event -> {
                String uuid = event.player.uuid();
                ObjectSet<String> uuids = surrendered.get(event.player.team(), ObjectSet::new);
                if(uuids.contains(uuid)){
                    uuids.remove(uuid);
                }
            });

            Events.on(GameOverEvent.class, __ -> surrendered.clear());
        }

        Events.run(Trigger.update, () -> {
            if(interval.get(1, 60 * 60 * 15) && state.isPlaying()){
                Call.infoPopup(bundle.format("misc.delay", TimeUnit.MILLISECONDS.toMinutes(Time.timeSinceMillis(delay))), 3f, 20, 50, 20, 450, 0);
            }
        });

        if(config.rest()){
            scheduler.scheduleAtFixedRate(() -> {
                actionService.getAllActions(AdminActionType.ban).forEach(action -> {
                    if(action.endTimestamp() != null && Instant.now().isAfter(action.endTimestamp())){
                        netServer.admins.unbanPlayerID(action.targetId());
                        Log.info("Unbanned: @", action.targetId());
                        actionService.delete(AdminActionType.ban, action.targetId());
                    }
                });
            }, 10, 1800, TimeUnit.SECONDS);
        }
    }

    @Override
    public void registerServerCommands(CommandHandler handler){

        handler.register("reload-config", "reload configuration", args -> {
            config = gson.fromJson(dataDirectory.child("config.json").readString(), Config.class);
            Log.info("Reloaded");
        });

        handler.register("tell", bundle.get("commands.tell.params"), bundle.get("commands.tell.description"), args -> {
            Player target = Groups.player.find(p -> p.name().equalsIgnoreCase(args[0]) || p.uuid().equalsIgnoreCase(args[0]));
            if(target == null){
                Log.info(bundle.get("commands.tell.player-not-found"));
                return;
            }

            target.sendMessage("[scarlet][[Server]:[] " + args[1]);
            Log.info(bundle.format("commands.tell.log", target.name(), args[1]));
        });

        handler.register("despw", bundle.get("commands.despw.description"), args -> {
            Groups.unit.each(Unit::kill);
            Log.info(bundle.get("commands.despw.log"));
        });

        if(config.rest()){
            handler.register("ban-sync", bundle.get("commands.ban-sync.description"), args -> {
                executor.submit(() -> {
                    int pre = netServer.admins.getBanned().size;
                    actionService.getAllActions(AdminActionType.ban).forEach(action -> netServer.admins.banPlayer(action.targetId()));
                    Log.info(bundle.format("commands.ban-sync.count", netServer.admins.getBanned().size - pre));
                });
            });
        }

        handler.register("kicks", bundle.get("commands.kicks.description"), args -> {
            Log.info("Kicks: @", netServer.admins.kickedIPs.isEmpty() ? "<none>" : "");
            for(Entry<String, Long> e : netServer.admins.kickedIPs){
                PlayerInfo info = netServer.admins.findByIPs(e.key).first();
                Log.info("  @ / ID: '@' / IP: '@' / END: @",
                         info.lastName, info.id, info.lastIP,
                         formatter.format(Instant.ofEpochMilli(e.value).atZone(ZoneId.systemDefault())));
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

        handler.<Player>register("history", bundle.get("commands.history.params"), bundle.get("commands.history.description"), (args, player) -> {
            String uuid = player.uuid();
            if(args.length > 0 && activeHistoryPlayers.contains(uuid)){
                if(!Strings.canParseInt(args[0]) && !Misc.bool(args[0])){
                    Info.bundled(player, "commands.page-not-int");
                    return;
                }

                boolean forward = !Strings.canParseInt(args[0]) ? Misc.bool(args[0]) : args.length > 1 && Misc.bool(args[1]);
                int mouseX = Mathf.clamp(Mathf.round(player.mouseX / 8), 1, world.width());
                int mouseY = Mathf.clamp(Mathf.round(player.mouseY / 8), 1, world.height());
                CacheSeq<HistoryEntry> entries = history[mouseX][mouseY];
                int page = Strings.canParseInt(args[0]) ? Strings.parseInt(args[0]) : 1;
                int pages = Mathf.ceil((float)entries.size / 6);

                page--;

                if((page >= pages || page < 0) && !entries.isEmpty()){
                    Info.bundled(player, "commands.under-page", pages);
                    return;
                }

                StringBuilder result = new StringBuilder();
                result.append(bundle.format("commands.history.page", mouseX, mouseY, page + 1, pages)).append("\n");
                if(entries.isEmpty()){
                    result.append("events.history.empty");
                }

                for(int i = 6 * page; i < Math.min(6 * (page + 1), entries.size); i++){
                    HistoryEntry e = entries.get(i);

                    result.append(e.getMessage());
                    if(forward){
                        result.append(bundle.format("events.history.last-access-time", e.getLastAccessTime(TimeUnit.SECONDS)));
                    }

                    result.append("\n");
                }

                player.sendMessage(result.toString());
            }else if(activeHistoryPlayers.contains(uuid)){
                activeHistoryPlayers.remove(uuid);
                Info.bundled(player, "commands.history.off");
            }else{
                activeHistoryPlayers.add(uuid);
                Info.bundled(player, "commands.history.on");
            }
        });

        if(config.rest()){
            handler.<Player>register("ban", bundle.get("commands.admin.ban.params"), bundle.get("commands.admin.ban.description"), (args, player) -> {
                if(!player.admin){
                    Info.bundled(player, "commands.permission-denied");
                    return;
                }

                if(!Strings.canParseInt(args[0])){
                    Info.bundled(player, "commands.id-not-int");
                    return;
                }

                int id = Strings.parseInt(args[0]);
                Player target = Groups.player.find(p -> p.id() == id);
                if(target == null){
                    Info.bundled(player, "commands.player-not-found");
                    return;
                }

                Instant delay = Misc.parseTime(args[1]);
                if(delay == null){
                    Info.bundled(player, "commands.admin.delay-not-int");
                    return;
                }

                if(Objects.equals(target, player) || target.admin()){
                    Info.bundled(player, "commands.not-allowed-target");
                    return;
                }

                Optional<String> reason = args.length > 2 ? Optional.ofNullable(args[2]) : Optional.empty();

                AdminAction action = new AdminAction();
                action.targetId(target.uuid());
                action.adminId(player.uuid());
                action.type(AdminActionType.ban);
                reason.ifPresent(action::reason);
                action.timestamp(Instant.now());
                action.endTimestamp(delay);

                actionService.save(action);
                if(netServer.admins.banPlayer(target.uuid())){
                    reason.ifPresentOrElse(target::kick, () -> target.kick(KickReason.banned));
                }
            });

            handler.<Player>register("mute", bundle.get("commands.admin.ban.params"), bundle.get("commands.admin.mute.description"), (args, player) -> {
                if(!player.admin){
                    Info.bundled(player, "commands.permission-denied");
                    return;
                }

                if(!Strings.canParseInt(args[0])){
                    Info.bundled(player, "commands.id-not-int");
                    return;
                }

                Instant delay = Misc.parseTime(args[1]);
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

                Optional<String> reason = args.length > 2 ? Optional.ofNullable(args[2]) : Optional.empty();

                AdminAction action = new AdminAction();
                action.targetId(target.uuid());
                action.adminId(player.uuid());
                action.type(AdminActionType.mute);
                reason.ifPresent(action::reason);
                action.timestamp(Instant.now());
                action.endTimestamp(delay);

                executor.submit(() -> actionService.save(action));
                Call.sendMessage(bundle.format("commands.admin.mute.text", Misc.colorizedName(target)));
             });

            handler.<Player>register("unmute", bundle.get("commands.admin.unmute.params"), bundle.get("commands.admin.unmute.description"), (args, player) -> {
                if(!player.admin){
                    Info.bundled(player, "commands.permission-denied");
                    return;
                }

                AdminAction action = actionService.getAction(AdminActionType.mute, args[0]);
                if(action != null){
                    actionService.delete(AdminActionType.mute, action.targetId());
                    Info.bundled(player, "commands.admin.unmute.successful");
                }else{
                    Info.bundled(player, "commands.admin.unban.not-banned");
                }
            });

            handler.<Player>register("unban", bundle.get("commands.admin.unban.params"), bundle.get("commands.admin.unban.description"), (args, player) -> {
                if(!player.admin){
                    Info.bundled(player, "commands.permission-denied");
                    return;
                }

                if(netServer.admins.unbanPlayerID(args[0]) || netServer.admins.unbanPlayerIP(args[0])){
                    PlayerInfo target = Optional.ofNullable(netServer.admins.findByIP(args[0])).orElse(netServer.admins.getInfo(args[0]));
                    actionService.delete(AdminActionType.ban, target.id);
                    Info.bundled(player, "commands.admin.unban.successful");
                }else{
                    Info.bundled(player, "commands.admin.unban.not-banned");
                }
            });
        }

        if(config.type == PluginType.pvp){
            handler.<Player>register("france", bundle.get("commands.surrender.description"), (args, player) -> {
                String uuid = player.uuid();
                Team team = player.team();
                ObjectSet<String> uuids = surrendered.get(team, ObjectSet::new);
                if(uuids.contains(uuid)){
                    Info.bundled(player, "commands.already-voted");
                    return;
                }

                uuids.add(uuid);
                surrendered.put(team, uuids);
                int cur = uuids.size;
                int req = (int)Math.ceil(config.voteRatio * Groups.player.count(p -> p.team() == team));
                Call.sendMessage(bundle.format("commands.surrender.ok",
                                               Misc.colorizedTeam(team),
                                               Misc.colorizedName(player), cur, req));

                if(cur < req){
                    return;
                }

                surrendered.remove(team);
                Call.sendMessage(bundle.format("commands.surrender.successful", Misc.colorizedTeam(team)));
                Groups.unit.each(u -> u.team == team, u -> Time.run(Mathf.random(360), u::kill));
                for(Tile tile : world.tiles){
                    if(tile.build != null && tile.team() == team){
                        Time.run(Mathf.random(360), tile.build::kill);
                    }
                }
            });
        }

        handler.<Player>register("pl", bundle.get("commands.pl.params"), bundle.get("commands.pl.description"), (args, player) -> {
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                Info.bundled(player, "commands.page-not-int");
                return;
            }

            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil((float)Groups.player.size() / 6);

            page--;

            if(page >= pages || page < 0){
                Info.bundled(player, "commands.under-page", pages);
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

        handler.<Player>register("rtv", bundle.get("commands.rtv.description"), (args, player) -> {
            if(votes.contains(player.uuid())){
                Info.bundled(player, "commands.already-voted");
                return;
            }

            votes.add(player.uuid());
            int cur = votes.size;
            int req = (int)Math.ceil(config.voteRatio * Groups.player.size());
            Call.sendMessage(bundle.format("commands.rtv.ok", Misc.colorizedName(player), cur, req));

            if(cur < req){
                return;
            }

            Call.sendMessage(bundle.get("commands.rtv.successful"));
            Events.fire(new GameOverEvent(Team.crux));
        });

        // handler.<Player>register("bc", bundle.get("commands.admin.bc.params"), bundle.get("commands.admin.bc.description"), (args, player) -> {
        //     if(!player.admin){
        //         Info.bundled(player, "commands.permission-denied");
        //     }else{
        //         Player target = Strings.canParseInt(args[0]) ? Groups.player.find(p -> p.id == Strings.parseInt(args[0])) : null;
        //         String text = Strings.format("\uE805@\uE805\n\n@\n", bundle.get("commands.admin.bc.text"), args[1]);
        //
        //         if(target != null){
        //             Call.infoMessage(target.con, text);
        //         }else{
        //             Call.infoMessage(text);
        //         }
        //     }
        // });

        handler.<Player>register("go", bundle.get("commands.admin.go.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
            }else{
                Events.fire(new GameOverEvent(Team.crux));
            }
        });

        // handler.<Player>register("spawn", bundle.get("commands.admin.spawn.params"), bundle.get("commands.admin.spawn.description"), (args, player) -> {
        //     if(!player.admin){
        //         Info.bundled(player, "commands.permission-denied");
        //         return;
        //     }
        //
        //     if(args.length > 1 && !Strings.canParseInt(args[1])){
        //         Info.bundled(player, "commands.count-not-int");
        //         return;
        //     }
        //
        //     UnitType unit = content.units().find(b -> b.name.equalsIgnoreCase(args[0]));
        //     if(unit == null){
        //         Info.bundled(player, "commands.admin.spawn.units");
        //         return;
        //     }
        //
        //     int count = args.length > 1 ? Strings.parseInt(args[1]) : 1;
        //
        //     Team team = args.length > 2 ? Structs.find(Team.baseTeams, t -> t.name.equalsIgnoreCase(args[2])) : player.team();
        //     if(team == null){
        //         Info.bundled(player, "commands.admin.team.teams");
        //         return;
        //     }
        //
        //     for(int i = 0; i < count; i++){
        //         unit.spawn(team, player.x, player.y);
        //     }
        //
        //     Info.bundled(player, "commands.admin.spawn.success", count, unit.name);
        //     if(unit.equals(UnitTypes.oct) || unit.equals(UnitTypes.horizon) || unit.equals(UnitTypes.quad)){
        //         Call.sendMessage("[scarlet]KIROV REPORTING");
        //     }
        // });

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

        handler.<Player>register("hub", bundle.get("commands.hub.description"), (args, player) -> Call.connect(player.con, config.hubIp, config.hubPort));

        handler.<Player>register("team", bundle.get("commands.admin.team.params"), bundle.get("commands.admin.teamp.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
                return;
            }

            Team team = Structs.find(Team.all, t -> t.name.toLowerCase().equals(args[0].toLowerCase()));
            if(team == null){
                Info.bundled(player, "commands.admin.team.teams");
                return;
            }

            Player target = args.length > 1 ? Groups.player.find(p -> Strings.stripColors(p.name).toLowerCase().equals(args[1].toLowerCase())) : player;
            if(target == null){
                Info.bundled(player, "commands.player-not-found");
                return;
            }

            Info.bundled(target, "commands.admin.team.success", team.name);
            target.team(team);
        });

        handler.<Player>register("s", bundle.get("commands.admin.vanish.description"), (args, player) -> {
            if(!player.admin){
                Info.bundled(player, "commands.permission-denied");
            }else{
                player.clearUnit();
                player.team(player.team() == Team.derelict ? Team.sharded : Team.derelict);
            }
        });

        // handler.<Player>register("give", bundle.get("commands.admin.give.params"), bundle.get("commands.admin.give.description"), (args, player) -> {
        //     if(!player.admin){
        //         Info.bundled(player, "commands.permission-denied");
        //         return;
        //     }
        //
        //     if(!Strings.canParseInt(args[0])){
        //         Info.bundled(player, "commands.count-not-int");
        //         return;
        //     }
        //
        //     int count = Strings.parseInt(args[0]);
        //
        //     Item item = content.items().find(b -> b.name.equalsIgnoreCase(args[1]));
        //     if(item == null){
        //         Info.bundled(player, "commands.admin.give.item-not-found");
        //         return;
        //     }
        //
        //     TeamData team = state.teams.get(player.team());
        //     if(!team.hasCore()){
        //         Info.bundled(player, "commands.admin.give.core-not-found");
        //         return;
        //     }
        //
        //     CoreBuild core = team.cores.first();
        //
        //     for(int i = 0; i < count; i++){
        //         core.items.set(item, count);
        //     }
        //
        //     Info.bundled(player, "commands.admin.give.success");
        // });

        // handler.<Player>register("tp", "<x> <y>", bundle.get("commands.admin.tp.description"), (args, player) -> {
        //     if(!player.admin){
        //         Info.bundled(player, "commands.permission-denied");
        //         return;
        //     }
        //
        //     int x = Mathf.clamp(Strings.parseInt(args[0]), 0, world.width()) * tilesize;
        //     int y = Mathf.clamp(Strings.parseInt(args[1]), 0, world.height()) * tilesize;
        //
        //     Call.setPosition(player.con, x, y);
        // });

        // handler.<Player>register("tpp", bundle.get("commands.admin.tpp.params"), bundle.get("commands.admin.tpp.description"), (args, player) -> {
        //     if(!player.admin){
        //         Info.bundled(player, "commands.permission-denied");
        //         return;
        //     }
        //
        //     if(!Strings.canParseInt(args[0])){
        //         Info.bundled(player, "commands.id-not-int");
        //         return;
        //     }
        //
        //     int id = Strings.parseInt(args[0]);
        //     Player target = Groups.player.find(p -> p.id() == id);
        //     if(target == null){
        //         Info.bundled(player, "commands.player-not-found");
        //         return;
        //     }
        //
        //     Call.setPosition(player.con, target.x(), target.y());
        // });

        // handler.<Player>register("tpa", bundle.get("commands.admin.tpa.params"), bundle.get("commands.admin.tpa.description"), (args, player) -> {
        //     if(!player.admin){
        //         Info.bundled(player, "commands.permission-denied");
        //         return;
        //     }
        //
        //     if(args.length > 0 && !Strings.canParseInt(args[0])){
        //         Info.bundled(player, "commands.id-not-int");
        //         return;
        //     }
        //
        //     Player target = args.length > 0 ? Groups.player.find(p -> p.id() == Strings.parseInt(args[0])) : player;
        //     if(target == null){
        //         Info.bundled(player, "commands.player-not-found");
        //         return;
        //     }
        //
        //     Groups.player.each(p -> Call.setPosition(p.con, target.x(), target.y()));
        // });

        handler.<Player>register("maps", bundle.get("commands.maps.params"), bundle.get("commands.maps.description"), (args, player) -> {
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                Info.bundled(player, "commands.page-not-int");
                return;
            }

            Seq<Map> mapList = maps.all();
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil(mapList.size / 6.0F);

            if(--page >= pages || page < 0){
                Info.bundled(player, "commands.under-page", pages);
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(bundle.format("commands.maps.page", page + 1, pages)).append("\n");
            for(int i = 6 * page; i < Math.min(6 * (page + 1), mapList.size); i++){
                result.append("[lightgray] ").append(i + 1).append("[orange] ").append(mapList.get(i).name()).append("[white] ").append("\n");
            }

            player.sendMessage(result.toString());
        });

        handler.<Player>register("saves", bundle.get("commands.saves.params"), bundle.get("commands.saves.description"), (args, player) -> {
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                Info.bundled(player, "commands.page-not-int");
                return;
            }

            Seq<Fi> saves = Seq.with(saveDirectory.list()).filter(f -> Objects.equals(f.extension(), saveExtension));
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil(saves.size / 6.0F);

            if(--page >= pages || page < 0){
                Info.bundled(player, "commands.under-page", pages);
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(bundle.format("commands.saves.page", page + 1, pages)).append("\n");
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

                    Map map = Misc.findMap(args[1]);
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

                    Fi save = Misc.findSave(args[1]);
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

        handler.<Player>register("playerinfo", "<name/ip/id...>", bundle.get("commands.playerinfo.desc"), (arg, player) -> {
            ObjectSet<Administration.PlayerInfo> infos = netServer.admins.findByName(arg[0]);
            if (infos.size > 0) {
                Log.info("Players found: @", infos.size);
                int i = 0;
                for(PlayerInfo playerInfo : infos){
                    StringBuilder result = new StringBuilder();
                    result.append(Strings.format("[@] @ '@' / UUID @", i++, bundle.get("commands.playerinfo.header"), playerInfo.lastName, playerInfo.id));
                    result.append(Strings.format("  @: @", bundle.get("commands.playerinfo.names"), playerInfo.names));
                    if(player.admin){
                        result.append("  IP: ").append(playerInfo.lastIP);
                        result.append(Strings.format("  IPs : @", playerInfo.ips));
                    }
                    result.append("  ").append(bundle.get("commands.playerinfo.joined")).append(": ").append(playerInfo.timesJoined);
                    result.append("  ").append(bundle.get("commands.playerinfo.kicked")).append(": ").append(playerInfo.timesKicked);
                    Call.infoMessage(player.con(), result.toString());
                }
            } else {
                Log.info(bundle.get("commands.player-not-found"));
            }
        });

        handler.<Player>register("judgelight", "<ip/id> <value>", bundle.get("commands.judgelight.desc"), (arg, player) -> {
            if(!player.admin){
                Call.infoMessage(player.con,bundle.get("commands.permission-denied"));
                return;
            }

            String type = arg[0].toLowerCase(); // игнорируем регистер
            switch(type){
                case "id" -> {
                    netServer.admins.banPlayerID(arg[1]);
                    Call.infoMessage(player.con,bundle.get("commands.judgelight.ban"));
                }
                case "ip" -> {
                    netServer.admins.banPlayerIP(arg[1]);
                    Call.infoMessage(player.con,bundle.get("commands.judgelight.ban"));
                }
                default -> Call.infoMessage(player.con,bundle.get("commands.judgelight.type-err"));
            }
        });
    }
}
