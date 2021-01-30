package pandorum.entry;

import arc.util.*;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.*;
import mindustry.world.Block;
import pandorum.Misc;

import java.util.concurrent.TimeUnit;

import static pandorum.PandorumPlugin.bundle;

public class BlockEntry implements HistoryEntry{
    @Nullable
    public final String name;
    public final Unit unit;
    public final Block block;
    public final boolean breaking;
    public int rotation;
    public long lastAccessTime;

    public BlockEntry(BlockBuildEndEvent event){
        this.unit = event.unit;
        this.name = unit.isPlayer() ? Misc.colorizedName(unit.getPlayer()) : unit.controller() instanceof Player ? Misc.colorizedName(unit.getPlayer()) : bundle.get("events.unknown");
        this.block = event.tile.build.block;
        this.breaking = event.breaking;

        lastAccessTime = Time.millis();
        this.rotation = event.tile.build.rotation;
    }

    @Override
    public String getMessage(){
        if(breaking){
            return name != null ? bundle.format("events.history.block.destroy.player", name) :
            bundle.format("events.history.block.destroy.unit", unit.type);
        }

        String base = name != null ? bundle.format("events.history.block.construct.player", name, block.name) :
                      bundle.format("events.history.block.construct.unit", unit.type, block.name);
        if(block.rotate){
            base += bundle.format("events.history.block.construct.rotate", RotateEntry.sides[rotation == 0 ? 1 : rotation + 3]);
        }
        return base;
    }

    @Override
    public long getLastAccessTime(TimeUnit unit){
        return unit.convert(Time.timeSinceMillis(lastAccessTime), TimeUnit.MILLISECONDS);
    }
}
