/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.sal;

import java.util.Collections;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.sal.tx.ReadOnlyTx;
import org.opendaylight.controller.sal.connect.netconf.sal.tx.ReadWriteTx;
import org.opendaylight.controller.sal.connect.netconf.sal.tx.WriteCandidateRunningTx;
import org.opendaylight.controller.sal.connect.netconf.sal.tx.WriteCandidateTx;
import org.opendaylight.controller.sal.connect.netconf.sal.tx.WriteRunningTx;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfBaseOps;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

final class NetconfDeviceDataBroker implements DOMDataBroker {
    private final RemoteDeviceId id;
    private final NetconfBaseOps netconfOps;
    private final long requestTimeoutMillis;

    private final boolean rollbackSupport;
    private boolean candidateSupported;
    private boolean runningWritable;

    public NetconfDeviceDataBroker(final RemoteDeviceId id, final SchemaContext schemaContext, final DOMRpcService rpc, final NetconfSessionPreferences netconfSessionPreferences, long requestTimeoutMillis) {
        this.id = id;
        this.netconfOps = new NetconfBaseOps(rpc, schemaContext);
        this.requestTimeoutMillis = requestTimeoutMillis;
        // get specific attributes from netconf preferences and get rid of it
        // no need to keep the entire preferences object, its quite big with all the capability QNames
        candidateSupported = netconfSessionPreferences.isCandidateSupported();
        runningWritable = netconfSessionPreferences.isRunningWritable();
        rollbackSupport = netconfSessionPreferences.isRollbackSupported();
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return new ReadOnlyTx(netconfOps, id);
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return new ReadWriteTx(newReadOnlyTransaction(), newWriteOnlyTransaction());
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        if(candidateSupported) {
            if(runningWritable) {
                return new WriteCandidateRunningTx(id, netconfOps, rollbackSupport, requestTimeoutMillis);
            } else {
                return new WriteCandidateTx(id, netconfOps, rollbackSupport, requestTimeoutMillis);
            }
        } else {
            return new WriteRunningTx(id, netconfOps, rollbackSupport, requestTimeoutMillis);
        }
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store, final YangInstanceIdentifier path, final DOMDataChangeListener listener, final DataChangeScope triggeringScope) {
        throw new UnsupportedOperationException(id + ": Data change listeners not supported for netconf mount point");
    }

    @Override
    public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
        return new CachingTransactionChain(schemaService);
    }

    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return Collections.emptyMap();
    }

}
