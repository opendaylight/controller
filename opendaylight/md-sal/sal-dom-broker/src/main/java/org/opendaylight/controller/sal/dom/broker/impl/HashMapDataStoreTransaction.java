/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class HashMapDataStoreTransaction implements
        DataCommitTransaction<YangInstanceIdentifier, CompositeNode> {
    private final DataModification<YangInstanceIdentifier, CompositeNode> modification;
    private final HashMapDataStore datastore;

    HashMapDataStoreTransaction(
            final DataModification<YangInstanceIdentifier, CompositeNode> modify,
            final HashMapDataStore store) {
        modification = modify;
        datastore = store;
    }

    @Override
    public RpcResult<Void> finish() throws IllegalStateException {
        return datastore.finish(this);
    }

    @Override
    public DataModification<YangInstanceIdentifier, CompositeNode> getModification() {
        return this.modification;
    }

    @Override
    public RpcResult<Void> rollback() throws IllegalStateException {
        return datastore.rollback(this);
    }
}