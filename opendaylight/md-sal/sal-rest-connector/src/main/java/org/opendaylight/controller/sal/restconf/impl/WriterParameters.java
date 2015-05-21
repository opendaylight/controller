package org.opendaylight.controller.sal.restconf.impl;

public class WriterParameters {
    private final int depth;
    private final boolean prettyPrint;

    public WriterParameters(final boolean prettyPrint, final int depth) {
        this.prettyPrint = prettyPrint;
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }
}

