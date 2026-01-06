package ch.qos.logback.tyler.base.handler;

import ch.qos.logback.core.Context;

/**
 * Utility class for managing instance counters in a Logback Context. Used by logback-tyler
 */
public class HandlerInstanceCounterHelper {

    /**
     * Increments the counter associated with the given key in the context.
     * If the counter does not exist, it is initialized to 0, otherwise it is incremented.
     *
     * @param context    the Logback context where the counter is stored
     * @param counterKey the key for the counter
     * @return the new value of the counter after incrementing
     */
    public static int inc(Context context, String counterKey) {
        Integer counter = (Integer) context.getObject(counterKey);
        if (counter == null)
            counter = 0;
        else
            counter+=1;
        context.putObject(counterKey, counter);
        return counter;
    }

}
