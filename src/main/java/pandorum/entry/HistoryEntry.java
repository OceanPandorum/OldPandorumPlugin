package pandorum.entry;

import java.util.concurrent.Delayed;

public interface HistoryEntry extends Delayed{

    String getMessage();

    long expire();
}
