package pandorum.components;

import java.util.Set;

public class Config{

    public int alertDistance = 300;

    public float voteRatio = 0.6F;

    public int hubPort = 8000;

    public PluginType type = PluginType.def;

    public String hubIp = "pandorum.su";

    public String locale = "ru_RU";

    public String url;

    public Set<String> bannedNames = Set.of(
            "IGGGAMES",
            "CODEX",
            "VALVE",
            "tuttop"
    );

    public boolean rest(){
        return url != null;
    }

    enum PluginType{
        // атака-выживание
        def,
        // песочница
        sandbox,
        // командное пвп
        pvp,
        // 1vs1
        duel
    }
}