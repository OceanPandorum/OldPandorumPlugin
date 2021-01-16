package pandorum.entry;

import arc.struct.StringMap;
import arc.util.Time;
import mindustry.content.Blocks;
import mindustry.entities.units.UnitCommand;
import mindustry.game.EventType.ConfigEvent;
import mindustry.gen.Groups;
import mindustry.type.*;
import mindustry.world.*;
import pandorum.CommonUtil;

import java.util.concurrent.*;

import static mindustry.Vars.world;
import static pandorum.PandorumPlugin.*;

public class ConfigEntry implements HistoryEntry{
    private static final StringMap icons = StringMap.of(
        "copper", "\uF838",
        "lead", "\uF837",
        "metaglass", "\uF836",
        "graphite", "\uF835",
        "sand", "\uF834",
        "coal", "\uF833",
        "titanium", "\uF832",
        "thorium", "\uF831",
        "scrap", "\uF830",
        "silicon", "\uF82F",
        "plastanium", "\uF82E",
        "phase-fabric", "\uF82D",
        "surge-alloy", "\uF82C",
        "spore-pod", "\uF82B",
        "blast-compound", "\uF82A",
        "pyratite", "\uF829",

        "water", "\uF828",
        "slag", "\uF827",
        "oil", "\uF826",
        "cryofluid", "\uF825"
    );

    private static final String[] commands;

    static{
        commands = bundle.get("events.history.config.command-center.all").split(", ");
    }

    public final String name;
    public final Block block;
    public final Object value;
    public final boolean connect;

    public ConfigEntry(ConfigEvent event, boolean connect){
        this.name = Groups.player.contains(p -> event.player == p) ? CommonUtil.colorizedName(event.player) : bundle.get("events.unknown");
        this.block = event.tile.block();
        this.value = event.value;
        this.connect = connect;
    }

    @Override
    public String getMessage(){
        if(block == Blocks.powerNode || block == Blocks.powerNodeLarge || block == Blocks.powerSource ||
           block == Blocks.powerVoid || block == Blocks.surgeTower || block == Blocks.phaseConduit || block == Blocks.phaseConveyor ||
           block == Blocks.bridgeConduit || block == Blocks.itemBridge || block == Blocks.massDriver){
            int data = (int)value;
            Tile tile = world.tile(data);
            if(tile == null){
                return bundle.get("events.history.unknown");
            }

            if(connect){
                return bundle.format("events.history.config.power-node.connect", name, block, tile.x, tile.y);
            }

            return bundle.format("events.history.config.power-node.disconnect", name, block, tile.x, tile.y);
        }

        if(block == Blocks.door || block == Blocks.doorLarge){
            boolean data = (boolean)value;
            return data ? bundle.format("events.history.config.door.on", name, block) : bundle.format("events.history.config.door.off", name, block);
        }

        if(block == Blocks.switchBlock){
            boolean data = (boolean)value;
            return data ? bundle.format("events.history.config.switch.on", name) : bundle.format("events.history.config.switch.off", name);
        }

        if(block == Blocks.commandCenter){
            return bundle.format("events.history.config.command-center", name, commands[((UnitCommand)value).ordinal()]);
        }

        if(block == Blocks.liquidSource){
            Liquid liquid = (Liquid)value;
            if(liquid == null){
                return bundle.format("events.history.config.default", name);
            }

            return bundle.format("events.history.config.liquid", name, icons.get(liquid.name));
        }

        if(block == Blocks.unloader || block == Blocks.sorter || block == Blocks.invertedSorter || block == Blocks.itemSource){
            Item item = (Item)value;
            if(item == null){
                return bundle.format("events.history.config.default", name);
            }

            return bundle.format("events.history.config.item", name, icons.get(item.name));
        }

        return bundle.get("events.history.unknown"); // ага да
    }
}
