/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.fromMdsal;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Optional;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class TransactionUtils {
    private TransactionUtils() {

    }

    static FluentFuture<Boolean> exists(final DOMDataReadTransaction tx,
            final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        return FluentFuture.from(tx.exists(fromMdsal(store), path));
    }

    static FluentFuture<Optional<NormalizedNode<?, ?>>> read(final DOMDataReadTransaction tx,
            final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        return FluentFuture.from(tx.read(fromMdsal(store), path)).transform(opt -> opt.toJavaUtil(),
            MoreExecutors.directExecutor());
    }

    static void delete(final DOMDataWriteTransaction tx, final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        tx.delete(fromMdsal(store), path);
    }

    static void merge(final DOMDataWriteTransaction tx, final LogicalDatastoreType store,
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        tx.merge(fromMdsal(store), path, data);
    }

    static void put(final DOMDataWriteTransaction tx, final LogicalDatastoreType store,
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        tx.put(fromMdsal(store), path, data);
    }
}
