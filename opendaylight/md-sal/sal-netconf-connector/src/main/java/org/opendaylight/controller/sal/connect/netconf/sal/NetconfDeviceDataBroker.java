/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.sal;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.connect.netconf.sal.tx.NetconfDeviceReadOnlyTx;
import org.opendaylight.controller.sal.connect.netconf.sal.tx.NetconfDeviceReadWriteTx;
import org.opendaylight.controller.sal.connect.netconf.sal.tx.NetconfDeviceWriteOnlyTx;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

final class NetconfDeviceDataBroker implements DOMDataBroker {
    private final RemoteDeviceId id;
    private final RpcImplementation rpc;
    private final NetconfSessionCapabilities netconfSessionPreferences;
    private final DataNormalizer normalizer;

    public NetconfDeviceDataBroker(final RemoteDeviceId id, final RpcImplementation rpc, final SchemaContext schemaContext, NetconfSessionCapabilities netconfSessionPreferences) {
        this.id = id;
        this.rpc = rpc;
        this.netconfSessionPreferences = netconfSessionPreferences;
        normalizer = new DataNormalizer(schemaContext);
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return new NetconfDeviceReadOnlyTx(rpc, normalizer, id);
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return new NetconfDeviceReadWriteTx(newReadOnlyTransaction(), newWriteOnlyTransaction());
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return new NetconfDeviceWriteOnlyTx(id, rpc, normalizer, netconfSessionPreferences.isCandidateSupported(), netconfSessionPreferences.isRollbackSupported());
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType store, final YangInstanceIdentifier path, final DOMDataChangeListener listener, final DataChangeScope triggeringScope) {
        throw new UnsupportedOperationException("Data change listeners not supported for netconf mount point");
    }

    @Override
    public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
        // TODO implement
        throw new UnsupportedOperationException("Transaction chains not supported for netconf mount point");
    }
}
