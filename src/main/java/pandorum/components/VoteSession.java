package pandorum.components;

import arc.struct.ObjectSet;
import arc.util.*;
import arc.util.Timer.Task;
import mindustry.gen.*;

import static mindustry.Vars.netServer;
import static pandorum.PandorumPlugin.config;

public abstract class VoteSession{
    protected static final float voteDuration = 0.7f * 60;

    protected ObjectSet<String> voted = new ObjectSet<>();
    protected VoteSession[] map;
    protected Task task;
    protected int votes;

    public VoteSession(VoteSession[] map){
        this.map = map;
        this.task = start();
    }

    public ObjectSet<String> voted(){
        return voted;
    }

    protected Task start(){
        return Timer.schedule(() -> {
            if(!checkPass()){
                map[0] = null;
                task.cancel();
            }
        }, voteDuration);
    }

    public void vote(Player player, int d){
        votes += d;
        voted.addAll(player.uuid(), netServer.admins.getInfo(player.uuid()).lastIP);
        checkPass();
    }

    boolean checkPass(){
        if(votes >= votesRequired()){
            map[0] = null;
            task.cancel();
            return true;
        }
        return false;
    }

    protected int votesRequired(){
        return (int)Math.ceil(config.voteRatio * Groups.player.size());
    }
}
