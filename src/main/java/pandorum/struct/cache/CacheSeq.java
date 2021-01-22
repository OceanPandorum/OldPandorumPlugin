package pandorum.struct.cache;

import arc.struct.*;
import arc.util.Time;

/**
 * Последовательность с некоторыми функциями кеш-карт. <br>
 * С такими как: выселение по истечению времени, лимит <br>
 * <p>
 * <b>На данный момент НЕПОТОКОБЕЗОПАСНА</b>
 */
public class CacheSeq<T> extends Seq<T>{
    protected static final int UNSET_INT = -1;

    private final ObjectMap<T, Long> expires = new ObjectMap<>();
    private final long expireAfterWriteNanos;
    private final int maximumSize;

    private boolean overflow;

    CacheSeq(Seqs.SeqBuilder<? super T> builder){
        maximumSize = builder.maximumSize;
        expireAfterWriteNanos = builder.expireAfterWriteNanos;
    }

    @Override
    public void add(T e){
        if(maximumSize != UNSET_INT && size + 1 > maximumSize){
            overflow = true;
        }else{
            overflow = false;
            expires.put(e, Time.nanos());
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

    public boolean isOverflown(){
        return overflow || size > maximumSize;
    }

    public void cleanup(){
        if(expireAfterWriteNanos == UNSET_INT) return;
        for(T t : this){
            Long time = expires.get(t);
            if(time != null && Time.timeSinceNanos(time) >= expireAfterWriteNanos){
                remove(t);
            }
        }
    }
}
