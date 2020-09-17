package pandorum;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.io.Streams;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.plugin.Plugin;
import org.hjson.*;

import java.io.IOException;
import java.util.stream.Stream;

import static mindustry.game.EventType.PlayerJoin;
import static mindustry.game.EventType.ServerLoadEvent;

public class Main extends Plugin{

    public static final Fi dir = Core.settings.getDataDirectory().child("/mods/pandorum/");

    public static final Config config = new Config();

    public Main(){}

    @Override
    public void init(){

        Events.on(ServerLoadEvent.class, event -> Vars.netServer.admins.addActionFilter(playerAction -> false));

        Events.on(PlayerJoin.class, event -> {
            new Updater(event.player);

            Call.onLabel(event.player.con, config.get("title", 1).asString(), 1100f, 508f, 304f);
            Call.onLabel(event.player.con, config.get("title", 2).asString(), 1100f, 284f, 304f);
            Call.onLabel(event.player.con, config.get("title", 3).asString(), 1100f, 508f, 529f);
            Call.onLabel(event.player.con, config.get("title", 4).asString(), 1100f, 284f, 529f);

            Vars.net.pingHost(config.get("ip", 1).asString(), config.get("port", 1).asInt(), host -> {
                Call.onLabel(event.player.con, "online", 1100f, 284f, 490f);
            }, e -> Call.onLabel(event.player.con, "offine", 1100f, 284f, 490f));

            Vars.net.pingHost(config.get("ip", 2).asString(), config.get("port", 2).asInt(), host -> {
                Call.onLabel(event.player.con, "online", 1100f, 508f, 490f);
            }, e -> Call.onLabel(event.player.con, "offline", 1100f, 508f, 490f));

            Vars.net.pingHost(config.get("ip", 3).asString(), config.get("port", 3).asInt(), host -> {
                Call.onLabel(event.player.con, "online", 1100f, 284f, 265f);
            }, e -> Call.onLabel(event.player.con, "offline", 1100f, 280f, 265f));

            Vars.net.pingHost(config.get("ip", 4).asString(), config.get("port", 4).asInt(), host -> {
                Call.onLabel(event.player.con, "online", 1100f, 508f, 265f);
            }, e -> Call.onLabel(event.player.con, "offline", 1100f, 508f, 265f));
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
            object.add("server1", new JsonObject().add("ip", "pandorum.su").add("port", 1000).add("title", "<text>"));
            object.add("server2", new JsonObject().add("ip", "pandorum.su").add("port", 2000).add("title", "<text>"));
            object.add("server3", new JsonObject().add("ip", "pandorum.su").add("port", 3000).add("title", "<text>"));
            object.add("server4", new JsonObject().add("ip", "pandorum.su").add("port", 4000).add("title", "<text>"));

            dir.child("config.hjson").writeString(object.toString(Stringify.HJSON), false);
            try{
                Streams.copy(Main.class.getClassLoader().getResourceAsStream("Hub.msav"), Core.settings.getDataDirectory().child("maps/Hub.msav").write(false));
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
