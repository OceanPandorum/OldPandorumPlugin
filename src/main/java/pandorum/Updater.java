package pandorum;

import arc.util.Log;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.gen.Call;

import static pandorum.Main.config;

public class Updater{

    private Thread th;

    public Updater(Player player){
        th = new Thread(() -> {
            while(!th.isInterrupted()){
                try{
                    Thread.sleep(100);
                    if(Vars.playerGroup.all().contains(player))
                        checkTile(player);
                    else
                        th.interrupt();
                }catch(InterruptedException ignored){Log.info(ignored);}
            }
        });
        th.start();
    }

    public void checkTile(Player player){
        if(player.x <= 304 && player.x >= 264 && player.y >= 488 && player.y <= 531){
            Vars.net.pingHost(config.get("ip", 1).asString(), config.get("port", 1).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 1).asString(), config.get("port", 1).asInt());
            }, e -> {});
        }

        if(player.x <= 416 && player.x >= 376 && player.y >= 519 && player.y <= 562){
            Vars.net.pingHost(config.get("ip", 2).asString(), config.get("port", 2).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 2).asString(), config.get("port", 2).asInt());
            }, e -> {});
        }

        if(player.x >= 488 && player.x <= 528 && player.y <= 531 && player.y >= 488){
            Vars.net.pingHost(config.get("ip", 3).asString(), config.get("port", 3).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 3).asString(), config.get("port", 3).asInt());
            }, e -> {});
        }

        if(player.x >= 488 && player.x <= 528 && player.y <= 420 && player.y >= 379){
            Vars.net.pingHost(config.get("ip", 4).asString(), config.get("port", 4).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 4).asString(), config.get("port", 4).asInt());
            }, e -> {});
        }

        if(player.x >= 488 && player.x <= 528 && player.y <= 306 && player.y >= 263){
            Vars.net.pingHost(config.get("ip", 5).asString(), config.get("port", 5).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 5).asString(), config.get("port", 5).asInt());
            }, e -> {});
        }

        if(player.x >= 376 && player.x <= 416 && player.y <= 306 && player.y >= 263){
            Vars.net.pingHost(config.get("ip", 6).asString(), config.get("port", 6).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 6).asString(), config.get("port", 6).asInt());
            }, e -> {});
        }

        if(player.x <= 304 && player.x >= 264 && player.y <= 306 && player.y >= 263){
            Vars.net.pingHost(config.get("ip", 7).asString(), config.get("port", 7).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 7).asString(), config.get("port", 7).asInt());
            }, e -> {});
        }

        if(player.x <= 304 && player.x >= 264 && player.y >= 379 && player.y <= 420){
            Vars.net.pingHost(config.get("ip", 8).asString(), config.get("port", 8).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 8).asString(), config.get("port", 8).asInt());
            }, e -> {});
        }
    }
}

