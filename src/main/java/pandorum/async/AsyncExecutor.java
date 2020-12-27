package pandorum.async;

import arc.util.*;

import java.util.concurrent.*;

public class AsyncExecutor implements Disposable{
    private final ExecutorService executor;

    public AsyncExecutor(int maxConcurrent){
        executor = Executors.newFixedThreadPool(maxConcurrent, r -> {
            Thread thread = new Thread(r, "AsynchExecutor-Thread");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, e) -> Log.err(e));
            return thread;
        });
    }

    public AsyncResult<Void> submit(Runnable run){
        return submit(() -> {
            run.run();
            return null;
        });
    }

    public <T> AsyncResult<T> submit(Callable<T> task){
        if(executor.isShutdown()){
            throw new ArcRuntimeException("Cannot run tasks on an executor that has been shutdown (disposed)");
        }
        return new AsyncResult<>(executor.submit(task));
    }

    @Override
    public void dispose(){
        executor.shutdown();
        try{
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        }catch(InterruptedException e){
            throw new ArcRuntimeException("Couldn't shutdown loading thread", e);
        }
    }
}
