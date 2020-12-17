package pandorum;

import arc.util.Strings;
import mindustry.gen.Call;
import mindustry.gen.Player;

import static pandorum.Main.bundle;

public class Info{

    private Info(){
        // Util class
    }

    public static void broadCast(Player player, String[] args){
        String text = Strings.format("\uE805@\uE805\n\n@\n", bundle.get("commands.admin.bc.text"), Strings.join(" ", args[0].split("\n")));

        if(player != null){
            Call.infoMessage(player.con, text);
        }else{
            Call.infoMessage(text);
        }
    }

    public static void bundled(Player player, String key, Object... values){
        player.sendMessage(bundle.format(key, values));
    }

    public static void format(Player player, String key, Object... values){
        player.sendMessage(Strings.format(key, values));
    }
}
