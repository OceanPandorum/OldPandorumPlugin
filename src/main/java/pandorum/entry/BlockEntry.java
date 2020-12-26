package pandorum.entry;

import arc.util.Time;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Player;
import mindustry.world.Block;

import java.util.concurrent.*;

import static pandorum.PandorumPlugin.*;

public class BlockEntry implements HistoryEntry{
    public final Player player;
    public final Block block;
    public final boolean breaking;
    public final long timestamp;

    public BlockEntry(BlockBuildEndEvent event){
        this.player = event.unit.getPlayer();
        this.block = event.tile.block();
        this.breaking = event.breaking;
        this.timestamp = Time.millis() + config.expireDelay;
    }

    @Override
    public String getMessage(){
        return breaking ? bundle.format("events.history.block.destroy", player.name) : bundle.format("events.history.block.construct", player.name, block.name);
    }

    @Override
    public long expire(){
        return timestamp;
    }

    @Override
    public long getDelay(TimeUnit unit){
        long diff = timestamp - Time.millis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o){
        HistoryEntry e = (HistoryEntry)o;
        return Long.compare(expire(), e.expire());
    }
}
