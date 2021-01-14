package pandorum.rest;

import arc.util.Log;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.*;

import static pandorum.PandorumPlugin.gson;

public class ForwardRouter implements Router{

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .executor(Executors.newFixedThreadPool(6))
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public RestHttpResponse exchange(RestHttpRequest request){
        try{
            Log.debug("request: @", gson.toJson(request));
            URI uri = URI.create(Routes.BASE_URL + request.uri());

            HttpRequest.Builder req = HttpRequest.newBuilder(uri);
            request.getHeaders().forEach(req::header);
            req.method(request.method().toString(), HttpRequest.BodyPublishers.ofString(gson.toJson(request.body())));

            return httpClient.sendAsync(req.build(), BodyHandlers.ofString())
                    .thenApply(res -> new RestHttpResponse(res.body(), HttpStatus.byCode(res.statusCode())))
                    .exceptionally(t -> RestHttpResponse.absent)
                    .get(2, TimeUnit.SECONDS);
        }catch(Throwable t){
            Log.err(t);
            return RestHttpResponse.absent;
        }
    }
}
