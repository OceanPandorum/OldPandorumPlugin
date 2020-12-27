package pandorum.struct;

import java.util.concurrent.*;

public class LimitedDelayQueue<E extends Delayed> extends DelayQueue<E>{
    private final int limit;

    private boolean overflown = false;

    public LimitedDelayQueue(int limit){
        this.limit = limit;
    }

    @Override
    public boolean add(E e){
        if(size() + 1 > limit){
            overflown = true;
            return false;
        }
        return super.add(e);
    }

    public boolean isOverflown(){
        return overflown;
    }
}
