package pandorum.rest;

import java.util.*;
import java.util.function.Predicate;

public class HttpRequest{

    private final Route route;

    public final String uri;

    public Object body;

    private Map<String, String> headers;

    public HttpRequest(Route route, Object... uriVars){
        this.route = route;
        this.uri = RouteUtil.expand(route.uriTemplate, uriVars);
    }

    public Route getRoute(){
        return route;
    }

    public HttpMethod method(){
        return route.method;
    }

    public String uri(){
        return uri;
    }

    public Object body(){
        return body;
    }

    public Map<String, String> getHeaders(){
        return headers;
    }

    public HttpRequest body(Object body){
        this.body = body;
        return this;
    }

    public HttpRequest header(String key, String value){
        if(headers == null){
            headers = new LinkedHashMap<>();
        }
        headers.put(key.toLowerCase(), value);
        return this;
    }

    public HttpRequest optionalHeader(String key, String value){
        return value == null ? this : header(key, value);
    }

    public HttpResponse exchange(Router router){
        return router.exchange(this);
    }

    @Override
    public String toString(){
        return "HttpRequest{" +
               "route=" + route +
               ", uri='" + uri + '\'' +
               ", body=" + body +
               ", headers=" + headers +
               '}';
    }
}

