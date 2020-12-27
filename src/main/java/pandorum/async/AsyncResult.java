package pandorum.async;

import arc.util.ArcRuntimeException;

import java.util.concurrent.*;

public final class AsyncResult<T>{
    private final Future<T> future;

    AsyncResult(Future<T> future){
        this.future = future;
    }

    public boolean isDone(){
        return future.isDone();
    }

    public T get(){
        try{
            return future.get();
        }catch(InterruptedException ex){
            return null;
        }catch(ExecutionException ex){
            throw new ArcRuntimeException(ex.getCause());
        }
    }
}
