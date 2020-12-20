package pandorum;

import arc.Net.*;
import arc.util.*;

import static arc.Core.net;
import static pandorum.PandorumPlugin.*;

public class ActionService{

    private ActionService(){}

    public static void delete(String targetId){
        net.http(new HttpRequest().method(HttpMethod.DELETE).url(Strings.format("@/ban/@", config.url, targetId)),
                      r -> Log.debug(r.getResultAsString()),
                      Log::err);
    }

    public static void save(AdminAction adminAction){
        net.http(new HttpRequest().method(HttpMethod.POST).content(gson.toJson(adminAction))
                                  .header("Content-type", "application/json").url(config.url),
                 res -> Log.debug(res.getResultAsString()),
                 Log::err);
    }
}
