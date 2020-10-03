package pandorum;

import arc.util.Strings;
import mindustry.gen.Call;

import static pandorum.Main.*;

public class Info{
    public static void broadCast(String[] args) {
        String text = Strings.join(" ", args[0].split("\n"));

        Call.onInfoMessage("\uE805" + bundle.get("bc.txt") + "\uE805\n\n" + text + "\n");
    }
}
