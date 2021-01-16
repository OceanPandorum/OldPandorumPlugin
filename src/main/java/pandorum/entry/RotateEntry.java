package pandorum.entry;

import mindustry.gen.Building;

import static pandorum.PandorumPlugin.bundle;

public class RotateEntry implements HistoryEntry{
    private static final String[] sides;

    static{
        sides = bundle.get("events.history.rotate.all").split(", ");
    }

    public final String name;
    public final Building building;

    public RotateEntry(String name, Building building){
        this.name = name;
        this.building = building;
    }

    @Override
    public String getMessage(){
        return bundle.format("events.history.rotate", name, building.block.name, sides[building.rotation]);
    }
}
