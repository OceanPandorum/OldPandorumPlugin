package pandorum.entry;

import arc.struct.StringMap;
import mindustry.content.Blocks;
import mindustry.entities.units.UnitCommand;
import mindustry.game.EventType.ConfigEvent;
import mindustry.gen.Player;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.world;

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

    private static final String[] commands = {"[red]attack[white]", "[yellow]retreat[white]", "[orange]rally[white]"};

    public Player player;
    public Block block;
    public Object value;
    public boolean connect;

    public ConfigEntry(ConfigEvent event, boolean connect){
        this.player = event.player;
        this.block = event.tile.block();
        this.value = event.value;
        this.connect = connect;
    }

    @Override
    public String getMessage(boolean admin){
        if(block == Blocks.powerNode || block == Blocks.powerNodeLarge || block == Blocks.powerSource ||
           block == Blocks.powerVoid || block == Blocks.surgeTower || block == Blocks.phaseConduit || block == Blocks.phaseConveyor ||
           block == Blocks.bridgeConduit || block == Blocks.itemBridge || block == Blocks.massDriver){
            int data = (int)value;
            Tile tile = world.tile(data);
            String pos = "x: " + tile.x + ", y: " + tile.y;
            if(connect){
                return "[orange]~ [white]" + player.name + " [green]connected[white] this " + block.name + " to [purple]" + pos + "[white]";
            }

            return "[orange]~ [white]" + player.name + " [red]disconnected[white] this " + block.name + " from [purple]" + pos + "[white]";
        }else if(block == Blocks.commandCenter){
            return "[orange]~ [white]" + player.name + " commanded units to " + commands[((UnitCommand)value).ordinal()] + "[white]";
        }else if(block == Blocks.liquidSource){
            Liquid liquid = (Liquid)value;
            if(liquid == null){
                return "[orange]~ [white]" + player.name + " changed config to default";
            }

            return "[orange]~ [white]" + player.name + " changed config to " + icons.get(liquid.name);
        }else{
            Item item = (Item)value;
            if(item == null){
                return "[orange]~ [white]" + player.name + " changed config to default";
            }

            return "[orange]~ [white]" + player.name + " changed config to " + icons.get(item.name);
        }
    }
}
