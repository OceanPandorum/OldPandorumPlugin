package pandorum.entry;

import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Player;
import mindustry.world.Block;

import static pandorum.PandorumPlugin.*;

public class BlockEntry implements HistoryEntry{
    public Player player;
    public Block block;
    public boolean breaking;

    public BlockEntry(BlockBuildEndEvent event){
        this.player = event.unit.getPlayer();
        this.block = event.tile.block();
        this.breaking = event.breaking;
    }

    @Override
    public String getMessage(){
        return breaking ? bundle.format("events.history.block.destroy", player.name) : bundle.format("events.history.block.construct", player.name, block.name);
    }
}
