package org.opendaylight.controller.datastore.infinispan.utils.queue;

import org.jgroups.util.ConcurrentLinkedBlockingQueue;

public class SingleItemProcessingQueue extends ConcurrentLinkedBlockingQueue {

    public SingleItemProcessingQueue(int capacity) {
        super(capacity);
    }
}
