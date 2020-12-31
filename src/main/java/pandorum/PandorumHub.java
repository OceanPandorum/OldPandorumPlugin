package pandorum;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Func;
import arc.math.Mathf;
import arc.util.*;
import arc.util.io.Streams;
import com.google.gson.*;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.*;
import mindustry.world.Tile;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static mindustry.Vars.*;
import static mindustry.game.EventType.PlayerJoin;
import static mindustry.game.EventType.ServerLoadEvent;

public class PandorumHub extends Plugin{
    public static Config config;

    private final AtomicInteger allPlayers = new AtomicInteger();
    private final Func<Host, String> formatter = h -> Strings.format("\uE837 [accent]Online @", h.players);
    private final String offline = "[red]Offline";

    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .setPrettyPrinting()
            .create();

    public static ExecutorService executor = Executors.newCachedThreadPool();

    public PandorumHub(){
        try{
            Streams.copy(Objects.requireNonNull(PandorumHub.class.getClassLoader().getResourceAsStream("hub-0.2.msav")),
                         customMapDirectory.child("hub-0.2.msav").write(false));
        }catch(IOException e){
            Log.err(e);
        }

        Fi cfg = dataDirectory.child("config-hub.json");
        if(!cfg.exists()){
            config = new Config();
            cfg.writeString(gson.toJson(config));
        }
        config = gson.fromJson(cfg.reader(), Config.class);
    }

    public static void teleport(Player player, Tile tile){
        for(HostData h : config.servers){
            if(h.teleport(tile != null ? tile.x : player.tileX(), tile != null ? tile.y : player.tileY())){
                net.pingHost(h.ip, h.port, host -> Call.connect(player.con, h.ip, h.port), e -> {});
            }
        }
    }

    @Override
    public void init(){

        Events.on(ServerLoadEvent.class, event -> netServer.admins.addActionFilter(playerAction -> false));

        Events.on(TapEvent.class, event -> teleport(event.player, event.tile));

        Events.on(PlayerJoin.class, event -> {
            executor.submit(new Updater(event.player));
            NetConnection con = event.player.con();

            for(HostData h : config.servers){
                Call.label(con, h.title, 1100f, h.titleX, h.titleY);
                net.pingHost(h.ip, h.port, host -> {
                    Call.label(con, formatter.get(host), 10, h.labelX, h.labelY);
                }, e -> Call.label(con, offline, 10, h.labelX, h.labelY));
            }
        });

        // tile x or y * 8 = coordinate
        Timer.schedule(() -> {
            for(HostData h : config.servers){
                net.pingHost(h.ip, h.port, host -> {
                    allPlayers.addAndGet(host.players);
                    Call.label(formatter.get(host), 10, h.labelX, h.labelY);
                }, e -> Call.label(offline, 10, h.labelX, h.labelY));
            }

            Timer.schedule(() -> {
                Log.debug("all: @", allPlayers.get());
                Core.settings.put("totalPlayers", allPlayers.get());
                allPlayers.set(0);
            }, 3);
        }, 3, 10);
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("stat", "Debug command", args -> {
            Log.info("threads: @", Thread.activeCount());
            Log.info("players: @", Core.settings.getInt("totalPlayers"));
        });

        handler.register("reload-cfg", "Reload config.", args -> {
            config = gson.fromJson(dataDirectory.child("config-hub.json").readString(), Config.class);
            Log.info("Reloaded");
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("bc", "<all/player> <text...>", "Broad cast", (args, player) -> {
            if(player.admin){
                if(Strings.canParseInt(args[0])){
                    int id = Strings.parseInt(args[0]);
                    Player target = Groups.player.find(p -> p.id() == id);
                    if(target == null){
                        player.sendMessage("[scarlet]Player not found");
                        return;
                    }
                    Call.infoToast(target.con(), args[1], 15);
                }else{
                    Call.infoToast(args[1], 15);
                }
            }else{
                player.sendMessage("[scarlet]You must be admin to use this command.");
            }
        });

        handler.<Player>register("pl", "[page]", "Player list.", (args, player) -> {
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }

            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil((float)Groups.player.size() / 6);

            page--;

            if(page >= pages || page < 0){
                player.sendMessage(Strings.format("[scarlet]'page' must be a number between[orange] 1[] and[orange] @[scarlet].", pages));
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Strings.format("[orange]-- Player List Page[lightgray] @[gray]/[lightgray]@[orange] --\n", (page + 1), pages));

            for(int i = 6 * page; i < Math.min(6 * (page + 1), Groups.player.size()); i++){
                Player t = Groups.player.index(i);
                result.append("[lightgray]* ").append(t.name).append(" [lightgray]/ ID: ").append(t.id());

                if(player.admin){
                    result.append(" / raw: ").append(t.name.replaceAll("\\[", "[[")).append("\n");
                }
            }
            player.sendMessage(result.toString());
        });
    }
}
