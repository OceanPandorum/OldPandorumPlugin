package pandorum.rest;

import java.lang.reflect.Type;
import java.util.Objects;

import static pandorum.PandorumPlugin.gson;

@SuppressWarnings("unchecked")
public class RestHttpResponse{
    public static final RestHttpResponse absent = new RestHttpResponse(null, HttpStatus.UNKNOWN_STATUS);

    private final HttpStatus status;
    private final String body;
    private Object defaultValue;

    public RestHttpResponse(String body, HttpStatus status){
        this.body = body;
        this.status = status;
    }

    public RestHttpResponse defaultValue(Object defaultValue){
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        return this;
    }

    public <T> T bodyTo(Type type){
        T t = gson.fromJson(body, type);
        return t != null ? t : (T)defaultValue;
    }

    public HttpStatus getStatus(){
        return status;
    }
}
