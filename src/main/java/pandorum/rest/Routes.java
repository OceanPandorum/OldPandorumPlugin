package pandorum.rest;

import static pandorum.PandorumPlugin.config;

public abstract class Routes{

    private Routes(){}

    public static final String BASE_URL = config.url;

    public static final Route ACTIONS_GET = Route.get("/actions/{type}");

    public static final Route ACTION_GET = Route.get("/actions/{type}/{id}");

    public static final Route ACTION_DELETE = Route.delete("/actions/{type}/{id}");

    public static final Route ACTION_POST = Route.post("/actions");
}
