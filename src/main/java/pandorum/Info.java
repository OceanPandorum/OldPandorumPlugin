package pandorum;

import arc.util.Strings;
import mindustry.gen.Call;
import mindustry.gen.Player;

import static pandorum.PandorumPlugin.bundle;

public abstract class Info{

    private Info(){}

    public static void bundled(Player player, String key, Object... values){
        player.sendMessage(bundle.format(key, values));
    }

    public static void format(Player player, String key, Object... values){
        player.sendMessage(Strings.format(key, values));
    }
}
