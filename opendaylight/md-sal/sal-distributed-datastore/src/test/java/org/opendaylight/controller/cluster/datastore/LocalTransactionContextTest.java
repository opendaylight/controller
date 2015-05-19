package org.opendaylight.controller.cluster.datastore;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
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

    LocalTransactionContext localTransactionContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        localTransactionContext = new LocalTransactionContext(readWriteTransaction, limiter);
    }

    @Test
    public void testWrite() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        localTransactionContext.writeData(yangInstanceIdentifier, normalizedNode);
        verify(limiter).release();
        verify(readWriteTransaction).write(yangInstanceIdentifier, normalizedNode);
    }

    @Test
    public void testMerge() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        localTransactionContext.mergeData(yangInstanceIdentifier, normalizedNode);
        verify(limiter).release();
        verify(readWriteTransaction).merge(yangInstanceIdentifier, normalizedNode);
    }

    @Test
    public void testDelete() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        localTransactionContext.deleteData(yangInstanceIdentifier);
        verify(limiter).release();
        verify(readWriteTransaction).delete(yangInstanceIdentifier);
    }


    @Test
    public void testRead() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        NormalizedNode<?, ?> normalizedNode = mock(NormalizedNode.class);
        doReturn(Futures.immediateCheckedFuture(Optional.of(normalizedNode))).when(readWriteTransaction).read(yangInstanceIdentifier);
        localTransactionContext.readData(yangInstanceIdentifier, SettableFuture.<Optional<NormalizedNode<?,?>>>create());
        verify(limiter).release();
        verify(readWriteTransaction).read(yangInstanceIdentifier);
    }

    @Test
    public void testExists() {
        YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().build();
        doReturn(Futures.immediateCheckedFuture(true)).when(readWriteTransaction).exists(yangInstanceIdentifier);
        localTransactionContext.dataExists(yangInstanceIdentifier, SettableFuture.<Boolean> create());
        verify(limiter).release();
        verify(readWriteTransaction).exists(yangInstanceIdentifier);
    }

    @Test
    public void testReady() {
        final LocalThreePhaseCommitCohort mockCohort = mock(LocalThreePhaseCommitCohort.class);
        doReturn(mock(ActorContext.class)).when(mockCohort).getActorContext();
        doReturn(mock(Future.class)).when(mockCohort).initiateCoordinatedCommit();
        doReturn(mockCohort).when(readWriteTransaction).ready();
        localTransactionContext.readyTransaction();
        verify(limiter).release();
        verify(readWriteTransaction).ready();
    }


}