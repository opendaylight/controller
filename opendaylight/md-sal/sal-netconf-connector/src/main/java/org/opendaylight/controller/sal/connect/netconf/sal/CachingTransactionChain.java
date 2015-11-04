package org.opendaylight.controller.sal.connect.netconf.sal;

import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.sal.core.api.model.SchemaService;

public class CachingTransactionChain implements DOMTransactionChain {

    private final org.opendaylight.controller.sal.core.api.model.SchemaService schemaService;
    private InMemoryDOMDataStore inMemoryDOMDataStore;
    private InMemoryDOMDataStore inMemoryDOMDataStore1;

    public CachingTransactionChain(final SchemaService schemaService) {
        this.schemaService = schemaService;
        // lock
        initCache();
    }

    private void initCache() {
        inMemoryDOMDataStore = InMemoryDOMDataStoreFactory.create(deviceID + "-CFG", schemaService);
        inMemoryDOMDataStore1 = InMemoryDOMDataStoreFactory.create("DOM-CFG", getSchemaServiceDependency());

    }

    @Override public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return null;
    }

    @Override public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return null;
    }

    @Override public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return null;
    }

    @Override public void close() {
        // destroy cache
        // unlock
    }
}
