package pandorum;

import mindustry.gen.*;

import static pandorum.Main.*;

public class Updater{

    private Thread th;

    public Updater(Player player){
        th = new Thread(() -> {
            while(!th.isInterrupted()){
                try{
                    Thread.sleep(100);
                    if(Groups.player.contains(p -> p == player)){
                        teleport(player, null);
                    }else{
                        th.interrupt();
                    }
                }catch(InterruptedException ignored){}
            }
        });
        th.start();
    }
}

