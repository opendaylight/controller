/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.modification;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public abstract class AbstractModificationTest {
    private static EffectiveModelContext TEST_SCHEMA_CONTEXT;

    protected InMemoryDOMDataStore store;

    @BeforeClass
    public static void beforeClass() {
        TEST_SCHEMA_CONTEXT = TestModel.createTestContext();
    }

    @AfterClass
    public static void afterClass() {
        TEST_SCHEMA_CONTEXT = null;
    }

    @Before
    public void setUp() {
        store = new InMemoryDOMDataStore("test", MoreExecutors.newDirectExecutorService());
        store.onModelContextUpdated(TEST_SCHEMA_CONTEXT);
    }

    protected void commitTransaction(final DOMStoreWriteTransaction transaction) {
        DOMStoreThreePhaseCommitCohort cohort = transaction.ready();
        cohort.preCommit();
        cohort.commit();
    }

    protected Optional<NormalizedNode> readData(final YangInstanceIdentifier path) throws Exception {
        // FIXME: close the transaction
        DOMStoreReadTransaction transaction = store.newReadOnlyTransaction();
        ListenableFuture<Optional<NormalizedNode>> future = transaction.read(path);
        return future.get();
    }
}
