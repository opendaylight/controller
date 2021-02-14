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
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.Future;

public class LocalTransactionContextTest {

    @Mock
    private OperationLimiter limiter;

    @Mock
    private DOMStoreReadWriteTransaction readWriteTransaction;

    @Mock
    private LocalTransactionReadySupport mockReadySupport;

    private LocalTransactionContext localTransactionContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        localTransactionContext = new LocalTransactionContext(readWriteTransaction, limiter.getIdentifier(),
                mockReadySupport) {
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
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.empty();
        NormalizedNode normalizedNode = mock(NormalizedNode.class);
        localTransactionContext.executeWrite(yangInstanceIdentifier, normalizedNode, null);
        verify(readWriteTransaction).write(yangInstanceIdentifier, normalizedNode);
    }

    @Test
    public void testMerge() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.empty();
        NormalizedNode normalizedNode = mock(NormalizedNode.class);
        localTransactionContext.executeMerge(yangInstanceIdentifier, normalizedNode, null);
        verify(readWriteTransaction).merge(yangInstanceIdentifier, normalizedNode);
    }

    @Test
    public void testDelete() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.empty();
        localTransactionContext.executeDelete(yangInstanceIdentifier, null);
        verify(readWriteTransaction).delete(yangInstanceIdentifier);
    }

    @Test
    public void testRead() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.empty();
        NormalizedNode normalizedNode = mock(NormalizedNode.class);
        doReturn(FluentFutures.immediateFluentFuture(Optional.of(normalizedNode))).when(readWriteTransaction)
            .read(yangInstanceIdentifier);
        localTransactionContext.executeRead(new ReadData(yangInstanceIdentifier, DataStoreVersions.CURRENT_VERSION),
                SettableFuture.create(), null);
        verify(readWriteTransaction).read(yangInstanceIdentifier);
    }

    @Test
    public void testExists() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.empty();
        doReturn(FluentFutures.immediateTrueFluentFuture()).when(readWriteTransaction).exists(yangInstanceIdentifier);
        localTransactionContext.executeRead(new DataExists(yangInstanceIdentifier, DataStoreVersions.CURRENT_VERSION),
                SettableFuture.create(), null);
        verify(readWriteTransaction).exists(yangInstanceIdentifier);
    }

    @Test
    public void testReady() {
        final LocalThreePhaseCommitCohort mockCohort = mock(LocalThreePhaseCommitCohort.class);
        doReturn(akka.dispatch.Futures.successful(null)).when(mockCohort).initiateCoordinatedCommit(Optional.empty());
        doReturn(mockCohort).when(mockReadySupport).onTransactionReady(readWriteTransaction, null);

        Future<ActorSelection> future = localTransactionContext.readyTransaction(null, Optional.empty());
        assertTrue(future.isCompleted());

        verify(mockReadySupport).onTransactionReady(readWriteTransaction, null);
    }

    @Test
    public void testReadyWithWriteError() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.empty();
        NormalizedNode normalizedNode = mock(NormalizedNode.class);
        RuntimeException error = new RuntimeException("mock");
        doThrow(error).when(readWriteTransaction).write(yangInstanceIdentifier, normalizedNode);

        localTransactionContext.executeWrite(yangInstanceIdentifier, normalizedNode, null);
        localTransactionContext.executeWrite(yangInstanceIdentifier, normalizedNode, null);

        verify(readWriteTransaction).write(yangInstanceIdentifier, normalizedNode);

        doReadyWithExpectedError(error);
    }

    @Test
    public void testReadyWithMergeError() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.empty();
        NormalizedNode normalizedNode = mock(NormalizedNode.class);
        RuntimeException error = new RuntimeException("mock");
        doThrow(error).when(readWriteTransaction).merge(yangInstanceIdentifier, normalizedNode);

        localTransactionContext.executeMerge(yangInstanceIdentifier, normalizedNode, null);
        localTransactionContext.executeMerge(yangInstanceIdentifier, normalizedNode, null);

        verify(readWriteTransaction).merge(yangInstanceIdentifier, normalizedNode);

        doReadyWithExpectedError(error);
    }

    @Test
    public void testReadyWithDeleteError() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.empty();
        RuntimeException error = new RuntimeException("mock");
        doThrow(error).when(readWriteTransaction).delete(yangInstanceIdentifier);

        localTransactionContext.executeDelete(yangInstanceIdentifier, null);
        localTransactionContext.executeDelete(yangInstanceIdentifier, null);

        verify(readWriteTransaction).delete(yangInstanceIdentifier);

        doReadyWithExpectedError(error);
    }

    private void doReadyWithExpectedError(final RuntimeException expError) {
        LocalThreePhaseCommitCohort mockCohort = mock(LocalThreePhaseCommitCohort.class);
        doReturn(akka.dispatch.Futures.successful(null)).when(mockCohort).initiateCoordinatedCommit(Optional.empty());
        doReturn(mockCohort).when(mockReadySupport).onTransactionReady(readWriteTransaction, expError);

        localTransactionContext.readyTransaction(null, Optional.empty());
    }
}
