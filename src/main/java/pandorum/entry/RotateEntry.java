package pandorum.entry;

import mindustry.world.Block;

import static pandorum.PandorumPlugin.bundle;

public class RotateEntry implements HistoryEntry{
    private static final String[] sides;

    static{
        sides = bundle.get("events.history.rotate.all").split(", ");
    }

    public final String name;
    public final Block block;
    public final int rotation;

    public RotateEntry(String name, Block block, int rotation){
        this.name = name;
        this.block = block;
        this.rotation = rotation;
    }

    @Override
    public String getMessage(){
        return bundle.format("events.history.rotate", name, block.name, sides[rotation]);
    }
}
