package pandorum.entry;

import arc.util.*;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.*;
import pandorum.Misc;

import java.util.concurrent.*;

import static pandorum.PandorumPlugin.*;

public class BlockEntry implements HistoryEntry{
    @Nullable
    public final String name;
    public final Unit unit;
    public final Building building;
    public final boolean breaking;
    public long lastAccessTime;

    public BlockEntry(BlockBuildEndEvent event){
        this.unit = event.unit;
        this.name = unit.isPlayer() ? Misc.colorizedName(unit.getPlayer()) : unit.controller() instanceof Player ? Misc.colorizedName(unit.getPlayer()) : null;
        this.building = event.tile.build;
        this.breaking = event.breaking;

        lastAccessTime = Time.millis();
    }

    @Override
    public String getMessage(){
        if(breaking){
            return name != null ? bundle.format("events.history.block.destroy.player", name) :
            bundle.format("events.history.block.destroy.unit", unit.type);
        }

        String base = name != null ? bundle.format("events.history.block.construct.player", name, building.block().name) :
                      bundle.format("events.history.block.construct.unit", unit.type, building.block().name);
        if(building.block().rotate){
            int rotation = building.rotation;
            base += building.block().rotate ? bundle.format("events.history.block.construct.rotate", RotateEntry.sides[rotation == 0 ? 1 : rotation + 3]) : "";
        }
        return base;
    }

    @Override
    public long getLastAccessTime(TimeUnit unit){
        return unit.convert(Time.timeSinceMillis(lastAccessTime), TimeUnit.MILLISECONDS);
    }
}
