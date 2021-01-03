package pandorum.entry;

import arc.util.*;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.*;
import mindustry.world.Block;
import pandorum.CommonUtil;

import java.util.concurrent.*;

import static pandorum.PandorumPlugin.*;

public class BlockEntry implements HistoryEntry{
    @Nullable
    public final String name;
    public final Unit unit;
    public final Block block;
    public final boolean breaking;
    public final long timestamp;

    public BlockEntry(BlockBuildEndEvent event){
        this.unit = event.unit;
        this.name = unit.isPlayer() ? CommonUtil.colorizedName(unit.getPlayer()) : unit.controller() instanceof Player ? CommonUtil.colorizedName(unit.getPlayer()) : null;
        this.block = event.tile.block();
        this.breaking = event.breaking;
        this.timestamp = Time.millis() + config.expireDelay;
    }

    @Override
    public String getMessage(){
        if(breaking){
            return name != null ? bundle.format("events.history.block.destroy.player", name) :
            bundle.format("events.history.block.destroy.unit", unit.type);
        }

        return name != null ? bundle.format("events.history.block.construct.player", name, block) :
        bundle.format("events.history.block.construct.unit", unit.type, block);
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
