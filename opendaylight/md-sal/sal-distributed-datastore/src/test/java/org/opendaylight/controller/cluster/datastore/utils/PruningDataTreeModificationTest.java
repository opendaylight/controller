package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.net.URI;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;

public class PruningDataTreeModificationTest {

    @Mock
    DataTreeModification delegate;

    @Mock
    Set<URI> validNamespaces;

    @Mock
    NormalizedNode<?,?> prunedNormalizedNode;

    PruningDataTreeModification pruningDataTreeModification;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        pruningDataTreeModification = new PruningDataTreeModification(delegate, validNamespaces) {
            @Override
            NormalizedNode<?, ?> pruneNormalizedNode(NormalizedNode<?, ?> input) {
                return prunedNormalizedNode;
            }
        };
    }

    @Test
    public void testDelete(){
        pruningDataTreeModification.delete(CarsModel.BASE_PATH);

        verify(delegate).delete(CarsModel.BASE_PATH);
    }

    @Test
    public void testMerge(){
        NormalizedNode<?, ?> normalizedNode = CarsModel.create();
        YangInstanceIdentifier path = CarsModel.BASE_PATH;
        pruningDataTreeModification.merge(path, normalizedNode);

        verify(delegate, times(1)).merge(path, normalizedNode);
    }

    @Test
    public void testMergeOnException(){
        NormalizedNode<?, ?> normalizedNode = CarsModel.create();
        YangInstanceIdentifier path = CarsModel.BASE_PATH;

        doThrow(IllegalArgumentException.class).when(delegate).merge(path, normalizedNode);
        doReturn(true).when(validNamespaces).contains(any(YangInstanceIdentifier.PathArgument.class));

        try {
            pruningDataTreeModification.merge(path, normalizedNode);
        } catch(Exception e){
            assertTrue(e instanceof IllegalArgumentException);
        }

        verify(delegate, times(1)).merge(path, normalizedNode);
        verify(delegate, times(1)).merge(path, prunedNormalizedNode);
    }

    @Test
    public void testWrite(){
        NormalizedNode<?, ?> normalizedNode = CarsModel.create();
        YangInstanceIdentifier path = CarsModel.BASE_PATH;
        pruningDataTreeModification.write(path, normalizedNode);

        verify(delegate, times(1)).write(path, normalizedNode);
    }

    @Test
    public void testWriteOnException(){
        NormalizedNode<?, ?> normalizedNode = CarsModel.create();
        YangInstanceIdentifier path = CarsModel.BASE_PATH;

        doThrow(IllegalArgumentException.class).when(delegate).write(path, normalizedNode);
        doReturn(true).when(validNamespaces).contains(any(YangInstanceIdentifier.PathArgument.class));

        try {
            pruningDataTreeModification.write(path, normalizedNode);
        } catch(Exception e){
            assertTrue(e instanceof IllegalArgumentException);
        }

        verify(delegate, times(1)).write(path, normalizedNode);
        verify(delegate, times(1)).write(path, prunedNormalizedNode);
    }

    @Test
    public void testReady(){
        pruningDataTreeModification.ready();

        verify(delegate).ready();
    }

    @Test
    public void testApplyToCursor(){
        DataTreeModificationCursor dataTreeModificationCursor = mock(DataTreeModificationCursor.class);
        pruningDataTreeModification.applyToCursor(dataTreeModificationCursor);

        verify(delegate).applyToCursor(dataTreeModificationCursor);
    }

    @Test
    public void testReadNode(){
        pruningDataTreeModification.readNode(CarsModel.BASE_PATH);

        verify(delegate).readNode(CarsModel.BASE_PATH);
    }

    @Test
    public void testNewModification(){
        DataTreeModification dataTreeModification = pruningDataTreeModification.newModification();

        assertTrue("new modification not of type PruningDataTreeModification", dataTreeModification instanceof PruningDataTreeModification);
    }
}