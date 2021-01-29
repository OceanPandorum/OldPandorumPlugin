package pandorum.struct;

import arc.struct.*;
import arc.util.Time;

import java.util.Objects;

/**
 * Последовательность с некоторыми функциями кеш-карт. <br>
 * С такими как: выселение по истечению времени, лимит <br>
 * <p>
 * <b>На данный момент НЕПОТОКОБЕЗОПАСНА</b>
 */
public class CacheSeq<T> extends Seq<T>{
    protected static final int UNSET_INT = -1;

    private final Queue<Tuple2<T, Long>> writeQueue;
    private final long expireAfterWriteNanos;
    private final int maximumSize;

    private boolean overflow;

    CacheSeq(Seqs.SeqBuilder<? super T> builder){
        maximumSize = builder.maximumSize;
        expireAfterWriteNanos = builder.expireAfterWriteNanos;
        writeQueue = Seqs.safeQueue();
    }

    @Override
    public void add(T e){
        if(evictsBySize() && size + 1 > maximumSize){
            overflow = true;
        }else{
            overflow = false;
            writeQueue.add(Tuple2.of(e, Time.nanos()));
            super.add(e);
        }

        cleanup();
    }

    @Override
    public T get(int index){
        try{
            return super.get(index);
        }finally{
            cleanup();
        }
    }

    @Override
    public T peek(){
        try{
            return isEmpty() ? null : super.peek();
        }finally{
            cleanup();
        }
    }

    @Override
    public T first(){
        try{
            return isEmpty() ? null : super.first();
        }finally{
            cleanup();
        }
    }

    @Override
    public boolean remove(T value){
        int index = writeQueue.indexOf(t -> Objects.equals(t.t1, value));
        if(index != -1){
            writeQueue.removeIndex(index);
        }
        return super.remove(value);
    }

    public boolean isOverflown(){
        return overflow || size > maximumSize;
    }

    public boolean expiresAfterWrite(){
        return expireAfterWriteNanos > 0;
    }

    public boolean evictsBySize(){
        return maximumSize >= 0;
    }

    public void cleanup(){
        Tuple2<T, Long> t;
        while((t = writeQueue.last()) != null && isExpired(t.t2)){
            remove(t.t1);
        }
    }

    private boolean isExpired(Long time){
        return expiresAfterWrite() && time != null && Time.timeSinceNanos(time) >= expireAfterWriteNanos;
    }
}
