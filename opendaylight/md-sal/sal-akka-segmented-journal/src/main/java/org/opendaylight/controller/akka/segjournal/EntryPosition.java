package org.opendaylight.controller.akka.segjournal;

public class EntryPosition {

    private final long firstIndex;
    private final long lastIndex;

    public EntryPosition(long firstIndex, long lastIndex) {
        this.firstIndex = firstIndex;
        this.lastIndex = lastIndex;
    }

    public long getFirstIndex() {
        return firstIndex;
    }

    public long getLastIndex() {
        return lastIndex;
    }
}
