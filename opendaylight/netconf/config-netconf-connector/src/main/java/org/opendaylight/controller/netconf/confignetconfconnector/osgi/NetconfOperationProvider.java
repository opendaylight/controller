/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.util.Set;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.Commit;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.DiscardChanges;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.Lock;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.UnLock;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.Validate;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.get.Get;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.getconfig.GetConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.runtimerpc.RuntimeRpc;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;

final class NetconfOperationProvider {
    private final Set<NetconfOperation> operations;

    NetconfOperationProvider(YangStoreContext yangStoreSnapshot, ConfigRegistryClient configRegistryClient,
            TransactionProvider transactionProvider, String netconfSessionIdForReporting) {

        operations = setUpOperations(yangStoreSnapshot, configRegistryClient, transactionProvider,
                netconfSessionIdForReporting);
    }

    Set<NetconfOperation> getOperations() {
        return operations;
    }

    private static Set<NetconfOperation> setUpOperations(YangStoreContext yangStoreSnapshot,
            ConfigRegistryClient configRegistryClient, TransactionProvider transactionProvider,
            String netconfSessionIdForReporting) {
        Set<NetconfOperation> ops = Sets.newHashSet();

        GetConfig getConfigOp = new GetConfig(yangStoreSnapshot, Optional.<String> absent(), transactionProvider,
                configRegistryClient, netconfSessionIdForReporting);

        ops.add(getConfigOp);
        ops.add(new EditConfig(yangStoreSnapshot, transactionProvider, configRegistryClient,
                netconfSessionIdForReporting));
        ops.add(new Commit(transactionProvider, configRegistryClient, netconfSessionIdForReporting));
        ops.add(new Lock(netconfSessionIdForReporting));
        ops.add(new UnLock(netconfSessionIdForReporting));
        ops.add(new Get(yangStoreSnapshot, configRegistryClient, netconfSessionIdForReporting));
        ops.add(new DiscardChanges(transactionProvider, configRegistryClient, netconfSessionIdForReporting));
        ops.add(new Validate(transactionProvider, configRegistryClient, netconfSessionIdForReporting));
        ops.add(new RuntimeRpc(yangStoreSnapshot, configRegistryClient, netconfSessionIdForReporting));

        return ops;
    }

}
