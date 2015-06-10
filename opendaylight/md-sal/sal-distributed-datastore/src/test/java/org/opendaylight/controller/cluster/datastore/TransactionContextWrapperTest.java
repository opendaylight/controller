package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;

public class TransactionContextWrapperTest {

    @Mock
    TransactionIdentifier identifier;

    @Mock
    ActorContext actorContext;

    @Mock
    TransactionContext transactionContext;

    TransactionContextWrapper transactionContextWrapper;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        doReturn(DatastoreContext.newBuilder().build()).when(actorContext).getDatastoreContext();
        transactionContextWrapper = new TransactionContextWrapper(identifier, actorContext);
    }

    @Test
    public void testExecutePriorTransactionOperations(){
        for(int i=0;i<100;i++) {
            transactionContextWrapper.maybeExecuteTransactionOperation(mock(TransactionOperation.class));
        }
        assertEquals(901, transactionContextWrapper.getLimiter().availablePermits());

        transactionContextWrapper.executePriorTransactionOperations(transactionContext);

        assertEquals(1001, transactionContextWrapper.getLimiter().availablePermits());
    }
}