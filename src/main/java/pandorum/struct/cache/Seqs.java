package pandorum.struct.cache;

import arc.util.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static pandorum.struct.cache.CacheSeq.UNSET_INT;

public abstract class Seqs{

    private Seqs(){}

    static void requireArgument(boolean expression, String template, @Nullable Object... args){
        if(!expression){
            throw new IllegalArgumentException(Strings.format(template, args));
        }
    }

    public static <T> SeqBuilder<T> newBuilder(){
        return new SeqBuilder<>();
    }

    public static class SeqBuilder<T>{
        protected long expireAfterWriteNanos = UNSET_INT;
        protected int maximumSize = UNSET_INT;

        private SeqBuilder(){}

        public SeqBuilder<T> maximumSize(int maximumSize){
            requireArgument(maximumSize >= 0, "maximum size must not be negative");
            this.maximumSize = maximumSize;
            return this;
        }

        public SeqBuilder<T> expireAfterWrite(Duration duration){
            return expireAfterWrite(toNanosSaturated(duration), TimeUnit.NANOSECONDS);
        }

        public SeqBuilder<T> expireAfterWrite(long duration, TimeUnit unit){
            requireArgument(duration >= 0, "duration cannot be negative: @ @", duration, unit);
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
}
