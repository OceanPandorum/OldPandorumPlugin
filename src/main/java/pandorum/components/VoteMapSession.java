package pandorum.components;

import arc.util.*;
import arc.util.Timer.Task;
import mindustry.game.Gamemode;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.net.WorldReloader;

import static mindustry.Vars.*;

public class VoteMapSession extends VoteSession{
    private final Map target;

    public VoteMapSession(VoteSession[] map, Map target){
        super(map);

        this.target = target;
    }

    @Override
    public void vote(Player player, int d){
        votes += d;
        voted.addAll(player.uuid(), netServer.admins.getInfo(player.uuid()).lastIP);
        Call.sendMessage(Strings.format("[lightgray]@[lightgray] has voted on kicking[orange] @[].[accent] (@/@)\n[lightgray]Type[orange] /vote <y/n>[] to agree.",
                                        player.name, target.name(), votes, votesRequired()));
        checkPass();
    }

    @Override
    boolean checkPass(){
        if(votes >= votesRequired()){
            Call.sendMessage(Strings.format("[orange]Vote passed.[scarlet] @[orange] will be loaded", target.name()));
            map[0] = null;
            task.cancel();

            Runnable r = () -> {
                WorldReloader reloader = new WorldReloader();

                reloader.begin();

                world.loadMap(target, target.applyRules(Gamemode.survival));

                state.rules = state.map.applyRules(Gamemode.survival);
                logic.play();

                reloader.end();
            };

            Timer.schedule(new Task(){
                @Override
                public void run(){
                    try{
                        r.run();
                    }catch(MapException e){
                        Call.sendMessage(Strings.format("[orange]Failed to load map @", target.name()));
                        Log.err(e);
                        net.closeServer();
                    }
                }
            }, 10);
            return true;
        }
        return false;
    }
}
