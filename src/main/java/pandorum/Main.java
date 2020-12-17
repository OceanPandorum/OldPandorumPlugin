package pandorum;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Func;
import arc.math.Mathf;
import arc.util.*;
import arc.util.io.Streams;
import mindustry.Vars;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.*;
import mindustry.world.Tile;
import org.hjson.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static mindustry.Vars.*;
import static mindustry.game.EventType.PlayerJoin;
import static mindustry.game.EventType.ServerLoadEvent;

public class Main extends Plugin{
    public static final Teleport[] teleports = new Teleport[]{
            new Teleport(11, 35),
            new Teleport(23, 39),
            new Teleport(35, 35),
            new Teleport(35, 11),
            new Teleport(23, 7),
            new Teleport(11, 11)
    };
    public static final Fi dir = Core.settings.getDataDirectory().child("/mods/pandorum/");
    public static final Config config = new Config();

    private final AtomicInteger allPlayers = new AtomicInteger();
    private final Func<Host, String> formatter = h -> Strings.format("\uE837 [accent]Online @", h.players);
    private final String offline = "[red]Offline";

    public static ExecutorService executor = Executors.newCachedThreadPool();

    public static void teleport(Player player, Tile tile){
        for(int i = 0; i < teleports.length; i++){
            if(teleports[i].valid(tile != null ? tile.x : player.tileX(), tile != null ? tile.y : player.tileY())){
                int finalI = i + 1;
                String ip = config.get("ip", finalI).asString();
                int port = config.get("port", finalI).asInt();
                net.pingHost(ip, port, host -> Call.connect(player.con, ip, port), e -> {});
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

            Call.label(con, config.get("title", 1).asString(), 1100f, 96,312);
            Call.label(con, config.get("title", 2).asString(), 1100f, 192, 344);
            Call.label(con, config.get("title", 3).asString(), 1100f, 288, 312);
            Call.label(con, config.get("title", 4).asString(), 1100f, 288, 120);
            Call.label(con, config.get("title", 5).asString(), 1100f, 192, 88);
            Call.label(con, config.get("title", 6).asString(), 1100f, 96, 120);

            //todo надо бы хранить координаты в конфиг
            net.pingHost(config.get("ip", 1).asString(), config.get("port", 1).asInt(), host -> {
                Call.label(con, formatter.get(host), 10, 96,272);
            }, e -> Call.label(con, offline, 3, 96,272));

            net.pingHost(config.get("ip", 2).asString(), config.get("port", 2).asInt(), host -> {
                Call.label(con, formatter.get(host), 10, 192, 304);
            }, e -> Call.label(con, offline, 3, 192, 304));

            net.pingHost(config.get("ip", 3).asString(), config.get("port", 3).asInt(), host -> {
                Call.label(con, formatter.get(host), 10, 288, 272);
            }, e -> Call.label(con, offline, 3, 288, 272));

            net.pingHost(config.get("ip", 4).asString(), config.get("port", 4).asInt(), host -> {
                Call.label(con, formatter.get(host), 10, 288, 80);
            }, e -> Call.label(con, offline, 3, 288, 80));

            net.pingHost(config.get("ip", 5).asString(), config.get("port", 5).asInt(), host -> {
                Call.label(con, formatter.get(host), 10, 192, 48);
            }, e -> Call.label(con, offline, 3, 192, 48));

            net.pingHost(config.get("ip", 6).asString(), config.get("port", 6).asInt(), host -> {
                Call.label(con, formatter.get(host), 10, 96, 80);
            }, e -> Call.label(con, offline, 3, 96, 80));
        });

        // tile x or y * 8 = coordinate
        Timer.schedule(() -> {
            net.pingHost(config.get("ip", 1).asString(), config.get("port", 1).asInt(), host -> {
                allPlayers.addAndGet(host.players);
                Call.label(formatter.get(host), 10, 96,272);
            }, e -> Call.label(offline, 10, 96,272));

            net.pingHost(config.get("ip", 2).asString(), config.get("port", 2).asInt(), host -> {
                allPlayers.addAndGet(host.players);
                Call.label(formatter.get(host), 10, 192, 304);
            }, e -> Call.label(offline, 10, 192, 304));

            net.pingHost(config.get("ip", 3).asString(), config.get("port", 3).asInt(), host -> {
                allPlayers.addAndGet(host.players);
                Call.label(formatter.get(host), 10, 288, 272);
            }, e -> Call.label(offline, 10, 288, 272));

            net.pingHost(config.get("ip", 4).asString(), config.get("port", 4).asInt(), host -> {
                allPlayers.addAndGet(host.players);
                Call.label(formatter.get(host), 10, 288, 80);
            }, e -> Call.label( offline, 10, 288, 80));

            net.pingHost(config.get("ip", 5).asString(), config.get("port", 5).asInt(), host -> {
                allPlayers.addAndGet(host.players);
                Call.label(formatter.get(host), 10, 192, 48);
            }, e -> Call.label(offline, 10, 192, 48));

            net.pingHost(config.get("ip", 6).asString(), config.get("port", 6).asInt(), host -> {
                allPlayers.addAndGet(host.players);
                Call.label(formatter.get(host), 10, 96, 80);
            }, e -> Call.label(offline, 10, 96, 80));

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
                    Call.infoToast(target.con(), args[0], 15);
                }else if(args[0].toLowerCase().equals("all")){
                    Call.infoToast(args[0], 15);
                }else{
                    player.sendMessage("[scarlet]Incorrect first argument");
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

    static class Config{
        private JsonObject object;

        public Config(){
            load();
        }

        private void write(){
            object = new JsonObject();
            object.add("server1", new JsonObject().add("ip", "pandorum.su").add("port", 9000).add("title", "<text>"));
            object.add("server2", new JsonObject().add("ip", "pandorum.su").add("port", 1000).add("title", "<text>"));
            object.add("server3", new JsonObject().add("ip", "pandorum.su").add("port", 7000).add("title", "<text>"));
            object.add("server4", new JsonObject().add("ip", "pandorum.su").add("port", 2000).add("title", "<text>"));
            object.add("server5", new JsonObject().add("ip", "pandorum.su").add("port", 3000).add("title", "<text>"));
            object.add("server6", new JsonObject().add("ip", "pandorum.su").add("port", 4000).add("title", "<text>"));

            dir.child("config.hjson").writeString(object.toString(Stringify.HJSON), false);
            try{
                Streams.copy(Main.class.getClassLoader().getResourceAsStream("hub-0.2.msav"),
                             customMapDirectory.child("hub-0.2.msav").write(false));
            }catch(IOException e){
                Log.err(e);
            }
        }

        private void load(){
            try{
                object = JsonValue.readHjson(dir.child("config.hjson").readString()).asObject();
            }catch(Exception e){
                write();
            }
        }

        public JsonValue get(String name, int server){
            load();
            return object.get("server" + server).asObject().get(name);
        }
    }
}
