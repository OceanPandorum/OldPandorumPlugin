package pandorum;

import arc.util.Strings;
import mindustry.entities.type.Player;
import mindustry.gen.Call;

import static pandorum.Main.*;

public class Info{
    public static void broadCast(String[] args) {
        String text = Strings.join(" ", args[0].split("\n"));

        Call.onInfoMessage("\uE805" + bundle.get("bc.txt") + "\uE805\n\n" + text + "\n");
    }

    public static void text(Player player, String key){
        player.sendMessage(key.startsWith("$") ? bundle.get(key.substring(1)) : key);
    }

    public static void bundled(Player player, String key, Object... values){
        player.sendMessage(bundle.format(key, values));
    }

    public static void format(Player player, String key, Object... values){
        player.sendMessage(Strings.format(key, values));
    }
}
