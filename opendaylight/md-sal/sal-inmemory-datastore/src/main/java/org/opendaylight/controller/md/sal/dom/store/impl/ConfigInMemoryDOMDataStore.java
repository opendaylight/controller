package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.concurrent.ExecutorService;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ConfigInMemoryDOMDataStore extends InMemoryDOMDataStore {

	//TODO: hold only one instance of configSchemaContext

	public ConfigInMemoryDOMDataStore(String name,
			ExecutorService dataChangeListenerExecutor) {
		super(name, dataChangeListenerExecutor);
	}

    public ConfigInMemoryDOMDataStore(String name,
			ExecutorService dataChangeListenerExecutor,
			int maxDataChangeListenerQueueSize, boolean debugTransactions) {
		super(name, dataChangeListenerExecutor, maxDataChangeListenerQueueSize,
				debugTransactions);
		// TODO Auto-generated constructor stub
	}

	@Override
    public synchronized void onGlobalContextUpdated(final SchemaContext ctx) {
        dataTree.setSchemaContext(new SchemaContextConfigProxy(ctx));
    }

}
