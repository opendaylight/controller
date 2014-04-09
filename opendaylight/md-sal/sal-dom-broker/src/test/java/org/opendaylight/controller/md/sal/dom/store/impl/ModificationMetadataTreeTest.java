package org.opendaylight.controller.md.sal.dom.store.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.md.sal.dom.store.impl.TestModel.ID_QNAME;
import static org.opendaylight.controller.md.sal.dom.store.impl.TestModel.INNER_LIST_QNAME;
import static org.opendaylight.controller.md.sal.dom.store.impl.TestModel.NAME_QNAME;
import static org.opendaylight.controller.md.sal.dom.store.impl.TestModel.OUTER_LIST_PATH;
import static org.opendaylight.controller.md.sal.dom.store.impl.TestModel.OUTER_LIST_QNAME;
import static org.opendaylight.controller.md.sal.dom.store.impl.TestModel.TEST_PATH;
import static org.opendaylight.controller.md.sal.dom.store.impl.TestModel.TEST_QNAME;
import static org.opendaylight.controller.md.sal.dom.store.impl.TestModel.VALUE_QNAME;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntry;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntryBuilder;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapNodeBuilder;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedLong;

/**
 *
 * Schema structure of document is
 *
 * <pre>
 * container root { 
 *      list list-a {
 *              key leaf-a;
 *              leaf leaf-a;
 *              choice choice-a {
 *                      case one {
 *                              leaf one;
 *                      }
 *                      case two-three {
 *                              leaf two;
 *                              leaf three;
 *                      }
 *              }
 *              list list-b {
 *                      key leaf-b;
 *                      leaf leaf-b;
 *              }
 *      }
 * }
 * </pre>
 *
 */
public class ModificationMetadataTreeTest {

    private static final Short ONE_ID = 1;
    private static final Short TWO_ID = 2;
    private static final String TWO_ONE_NAME = "one";
    private static final String TWO_TWO_NAME = "two";

    private static final InstanceIdentifier OUTER_LIST_1_PATH = InstanceIdentifier.builder(OUTER_LIST_PATH)
            .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, ONE_ID) //
            .build();

    private static final InstanceIdentifier OUTER_LIST_2_PATH = InstanceIdentifier.builder(OUTER_LIST_PATH)
            .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, TWO_ID) //
            .build();

    private static final InstanceIdentifier TWO_TWO_PATH = InstanceIdentifier.builder(OUTER_LIST_2_PATH)
            .node(INNER_LIST_QNAME) //
            .nodeWithKey(INNER_LIST_QNAME, NAME_QNAME, TWO_TWO_NAME) //
            .build();

    private static final InstanceIdentifier TWO_TWO_VALUE_PATH = InstanceIdentifier.builder(TWO_TWO_PATH)
            .node(VALUE_QNAME) //
            .build();

    private static final MapEntryNode BAR_NODE = mapEntryBuilder(OUTER_LIST_QNAME, ID_QNAME, TWO_ID) //
            .withChild(mapNodeBuilder(INNER_LIST_QNAME) //
                    .withChild(mapEntry(INNER_LIST_QNAME, NAME_QNAME, TWO_ONE_NAME)) //
                    .withChild(mapEntry(INNER_LIST_QNAME, NAME_QNAME, TWO_TWO_NAME)) //
                    .build()) //
            .build();

    private SchemaContext schemaContext;

    @Before
    public void prepare() {
        schemaContext = TestModel.createTestContext();
        assertNotNull("Schema context must not be null.", schemaContext);
    }

    /**
     * Returns a test document
     *
     * <pre>
     * test
     *     outer-list
     *          id 1
     *     outer-list
     *          id 2
     *          inner-list
     *                  name "one"
     *          inner-list
     *                  name "two"
     *
     * </pre>
     *
     * @return
     */
    public NormalizedNode<?, ?> createDocumentOne() {
        return ImmutableContainerNodeBuilder
                .create()
                .withNodeIdentifier(new NodeIdentifier(schemaContext.getQName()))
                .withChild(createTestContainer()).build();

    }

    private ContainerNode createTestContainer() {
        return ImmutableContainerNodeBuilder
                .create()
                .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
                .withChild(
                        mapNodeBuilder(OUTER_LIST_QNAME)
                                .withChild(mapEntry(OUTER_LIST_QNAME, ID_QNAME, ONE_ID))
                                .withChild(BAR_NODE).build()).build();
    }

    @Test
    public void basicReadWrites() {
        MutableDataTree modificationTree = MutableDataTree.from(
                DataAndMetadataSnapshot.builder() //
                        .setMetadataTree(StoreMetadataNode.createRecursively(createDocumentOne(), UnsignedLong.valueOf(5))) //
                        .setSchemaContext(schemaContext) //
                        .build(), new SchemaAwareApplyOperationRoot(schemaContext));
        Optional<NormalizedNode<?, ?>> originalBarNode = modificationTree.read(OUTER_LIST_2_PATH);
        assertTrue(originalBarNode.isPresent());
        assertSame(BAR_NODE, originalBarNode.get());

        // writes node to /outer-list/1/inner_list/two/value
        modificationTree.write(TWO_TWO_VALUE_PATH, ImmutableNodes.leafNode(VALUE_QNAME, "test"));

        // reads node to /outer-list/1/inner_list/two/value
        // and checks if node is already present
        Optional<NormalizedNode<?, ?>> barTwoCModified = modificationTree.read(TWO_TWO_VALUE_PATH);
        assertTrue(barTwoCModified.isPresent());
        assertEquals(ImmutableNodes.leafNode(VALUE_QNAME, "test"), barTwoCModified.get());

        // delete node to /outer-list/1/inner_list/two/value
        modificationTree.delete(TWO_TWO_VALUE_PATH);
        Optional<NormalizedNode<?, ?>> barTwoCAfterDelete = modificationTree.read(TWO_TWO_VALUE_PATH);
        assertFalse(barTwoCAfterDelete.isPresent());
    }


    public MutableDataTree createEmptyModificationTree() {
        /**
         * Creates empty Snapshot with associated schema context.
         */
        DataAndMetadataSnapshot emptySnapshot = DataAndMetadataSnapshot.createEmpty(schemaContext);

        /**
         *
         * Creates Mutable Data Tree based on provided snapshot and schema
         * context.
         *
         */
        MutableDataTree modificationTree = MutableDataTree.from(emptySnapshot, new SchemaAwareApplyOperationRoot(
                schemaContext));
        return modificationTree;
    }

    @Test
    public void createFromEmptyState() {

        MutableDataTree modificationTree = createEmptyModificationTree();
        /**
         * Writes empty container node to /test
         *
         */
        modificationTree.write(TEST_PATH, ImmutableNodes.containerNode(TEST_QNAME));

        /**
         * Writes empty list node to /test/outer-list
         */
        modificationTree.write(OUTER_LIST_PATH, ImmutableNodes.mapNodeBuilder(OUTER_LIST_QNAME).build());

        /**
         * Reads list node from /test/outer-list
         */
        Optional<NormalizedNode<?, ?>> potentialOuterList = modificationTree.read(OUTER_LIST_PATH);
        assertTrue(potentialOuterList.isPresent());

        /**
         * Reads container node from /test and verifies that it contains test
         * node
         */
        Optional<NormalizedNode<?, ?>> potentialTest = modificationTree.read(TEST_PATH);
        ContainerNode containerTest = assertPresentAndType(potentialTest, ContainerNode.class);

        /**
         *
         * Gets list from returned snapshot of /test and verifies it contains
         * outer-list
         *
         */
        assertPresentAndType(containerTest.getChild(new NodeIdentifier(OUTER_LIST_QNAME)), MapNode.class);

    }

    @Test
    public void writeSubtreeReadChildren() {
        MutableDataTree modificationTree = createEmptyModificationTree();
        modificationTree.write(TEST_PATH, createTestContainer());
        Optional<NormalizedNode<?, ?>> potential = modificationTree.read(TWO_TWO_PATH);
        MapEntryNode node = assertPresentAndType(potential, MapEntryNode.class);
    }

    @Test
    public void writeSubtreeDeleteChildren() {
        MutableDataTree modificationTree = createEmptyModificationTree();
        modificationTree.write(TEST_PATH, createTestContainer());

        // We verify data are present
        Optional<NormalizedNode<?, ?>> potentialBeforeDelete = modificationTree.read(TWO_TWO_PATH);
        MapEntryNode node = assertPresentAndType(potentialBeforeDelete, MapEntryNode.class);

        modificationTree.delete(TWO_TWO_PATH);
        Optional<NormalizedNode<?, ?>> potentialAfterDelete = modificationTree.read(TWO_TWO_PATH);
        assertFalse(potentialAfterDelete.isPresent());

    }

    private static <T> T assertPresentAndType(final Optional<?> potential, final Class<T> type) {
        assertNotNull(potential);
        assertTrue(potential.isPresent());
        assertTrue(type.isInstance(potential.get()));
        return type.cast(potential.get());
    }

}
