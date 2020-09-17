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
        if(player.x <= 306 && player.x >= 263 && player.y >= 488 && player.y <= 531){
            Vars.net.pingHost(config.get("ip", 1).asString(), config.get("port", 1).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 1).asString(), config.get("port", 1).asInt());
            }, e -> {});
        }

        if(player.x >= 487 && player.x <= 528 && player.y >= 488 && player.y <= 531){
            Vars.net.pingHost(config.get("ip", 2).asString(), config.get("port", 2).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 2).asString(), config.get("port", 2).asInt());
            }, e -> {});
        }

        if(player.x <= 306 && player.x >= 263 && player.y <= 306 && player.y >= 263){
            Vars.net.pingHost(config.get("ip", 3).asString(), config.get("port", 3).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 3).asString(), config.get("port", 3).asInt());
            }, e -> {});
        }

        if(player.x >= 487 && player.x <= 528 && player.y <= 306 && player.y >= 263){
            Vars.net.pingHost(config.get("ip", 4).asString(), config.get("port", 4).asInt(), host -> {
                Call.onConnect(player.con, config.get("ip", 4).asString(), config.get("port", 4).asInt());
            }, e -> {});
        }
    }
}

