package pandorum.entry;

import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.Player;
import mindustry.world.Block;

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
    public String getMessage(boolean admin){
        return breaking ? "[red]- [white]" + player.name + " broke this tile" : "[green]+ [white]" + player.name + " placed [purple]" + block + "[white]";
    }
}
