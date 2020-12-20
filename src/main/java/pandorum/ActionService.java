package pandorum;

import arc.Net.*;
import arc.func.Cons;
import arc.util.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import static arc.Core.net;
import static pandorum.PandorumPlugin.*;

public class ActionService{

    private ActionService(){}

    public static final Type array = new TypeToken<List<AdminAction>>(){}.getType();

    public static void get(AdminActionType type, String targetId, Cons<List<AdminAction>> cons){
        net.httpGet(
                Strings.format("@/@/@", config.url, type, targetId),
                res -> cons.get(gson.fromJson(res.getResultAsString(), array)),
                Log::err
        );
    }

    public static void get(AdminActionType type, Cons<List<AdminAction>> cons){
        net.httpGet(
                Strings.format("@/@", config.url, type),
                res -> cons.get(gson.fromJson(res.getResultAsString(), array)),
                Log::err
        );
    }

    public static void delete(AdminActionType type, String targetId){
        net.http(
                new HttpRequest().method(HttpMethod.DELETE).url(Strings.format("@/@/@", config.url, type, targetId)),
                res -> {/* no-op */},
                Log::err
        );
    }

    public static void save(AdminAction adminAction){
        net.http(
                new HttpRequest().method(HttpMethod.POST).content(gson.toJson(adminAction))
                                 .header("Content-type", "application/json").url(config.url),
                res -> Log.debug(res.getResultAsString()),
                Log::err
        );
    }
}
