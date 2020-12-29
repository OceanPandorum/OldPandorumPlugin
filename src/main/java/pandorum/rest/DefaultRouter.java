package pandorum.rest;

import arc.util.*;
import arc.util.io.Streams;
import pandorum.async.AsyncExecutor;

import java.io.*;
import java.net.*;

import static pandorum.PandorumPlugin.gson;

public class DefaultRouter implements Router{
    private static final int timeout = 1500;

    private final AsyncExecutor asyncExecutor = new AsyncExecutor(6);

    @Override
    public HttpResponse exchange(HttpRequest request){
        try{
            Log.debug(gson.toJson(request));
            HttpMethod method = request.method();
            URL url;

            if(method == HttpMethod.GET){
                String queryString = "";
                String value = serializeBody(request.body()); // todo в RouteUtil перенести
                if(value != null && !value.isEmpty()){
                    queryString = "?" + value;
                }

                url = new URL(Routes.BASE_URL + request.uri() + queryString);
            }else{
                url = new URL(Routes.BASE_URL + request.uri());
            }

            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            boolean doingOutput = method == HttpMethod.POST || method == HttpMethod.PUT;
            connection.setDoOutput(doingOutput);
            connection.setDoInput(true);
            connection.setRequestMethod(method.toString());
            HttpURLConnection.setFollowRedirects(false);

            if(request.getHeaders() != null){
                request.getHeaders().forEach(connection::addRequestProperty);
            }

            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            return asyncExecutor.submit(() -> {
                try{
                    if(doingOutput){
                        String contentAsString = serializeBody(request.body());
                        if(contentAsString != null){
                            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                            try{
                                writer.write(contentAsString);
                            }finally{
                                Streams.close(writer);
                            }
                        }
                    }

                    connection.connect();

                    try{
                        return new HttpResponse(connection);
                    }finally{
                        connection.disconnect();
                    }

                }catch(Throwable e){
                    return new HttpResponse(HttpStatus.byCode(connection.getResponseCode()));
                }finally{
                    connection.disconnect();
                }
            }).get();
        }catch(Throwable t){
            Log.debug("NetException: @", Strings.neatError(t));
            return new HttpResponse(HttpStatus.UNKNOWN_STATUS);
        }
    }

    private String serializeBody(Object value){
        try{
            return gson.toJson(value);
        }catch(Throwable t){
            return null;
        }
    }
}
