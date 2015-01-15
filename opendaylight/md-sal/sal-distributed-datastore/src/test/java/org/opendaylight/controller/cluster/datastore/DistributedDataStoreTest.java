package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import akka.util.Timeout;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.duration.FiniteDuration;

public class DistributedDataStoreTest extends AbstractActorTest {

    private SchemaContext schemaContext;

    @Mock
    private ActorContext actorContext;

    @Mock
    private DatastoreContext datastoreContext;

    @Mock
    private Timeout shardElectionTimeout;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        schemaContext = TestModel.createTestContext();

        doReturn(schemaContext).when(actorContext).getSchemaContext();
        doReturn(DatastoreContext.newBuilder().build()).when(actorContext).getDatastoreContext();
    }

    @Test
    public void testRateLimitingUsedInReadWriteTxCreation(){
        DistributedDataStore distributedDataStore = new DistributedDataStore(actorContext);

        distributedDataStore.newReadWriteTransaction();

        verify(actorContext, times(1)).acquireTxCreationPermit();
    }

    @Test
    public void testRateLimitingUsedInWriteOnlyTxCreation(){
        DistributedDataStore distributedDataStore = new DistributedDataStore(actorContext);

        distributedDataStore.newWriteOnlyTransaction();

        verify(actorContext, times(1)).acquireTxCreationPermit();
    }


    @Test
    public void testRateLimitingNotUsedInReadOnlyTxCreation(){
        DistributedDataStore distributedDataStore = new DistributedDataStore(actorContext);

        distributedDataStore.newReadOnlyTransaction();
        distributedDataStore.newReadOnlyTransaction();
        distributedDataStore.newReadOnlyTransaction();

        verify(actorContext, times(0)).acquireTxCreationPermit();
    }

    @Test
    public void testWaitTillReadyBlocking(){
        doReturn(datastoreContext).when(actorContext).getDatastoreContext();
        doReturn(shardElectionTimeout).when(datastoreContext).getShardLeaderElectionTimeout();
        doReturn(FiniteDuration.apply(50, TimeUnit.MILLISECONDS)).when(shardElectionTimeout).duration();
        DistributedDataStore distributedDataStore = new DistributedDataStore(actorContext);

        long start = System.currentTimeMillis();

        distributedDataStore.waitTillReady();

        long end = System.currentTimeMillis();

        assertTrue("Expected to be blocked for 50 millis", (end-start) >= 50);
    }

    @Test
    public void testWaitTillReadyRelease(){
        final DistributedDataStore distributedDataStore = new DistributedDataStore(actorContext);
        doReturn(datastoreContext).when(actorContext).getDatastoreContext();
        doReturn(shardElectionTimeout).when(datastoreContext).getShardLeaderElectionTimeout();
        doReturn(FiniteDuration.apply(5000, TimeUnit.MILLISECONDS)).when(shardElectionTimeout).duration();

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
                distributedDataStore.getReady().release();
            }
        });

        long start = System.currentTimeMillis();

        distributedDataStore.waitTillReady();

        long end = System.currentTimeMillis();

        assertTrue("Expected to be released in 500 millis", (end-start) < 5000);

    }

}