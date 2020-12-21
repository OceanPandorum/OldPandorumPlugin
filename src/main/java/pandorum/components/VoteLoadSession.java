package pandorum.components;

import arc.files.Fi;
import arc.util.*;
import mindustry.game.Gamemode;
import mindustry.gen.*;
import mindustry.io.SaveIO;
import mindustry.maps.MapException;
import mindustry.net.WorldReloader;

import static mindustry.Vars.*;

public class VoteLoadSession extends VoteSession{
    private final Fi target;

    public VoteLoadSession(VoteSession[] map, Fi target){
        super(map);

        this.target = target;
    }

    @Override
    public void vote(Player player, int d){
        votes += d;
        voted.addAll(player.uuid(), netServer.admins.getInfo(player.uuid()).lastIP);
        Call.sendMessage(Strings.format("[lightgray]@[lightgray] has voted on kicking[orange] @[].[accent] (@/@)\n[lightgray]Type[orange] /vote <y/n>[] to agree.",
                                        player.name, target.nameWithoutExtension(), votes, votesRequired()));
        checkPass();
    }

    @Override
    boolean checkPass(){
        if(votes >= votesRequired()){
            Call.sendMessage(Strings.format("[orange]Vote passed.[scarlet] @[orange] will be loaded", target.nameWithoutExtension()));
            map[0] = null;
            task.cancel();

            Runnable r = () -> {
                WorldReloader reloader = new WorldReloader();

                reloader.begin();
                SaveIO.load(target);

                state.rules = state.map.applyRules(Gamemode.survival);
                logic.play();

                reloader.end();
            };

            Timer.schedule(new Timer.Task(){
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
