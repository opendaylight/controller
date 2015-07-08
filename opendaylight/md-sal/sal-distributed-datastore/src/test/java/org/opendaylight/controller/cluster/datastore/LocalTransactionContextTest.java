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
        localTransactionContext.writeData(yangInstanceIdentifier, normalizedNode);
        verify(readWriteTransaction).write(yangInstanceIdentifier, normalizedNode);
    }

    @Test
    public void testMerge() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        localTransactionContext.mergeData(yangInstanceIdentifier, normalizedNode);
        verify(readWriteTransaction).merge(yangInstanceIdentifier, normalizedNode);
    }

    @Test
    public void testDelete() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        localTransactionContext.deleteData(yangInstanceIdentifier);
        verify(readWriteTransaction).delete(yangInstanceIdentifier);
    }


    @Test
    public void testRead() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        doReturn(Futures.immediateCheckedFuture(Optional.of(normalizedNode))).when(readWriteTransaction).read(yangInstanceIdentifier);
        localTransactionContext.readData(yangInstanceIdentifier, SettableFuture.<Optional<NormalizedNode<?,?>>>create());
        verify(readWriteTransaction).read(yangInstanceIdentifier);
    }

    @Test
    public void testExists() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        doReturn(Futures.immediateCheckedFuture(true)).when(readWriteTransaction).exists(yangInstanceIdentifier);
        localTransactionContext.dataExists(yangInstanceIdentifier, SettableFuture.<Boolean> create());
        verify(readWriteTransaction).exists(yangInstanceIdentifier);
    }

    @Test
    public void testReady() {
        final LocalThreePhaseCommitCohort mockCohort = mock(LocalThreePhaseCommitCohort.class);
        doReturn(akka.dispatch.Futures.successful(null)).when(mockCohort).initiateCoordinatedCommit();
        doReturn(mockCohort).when(mockReadySupport).onTransactionReady(readWriteTransaction);

        Future<ActorSelection> future = localTransactionContext.readyTransaction();
        assertTrue(future.isCompleted());

        verify(mockReadySupport).onTransactionReady(readWriteTransaction);
    }

    @Test
    public void testReadyWithWriteError() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        RuntimeException error = new RuntimeException("mock");
        doThrow(error).when(readWriteTransaction).write(yangInstanceIdentifier, normalizedNode);

        localTransactionContext.writeData(yangInstanceIdentifier, normalizedNode);
        localTransactionContext.writeData(yangInstanceIdentifier, normalizedNode);

        verify(readWriteTransaction).write(yangInstanceIdentifier, normalizedNode);

        doReadyWithExpectedError(error);
    }

    @Test
    public void testReadyWithMergeError() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        RuntimeException error = new RuntimeException("mock");
        doThrow(error).when(readWriteTransaction).merge(yangInstanceIdentifier, normalizedNode);

        localTransactionContext.mergeData(yangInstanceIdentifier, normalizedNode);
        localTransactionContext.mergeData(yangInstanceIdentifier, normalizedNode);

        verify(readWriteTransaction).merge(yangInstanceIdentifier, normalizedNode);

        doReadyWithExpectedError(error);
    }

    @Test
    public void testReadyWithDeleteError() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        RuntimeException error = new RuntimeException("mock");
        doThrow(error).when(readWriteTransaction).delete(yangInstanceIdentifier);

        localTransactionContext.deleteData(yangInstanceIdentifier);
        localTransactionContext.deleteData(yangInstanceIdentifier);

        verify(readWriteTransaction).delete(yangInstanceIdentifier);

        doReadyWithExpectedError(error);
    }

    private void doReadyWithExpectedError(RuntimeException expError) {
        LocalThreePhaseCommitCohort mockCohort = mock(LocalThreePhaseCommitCohort.class);
        doReturn(akka.dispatch.Futures.successful(null)).when(mockCohort).initiateCoordinatedCommit();
        doReturn(mockCohort).when(mockReadySupport).onTransactionReady(readWriteTransaction);

        localTransactionContext.readyTransaction();

        verify(mockCohort).setOperationError(expError);
    }
}
