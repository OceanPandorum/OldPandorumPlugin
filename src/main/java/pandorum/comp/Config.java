package pandorum.comp;

import java.util.Set;

public class Config{

    public int alertDistance = 300;

    public float voteRatio = 0.6F;

    public int historyLimit = 16;

    /** Время через которое запись в истории тайла будет удалена. По умолчанию 30 минут. Записывается в миллисекундах */
    public long expireDelay = 1800000;

    /** Время голосования. В секундах */
    public float voteDuration = 75f;

    public String hubIp = "pandorum.su";

    public int hubPort = 8000;

    public PluginType type = PluginType.def;

    public String locale = "ru";

    /** Ссылка на REST сервер */
    public String url;

    public Set<String> bannedNames = Set.of(
            "IGGGAMES",
            "CODEX",
            "VALVE",
            "tuttop"
    );

    public boolean rest(){
        return url != null && !url.isEmpty();
    }

    public enum PluginType{
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
