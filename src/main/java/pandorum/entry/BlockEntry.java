package pandorum.entry;

import arc.util.Time;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.*;
import mindustry.world.Block;

import java.util.concurrent.*;

import static pandorum.CommonUtil.colorizedName;
import static pandorum.PandorumPlugin.*;

public class BlockEntry implements HistoryEntry{
    public final Unit unit;
    public final Block block;
    public final boolean breaking;
    public final long timestamp;

    public BlockEntry(BlockBuildEndEvent event){
        this.unit = event.unit;
        this.block = event.tile.block();
        this.breaking = event.breaking;
        this.timestamp = Time.millis() + config.expireDelay;
    }

    @Override
    public String getMessage(){
        if(breaking){
            return unit.isPlayer() ? bundle.format("events.history.block.destroy.player", colorizedName(unit.getPlayer())) :
            bundle.format("events.history.block.destroy.unit", unit.type.name);
        }

        return unit.isPlayer() ? bundle.format("events.history.block.construct.player", colorizedName(unit.getPlayer()), block.name) :
        bundle.format("events.history.block.construct.unit", unit.type.name, block.name);
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
