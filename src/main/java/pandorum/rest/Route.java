package pandorum.rest;

import java.util.Objects;

public class Route{
    public final HttpMethod method;
    public final String uriTemplate;

    private Route(HttpMethod method, String uriTemplate){
        this.method = method;
        this.uriTemplate = uriTemplate;
    }

    public static Route get(String uri){
        return new Route(HttpMethod.GET, uri);
    }

    public static Route post(String uri){
        return new Route(HttpMethod.POST, uri);
    }

    public static Route delete(String uri){
        return new Route(HttpMethod.DELETE, uri);
    }

    public RestHttpRequest newRequest(Object... uriVars){
        return new RestHttpRequest(this, uriVars);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Route route = (Route)o;
        return method == route.method &&
               uriTemplate.equals(route.uriTemplate);
    }

    @Override
    public int hashCode(){
        return Objects.hash(method, uriTemplate);
    }

    @Override
    public String toString(){
        return "Route{" +
               "method=" + method +
               ", uriTemplate='" + uriTemplate + '\'' +
               '}';
    }
}

