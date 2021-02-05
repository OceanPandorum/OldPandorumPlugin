package pandorum.entry;

import arc.util.Time;
import mindustry.world.Block;
import pandorum.struct.Tuple2;

import java.util.concurrent.TimeUnit;

import static pandorum.PandorumPlugin.bundle;

public class RotateEntry implements HistoryEntry{
    protected static final String[] sides;

    static{
        sides = bundle.get("events.history.rotate.all").split(", ");
    }

    public final String name;
    public final Block block;
    public final Tuple2<Integer, Integer> rotation;
    public long lastAccessTime = Time.millis();

    public RotateEntry(String name, Block block, Tuple2<Integer, Integer> rotation){
        this.name = name;
        this.block = block;
        this.rotation = rotation;
    }

    @Override
    public String getMessage(){
        int index = rotation.t1 > rotation.t2 && rotation.t2 != 0 || rotation.t2 == 0 && rotation.t1 == 1 || rotation.t1 == 0 && rotation.t2 > 1 ? rotation.t2 + 4 : rotation.t2;
        return bundle.format("events.history.rotate", name, block.name, sides[index]);
    }

    @Override
    public long getLastAccessTime(TimeUnit unit){
        return unit.convert(Time.timeSinceMillis(lastAccessTime), TimeUnit.MILLISECONDS);
    }
}
