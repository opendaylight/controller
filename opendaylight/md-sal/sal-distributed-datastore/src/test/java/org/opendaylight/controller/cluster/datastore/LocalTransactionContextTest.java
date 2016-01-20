/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import akka.actor.ActorSelection;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.Future;

public class LocalTransactionContextTest {

    @Mock
    OperationLimiter limiter;

    @Mock
    TransactionIdentifier identifier;

    @Mock
    DOMStoreReadWriteTransaction readWriteTransaction;

    @Mock
    LocalTransactionReadySupport mockReadySupport;

    LocalTransactionContext localTransactionContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        localTransactionContext = new LocalTransactionContext(readWriteTransaction, limiter.getIdentifier(), mockReadySupport) {
            @Override
            protected DOMStoreWriteTransaction getWriteDelegate() {
                return readWriteTransaction;
            }

            @Override
            protected DOMStoreReadTransaction getReadDelegate() {
                return readWriteTransaction;
            }
        };
    }

    @Test
    public void testWrite() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        localTransactionContext.executeModification(new WriteModification(yangInstanceIdentifier, normalizedNode));
        verify(readWriteTransaction).write(yangInstanceIdentifier, normalizedNode);
    }

    @Test
    public void testMerge() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        localTransactionContext.executeModification(new MergeModification(yangInstanceIdentifier, normalizedNode));
        verify(readWriteTransaction).merge(yangInstanceIdentifier, normalizedNode);
    }

    @Test
    public void testDelete() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        localTransactionContext.executeModification(new DeleteModification(yangInstanceIdentifier));
        verify(readWriteTransaction).delete(yangInstanceIdentifier);
    }


    @Test
    public void testRead() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        doReturn(Futures.immediateCheckedFuture(Optional.of(normalizedNode))).when(readWriteTransaction).read(yangInstanceIdentifier);
        localTransactionContext.executeRead(new ReadData(yangInstanceIdentifier, DataStoreVersions.CURRENT_VERSION),
                SettableFuture.<Optional<NormalizedNode<?,?>>>create());
        verify(readWriteTransaction).read(yangInstanceIdentifier);
    }

    @Test
    public void testExists() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        doReturn(Futures.immediateCheckedFuture(true)).when(readWriteTransaction).exists(yangInstanceIdentifier);
        localTransactionContext.executeRead(new DataExists(yangInstanceIdentifier, DataStoreVersions.CURRENT_VERSION),
                SettableFuture.<Boolean>create());
        verify(readWriteTransaction).exists(yangInstanceIdentifier);
    }

    @Test
    public void testReady() {
        final LocalThreePhaseCommitCohort mockCohort = mock(LocalThreePhaseCommitCohort.class);
        doReturn(akka.dispatch.Futures.successful(null)).when(mockCohort).initiateCoordinatedCommit();
        doReturn(mockCohort).when(mockReadySupport).onTransactionReady(readWriteTransaction, null);

        Future<ActorSelection> future = localTransactionContext.readyTransaction();
        assertTrue(future.isCompleted());

        verify(mockReadySupport).onTransactionReady(readWriteTransaction, null);
    }

    @Test
    public void testReadyWithWriteError() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        RuntimeException error = new RuntimeException("mock");
        doThrow(error).when(readWriteTransaction).write(yangInstanceIdentifier, normalizedNode);

        localTransactionContext.executeModification(new WriteModification(yangInstanceIdentifier, normalizedNode));
        localTransactionContext.executeModification(new WriteModification(yangInstanceIdentifier, normalizedNode));

        verify(readWriteTransaction).write(yangInstanceIdentifier, normalizedNode);

        doReadyWithExpectedError(error);
    }

    @Test
    public void testReadyWithMergeError() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        RuntimeException error = new RuntimeException("mock");
        doThrow(error).when(readWriteTransaction).merge(yangInstanceIdentifier, normalizedNode);

        localTransactionContext.executeModification(new MergeModification(yangInstanceIdentifier, normalizedNode));
        localTransactionContext.executeModification(new MergeModification(yangInstanceIdentifier, normalizedNode));

        verify(readWriteTransaction).merge(yangInstanceIdentifier, normalizedNode);

        doReadyWithExpectedError(error);
    }

    @Test
    public void testReadyWithDeleteError() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        RuntimeException error = new RuntimeException("mock");
        doThrow(error).when(readWriteTransaction).delete(yangInstanceIdentifier);

        localTransactionContext.executeModification(new DeleteModification(yangInstanceIdentifier));
        localTransactionContext.executeModification(new DeleteModification(yangInstanceIdentifier));

        verify(readWriteTransaction).delete(yangInstanceIdentifier);

        doReadyWithExpectedError(error);
    }

    private void doReadyWithExpectedError(RuntimeException expError) {
        LocalThreePhaseCommitCohort mockCohort = mock(LocalThreePhaseCommitCohort.class);
        doReturn(akka.dispatch.Futures.successful(null)).when(mockCohort).initiateCoordinatedCommit();
        doReturn(mockCohort).when(mockReadySupport).onTransactionReady(readWriteTransaction, expError);

        localTransactionContext.readyTransaction();
    }
}
