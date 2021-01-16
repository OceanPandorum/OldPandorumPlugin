package pandorum.struct.cache;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static pandorum.struct.cache.CacheSeq.UNSET_INT;

public class SeqBuilder<T>{
    protected long expireAfterWriteNanos = UNSET_INT;
    protected int limit = UNSET_INT;

    private SeqBuilder(){}

    public static <T> SeqBuilder<T> newBuilder(){
        return new SeqBuilder<>();
    }

    public SeqBuilder<T> limit(int limit){
        this.limit = limit;
        return this;
    }

    public SeqBuilder<T> expireAfterWrite(Duration duration){
        return expireAfterWrite(toNanosSaturated(duration), TimeUnit.NANOSECONDS);
    }

    public SeqBuilder<T> expireAfterWrite(long duration, TimeUnit unit){
        this.expireAfterWriteNanos = unit.toNanos(duration);
        return this;
    }

    private long toNanosSaturated(Duration duration){
        try{
            return duration.toNanos();
        }catch(ArithmeticException tooBig){
            return duration.isNegative() ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
    }

    public <T1 extends T> CacheSeq<T1> build(){
        return new CacheSeq<>(this);
    }
}
