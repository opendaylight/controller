package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.store.impl.TestModel;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DataNormalizerTest {

    private static final Short OUTER_LIST_ID = (short)10;

    private static final InstanceIdentifier OUTER_LIST_PATH_LEGACY = InstanceIdentifier.builder(TestModel.TEST_QNAME)
            .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME,OUTER_LIST_ID).build();

    private  static final InstanceIdentifier LEAF_TWO_PATH_LEGACY = InstanceIdentifier.builder(OUTER_LIST_PATH_LEGACY)
            .node(TestModel.TWO_QNAME).build();

    private static final ChoiceNode OUTER_CHOICE_ITEM = Builders.choiceBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.OUTER_CHOICE_QNAME))
            .withChild(ImmutableNodes.leafNode(TestModel.TWO_QNAME, "two"))
            .withChild(ImmutableNodes.leafNode(TestModel.THREE_QNAME, "three"))
            .build();

    private static final MapEntryNode OUTER_LIST_WITHOUT_CHOICE = Builders.mapEntryBuilder()
            .withNodeIdentifier(new NodeIdentifierWithPredicates(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME,OUTER_LIST_ID))
            .withChild(ImmutableNodes.leafNode(TestModel.ID_QNAME, OUTER_LIST_ID))
            .build();

    private static final MapEntryNode OUTER_LIST_WITH_CHOICE = Builders.mapEntryBuilder()
            .withNodeIdentifier(new NodeIdentifierWithPredicates(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME,OUTER_LIST_ID))
            .withChild(ImmutableNodes.leafNode(TestModel.ID_QNAME, OUTER_LIST_ID))
            .withChild(OUTER_CHOICE_ITEM)
            .build();

    @Test
    public void test() {
        SchemaContext testCtx = TestModel.createTestContext();
        DataNormalizer normalizer = new DataNormalizer(testCtx);

        InstanceIdentifier normalizedPath = normalizer.toNormalized(LEAF_TWO_PATH_LEGACY);

        Node<?> outerListLegacy = normalizer.toLegacy(OUTER_LIST_WITH_CHOICE);
        assertNotNull(outerListLegacy);




    }

}
