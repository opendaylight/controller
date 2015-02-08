package org.opendaylight.controller.cluster.datastore.modification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

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
    public void testSerialization() {
        YangInstanceIdentifier writePath = TestModel.TEST_PATH;
        NormalizedNode<?, ?> writeData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        YangInstanceIdentifier mergePath = TestModel.OUTER_LIST_PATH;
        NormalizedNode<?, ?> mergeData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.OUTER_LIST_QNAME)).build();

        YangInstanceIdentifier deletePath = TestModel.TEST_PATH;

        MutableCompositeModification compositeModification = new MutableCompositeModification();
        compositeModification.addModification(new WriteModification(writePath, writeData));
        compositeModification.addModification(new MergeModification(mergePath, mergeData));
        compositeModification.addModification(new DeleteModification(deletePath));

        MutableCompositeModification clone = (MutableCompositeModification) SerializationUtils.clone(compositeModification);

        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, clone.getVersion());

        assertEquals("getModifications size", 3, clone.getModifications().size());

        WriteModification write = (WriteModification)clone.getModifications().get(0);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, write.getVersion());
        assertEquals("getPath", writePath, write.getPath());
        assertEquals("getData", writeData, write.getData());

        MergeModification merge = (MergeModification)clone.getModifications().get(1);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, merge.getVersion());
        assertEquals("getPath", mergePath, merge.getPath());
        assertEquals("getData", mergeData, merge.getData());

        DeleteModification delete = (DeleteModification)clone.getModifications().get(2);
        assertEquals("getVersion", DataStoreVersions.CURRENT_VERSION, delete.getVersion());
        assertEquals("getPath", deletePath, delete.getPath());
    }

    @Test
    @Ignore
    public void testSerializationScale() throws Exception {
        YangInstanceIdentifier writePath = TestModel.TEST_PATH;
        NormalizedNode<?, ?> writeData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

        MutableCompositeModification compositeModification = new MutableCompositeModification();
        for(int i = 0; i < 1000; i++) {
            compositeModification.addModification(new WriteModification(writePath, writeData));
        }

        Stopwatch sw = new Stopwatch();
        sw.start();
        for(int i = 0; i < 1000; i++) {
            new ModificationPayload(compositeModification);
        }

        sw.stop();
        System.out.println("Elapsed: "+sw);

        ModificationPayload p = new ModificationPayload(compositeModification);
        sw.start();
        for(int i = 0; i < 1000; i++) {
            p.getModification();
        }

        sw.stop();
        System.out.println("Elapsed: "+sw);
    }
}
