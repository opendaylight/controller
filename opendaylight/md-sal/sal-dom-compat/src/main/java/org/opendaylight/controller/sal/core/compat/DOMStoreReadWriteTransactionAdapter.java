/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DOMStoreReadWriteTransactionAdapter
        extends DOMStoreReadTransactionAdapter<org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction>
        implements DOMStoreReadWriteTransaction {
    public DOMStoreReadWriteTransactionAdapter(
            final org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction delegate) {
        super(delegate);
    }

    @Override
    public void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        delegate().write(path, data);
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        return new DOMStoreThreePhaseCommitCohortAdapter(delegate().ready());
    }

    @Override
    public void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        delegate().merge(path, data);
    }

    @Override
    public void delete(final YangInstanceIdentifier path) {
        delegate().delete(path);
    }
}