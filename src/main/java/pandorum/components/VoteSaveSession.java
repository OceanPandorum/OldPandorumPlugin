package pandorum.components;

import arc.util.Strings;
import mindustry.gen.*;
import mindustry.io.SaveIO;

import static mindustry.Vars.*;

public class VoteSaveSession extends VoteSession{
    private final String target;

    public VoteSaveSession(VoteSession[] map, String target){
        super(map);

        this.target = target;
    }

    @Override
    public void vote(Player player, int d){
        votes += d;
        voted.addAll(player.uuid(), netServer.admins.getInfo(player.uuid()).lastIP);
        Call.sendMessage(Strings.format("[lightgray]@[lightgray] has voted on kicking[orange] @[].[accent] (@/@)\n[lightgray]Type[orange] /vote <y/n>[] to agree.",
                                        player.name, target, votes, votesRequired()));
        checkPass();
    }

    @Override
    boolean checkPass(){
        if(votes >= votesRequired()){
            Call.sendMessage(Strings.format("[orange]Vote passed.[scarlet] @[orange] will be loaded", target));
            SaveIO.save(saveDirectory.child(String.format("%s.%s", target, saveExtension)));
            map[0] = null;
            task.cancel();
            return true;
        }
        return false;
    }
}
