/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import java.util.Set;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;

/**
 * Manages life cycle of {@link YangStoreContext}.
 */
public class NetconfOperationServiceImpl implements NetconfOperationService {

    private final NetconfOperationProvider operationProvider;
    private final TransactionProvider transactionProvider;

    public NetconfOperationServiceImpl(final YangStoreService yangStoreService, final ConfigRegistryJMXClient jmxClient,
            final String netconfSessionIdForReporting) {

        transactionProvider = new TransactionProvider(jmxClient, netconfSessionIdForReporting);
        operationProvider = new NetconfOperationProvider(yangStoreService, jmxClient, transactionProvider,
                netconfSessionIdForReporting);
    }

    @Override
    public Set<NetconfOperation> getNetconfOperations() {
        return operationProvider.getOperations();
    }

    @Override
    public void close() {
        transactionProvider.close();
    }

}
