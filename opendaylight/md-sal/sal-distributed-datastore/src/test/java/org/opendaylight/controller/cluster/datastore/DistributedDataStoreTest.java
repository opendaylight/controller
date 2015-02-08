package org.opendaylight.controller.cluster.datastore;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DistributedDataStoreTest extends AbstractActorTest {

    private SchemaContext schemaContext;

    @Mock
    private ActorContext actorContext;

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

}