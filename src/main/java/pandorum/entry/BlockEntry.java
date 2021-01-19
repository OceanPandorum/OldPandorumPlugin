package pandorum.entry;

import arc.util.*;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.*;
import mindustry.world.Block;
import pandorum.Misc;

import java.util.concurrent.*;

import static pandorum.PandorumPlugin.*;

public class BlockEntry implements HistoryEntry{
    @Nullable
    public final String name;
    public final Unit unit;
    public final Block block;
    public final boolean breaking;
    public long lastAccessTime;

    public BlockEntry(BlockBuildEndEvent event){
        this.unit = event.unit;
        this.name = unit.isPlayer() ? Misc.colorizedName(unit.getPlayer()) : unit.controller() instanceof Player ? Misc.colorizedName(unit.getPlayer()) : null;
        this.block = event.tile.block();
        this.breaking = event.breaking;

        lastAccessTime = Time.millis();
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
    public long getLastAccessTime(TimeUnit unit){
        return unit.convert(Time.timeSinceMillis(lastAccessTime), TimeUnit.MILLISECONDS);
    }
}
