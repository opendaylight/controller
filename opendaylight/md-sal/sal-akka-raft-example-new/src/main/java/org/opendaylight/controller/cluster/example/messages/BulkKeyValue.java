package org.opendaylight.controller.cluster.example.messages;

/**
 * Created by django on 6/30/16.
 */
public class BulkKeyValue {
    private int rangeStart;
    private int rangeEnd;

    public BulkKeyValue(int rangeStart, int rangeEnd) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    public int getRangeStart() {
        return rangeStart;
    }

    public int getRangeEnd() {
        return rangeEnd;
    }
}
