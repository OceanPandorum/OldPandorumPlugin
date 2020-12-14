package pandorum;

import mindustry.gen.*;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static pandorum.Main.teleport;

public class Updater implements Runnable{
    private final Player target;

    public Updater(Player target){
        this.target = target;
    }

    @Override
    public void run(){
        Thread t = Thread.currentThread();
        while(!t.isInterrupted()){
            try{
                TimeUnit.MILLISECONDS.sleep(100);
                if(Groups.player.contains(p -> Objects.equals(p, target))){
                    teleport(target, null);
                }else{
                    t.interrupt();
                }
            }catch(InterruptedException ignored){}
        }
    }
}

