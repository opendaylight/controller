package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import java.net.URI;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.utils.PruningDataTreeModification;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

public class ShardDataTreeSnapshotTest {

    @Mock
    DataTreeSnapshot dataTreeSnapshot;

    @Mock
    Set<URI> validNamespaces;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testNewModification(){
        ShardDataTreeSnapshot snapshot1
                = new ShardDataTreeSnapshot(dataTreeSnapshot, validNamespaces);

        DataTreeModification dataTreeModification1 = snapshot1.newModification();

        assertTrue(dataTreeModification1 instanceof PruningDataTreeModification);
    }

    @Test
    public void testReadNode(){
        ShardDataTreeSnapshot snapshot
                = new ShardDataTreeSnapshot(dataTreeSnapshot, validNamespaces);

        snapshot.readNode(CarsModel.BASE_PATH);

        verify(dataTreeSnapshot).readNode(CarsModel.BASE_PATH);
    }
}