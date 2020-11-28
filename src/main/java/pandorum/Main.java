package pandorum;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.io.Streams;
import mindustry.Vars;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.world.Tile;
import org.hjson.*;
import java.io.IOException;

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

    public static void teleport(Player player, Tile tile){
        for(int i = 0; i < teleports.length; i++){
            if(teleports[i].valid(tile != null ? tile.x : player.tileX(), tile != null ? tile.y : player.tileY())){
                int finalI = i + 1;
                Vars.net.pingHost(config.get("ip", finalI).asString(), config.get("port", finalI).asInt(), host -> Call.connect(player.con, config.get("ip", finalI).asString(), config.get("port", finalI).asInt()), e -> {});
            }
        }
    }

    @Override
    public void init(){
        Events.on(ServerLoadEvent.class, event -> netServer.admins.addActionFilter(playerAction -> false));

        Events.on(TapEvent.class, event -> teleport(event.player, event.tile));

        Events.on(PlayerJoin.class, event -> {
            new Updater(event.player);

            Call.label(event.player.con, config.get("title", 1).asString(), 1100f, 96,312);
            Call.label(event.player.con, config.get("title", 2).asString(), 1100f, 192, 344);
            Call.label(event.player.con, config.get("title", 3).asString(), 1100f, 288, 312);
            Call.label(event.player.con, config.get("title", 4).asString(), 1100f, 288, 120);
            Call.label(event.player.con, config.get("title", 5).asString(), 1100f, 192, 88);
            Call.label(event.player.con, config.get("title", 6).asString(), 1100f, 96, 120);

            // tile x or y * 8 = coordinate
            Vars.net.pingHost(config.get("ip", 1).asString(), config.get("port", 1).asInt(), host -> Call.label(event.player.con, "\uE837 [accent]Online " + host.players, 1100f, 96,272), e -> Call.label(event.player.con, "[red]Offline", 1100f, 96,272));

            Vars.net.pingHost(config.get("ip", 2).asString(), config.get("port", 2).asInt(), host -> Call.label(event.player.con, "\uE837 [accent]Online " + host.players, 1100f, 192, 304), e -> Call.label(event.player.con, "[red]Offline", 1100f, 192, 304));

            Vars.net.pingHost(config.get("ip", 3).asString(), config.get("port", 3).asInt(), host -> Call.label(event.player.con, "\uE837 [accent]Online " + host.players, 1100f, 288, 272), e -> Call.label(event.player.con, "[red]Offline", 1100f, 288, 272));

            Vars.net.pingHost(config.get("ip", 4).asString(), config.get("port", 4).asInt(), host -> Call.label(event.player.con, "\uE837 [accent]Online " + host.players, 1100f, 288, 80), e -> Call.label(event.player.con, "[red]Offline", 1100f, 288, 80));

            Vars.net.pingHost(config.get("ip", 5).asString(), config.get("port", 5).asInt(), host -> Call.label(event.player.con, "\uE837 [accent]Online " + host.players, 1100f, 192, 48), e -> Call.label(event.player.con, "[red]Offline", 1100f, 192, 48));

            Vars.net.pingHost(config.get("ip", 6).asString(), config.get("port", 6).asInt(), host -> Call.label(event.player.con, "\uE837 [accent]Online " + host.players, 1100f, 96, 80), e -> Call.label(event.player.con, "[red]Offline", 1100f, 96, 80));

            Vars.net.pingHost("pandorum.su", 9000, host -> Core.settings.put("totalPlayers", host.players), Log::err);

        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){}

    @Override
    public void registerClientCommands(CommandHandler handler){}

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