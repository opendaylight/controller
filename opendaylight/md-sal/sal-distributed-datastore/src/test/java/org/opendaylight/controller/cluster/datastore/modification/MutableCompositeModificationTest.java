package org.opendaylight.controller.cluster.datastore.modification;

import java.util.List;
import com.google.common.base.Optional;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MutableCompositeModificationTest extends AbstractModificationTest {

    @Test
    public void testApply() throws Exception {

        MutableCompositeModification compositeModification = new MutableCompositeModification();
        compositeModification.addModification(new WriteModification(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME)));

        DOMStoreReadWriteTransaction transaction = store.newReadWriteTransaction();
        compositeModification.apply(transaction);
        commitTransaction(transaction);

        Optional<NormalizedNode<?, ?>> data = readData(TestModel.TEST_PATH);

        assertNotNull(data.get());
        assertEquals(TestModel.TEST_QNAME, data.get().getNodeType());
    }

    @Test
    public void testSerialization(){
        MutableCompositeModification expected = new MutableCompositeModification();
        expected.addModification(new WriteModification(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME)));
        expected.addModification(new MergeModification(TestModel.OUTER_LIST_PATH,
                ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build()));
        expected.addModification(new DeleteModification(TestModel.TEST_PATH));

        MutableCompositeModification actual =
                (MutableCompositeModification) SerializationUtils.clone(expected);

        List<Modification> expectedMods = expected.getModifications();
        List<Modification> actualMods = actual.getModifications();
        assertEquals("getModifications size", expectedMods.size(), actualMods.size());
        for(int i = 0; i < expectedMods.size(); i++ ) {
            Modification expectedMod = expectedMods.get(i);
            Modification actualMod = expectedMods.get(i);
            assertEquals("getClass", expectedMod.getClass(), actualMod.getClass());
            assertEquals("getPath", ((AbstractModification)expectedMod).getPath(),
                    ((AbstractModification)actualMod).getPath());
            if(expectedMod instanceof WriteModification) {
                assertEquals("getData", ((WriteModification)expectedMod).getData(),
                        ((WriteModification)actualMod).getData());
            } else if(expectedMod instanceof MergeModification) {
                assertEquals("getData", ((MergeModification)expectedMod).getData(),
                        ((MergeModification)actualMod).getData());
            }
        }
    }
}
