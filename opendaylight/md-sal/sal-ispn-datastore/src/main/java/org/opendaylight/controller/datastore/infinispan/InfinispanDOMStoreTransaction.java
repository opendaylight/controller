package org.opendaylight.controller.datastore.infinispan;

import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;

public interface InfinispanDOMStoreTransaction extends DOMStoreTransaction {
    void resumeWrappedTransaction();
    void commitWrappedTransaction();
}
