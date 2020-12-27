package pandorum.rest;

import arc.func.Cons;
import arc.struct.*;
import arc.util.io.Streams;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.*;

import static pandorum.PandorumPlugin.gson;

@SuppressWarnings("unchecked")
public class HttpResponse{
    private final HttpURLConnection connection;
    private final HttpStatus status;
    private Object defaultValue;

    public HttpResponse(HttpURLConnection connection) throws IOException{
        this.connection = connection;
        this.status = HttpStatus.byCode(connection.getResponseCode());
    }

    public HttpResponse defaultValue(Object defaultValue){
        this.defaultValue = defaultValue;
        return this;
    }

    public <T> T bodyTo(Type type){
        T t = gson.fromJson(getResultAsString(), type);
        return t != null ? t : (T)defaultValue;
    }

    public byte[] getResult(){
        InputStream input = getInputStream();

        if(input == null){
            return Streams.EMPTY_BYTES;
        }

        try{
            return Streams.copyBytes(input, connection.getContentLength());
        }catch(IOException e){
            return Streams.EMPTY_BYTES;
        }finally{
            Streams.close(input);
        }
    }

    public String getResultAsString(){
        InputStream input = getInputStream();

        if(input == null){
            return "";
        }

        try{
            return Streams.copyString(input, connection.getContentLength());
        }catch(IOException e){
            return "";
        }finally{
            Streams.close(input);
        }
    }

    public InputStream getResultAsStream(){
        return getInputStream();
    }

    public HttpStatus getStatus(){
        return status;
    }

    public String getHeader(String name){
        return connection.getHeaderField(name);
    }

    public ObjectMap<String, Seq<String>> getHeaders(){
        ObjectMap<String, Seq<String>> out = new ObjectMap<>();
        Map<String, List<String>> fields = connection.getHeaderFields();
        for(String key : fields.keySet()){
            if(key != null){
                out.put(key, Seq.with(fields.get(key).toArray(new String[0])));
            }
        }
        return out;
    }

    private InputStream getInputStream(){
        try{
            return connection.getInputStream();
        }catch(IOException e){
            return connection.getErrorStream();
        }
    }
}
