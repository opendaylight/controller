/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NetconfDeviceReadWriteTx implements DOMDataReadWriteTransaction {

    private final DOMDataReadTransaction readTx;
    private final DOMDataWriteTransaction writeTx;

    public NetconfDeviceReadWriteTx(final DOMDataReadTransaction readTx, final DOMDataWriteTransaction writeTx) {
        this.readTx = readTx;
        this.writeTx = writeTx;
    }

    @Override
    public boolean cancel() {
        return writeTx.cancel();
    }

    @Override
    public void put(final LogicalDatastoreType store, final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
        writeTx.put(store, path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
        writeTx.merge(store, path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final InstanceIdentifier path) {
        writeTx.delete(store, path);
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return writeTx.commit();
    }

    @Override
    public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store, final InstanceIdentifier path) {
        return readTx.read(store, path);
    }

    @Override
    public Object getIdentifier() {
        return this;
    }
}
