package pandorum.entry;

import arc.util.Log;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Player;
import mindustry.world.Block;

import java.util.concurrent.*;

import static pandorum.PandorumPlugin.*;

public class BlockEntry implements HistoryEntry{
    public Player player;
    public Block block;
    public boolean breaking;
    public long timestamp;

    public BlockEntry(BlockBuildEndEvent event){
        this.player = event.unit.getPlayer();
        this.block = event.tile.block();
        this.breaking = event.breaking;
        this.timestamp = System.currentTimeMillis() + 30000;
    }

    @Override
    public String getMessage(){
        return breaking ? bundle.format("events.history.block.destroy", player.name) : bundle.format("events.history.block.construct", player.name, block.name);
    }

    @Override
    public long getDelay(TimeUnit unit){
        Log.info("req");
        long diff = timestamp - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o){
        BlockEntry e = (BlockEntry)o;
        return Long.compare(timestamp, e.timestamp);
    }
}
