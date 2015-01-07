package org.opendaylight.datasand.codec;

import java.util.LinkedList;
import java.util.List;

public class LeftOvers {
    private List<Object> leftOverList = new LinkedList<Object>();

    public LeftOvers() {
    }

    public void addLeftOver(Object o) {
        this.leftOverList.add(o);
    }

    public List<Object> getLeftOvers() {
        return this.leftOverList;
    }
}
