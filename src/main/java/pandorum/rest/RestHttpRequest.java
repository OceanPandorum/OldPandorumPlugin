package pandorum.rest;

import java.util.*;

public class RestHttpRequest{

    private final Route route;

    private final String uri;

    private Object body;

    private Map<String, String> headers;

    public RestHttpRequest(Route route, Object... uriVars){
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
        if(headers == null){
            headers = new LinkedHashMap<>();
        }
        return headers;
    }

    public RestHttpRequest body(Object body){
        this.body = body;
        return this;
    }

    public RestHttpRequest header(String key, String value){
        if(headers == null){
            headers = new LinkedHashMap<>();
        }
        headers.put(key.toLowerCase(), value);
        return this;
    }

    public RestHttpRequest optionalHeader(String key, String value){
        return value == null ? this : header(key, value);
    }

    public RestHttpResponse exchange(Router router){
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

