/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.concurrent.ExecutorService;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ConfigInMemoryDOMDataStore extends InMemoryDOMDataStore {

	public ConfigInMemoryDOMDataStore(String name,
			ExecutorService dataChangeListenerExecutor) {
		super(name, dataChangeListenerExecutor);
	}

    public ConfigInMemoryDOMDataStore(String name,
			ExecutorService dataChangeListenerExecutor,
			int maxDataChangeListenerQueueSize, boolean debugTransactions) {
		super(name, dataChangeListenerExecutor, maxDataChangeListenerQueueSize,
				debugTransactions);
	}

	@Override
    public synchronized void onGlobalContextUpdated(final SchemaContext ctx) {
        dataTree.setSchemaContext(new ProxyConfigSchemaContext(ctx));
    }

}
