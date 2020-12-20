package pandorum.components;

import java.util.Set;

public class Config{
    public int alertDistance = 300;

    public float voteRatio = 0.6F;

    public int hubPort = 8000;

    public String hubIp = "pandorum.su";

    public String locale = "ru_RU";

    public String url;

    public Set<String> bannedNames = Set.of(
            "IGGGAMES",
            "CODEX",
            "VALVE",
            "tuttop"
    );
}
