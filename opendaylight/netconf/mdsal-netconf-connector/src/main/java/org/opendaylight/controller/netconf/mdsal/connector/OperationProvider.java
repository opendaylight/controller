/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector;

import com.google.common.collect.Sets;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mdsal.connector.ops.get.GetConfig;

final class OperationProvider {

    private final String netconfSessionIdForReporting;
    private final CurrentSchemaContext schemaContext;
    private final DOMDataBroker dataBroker;
    private final TransactionProvider transactionProvider;

    public OperationProvider(final String netconfSessionIdForReporting, final CurrentSchemaContext schemaContext, final DOMDataBroker dataBroker) {
        this.netconfSessionIdForReporting = netconfSessionIdForReporting;
        this.schemaContext = schemaContext;
        this.dataBroker = dataBroker;
        this.transactionProvider = new TransactionProvider(dataBroker, netconfSessionIdForReporting);

    }

    Set<NetconfOperation> getOperations() {
        return Sets.<NetconfOperation>newHashSet(
                new GetConfig(netconfSessionIdForReporting, schemaContext, transactionProvider)
        );
    }

}
