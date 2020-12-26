package pandorum.entry;

import arc.struct.StringMap;
import mindustry.content.Blocks;
import mindustry.entities.units.UnitCommand;
import mindustry.game.EventType.ConfigEvent;
import mindustry.gen.Player;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;
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
    public String getMessage(){
        if(block == Blocks.powerNode || block == Blocks.powerNodeLarge || block == Blocks.powerSource ||
           block == Blocks.powerVoid || block == Blocks.surgeTower || block == Blocks.phaseConduit || block == Blocks.phaseConveyor ||
           block == Blocks.bridgeConduit || block == Blocks.itemBridge || block == Blocks.massDriver){
            int data = (int)value;
            Tile tile = world.tile(data);
            if(connect){
                return bundle.format("events.history.config.power-node.connect", player.name, block.name, tile.x, tile.y);
            }

            return bundle.format("events.history.config.power-node.disconnect", player.name, block.name, tile.x, tile.y);
        }else if(block == Blocks.commandCenter){
            return bundle.format("events.history.config.command-center", player.name, commands[((UnitCommand)value).ordinal()]);
        }else if(block == Blocks.liquidSource){
            Liquid liquid = (Liquid)value;
            if(liquid == null){
                return bundle.format("events.history.config.default", player.name);
            }

            return bundle.format("events.history.config.liquid", player.name, icons.get(liquid.name));
        }else{
            Item item = (Item)value;
            if(item == null){
                return bundle.format("events.history.config.default", player.name);
            }

            return bundle.format("events.history.config.item", player.name, icons.get(item.name));
        }
    }
}
