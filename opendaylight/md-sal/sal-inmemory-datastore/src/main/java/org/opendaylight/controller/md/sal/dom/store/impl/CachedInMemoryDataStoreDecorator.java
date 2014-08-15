/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class CachedInMemoryDataStoreDecorator extends InMemoryDOMDataStoreAbstractDecorator {

    private YangInstanceIdentifier cachingWildcardedPath; // path on which caching should be applied
    private NormalizedNodeCache normalizedNodeCache = new NormalizedNodeCache(); // cache of normalizedNodes

    public CachedInMemoryDataStoreDecorator(InMemoryDOMDataStore domDataStoreToBeDecorated, YangInstanceIdentifier cachingWildcardedPath) {
        super(domDataStoreToBeDecorated);
        this.cachingWildcardedPath = cachingWildcardedPath;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return super.newReadOnlyTransaction();
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        DOMStoreReadWriteTransaction domStoreReadWriteTransaction = super.newReadWriteTransaction();
        return new CachedSnapshotBackedReadWriteTransaction(normalizedNodeCache, cachingWildcardedPath, domStoreReadWriteTransaction);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        DOMStoreWriteTransaction domStoreWriteTransaction = super.newWriteOnlyTransaction();
        return new CachedSnapshotBackedWriteTransaction(normalizedNodeCache, cachingWildcardedPath, domStoreWriteTransaction);
    }
}
